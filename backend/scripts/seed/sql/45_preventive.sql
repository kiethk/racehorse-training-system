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
    -- Showcase Care Scheduling V2: Làm đẹp dữ liệu 4 ca Routine hôm nay cho Dr. James Carter (VT01)
    -- (Enrich the Routine slots of VT01 on Today's bucket for showcase)
    -- ---------------------------------------------------------------------
    DECLARE
        v_slots care_schedule[];
        v_slot_times TIME[] := ARRAY[TIME '13:30', TIME '14:00', TIME '14:30', TIME '15:00'];
        v_cs RECORD;
        v_idx INT := 0;
    BEGIN
        -- Nếu VT01 chưa đủ 4 ca hôm nay, lấy thêm ca từ ngày mai chuyển về slot còn trống hôm nay
        FOR v_t IN 1..4 LOOP
            IF NOT EXISTS (SELECT 1 FROM care_schedule WHERE veterinarian_id = pg_temp.uid('VT01') AND scheduled_at = pg_temp.at(0, v_slot_times[v_t])) THEN
                SELECT id INTO v_cs FROM care_schedule
                 WHERE veterinarian_id = pg_temp.uid('VT01') AND care_type = 'ROUTINE' AND status = 'SCHEDULED'
                   AND scheduled_date > pg_temp.today()
                 ORDER BY scheduled_at ASC LIMIT 1;
                IF v_cs.id IS NOT NULL THEN
                    UPDATE care_schedule
                       SET scheduled_at = pg_temp.at(0, v_slot_times[v_t]),
                           scheduled_date = pg_temp.today()
                     WHERE id = v_cs.id;
                END IF;
            END IF;
        END LOOP;

        -- Slot 1 (13:30): Ca bình thường chờ khám (Normal pending)
        UPDATE care_schedule
           SET rollover_count = 0, reschedule_reason = NULL, reschedule_note = NULL
         WHERE veterinarian_id = pg_temp.uid('VT01') AND scheduled_at = pg_temp.at(0, TIME '13:30');

        -- Slot 2 (14:00): Ca dời lịch từ hôm qua do hết giờ (Rolled 1x - TIME_EXCEEDED)
        UPDATE care_schedule
           SET rollover_count = 1,
               reschedule_reason = 'TIME_EXCEEDED',
               reschedule_note = 'Daily routine examination window ended at 15:30, rescheduled to next day'
         WHERE veterinarian_id = pg_temp.uid('VT01') AND scheduled_at = pg_temp.at(0, TIME '14:00');

        -- Slot 3 (14:30): Ca dời lịch do ưu tiên ca cấp cứu (Emergency Priority - URGENT_PREEMPTION)
        UPDATE care_schedule
           SET rollover_count = 1,
               reschedule_reason = 'URGENT_PREEMPTION',
               reschedule_note = 'Routine examination deferred to prioritize acute emergency response'
         WHERE veterinarian_id = pg_temp.uid('VT01') AND scheduled_at = pg_temp.at(0, TIME '14:30');

        -- Slot 4 (15:00): Ca san sẻ từ Bác sĩ khác (Transferred - PEER_OFFLOAD)
        UPDATE care_schedule
           SET rollover_count = 0,
               reschedule_reason = 'PEER_OFFLOAD',
               reschedule_note = 'Transferred from Dr. Emily Wilson (VT02) due to emergency case workload'
         WHERE veterinarian_id = pg_temp.uid('VT01') AND scheduled_at = pg_temp.at(0, TIME '15:00');

        -- Làm đẹp thêm cho ngày mai (Saturday 2026-11-07): xếp 2 ca Routine để thanh quota hiện R: 2/4 · A: 1/2
        UPDATE care_schedule
           SET scheduled_at = pg_temp.at(1, TIME '13:30'),
               scheduled_date = pg_temp.today() + 1
         WHERE id = (SELECT id FROM care_schedule WHERE veterinarian_id = pg_temp.uid('VT01') AND care_type = 'ROUTINE' AND scheduled_date > pg_temp.today() + 1 ORDER BY scheduled_at ASC LIMIT 1);

        UPDATE care_schedule
           SET scheduled_at = pg_temp.at(1, TIME '14:00'),
               scheduled_date = pg_temp.today() + 1
         WHERE id = (SELECT id FROM care_schedule WHERE veterinarian_id = pg_temp.uid('VT01') AND care_type = 'ROUTINE' AND scheduled_date > pg_temp.today() + 1 ORDER BY scheduled_at ASC LIMIT 1);
    END;
END $$;
