-- =====================================================================
-- 25_assign.sql — chọn Groom duyệt đơn, chuồng cách ly, chuồng thường và Head Trainer đúng như app sẽ chọn.
-- (Picks the reviewing groom, the quarantine stall, the regular stall and the head trainer exactly as the app would.)
--
-- Các cột groom_k, q_stall, trainer_k, regular_stall trong seed_admission (10_data.sql) chỉ là chỗ giữ chỗ: file này
-- chạy lại từng sự kiện theo đúng thứ tự thời gian và ghi đè chúng bằng kết quả của các luật trong code:
--   * nộp đơn          -> GroomWorkloadService.selectGroom (ít việc nhất: ngựa đang ở chuồng Q + đơn đang chờ duyệt;
--                         hòa thì Groom có lần xác nhận ngựa tới cũ nhất / chưa có, rồi id nhỏ)
--   * Groom duyệt      -> findAvailableQuarantineStallOfGroomForUpdate (chuồng Q trống số nhỏ nhất của chính Groom);
--                         khi Groom hết chuồng trống, các đơn đang chờ của Groom đó được giao lại (GroomAssignmentService, lý do FULL)
--   * Thú y khám xong  -> HeadTrainerWorkloadService.selectLeastLoadedHeadTrainer (tải = ngựa trong khu + đơn đang xử lý,
--                         tối đa 24, hòa thì id nhỏ)
--   * Manager duyệt    -> StableStallRepository.findFairAvailableStallForUpdate (Groom ít ngựa nhất, rồi lần nhận ngựa cũ nhất,
--                         rồi id; trong Groom đó: khu rồi số chuồng nhỏ nhất)
-- (Replays every event in time order and overwrites the placeholder columns with what those rules return.)
-- =====================================================================
CREATE TEMP TABLE sim_stall (code TEXT PRIMARY KEY, area TEXT NOT NULL, atype TEXT NOT NULL, num INT NOT NULL,
                             groom TEXT NOT NULL, busy BOOLEAN NOT NULL DEFAULT FALSE);
CREATE TEMP TABLE sim_groom (k TEXT PRIMARY KEY, is_q BOOLEAN NOT NULL, last_horse TIMESTAMP, last_arrival TIMESTAMP,
                             horses INT NOT NULL DEFAULT 0);
CREATE TEMP TABLE sim_pending (adm TEXT PRIMARY KEY, groom TEXT NOT NULL, since TIMESTAMP NOT NULL);
CREATE TEMP TABLE sim_area_horse (horse_k TEXT PRIMARY KEY, trainer TEXT NOT NULL);
CREATE TEMP TABLE sim_open (adm TEXT PRIMARY KEY, horse_k TEXT NOT NULL, trainer TEXT NOT NULL);
-- Lịch sử phân công Groom cho bước duyệt hồ sơ; 30_admissions.sql ghi bảng này vào admission_groom_assignments.
-- (History of the groom assignments; 30_admissions.sql writes it to admission_groom_assignments.)
CREATE TEMP TABLE seed_groom_assignment (adm TEXT NOT NULL, groom_k TEXT NOT NULL, assigned_at TIMESTAMP NOT NULL,
                                         ended_at TIMESTAMP, end_reason TEXT, by_manager BOOLEAN NOT NULL DEFAULT FALSE);
-- Manager giao tay một đơn đang chờ Groom duyệt (POST /api/admissions/{id}/groom-assignment, GroomAssignmentService MANUAL):
-- dùng để Groom demo (groom1) có đơn chờ duyệt. (The manager assigns a waiting admission by hand, so the demo groom has one to review.)
CREATE TEMP TABLE sim_manual (adm TEXT PRIMARY KEY, groom TEXT NOT NULL, after_submit INTERVAL NOT NULL);
INSERT INTO sim_manual VALUES ('AP049', 'GR01', INTERVAL '4 hours');
CREATE TEMP TABLE sim_event (t TIMESTAMP NOT NULL, rank INT NOT NULL, kind TEXT NOT NULL, adm TEXT NOT NULL);

INSERT INTO sim_stall
SELECT ar.code || lpad(n::TEXT, 2, '0'), ar.code, ar.type, n,
       (SELECT b.groom_k FROM seed_groom_block b WHERE b.area_code = ar.code AND n BETWEEN b.first_stall AND b.first_stall + 2)
  FROM seed_area ar CROSS JOIN generate_series(1, 12) n;

