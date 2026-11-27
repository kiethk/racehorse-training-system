-- =====================================================================
-- 46_showcase_vt02_vt03.sql — Bổ sung dữ liệu đa dạng cho Dr. Emily Wilson (VT02)
-- và Dr. Robert Hayes (VT03) hôm nay (today) và ngày mai (tomorrow).
-- Đảm bảo cả 3 bác sĩ đều có ca khám phong phú, đúng quota (ROUTINE <= 3, PREVENTIVE <= 3, INITIAL <= 2).
-- =====================================================================

DO $$
DECLARE
    v_id BIGINT;
    v_ids BIGINT[];
BEGIN
    -- 1. Cho Dr. Robert Hayes (VT03)
    -- Lấy 2 ca Routine của VT03 đang SCHEDULED dời về hôm nay
    SELECT ARRAY_AGG(id) INTO v_ids
      FROM (SELECT id FROM care_schedule
             WHERE veterinarian_id = pg_temp.uid('VT03') AND care_type = 'ROUTINE'
               AND purpose != 'PREVENTIVE'
               AND status = 'SCHEDULED'
             ORDER BY scheduled_date ASC NULLS LAST, id ASC
             LIMIT 2) s;

    IF v_ids[1] IS NOT NULL THEN
        UPDATE care_schedule
           SET scheduled_date = pg_temp.today(),
               scheduled_at = NULL,
               status = 'SCHEDULED',
               rollover_count = 1,
               reschedule_reason = 'TIME_EXCEEDED',
               reschedule_note = 'Routine examination window closed at 15:30, rescheduled to next day'
         WHERE id = v_ids[1];
    END IF;

    IF v_ids[2] IS NOT NULL THEN
        UPDATE care_schedule
           SET scheduled_date = pg_temp.today(),
               scheduled_at = NULL,
               status = 'SCHEDULED',
               rollover_count = 0,
               reschedule_reason = NULL,
               reschedule_note = NULL
         WHERE id = v_ids[2];
    END IF;

    -- VT03 thêm 1 ca Preventive hôm nay: lấy 1 ca PREVENTIVE đang SCHEDULED của VT03 dời về hôm nay
    SELECT id INTO v_id
      FROM care_schedule
     WHERE veterinarian_id = pg_temp.uid('VT03')
       AND purpose = 'PREVENTIVE'
       AND status = 'SCHEDULED'
     ORDER BY scheduled_date ASC NULLS LAST, id ASC
     LIMIT 1;

    IF v_id IS NOT NULL THEN
        UPDATE care_schedule
           SET scheduled_date = pg_temp.today(),
               scheduled_at = NULL,
               status = 'SCHEDULED',
               rollover_count = 0,
               reschedule_reason = NULL,
               reschedule_note = NULL
         WHERE id = v_id;
    END IF;

    -- Ca Routine ngày mai của VT03 (today + 1)
    SELECT id INTO v_id
      FROM care_schedule
     WHERE veterinarian_id = pg_temp.uid('VT03') AND care_type = 'ROUTINE'
       AND purpose != 'PREVENTIVE'
       AND status = 'SCHEDULED'
       AND scheduled_date != pg_temp.today()
     ORDER BY scheduled_date ASC NULLS LAST, id ASC
     LIMIT 1;

    IF v_id IS NOT NULL THEN
        UPDATE care_schedule
           SET scheduled_date = pg_temp.today() + 1,
               scheduled_at = NULL,
               status = 'SCHEDULED',
               rollover_count = 0,
               reschedule_reason = NULL,
               reschedule_note = NULL
         WHERE id = v_id;
    END IF;

    -- 2. Cho Dr. Emily Wilson (VT02)
    -- Kéo 1 ca Routine hôm nay (rollover 1x)
    SELECT id INTO v_id
      FROM care_schedule
     WHERE veterinarian_id = pg_temp.uid('VT02') AND care_type = 'ROUTINE'
       AND purpose != 'PREVENTIVE'
       AND status = 'SCHEDULED'
     ORDER BY scheduled_date ASC NULLS LAST, id ASC
     LIMIT 1;

    IF v_id IS NOT NULL THEN
        UPDATE care_schedule
           SET scheduled_date = pg_temp.today(),
               scheduled_at = NULL,
               status = 'SCHEDULED',
               rollover_count = 1,
               reschedule_reason = 'TIME_EXCEEDED',
               reschedule_note = 'Routine examination window closed at 15:30, rescheduled to next day'
         WHERE id = v_id;
    END IF;

    -- Kéo 1 ca Routine của VT02 về ngày mai
    SELECT id INTO v_id
      FROM care_schedule
     WHERE veterinarian_id = pg_temp.uid('VT02') AND care_type = 'ROUTINE'
       AND purpose != 'PREVENTIVE'
       AND status = 'SCHEDULED'
       AND scheduled_date != pg_temp.today()
     ORDER BY scheduled_date ASC NULLS LAST, id ASC
     LIMIT 1;

    IF v_id IS NOT NULL THEN
        UPDATE care_schedule
           SET scheduled_date = pg_temp.today() + 1,
               scheduled_at = NULL,
               status = 'SCHEDULED',
               rollover_count = 0,
               reschedule_reason = NULL,
               reschedule_note = NULL
         WHERE id = v_id;
    END IF;

    -- 3. Cho Dr. James Carter (VT01)
    -- Thêm 1 ca Routine ngày mai (today + 1)
    SELECT id INTO v_id
      FROM care_schedule
     WHERE veterinarian_id = pg_temp.uid('VT01') AND care_type = 'ROUTINE'
       AND purpose != 'PREVENTIVE'
       AND status = 'SCHEDULED'
       AND scheduled_date != pg_temp.today()
     ORDER BY scheduled_date ASC NULLS LAST, id ASC
     LIMIT 1;

    IF v_id IS NOT NULL THEN
        UPDATE care_schedule
           SET scheduled_date = pg_temp.today() + 1,
               scheduled_at = NULL,
               status = 'SCHEDULED',
               rollover_count = 0,
               reschedule_reason = NULL,
               reschedule_note = NULL
         WHERE id = v_id;
    END IF;

END $$;
