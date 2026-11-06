-- =====================================================================
-- 45_preventive.sql — lịch tiêm phòng / tẩy giun của mọi ngựa từ ngày được duyệt tới hôm nay, rồi xếp giờ mọi lịch
-- đang chờ trong 14 ngày tới (như job CareScheduleScheduler làm mỗi phút).
-- (Vaccination / deworming of every horse from its approval up to today, then booking every pending schedule of the
-- next 14 days, like the CareScheduleScheduler job does every minute.)
--
-- Chạy SAU 40_vet_cases.sql vì ngựa đang tạm nghỉ sẽ được hoãn tiêm tới khi hết nghỉ.
-- (Runs AFTER 40_vet_cases.sql because a resting horse is vaccinated after its rest period.)
-- =====================================================================
DO $$
DECLARE
    h RECORD;
    c RECORD;
BEGIN
    FOR h IN
        SELECT x.id, x.k, a.code AS area
          FROM seed_horse_id x JOIN horses hs ON hs.id = x.id
          LEFT JOIN stable_stalls ss ON ss.id = hs.current_stall_id LEFT JOIN areas a ON a.id = ss.area_id
         WHERE hs.current_status IN ('ELIGIBLE', 'CANDIDATE')
         ORDER BY x.k
    LOOP
        PERFORM pg_temp.run_preventive(
            h.id,
            pg_temp.uid(CASE WHEN h.area IN ('C', 'D') THEN 'VT02' WHEN h.area = 'E' THEN 'VT03' ELSE 'VT01' END),
            pg_temp.clock(),
            p_defer => h.k IN ('H021', 'H033'),          -- lần đầu hoãn tiêm 7 ngày (first visit deferred 7 days)
            p_hub => h.k IN ('H011', 'H025'),            -- lần hai tiêm từ "To do" (second round from the To do list)
            p_leave_overdue => h.k = 'H017');            -- một ca tiêm đã quá giờ chưa làm (one overdue visit)
    END LOOP;

    -- Lịch đang chờ đã vào cửa sổ 14 ngày thì đã được xếp giờ. (Pending schedules inside the 14-day horizon are booked.)
    FOR c IN SELECT cs.id, cs.horse_id FROM care_schedule cs
              WHERE cs.status = 'REQUESTED' AND cs.care_type = 'ROUTINE'
                AND GREATEST(cs.created_at, cs.requested_at - INTERVAL '14 days') <= pg_temp.clock()
              ORDER BY cs.requested_at
    LOOP
        PERFORM pg_temp.book(c.id, NULL, pg_temp.clock());
    END LOOP;

    -- ---------------------------------------------------------------------
    -- Showcase Care Scheduling V2: Dữ liệu mẫu Routine & Preventive hôm nay cho Dr. James Carter (VT01)
    -- Routine max 3 ca/ngày, Preventive max 3 ca/ngày, Admission max 2 ca/ngày
    -- ---------------------------------------------------------------------
    DECLARE
        v_routine_ids BIGINT[];
        v_prev_ids BIGINT[];
        v_cs RECORD;
    BEGIN
        -- Đảm bảo tất cả ca Routine, Preventive và Initial đều lên lịch theo ngày (scheduled_at = NULL)
        -- và luôn ở trạng thái SCHEDULED (không có IN_PROGRESS)
        UPDATE care_schedule
           SET scheduled_at = NULL,
               status = 'SCHEDULED'
         WHERE (care_type IN ('ROUTINE', 'INITIAL') OR purpose = 'PREVENTIVE')
           AND status IN ('IN_PROGRESS', 'PAUSED');

        UPDATE care_schedule SET scheduled_at = NULL WHERE care_type IN ('ROUTINE', 'INITIAL') OR purpose = 'PREVENTIVE';

        -- Lấy các ca Routine lâm sàng (purpose != 'PREVENTIVE') hôm nay của VT01
        SELECT ARRAY_AGG(id ORDER BY id ASC) INTO v_routine_ids
          FROM (SELECT id FROM care_schedule
                 WHERE veterinarian_id = pg_temp.uid('VT01') AND care_type = 'ROUTINE'
                   AND purpose != 'PREVENTIVE'
                   AND scheduled_date = pg_temp.today()
                 LIMIT 3) s;

        -- Nếu chưa đủ 2 ca Routine lâm sàng, kéo từ ngày mai về
        IF COALESCE(array_length(v_routine_ids, 1), 0) < 2 THEN
            FOR v_cs IN SELECT id FROM care_schedule
                         WHERE veterinarian_id = pg_temp.uid('VT01') AND care_type = 'ROUTINE'
                           AND purpose != 'PREVENTIVE' AND status = 'SCHEDULED'
                           AND scheduled_date > pg_temp.today()
                         ORDER BY id ASC LIMIT (2 - COALESCE(array_length(v_routine_ids, 1), 0)) LOOP
                UPDATE care_schedule SET scheduled_date = pg_temp.today(), scheduled_at = NULL WHERE id = v_cs.id;
            END LOOP;
            SELECT ARRAY_AGG(id ORDER BY id ASC) INTO v_routine_ids
              FROM (SELECT id FROM care_schedule
                     WHERE veterinarian_id = pg_temp.uid('VT01') AND care_type = 'ROUTINE'
                       AND purpose != 'PREVENTIVE'
                       AND scheduled_date = pg_temp.today()
                     LIMIT 3) s;
        END IF;

        -- Routine Slot 1: Ca bình thường chờ khám
        IF v_routine_ids[1] IS NOT NULL THEN
            UPDATE care_schedule
               SET status = 'SCHEDULED', rollover_count = 0, reschedule_reason = NULL, reschedule_note = NULL
             WHERE id = v_routine_ids[1];
        END IF;

        -- Routine Slot 2: Ca dời lịch từ hôm qua do hết giờ (Rolled 1x)
        IF v_routine_ids[2] IS NOT NULL THEN
            UPDATE care_schedule
               SET status = 'SCHEDULED', rollover_count = 1,
                   reschedule_reason = 'TIME_EXCEEDED',
                   reschedule_note = 'Routine examination window closed at 15:30, rescheduled to next day'
             WHERE id = v_routine_ids[2];
        END IF;

        -- Lấy các ca Preventive tiêm phòng (purpose = 'PREVENTIVE') hôm nay của VT01
        SELECT ARRAY_AGG(id ORDER BY id ASC) INTO v_prev_ids
          FROM (SELECT id FROM care_schedule
                 WHERE veterinarian_id = pg_temp.uid('VT01') AND care_type = 'ROUTINE'
                   AND purpose = 'PREVENTIVE'
                   AND scheduled_date = pg_temp.today()
                 LIMIT 3) s;

        -- Nếu chưa đủ 2 ca Preventive, kéo từ ngày mai về
        IF COALESCE(array_length(v_prev_ids, 1), 0) < 2 THEN
            FOR v_cs IN SELECT id FROM care_schedule
                         WHERE veterinarian_id = pg_temp.uid('VT01') AND care_type = 'ROUTINE'
                           AND purpose = 'PREVENTIVE' AND status = 'SCHEDULED'
                           AND scheduled_date > pg_temp.today()
                         ORDER BY id ASC LIMIT (2 - COALESCE(array_length(v_prev_ids, 1), 0)) LOOP
                UPDATE care_schedule SET scheduled_date = pg_temp.today(), scheduled_at = NULL WHERE id = v_cs.id;
            END LOOP;
            SELECT ARRAY_AGG(id ORDER BY id ASC) INTO v_prev_ids
              FROM (SELECT id FROM care_schedule
                     WHERE veterinarian_id = pg_temp.uid('VT01') AND care_type = 'ROUTINE'
                       AND purpose = 'PREVENTIVE'
                       AND scheduled_date = pg_temp.today()
                     LIMIT 3) s;
        END IF;

        -- Preventive Slot 1: Chờ tiêm bình thường
        IF v_prev_ids[1] IS NOT NULL THEN
            UPDATE care_schedule
               SET status = 'SCHEDULED', rollover_count = 0, reschedule_reason = NULL, reschedule_note = NULL
             WHERE id = v_prev_ids[1];
        END IF;

        -- Preventive Slot 2: San sẻ từ bác sĩ khác (Transferred)
        IF v_prev_ids[2] IS NOT NULL THEN
            UPDATE care_schedule
               SET status = 'SCHEDULED', rollover_count = 0,
                   reschedule_reason = 'PEER_OFFLOAD',
                   reschedule_note = 'Transferred from Dr. Emily Wilson (VT02) due to emergency case workload'
             WHERE id = v_prev_ids[2];
        END IF;

        -- Preventive Slot 3: Dời lịch từ hôm qua (Rolled 1x) nếu có
        IF v_prev_ids[3] IS NOT NULL THEN
            UPDATE care_schedule
               SET status = 'SCHEDULED', rollover_count = 1,
                   reschedule_reason = 'TIME_EXCEEDED',
                   reschedule_note = 'Preventive vaccination window closed at 15:30, rescheduled to next day'
             WHERE id = v_prev_ids[3];
        END IF;

        -- Làm đẹp thêm cho ngày mai (Saturday 2026-11-07): xếp 1 ca Routine và 1 ca Preventive
        UPDATE care_schedule
           SET status = 'SCHEDULED', scheduled_at = NULL,
               scheduled_date = pg_temp.today() + 1
         WHERE id = (SELECT id FROM care_schedule WHERE veterinarian_id = pg_temp.uid('VT01') AND care_type = 'ROUTINE' AND purpose != 'PREVENTIVE' AND scheduled_date > pg_temp.today() + 1 ORDER BY id ASC LIMIT 1);

        UPDATE care_schedule
           SET status = 'SCHEDULED', scheduled_at = NULL,
               scheduled_date = pg_temp.today() + 1
         WHERE id = (SELECT id FROM care_schedule WHERE veterinarian_id = pg_temp.uid('VT01') AND care_type = 'ROUTINE' AND purpose = 'PREVENTIVE' AND scheduled_date > pg_temp.today() + 1 ORDER BY id ASC LIMIT 1);
    END;
END $$;
