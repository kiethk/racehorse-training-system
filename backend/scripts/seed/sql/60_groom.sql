-- =====================================================================
-- 60_groom.sql — việc hằng ngày của Groom (SOP) trong 14 ngày gần nhất, việc giao thêm và bằng chứng hoàn thành.
-- (Groom daily routine tasks of the last 14 days, extra tasks and completion evidence.)
--
-- Luật đang giữ (GroomDailyTaskService / SopSlot):
--   - Mỗi ngựa đang ở trong chuồng có Groom: 5 việc mỗi ngày — 05:00 cho ăn, 05:30 dọn chuồng, 10:00 tắm chải,
--     11:30 cho ăn, 16:30 cho ăn — giao cho Groom của chuồng ngựa đang ở lúc đó (chuồng Q khi cách ly, chuồng thường
--     sau khi được duyệt).
--   - Hoàn thành theo khối: cùng một việc cùng giờ của một Groom chung một groom_task_completions (ghi chú + tối đa 5 ảnh).
--   - Việc chưa tới giờ thì chưa hoàn thành; vài việc trong quá khứ bị bỏ lỡ để màn hình có trường hợp "chưa làm".
-- (Rules kept: 5 routine tasks a day for each stabled horse, given to the groom of the stall the horse is in at that
-- time; completed in blocks sharing one completion row; nothing in the future is completed; a few past tasks missed.)
-- =====================================================================

-- Ngựa ở chuồng nào, từ lúc nào tới lúc nào. (Which stall each horse was in, and when.)
CREATE TEMP TABLE seed_stay (horse_id BIGINT, stall_id BIGINT, from_ts TIMESTAMP, to_ts TIMESTAMP);
INSERT INTO seed_stay
SELECT a.horse_id, a.quarantine_stall_id, a.arrived_at, a.manager_reviewed_at
  FROM admission_applications a WHERE a.arrived_at IS NOT NULL
UNION ALL
SELECT a.horse_id, h.current_stall_id, a.manager_reviewed_at, NULL
  FROM admission_applications a JOIN horses h ON h.id = a.horse_id WHERE a.status = 'APPROVED';

DO $$
DECLARE
    v_day DATE;
    g RECORD;
    v_completion BIGINT;
    v_done TIMESTAMP;
    v_groom1 BIGINT := pg_temp.uid('GR01');