INSERT INTO sim_groom (k, is_q)
SELECT b.groom_k, bool_or(ar.type = 'QUARANTINE')
  FROM seed_groom_block b JOIN seed_area ar ON ar.code = b.area_code GROUP BY b.groom_k;

INSERT INTO sim_event
SELECT pg_temp.ts(a.submitted) + m.after_submit, 2, 'MANUAL', a.k FROM seed_admission a JOIN sim_manual m ON m.adm = a.k
UNION ALL SELECT pg_temp.ts(submitted), 1, 'SUBMIT', k FROM seed_admission
UNION ALL SELECT pg_temp.ts(groom_at), 3, 'REVIEW', k FROM seed_admission WHERE groom_at IS NOT NULL
UNION ALL SELECT pg_temp.ts(arrived), 4, 'ARRIVAL', k FROM seed_admission WHERE arrived IS NOT NULL
-- Thú y khám ngay lượt đầu của ngày ngựa tới (xem find_slot): khoảng 30-40 phút sau giờ tới.
-- (The vet examines in the first slot after the arrival, roughly 30-40 minutes later.)
UNION ALL SELECT pg_temp.ts(arrived) + INTERVAL '40 minutes', 5, 'VET', k FROM seed_admission
           WHERE arrived IS NOT NULL AND status <> 'VET_REVIEW'
UNION ALL SELECT pg_temp.ts(groom_at) + INTERVAL '14 days', 6, 'EXPIRE', k FROM seed_admission WHERE status = 'ARRIVAL_EXPIRED'
UNION ALL SELECT pg_temp.ts(manager_at), 7, 'MANAGER', k FROM seed_admission WHERE manager_at IS NOT NULL;

DO $$
DECLARE
    e RECORD;
    a seed_admission;
    g TEXT;
    s TEXT;
    tr TEXT;
    p RECORD;
