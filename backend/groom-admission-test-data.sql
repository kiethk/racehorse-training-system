-- Groom Admission test data for PostgreSQL / DBeaver
--
-- Run this script after the database migrations have completed.
-- Test users expected by this script:
--   owner@rtms.com / admin123
--   groom@rtms.com / admin123
--
-- Intended execution order through the API/UI:
--   1. Reject TEST GROOM REJECT.
--   2. Approve TEST GROOM CAPACITY while one Q stall is available.
--   3. Approve TEST GROOM WAITING. It should become WAITING_FOR_STALL.
--   4. Release one test blocker stall, then retry allocation for TEST GROOM WAITING.
--
-- This is seed data, not a Flyway migration. Run it only on a test database.

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'owner@rtms.com') THEN
        RAISE EXCEPTION 'Missing test owner owner@rtms.com';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'groom@rtms.com') THEN
        RAISE EXCEPTION 'Missing test groom groom@rtms.com';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM stable_stalls ss
        JOIN areas a ON a.id = ss.area_id
        WHERE a.type = 'QUARANTINE'
    ) THEN
        RAISE EXCEPTION 'No quarantine stalls found. Run the stable-stall migrations first.';
    END IF;
END $$;

-- Keep all generated admission ids available to later statements in this run.
CREATE TEMP TABLE groom_test_admissions (
    test_key VARCHAR(40) PRIMARY KEY,
    admission_id BIGINT NOT NULL
) ON COMMIT DROP;

-- Create three fresh admissions. The fixed timestamps make this script easy to
-- inspect in DBeaver and prevent accidental mixing with normal test records.
WITH seed(test_key, submitted_at) AS (
    VALUES
        ('REJECT',   TIMESTAMP '2026-09-28 08:00:00'),
        ('CAPACITY', TIMESTAMP '2026-09-28 08:05:00'),
        ('WAITING',  TIMESTAMP '2026-09-28 08:10:00')
)
INSERT INTO admission_applications (
    owner_id,
    status,
    submitted_at,
    created_at,
    updated_at
)
SELECT
    owner.id,
    'GROOM_REVIEW',
    seed.submitted_at,
    seed.submitted_at,
    seed.submitted_at
FROM users owner
CROSS JOIN seed
WHERE owner.email = 'owner@rtms.com';

WITH seed(test_key, submitted_at) AS (
    VALUES
        ('REJECT',   TIMESTAMP '2026-09-28 08:00:00'),
        ('CAPACITY', TIMESTAMP '2026-09-28 08:05:00'),
        ('WAITING',  TIMESTAMP '2026-09-28 08:10:00')
)
INSERT INTO groom_test_admissions (test_key, admission_id)
SELECT seed.test_key, aa.id
FROM seed
JOIN admission_applications aa ON aa.submitted_at = seed.submitted_at
JOIN users owner ON owner.id = aa.owner_id AND owner.email = 'owner@rtms.com';

INSERT INTO candidate_horse_profiles (
    admission_id,
    name,
    breed,
    date_of_birth,
    registration_number,
    registry_name,
    sire_name,
    dam_name,
    pedigree_notes,
    created_at,
    updated_at
)
SELECT
    gta.admission_id,
    CASE gta.test_key
        WHEN 'REJECT' THEN 'TEST GROOM REJECT'
        WHEN 'CAPACITY' THEN 'TEST GROOM CAPACITY'
        WHEN 'WAITING' THEN 'TEST GROOM WAITING'
    END,
    'Thoroughbred',
    DATE '2022-04-15',
    CASE gta.test_key
        WHEN 'REJECT' THEN 'TEST-UELN-REJECT'
        WHEN 'CAPACITY' THEN 'TEST-UELN-CAPACITY'
        WHEN 'WAITING' THEN 'TEST-UELN-WAITING'
    END,
    'TEST REGISTRY',
    'TEST SIRE',
    'TEST DAM',
    'Groom Admission test candidate.',
    NOW(),
    NOW()
FROM groom_test_admissions gta;

-- Add realistic document rows for the Groom detail screen.
INSERT INTO admission_documents (
    admission_id,
    document_type,
    file_url,
    original_file_name,
    record_date,
    note,
    uploaded_at
)
SELECT gta.admission_id, 'HORSE_PHOTO', '/test-data/groom-admission/horse-photo.jpg',
       'test-horse-photo.jpg', DATE '2026-09-27', 'Test horse photo', NOW()
