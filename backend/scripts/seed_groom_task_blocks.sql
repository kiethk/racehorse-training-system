-- Local/manual seed for testing the grouped routine tasks on the Groom's Daily Tasks screen.
-- Run after all Flyway migrations, against a test database only. Safe to run again: it never
-- duplicates a stall assignment or a task, so running it on another day adds that day's tasks.
-- Login: groom@rtms.com (admin123).
--
-- What it sets up:
--   1. groom@rtms.com looks after up to three stalls that hold a horse (BR-06: at most 3 stalls).
--      Stalls already assigned to another groom are left alone.
--   2. Today's five routine duties for each of those horses: 05:00 Feeding, 05:30 Mucking out,
--      10:00 Washing & grooming, 11:30 Feeding, 16:30 Feeding. Today is the club's date (Asia/Ho_Chi_Minh).
--
-- Test flow:
--   1. Log in as groom@rtms.com -> Daily Tasks. Each duty shows once, as a block listing every horse.
--   2. "Complete all" on a block -> add a note and a few photos -> every horse in the block is done,
--      and the note and photos show once under the block.
--   3. Run the verification query at the end to see the blocks and which completion closed them.
--
-- Run it with the database container from docker-compose.yml:
--   docker exec -i rtms-postgres psql -U rtms_user -d rtms_db < backend/scripts/seed_groom_task_blocks.sql

BEGIN;

-- SQL-only preflight (avoids a procedural DO block in SQL clients that split it).
-- If the account is missing or fewer than two horses are stabled, this INSERT fails its CHECK
-- constraint and safely aborts the transaction: a block of one horse tests nothing.
CREATE TEMP TABLE groom_block_seed_preflight (
    requirements_met BOOLEAN NOT NULL CHECK (requirements_met)
) ON COMMIT DROP;

INSERT INTO groom_block_seed_preflight (requirements_met)
SELECT
    EXISTS (
        SELECT 1 FROM users u JOIN roles r ON r.id = u.role_id
        WHERE u.email = 'groom@rtms.com' AND r.name = 'GROOM' AND u.is_active
    )
    AND (SELECT count(*) FROM horses WHERE current_stall_id IS NOT NULL) >= 2;

-- 1. Up to three stalls holding a horse for the test groom: the stalls already theirs first,
--    then regular areas before quarantine, then by stall code. Capped so the groom never
--    ends up with more than three stalls, counting any empty stall they already have.
WITH test_groom AS (
    SELECT u.id FROM users u JOIN roles r ON r.id = u.role_id
    WHERE u.email = 'groom@rtms.com' AND r.name = 'GROOM'
),
picked AS (
    SELECT s.id
    FROM stable_stalls s
    JOIN horses h ON h.current_stall_id = s.id
    JOIN areas a ON a.id = s.area_id
    CROSS JOIN test_groom g
    WHERE s.groom_id IS NULL OR s.groom_id = g.id
    ORDER BY (s.groom_id = g.id) DESC NULLS LAST, (a.type = 'REGULAR') DESC, s.stall_code
    LIMIT GREATEST(0, 3 - (
        SELECT count(*) FROM stable_stalls own
        WHERE own.groom_id = (SELECT id FROM test_groom)
          AND NOT EXISTS (SELECT 1 FROM horses h2 WHERE h2.current_stall_id = own.id)
    ))
)
UPDATE stable_stalls
SET groom_id = (SELECT id FROM test_groom),
    updated_at = now()
WHERE id IN (SELECT id FROM picked)
  AND groom_id IS DISTINCT FROM (SELECT id FROM test_groom);

-- 2. Today's routine duties for every horse in those stalls, skipping any that already exist.
INSERT INTO groom_daily_tasks (groom_id, horse_id, task_type, scheduled_time, is_completed)
SELECT s.groom_id,
       h.id,
       slot.task_type,
       (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date + slot.at,
       false
FROM stable_stalls s
JOIN horses h ON h.current_stall_id = s.id
JOIN users u ON u.id = s.groom_id AND u.email = 'groom@rtms.com'
CROSS JOIN (VALUES
    ('FEEDING',     TIME '05:00'),
    ('MUCKING_OUT', TIME '05:30'),
    ('GROOMING',    TIME '10:00'),
    ('FEEDING',     TIME '11:30'),
    ('FEEDING',     TIME '16:30')
) AS slot (task_type, at)
WHERE NOT EXISTS (
    SELECT 1 FROM groom_daily_tasks t
    WHERE t.groom_id = s.groom_id
      AND t.horse_id = h.id
      AND t.task_type = slot.task_type
      AND t.scheduled_time = (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date + slot.at
);

COMMIT;

-- Verification: today's blocks for the test groom, one row per duty and time.
SELECT to_char(t.scheduled_time, 'HH24:MI')                         AS at,
       t.task_type,
       count(*)                                                     AS horses,
       count(*) FILTER (WHERE t.is_completed)                       AS done,
       string_agg(h.name, ', ' ORDER BY h.name)                     AS horse_names,
       string_agg(DISTINCT t.completion_id::text, ', ')             AS completions
FROM groom_daily_tasks t
JOIN horses h ON h.id = t.horse_id
JOIN users u ON u.id = t.groom_id AND u.email = 'groom@rtms.com'
WHERE t.scheduled_time::date = (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
GROUP BY t.scheduled_time, t.task_type
ORDER BY t.scheduled_time, t.task_type;