BEGIN
    FOR e IN SELECT * FROM sim_event ORDER BY t, rank, adm LOOP
        SELECT * INTO a FROM seed_admission WHERE k = e.adm;

        IF e.kind = 'SUBMIT' THEN
            -- GroomWorkloadService.selectGroom (không bắt buộc còn chuồng trống). (Free stall not required.)
            SELECT sg.k INTO g FROM sim_groom sg WHERE sg.is_q
             ORDER BY sg.horses + (SELECT count(*) FROM sim_pending sp WHERE sp.groom = sg.k),
                      sg.last_arrival NULLS FIRST, pg_temp.uid(sg.k) LIMIT 1;
            INSERT INTO sim_pending VALUES (e.adm, g, e.t);
            INSERT INTO seed_groom_assignment (adm, groom_k, assigned_at) VALUES (e.adm, g, e.t);
            UPDATE seed_admission SET groom_k = g WHERE k = e.adm;

        ELSIF e.kind = 'MANUAL' THEN
            SELECT m.groom INTO g FROM sim_manual m WHERE m.adm = e.adm;
            UPDATE sim_pending SET groom = g WHERE adm = e.adm;
            UPDATE seed_groom_assignment SET ended_at = e.t, end_reason = 'MANUAL' WHERE adm = e.adm AND ended_at IS NULL;
            INSERT INTO seed_groom_assignment VALUES (e.adm, g, e.t, NULL, NULL, TRUE);
            UPDATE seed_admission SET groom_k = g WHERE k = e.adm;

        ELSIF e.kind = 'REVIEW' THEN
            DELETE FROM sim_pending WHERE adm = e.adm;
            UPDATE seed_groom_assignment SET ended_at = e.t, end_reason = 'DECIDED' WHERE adm = e.adm AND ended_at IS NULL;
            IF a.groom_decision = 'APPROVED' THEN
                SELECT ss.code INTO s FROM sim_stall ss
                 WHERE ss.groom = a.groom_k AND ss.atype = 'QUARANTINE' AND NOT ss.busy ORDER BY ss.area, ss.num LIMIT 1;
                IF s IS NULL THEN
                    RAISE EXCEPTION 'Groom % has no free quarantine stall when reviewing %', a.groom_k, e.adm;
                END IF;
                UPDATE sim_stall SET busy = TRUE WHERE code = s;
                UPDATE sim_groom SET last_horse = e.t WHERE k = a.groom_k;
                UPDATE seed_admission SET q_stall = s WHERE k = e.adm;
                -- Groom vừa hết chuồng trống: các đơn đang chờ của Groom đó được giao lại cho người còn chuồng (lý do FULL).
                -- (The groom just ran out of free quarantine stalls: his waiting admissions are reassigned, reason FULL.)
                IF NOT EXISTS (SELECT 1 FROM sim_stall ss WHERE ss.groom = a.groom_k AND ss.atype = 'QUARANTINE' AND NOT ss.busy) THEN
                    FOR p IN SELECT * FROM sim_pending WHERE groom = a.groom_k ORDER BY since, adm LOOP
                        SELECT sg.k INTO g FROM sim_groom sg
                         WHERE sg.is_q AND sg.k <> a.groom_k
                           AND NOT EXISTS (SELECT 1 FROM seed_groom_assignment h WHERE h.adm = p.adm AND h.groom_k = sg.k)
                           AND EXISTS (SELECT 1 FROM sim_stall ss WHERE ss.groom = sg.k AND ss.atype = 'QUARANTINE' AND NOT ss.busy)
                         ORDER BY sg.horses + (SELECT count(*) FROM sim_pending sp WHERE sp.groom = sg.k),
                                  sg.last_arrival NULLS FIRST, pg_temp.uid(sg.k) LIMIT 1;
                        IF g IS NOT NULL THEN
                            UPDATE sim_pending SET groom = g WHERE adm = p.adm;
                            UPDATE seed_groom_assignment SET ended_at = e.t, end_reason = 'FULL' WHERE adm = p.adm AND ended_at IS NULL;
                            INSERT INTO seed_groom_assignment (adm, groom_k, assigned_at) VALUES (p.adm, g, e.t);
                            UPDATE seed_admission SET groom_k = g WHERE k = p.adm;
                        END IF;
                    END LOOP;
                END IF;
            END IF;

        ELSIF e.kind = 'ARRIVAL' THEN
            UPDATE sim_groom SET horses = horses + 1, last_arrival = e.t WHERE k = a.groom_k;

        ELSIF e.kind = 'VET' THEN
            SELECT t.k INTO tr FROM seed_account t WHERE t.role = 'HEAD_TRAINER' AND t.is_active
               AND (SELECT count(DISTINCT x.horse_k) FROM (SELECT horse_k FROM sim_area_horse WHERE trainer = t.k
                                                          UNION SELECT horse_k FROM sim_open WHERE trainer = t.k) x) < 24
             ORDER BY (SELECT count(DISTINCT x.horse_k) FROM (SELECT horse_k FROM sim_area_horse WHERE trainer = t.k
                                                             UNION SELECT horse_k FROM sim_open WHERE trainer = t.k) x),
                      pg_temp.uid(t.k) LIMIT 1;
            INSERT INTO sim_open VALUES (e.adm, a.horse_k, tr);
            UPDATE seed_admission SET trainer_k = tr WHERE k = e.adm;

        ELSIF e.kind = 'EXPIRE' THEN
            UPDATE sim_stall SET busy = FALSE WHERE code = a.q_stall;

        ELSIF e.kind = 'MANAGER' THEN
            DELETE FROM sim_open WHERE adm = e.adm;
            UPDATE sim_stall SET busy = FALSE WHERE code = a.q_stall;
            UPDATE sim_groom SET horses = horses - 1 WHERE k = a.groom_k;
            IF a.manager_decision = 'APPROVED' THEN
                SELECT ss.code INTO s FROM sim_stall ss JOIN sim_groom sg ON sg.k = ss.groom
                 WHERE ss.atype = 'REGULAR' AND NOT ss.busy
                 ORDER BY (SELECT count(*) FROM sim_stall x WHERE x.groom = ss.groom AND x.busy),
                          sg.last_horse NULLS FIRST, pg_temp.uid(ss.groom), ss.area, ss.num LIMIT 1;
                IF s IS NULL THEN RAISE EXCEPTION 'No regular stall left for %', e.adm; END IF;
                UPDATE sim_stall SET busy = TRUE WHERE code = s;
                UPDATE sim_groom SET last_horse = e.t WHERE k = (SELECT groom FROM sim_stall WHERE code = s);
                UPDATE seed_admission SET regular_stall = s WHERE k = e.adm;
                INSERT INTO sim_area_horse
                SELECT a.horse_k, ar.trainer_k FROM sim_stall ss JOIN seed_area ar ON ar.code = ss.area WHERE ss.code = s
                ON CONFLICT (horse_k) DO UPDATE SET trainer = EXCLUDED.trainer;
            END IF;
        END IF;
    END LOOP;
END $$;