FROM groom_test_admissions gta
UNION ALL
SELECT gta.admission_id, 'REGISTRATION_DOCUMENT', '/test-data/groom-admission/registration.pdf',
       'test-registration.pdf', DATE '2026-09-27', 'Test registration document', NOW()
FROM groom_test_admissions gta;

-- Reserve every currently available Q stall except one. The first Groom
-- approval can therefore succeed; after it consumes the last free Q stall,
-- the next approval must become WAITING_FOR_STALL.
WITH available AS (
    SELECT
        ss.id,
        ROW_NUMBER() OVER (ORDER BY ss.id) AS stall_order,
        COUNT(*) OVER () AS available_count
    FROM stable_stalls ss
    JOIN areas a ON a.id = ss.area_id
    WHERE a.type = 'QUARANTINE'
      AND ss.status = 'AVAILABLE'
), blocker_slots AS (
    SELECT id
    FROM available
    WHERE stall_order < available_count
), inserted_blockers AS (
    INSERT INTO horses (
        name,
        breed,
        date_of_birth,
        current_status,
        stable_location,
        current_stall_id,
        owner_id,
        registry_name,
        registration_number,
        created_at,
        updated_at
    )
    SELECT
        'TEST GROOM Q BLOCKER ' || ROW_NUMBER() OVER (ORDER BY blocker_slots.id),
        'TEST',
        DATE '2020-01-01',
        'QUARANTINED',
        NULL,
        blocker_slots.id,
        owner.id,
        'TEST BLOCKER',
        NULL,
        NOW(),
        NOW()
    FROM blocker_slots
    CROSS JOIN (SELECT id FROM users WHERE email = 'owner@rtms.com') owner
    RETURNING current_stall_id
)
UPDATE stable_stalls ss
SET status = 'OCCUPIED', updated_at = NOW()
FROM inserted_blockers ib
WHERE ss.id = ib.current_stall_id;

DO $$
DECLARE
    available_quarantine BIGINT;
BEGIN
    SELECT COUNT(*)
    INTO available_quarantine
    FROM stable_stalls ss
    JOIN areas a ON a.id = ss.area_id
    WHERE a.type = 'QUARANTINE'
      AND ss.status = 'AVAILABLE';

    IF available_quarantine <> 1 THEN
        RAISE EXCEPTION 'Expected exactly 1 available quarantine stall, found %', available_quarantine;
    END IF;
END $$;

COMMIT;

-- Verification query for DBeaver.
SELECT
    aa.id AS admission_id,
    chp.name AS candidate_name,
    aa.status,
    aa.groom_decision,
    aa.horse_id,
    aa.quarantine_stall_id,
    aa.submitted_at
FROM admission_applications aa
JOIN candidate_horse_profiles chp ON chp.admission_id = aa.id
WHERE chp.name IN ('TEST GROOM REJECT', 'TEST GROOM CAPACITY', 'TEST GROOM WAITING')
ORDER BY aa.submitted_at;

SELECT
    ss.stall_code,
    ss.status,
    h.name AS occupying_horse
FROM stable_stalls ss
JOIN areas a ON a.id = ss.area_id
LEFT JOIN horses h ON h.current_stall_id = ss.id
WHERE a.type = 'QUARANTINE'
ORDER BY ss.stall_code;

-- After TEST GROOM WAITING reaches WAITING_FOR_STALL, run this block once
-- to simulate one test quarantine stall becoming available. Then call the
-- retry-allocation endpoint from the Groom UI.
--
-- BEGIN;
-- UPDATE stable_stalls ss
-- SET status = 'AVAILABLE', updated_at = NOW()
-- WHERE ss.id = (
--     SELECT h.current_stall_id
--     FROM horses h
--     WHERE h.name LIKE 'TEST GROOM Q BLOCKER %'
--       AND h.current_stall_id IS NOT NULL
--     ORDER BY h.id
--     LIMIT 1
-- );
-- UPDATE horses h
-- SET current_stall_id = NULL,
--     current_status = 'REJECTED',
--     updated_at = NOW()
-- WHERE h.name = (
--     SELECT h2.name
--     FROM horses h2
--     WHERE h2.name LIKE 'TEST GROOM Q BLOCKER %'
--       AND h2.current_stall_id IS NOT NULL
--     ORDER BY h2.id
--     LIMIT 1
-- );
-- COMMIT;
