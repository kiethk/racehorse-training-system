-- =====================================================================
-- 65_vitals.sql — chỉ số sinh hiệu Thú y đo khi đi buồng (không gắn ca khám) trong 5 tuần gần nhất, cho mọi ngựa đang ở
-- trong chuồng. (Vital readings taken by the vets on stable rounds, not tied to a visit, for the last 5 weeks, for every
-- stabled horse.)
--
-- Chạy SAU 50_training.sql vì buổi tập mệt (nhịp tim hồi phục cao) để lại số đo bất thường chiều hôm đó.
-- (Runs AFTER 50_training.sql because a tiring session leaves an abnormal reading that afternoon.)
--
--   - Đo thường lệ mỗi tuần một lần, mỗi ngựa một ngày khác nhau trong tuần (Thú y đi buồng rải đều cả tuần).
--     Thỉnh thoảng có lệch nhẹ (sốt nhẹ, nhịp tim cao); thể trạng mỗi ngựa một mức riêng.
--   - Ngựa đang tạm nghỉ (seed_block) được đo mỗi ngày, số đo bất thường cho tới khi khỏi.
--   - Buổi tập có nhịp tim hồi phục >= 118 -> chiều hôm đó đo lại, nhịp tim cao.
-- Ngưỡng bất thường của app: HealthReadingPolicy (nhiệt 37,2-38,5 °C, nhịp tim 28-44, thể trạng 4-7).
-- (Weekly round on a different weekday per horse with occasional mild deviations; daily abnormal readings while a
-- horse is resting; a high reading after a tiring session. Abnormal limits: HealthReadingPolicy.)
-- =====================================================================
INSERT INTO horse_health_metrics (horse_id, recorded_at, heart_rate, temperature, weight, respiratory_rate, hydration_status,
                                  body_condition_score, notes, created_at)
SELECT r.horse_id, r.at,
       CASE r.kind WHEN 'sick' THEN pg_temp.rndi('hs' || r.horse_id || r.at, 45, 56)
                   WHEN 'fatigue' THEN pg_temp.rndi('hf' || r.horse_id || r.at, 46, 50)
                   WHEN 'high_hr' THEN pg_temp.rndi('hh' || r.horse_id || r.at, 45, 48)
                   ELSE pg_temp.rndi('hn' || r.horse_id || r.at, 30, 40) END,
       CASE r.kind WHEN 'sick' THEN round(pg_temp.rnd('ts' || r.horse_id || r.at, 38.6, 39.3), 1)
                   WHEN 'fever' THEN round(pg_temp.rnd('tf' || r.horse_id || r.at, 38.6, 38.9), 1)
                   WHEN 'fatigue' THEN round(pg_temp.rnd('tg' || r.horse_id || r.at, 38.2, 38.5), 1)
                   ELSE round(pg_temp.rnd('tn' || r.horse_id || r.at, 37.4, 38.1), 1) END,
       round(pg_temp.rnd('w' || r.horse_id, 410, 480) + pg_temp.rnd('wd' || r.horse_id || r.at, -4, 4), 1),
       CASE r.kind WHEN 'sick' THEN pg_temp.rndi('rs' || r.horse_id || r.at, 16, 24)
                   ELSE pg_temp.rndi('rn' || r.horse_id || r.at, 10, 16) END,
       CASE WHEN r.kind = 'sick' AND pg_temp.rndi('dh' || r.horse_id || r.at, 1, 3) = 1 THEN 'MILD_DEHYDRATION'
            ELSE 'NORMAL' END,
       pg_temp.rndi('b' || r.horse_id, 9, 13) / 2.0,
       CASE r.kind WHEN 'sick' THEN 'Daily check during the rest period.'
                   WHEN 'fatigue' THEN 'Rechecked after a tiring session: heart rate still up in the afternoon.'
                   WHEN 'fever' THEN 'Slightly warm on the round; recheck tomorrow.'
                   WHEN 'high_hr' THEN 'Heart rate up on the round, settled after 15 minutes.'
                   ELSE 'Stable round.' END,
       r.at
  FROM (
        -- Đo thường lệ hằng tuần, mỗi ngựa một ngày riêng. (Weekly round, a weekday of its own for each horse.)
        SELECT st.horse_id, d + TIME '15:45' + make_interval(mins => (st.horse_id % 4)::INT * 10) AS at,
               CASE pg_temp.rndi('va' || st.horse_id || d, 1, 25) WHEN 1 THEN 'fever' WHEN 2 THEN 'high_hr' ELSE 'normal' END AS kind
          FROM generate_series(pg_temp.today() - 34, pg_temp.today(), INTERVAL '1 day') g(d0)
          CROSS JOIN LATERAL (SELECT g.d0::DATE AS d) x
          JOIN seed_stay st ON st.from_ts <= d + TIME '15:45' AND (st.to_ts IS NULL OR st.to_ts > d + TIME '16:30')
         WHERE (d - DATE '2000-01-03') % 7 = st.horse_id % 7
           AND NOT pg_temp.blocked_at(st.horse_id, d + TIME '16:00')
        UNION ALL
        -- Ngựa đang tạm nghỉ: đo mỗi ngày. (Resting horse: checked daily.)
        SELECT st.horse_id, d + TIME '16:00', 'sick'
          FROM generate_series(pg_temp.today() - 34, pg_temp.today(), INTERVAL '1 day') g(d0)
          CROSS JOIN LATERAL (SELECT g.d0::DATE AS d) x
          JOIN seed_stay st ON st.from_ts <= d + TIME '16:00' AND (st.to_ts IS NULL OR st.to_ts > d + TIME '16:00')
         WHERE pg_temp.blocked_at(st.horse_id, d + TIME '16:00')
        UNION ALL
        -- Sau buổi tập mệt. (After a tiring session.)
        SELECT DISTINCT w.horse_id, l.lot_date + TIME '15:00', 'fatigue'
          FROM training_workouts w JOIN training_lots l ON l.id = w.lot_id
         WHERE w.status = 'COMPLETED' AND w.recovery_heart_rate >= 118 AND l.lot_date >= pg_temp.today() - 34
       ) r
 WHERE r.at <= pg_temp.clock()
 ORDER BY r.at, r.horse_id;
