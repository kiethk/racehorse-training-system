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
END $$;
