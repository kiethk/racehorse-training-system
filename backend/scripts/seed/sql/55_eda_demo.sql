-- =====================================================================
-- 55_eda_demo.sql — Kịch bản mẫu chuyên biệt cho tính năng Exploratory Data Analysis (EDA).
-- Tối ưu hóa cho Trainer 1 (trainer1@rtms.com):
-- 1. Fleet View: Tương quan tuyến tính âm rõ nét (r ≈ -0.88)
--    - Outlier 1: Ngựa #4 (ENUMA) -> Quá tải (21.0 km/h, nhịp tim hồi phục 108 bpm) -> Báo động đỏ
--    - Outlier 2: Ngựa #14 (MYSTICA MAHRIACHI) -> Siêu sao (46.5 km/h, nhịp tim 59 bpm) -> Điểm xanh ưu tú
-- 2. Individual View: Ngựa #1 (NAUSICAA BY NIKA)
--    - 47 buổi tập mô phỏng tiến trình thể lực thích nghi tăng dần từ 22 km/h -> 38 km/h
--    - Buổi tập #15: Quá tải đột xuất (15.0 km/h, nhịp tim hồi phục 112 bpm)
-- =====================================================================

BEGIN;

-- Ngựa 4: ENUMA -> Dị biệt Quá tải (Overload Alert)
UPDATE training_workouts
SET average_speed_kmh = 21.0, top_speed_kmh = 24.5, recovery_heart_rate = 108, average_heart_rate = 145, max_heart_rate = 195
WHERE horse_id = 4 AND status = 'RAN';

-- Ngựa 14: MYSTICA MAHRIACHI -> Dị biệt Siêu sao (Elite Cardio)
UPDATE training_workouts
SET average_speed_kmh = 46.5, top_speed_kmh = 50.0, recovery_heart_rate = 59, average_heart_rate = 135, max_heart_rate = 215
WHERE horse_id = 14 AND status = 'RAN';

-- Ngựa 3: SI RENESME -> Nhóm thể lực cao
UPDATE training_workouts
SET average_speed_kmh = 38.5, top_speed_kmh = 42.0, recovery_heart_rate = 66, max_heart_rate = 220
WHERE horse_id = 3 AND status = 'RAN';

-- Ngựa 11: CHILIVANESU -> Nhóm thể lực cao
UPDATE training_workouts
SET average_speed_kmh = 37.0, top_speed_kmh = 40.5, recovery_heart_rate = 67, max_heart_rate = 185
WHERE horse_id = 11 AND status = 'RAN';

-- Ngựa 6: IMPERIUM -> Nhóm trung bình khá
UPDATE training_workouts
SET average_speed_kmh = 34.0, top_speed_kmh = 37.5, recovery_heart_rate = 72, max_heart_rate = 190
WHERE horse_id = 6 AND status = 'RAN';

-- Ngựa 8: WILD IVANA -> Nhóm trung bình khá
UPDATE training_workouts
SET average_speed_kmh = 33.0, top_speed_kmh = 36.5, recovery_heart_rate = 74, max_heart_rate = 225
WHERE horse_id = 8 AND status = 'RAN';

-- Ngựa 17: ASIA BY DAHAB -> Nhóm trung bình
UPDATE training_workouts
SET average_speed_kmh = 31.5, top_speed_kmh = 35.0, recovery_heart_rate = 76, max_heart_rate = 220
WHERE horse_id = 17 AND status = 'RAN';

-- Ngựa 12: BADEA MAFAL -> Nhóm trung bình
UPDATE training_workouts
SET average_speed_kmh = 30.0, top_speed_kmh = 33.5, recovery_heart_rate = 78, max_heart_rate = 185
WHERE horse_id = 12 AND status = 'RAN';

-- Ngựa 16: ZGOLD ZEINAB -> Nhóm trung bình
UPDATE training_workouts
SET average_speed_kmh = 29.5, top_speed_kmh = 33.0, recovery_heart_rate = 79, max_heart_rate = 220
WHERE horse_id = 16 AND status = 'RAN';

-- Ngựa 13: TO BE OR NOT TO BE -> Nhóm trung bình
UPDATE training_workouts
SET average_speed_kmh = 28.5, top_speed_kmh = 32.0, recovery_heart_rate = 81, max_heart_rate = 185
WHERE horse_id = 13 AND status = 'RAN';

-- Ngựa 18: FELICITY -> Nhóm trung bình yếu
UPDATE training_workouts
SET average_speed_kmh = 27.5, top_speed_kmh = 31.0, recovery_heart_rate = 82, max_heart_rate = 185
WHERE horse_id = 18 AND status = 'RAN';

-- Ngựa 7: AG NOBEL EMIR -> Nhóm trung bình yếu
UPDATE training_workouts
SET average_speed_kmh = 26.5, top_speed_kmh = 30.0, recovery_heart_rate = 83, max_heart_rate = 220
WHERE horse_id = 7 AND status = 'RAN';

-- Ngựa 15: ALABAMA MPE -> Nhóm trung bình yếu
UPDATE training_workouts
SET average_speed_kmh = 25.5, top_speed_kmh = 29.0, recovery_heart_rate = 85, max_heart_rate = 225
WHERE horse_id = 15 AND status = 'RAN';

-- Ngựa 10: FC NAJAA -> Nhóm yếu
UPDATE training_workouts
SET average_speed_kmh = 24.0, top_speed_kmh = 27.5, recovery_heart_rate = 87, max_heart_rate = 185
WHERE horse_id = 10 AND status = 'RAN';

-- Ngựa 9: RACHEL BY WIDA -> Nhóm yếu
UPDATE training_workouts
SET average_speed_kmh = 23.0, top_speed_kmh = 26.5, recovery_heart_rate = 89, max_heart_rate = 225
WHERE horse_id = 9 AND status = 'RAN';

-- Ngựa 1: NAUSICAA BY NIKA -> Phân bố tiến trình thể lực theo thời gian
WITH ranked_workouts AS (
    SELECT w.id,
           row_number() OVER (ORDER BY s.session_date ASC) AS rn,
           count(*) OVER () AS total_cnt
    FROM training_workouts w
    JOIN training_sessions s ON w.session_id = s.id
    WHERE w.horse_id = 1 AND w.status = 'RAN'
)
UPDATE training_workouts tw
SET average_speed_kmh = CASE 
        WHEN rw.rn = 15 THEN 15.0
        ELSE round(CAST(22.0 + (rw.rn::numeric / rw.total_cnt::numeric) * 16.0 + (random() * 1.5 - 0.75) AS numeric), 1)
    END,
    top_speed_kmh = CASE 
        WHEN rw.rn = 15 THEN 18.0
        ELSE round(CAST(26.0 + (rw.rn::numeric / rw.total_cnt::numeric) * 16.0 + (random() * 1.5 - 0.75) AS numeric), 1)
    END,
    recovery_heart_rate = CASE 
        WHEN rw.rn = 15 THEN 112
        ELSE round(CAST(92.0 - (rw.rn::numeric / rw.total_cnt::numeric) * 20.0 + (random() * 3.0 - 1.5) AS numeric))
    END,
    max_heart_rate = CASE 
        WHEN rw.rn = 15 THEN 225
        ELSE round(CAST(180 + (rw.rn::numeric / rw.total_cnt::numeric) * 25.0 AS numeric))
    END
FROM ranked_workouts rw
WHERE tw.id = rw.id;

COMMIT;