BEGIN
    -- 1. Việc SOP. (Routine tasks.)
    FOR v_day IN SELECT generate_series(pg_temp.today() - 13, pg_temp.today(), INTERVAL '1 day')::DATE LOOP
        INSERT INTO groom_daily_tasks (groom_id, horse_id, task_type, scheduled_time, is_completed, notes)
        SELECT ss.groom_id, st.horse_id, slot.task_type, v_day + slot.t, FALSE, NULL
          FROM (VALUES (TIME '05:00', 'FEEDING'), (TIME '05:30', 'MUCKING_OUT'), (TIME '10:00', 'GROOMING'),
                       (TIME '11:30', 'FEEDING'), (TIME '16:30', 'FEEDING')) slot(t, task_type)
          JOIN seed_stay st ON st.from_ts <= v_day + slot.t AND (st.to_ts IS NULL OR st.to_ts > v_day + slot.t)
          JOIN stable_stalls ss ON ss.id = st.stall_id
         WHERE ss.groom_id IS NOT NULL
         ORDER BY slot.t, ss.groom_id, st.horse_id;
    END LOOP;

    -- 2. Việc giao thêm do Trainer / Thú y nhờ. (Extra tasks asked by a trainer or a vet.)
    INSERT INTO groom_daily_tasks (groom_id, horse_id, task_type, scheduled_time, is_completed, notes)
    SELECT ss.groom_id, h.id, x.task_type, x.at, FALSE, x.notes
      FROM (VALUES
              ('H002', 'SPECIAL_CARE', pg_temp.at(0, TIME '15:30'), 'Cold hose the right fore for 20 minutes, then a stable bandage.'),
              ('H002', 'VET_ASSIST', pg_temp.at(1, TIME '13:30'), 'Hold her for the follow-up ultrasound.'),
              ('H014', 'SPECIAL_CARE', pg_temp.at(-9, TIME '15:00'), 'Cold hose the right fore tendon and re-bandage.'),
              ('H014', 'SPECIAL_CARE', pg_temp.at(-8, TIME '15:00'), 'Cold hose the right fore tendon and re-bandage.'),
              ('H031', 'HEALTH_CHECK', pg_temp.at(0, TIME '08:00'), 'Take her temperature and write it on the stall board.'),
              ('H031', 'HEALTH_CHECK', pg_temp.at(0, TIME '18:00'), 'Evening temperature.'),
              ('H038', 'HOOF_CARE', pg_temp.at(-4, TIME '09:30'), 'Change the poultice on the right fore.'),
              ('H010', 'HOOF_CARE', pg_temp.at(-3, TIME '09:00'), 'Pick out and check the shoes before the farrier visit.'),
              ('H003', 'SPECIAL_CARE', pg_temp.at(-3, TIME '15:00'), 'Antifungal wash on the neck patches; use her own brushes only.'),
              ('H003', 'SPECIAL_CARE', pg_temp.at(0, TIME '15:00'), 'Antifungal wash on the neck patches; use her own brushes only.')
           ) x(k, task_type, at, notes)
      JOIN horses h ON h.id = pg_temp.hid(x.k) JOIN stable_stalls ss ON ss.id = h.current_stall_id;

    -- 3. Hoàn thành theo khối (Groom, giờ). (Complete in blocks per groom and time.)
    FOR g IN
        SELECT t.groom_id, t.scheduled_time, array_agg(t.id) AS ids
          FROM groom_daily_tasks t
         WHERE t.scheduled_time + INTERVAL '40 minutes' < pg_temp.clock()
           -- groom1 hôm nay còn việc từ 10:00 để thử tick trên màn hình. (groom1 keeps today's tasks from 10:00 open.)
           AND NOT (t.groom_id = v_groom1 AND t.scheduled_time >= pg_temp.at(0, TIME '10:00'))
           -- Vài khối bị bỏ lỡ. (A few missed blocks.)
           AND pg_temp.rndi('miss' || t.groom_id || t.scheduled_time, 1, 40) <> 1
         GROUP BY t.groom_id, t.scheduled_time
         ORDER BY t.scheduled_time, t.groom_id
    LOOP
        v_done := g.scheduled_time + make_interval(mins => pg_temp.rndi('done' || g.groom_id || g.scheduled_time, 8, 35));
        INSERT INTO groom_task_completions (groom_id, note, completed_at)
        VALUES (g.groom_id,
                CASE pg_temp.rndi('note' || g.groom_id || g.scheduled_time, 1, 12)
                    WHEN 1 THEN 'All ate up well.'
                    WHEN 2 THEN 'Water buckets scrubbed and refilled.'
                    WHEN 3 THEN 'Left fore shoe a bit loose on one horse; told the farrier.'
                    WHEN 4 THEN 'Fresh shavings in every box.'
                    WHEN 5 THEN 'One horse left some hay; keep an eye on her appetite.'
                END,
                v_done)
        RETURNING id INTO v_completion;
        UPDATE groom_daily_tasks SET is_completed = TRUE, completed_at = v_done, completion_id = v_completion
         WHERE id = ANY (g.ids);
        -- Ảnh bằng chứng cho một số khối: dùng lại ảnh ngựa đã có trong kho file. (Photo evidence for some blocks,
        -- reusing a horse photo that is already in the file store.)
        IF pg_temp.rndi('photo' || g.groom_id || g.scheduled_time, 1, 15) = 1 THEN
            INSERT INTO groom_task_completion_photos (completion_id, "position", image_key)
            SELECT v_completion, 0, d.file_url
              FROM groom_daily_tasks t JOIN admission_documents d ON d.document_type = 'HORSE_PHOTO'
              JOIN admission_applications a ON a.id = d.admission_id AND a.horse_id = t.horse_id
             WHERE t.id = g.ids[1]
             ORDER BY a.submitted_at DESC LIMIT 1;
        END IF;
    END LOOP;
END $$;
