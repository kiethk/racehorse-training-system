-- =====================================================================
-- 50_training.sql — bài tập, khóa học, kế hoạch huấn luyện, lot và buổi tập của 3 Head Trainer (~13 tuần + sắp tới).
-- (Subjects, courses, training plans, lots and workouts of the 3 head trainers: ~13 weeks of history plus upcoming.)
--
-- Luật đang giữ (HorseTrainingPlanService / TrainingLotService / TrainingWorkoutService, migration V202610251100):
--   - Plan chỉ cho ngựa ELIGIBLE, không bị chặn tập, trong khu của Trainer; ngày tập không chồng plan khác (BR-01).
--   - Đúng total_sessions buổi, xoay vòng bài của khóa, chỉ vào các thứ đã chọn; Groom = Groom của chuồng.
--   - Lot trong khung 06:00-10:00, bắt đầu :00/:15/:30/:45, không chồng lot khác của Trainer (BR-02, BR-03),
--     tối đa 6 ngựa (BR-10), mỗi Groom một ngựa trong một lot (BR-09).
--   - Ngựa bị chặn tập: buổi chưa diễn ra bị hủy, lot rỗng bị hủy, plan đang chạy thành CANCELLED.
--   - Buổi đã ghi kết quả có đủ 7 chỉ số; điểm phong độ tính đúng công thức WorkoutPerformanceCalculator.
-- (Rules kept: plans only for ELIGIBLE, non-blocked horses of the trainer's areas, no overlapping plans; exact session
-- count, subject rotation, chosen weekdays, stall groom; lots inside the golden window on quarter hours, no overlap,
-- max 6 horses, one horse per groom per lot; blocking cancels what is not held yet; ratings use the app formula.)
-- =====================================================================

-- Ngày các buổi tập: từ p_start, lấy ngày có thứ nằm trong p_days, đủ p_total buổi.
-- (Session dates: from p_start, keep days whose weekday is in p_days, until p_total sessions.)
CREATE FUNCTION pg_temp.session_dates(p_start DATE, p_days TEXT, p_total INT) RETURNS DATE[] AS $$
DECLARE
    v_dates DATE[] := '{}';
    v_day DATE := p_start;
BEGIN
    WHILE COALESCE(array_length(v_dates, 1), 0) < p_total LOOP
        IF trim(to_char(v_day, 'DAY')) = ANY (string_to_array(p_days, ',')) THEN v_dates := v_dates || v_day; END IF;
        v_day := v_day + 1;
    END LOOP;
    RETURN v_dates;
END $$ LANGUAGE plpgsql;

-- Thứ trong tuần lưu theo thứ tự chữ cái, như TrainingDaySetConverter. (Weekdays stored sorted, like the converter.)
CREATE FUNCTION pg_temp.sorted_days(p_days TEXT) RETURNS TEXT AS $$
    SELECT string_agg(d, ',' ORDER BY d) FROM unnest(string_to_array(p_days, ',')) d
$$ LANGUAGE sql IMMUTABLE;

-- Xếp một buổi vào lot (findOrCreateLot): dùng lại lot cùng ngày + cùng bài còn chỗ và chưa có Groom này; nếu không thì
-- mở lot mới ở khe 15 phút đầu tiên còn trống trong khung giờ vàng. (Place a session in a lot, like findOrCreateLot.)
CREATE FUNCTION pg_temp.place(p_trainer BIGINT, p_date DATE, p_subject BIGINT, p_groom BIGINT, p_created TIMESTAMP)
RETURNS BIGINT AS $$
DECLARE
    v_minutes INT;
    v_lot BIGINT;
    v_start TIME := TIME '06:00';
    v_end TIME;
BEGIN
    SELECT duration_minutes INTO v_minutes FROM subjects WHERE id = p_subject;
    SELECT l.id INTO v_lot FROM training_lots l
     WHERE l.trainer_id = p_trainer AND l.lot_date = p_date AND l.subject_id = p_subject AND l.status <> 'CANCELLED'
       AND (SELECT count(*) FROM training_workouts w WHERE w.lot_id = l.id AND w.status <> 'CANCELLED') < l.max_capacity
       AND NOT EXISTS (SELECT 1 FROM training_workouts w
                        WHERE w.lot_id = l.id AND w.status <> 'CANCELLED' AND w.assigned_to_id = p_groom)
     ORDER BY l.start_time LIMIT 1;
    IF v_lot IS NOT NULL THEN RETURN v_lot; END IF;
    LOOP
        v_end := v_start + make_interval(mins => v_minutes);
        IF v_end > TIME '10:00' OR v_end < v_start THEN
            RAISE EXCEPTION 'Schedule full on % for trainer % (too many lots that day; move a cohort to other weekdays)',
                p_date, p_trainer;
        END IF;
        EXIT WHEN NOT EXISTS (SELECT 1 FROM training_lots o
                               WHERE o.trainer_id = p_trainer AND o.lot_date = p_date AND o.status <> 'CANCELLED'
                                 AND o.start_time < v_end AND o.end_time > v_start);
        v_start := v_start + INTERVAL '15 minutes';
    END LOOP;
    INSERT INTO training_lots (trainer_id, subject_id, lot_date, start_time, end_time, max_capacity, status, created_at, updated_at)
    VALUES (p_trainer, p_subject, p_date, v_start, v_end, 6, 'SCHEDULED', p_created, p_created)
    RETURNING id INTO v_lot;
    RETURN v_lot;
END $$ LANGUAGE plpgsql;

-- Điểm phong độ — chép đúng WorkoutPerformanceCalculator.calculate. (Performance rating, same as the app formula.)
CREATE FUNCTION pg_temp.rating(d NUMERIC, t NUMERIC, v_top NUMERIC, v_avg NUMERIC, hr_avg NUMERIC, hr_max NUMERIC,
                               hr_rec NUMERIC) RETURNS INT AS $$
DECLARE
    vt NUMERIC := GREATEST(0.1, v_top);
    va NUMERIC := GREATEST(0.1, v_avg);
    ha NUMERIC := GREATEST(30, hr_avg);
    hm NUMERIC := GREATEST(GREATEST(30, hr_avg), hr_max);
    hrr NUMERIC := GREATEST(30, hr_rec);
    p_speed NUMERIC; p_cardio NUMERIC; p_eff NUMERIC; v_calc NUMERIC;
BEGIN
    p_speed := 0.60 * LEAST(10, va / 55 * 10) + 0.40 * LEAST(10, va / vt * 10);
    p_cardio := GREATEST(1, LEAST(10, 1 + (((hm - hrr) / hm) - 0.20) / 0.35 * 9));
    v_calc := CASE WHEN t > 0 THEN (d / 1000) / (t / 60) ELSE va END;
    p_eff := 0.60 * GREATEST(1, LEAST(10, (va / ha * 100) / 28 * 10)) + 0.40 * LEAST(10, GREATEST(1, v_calc / va * 10));
    -- Math.round của Java làm tròn .5 lên. (Java Math.round rounds .5 up.)
    RETURN GREATEST(1, LEAST(10, floor(0.35 * p_speed + 0.40 * p_cardio + 0.25 * p_eff + 0.5)::INT));
END $$ LANGUAGE plpgsql IMMUTABLE;

-- Kết quả một buổi tập theo cách WorkoutResultSimulationService sinh số liệu, chỉnh theo "năng lực" của ngựa
-- (0..1, cao = khỏe) và cờ mệt mỏi. (Workout result the way WorkoutResultSimulationService generates it, tuned by the
-- horse's ability (0..1) and a fatigue flag.)
CREATE FUNCTION pg_temp.workout_result(p_seed TEXT, p_subject BIGINT, p_ability NUMERIC, p_fatigue BOOLEAN,
    OUT dist NUMERIC, OUT dur NUMERIC, OUT top NUMERIC, OUT avg NUMERIC, OUT hr INT, OUT hr_max INT, OUT hr_rec INT,
    OUT rating INT) AS $$
DECLARE
    s subjects;
    v_lo NUMERIC; v_hi NUMERIC; v_hr_lo INT; v_hr_hi INT;
    a NUMERIC := LEAST(1, GREATEST(0, p_ability + pg_temp.rnd(p_seed || 'n', -0.12, 0.12)));
BEGIN
    SELECT * INTO s FROM subjects WHERE id = p_subject;
    SELECT x.lo, x.hi, x.hlo, x.hhi INTO v_lo, v_hi, v_hr_lo, v_hr_hi
      FROM (VALUES ('LOW', 15.0, 24.0, 115, 145), ('MEDIUM', 24.0, 36.0, 145, 175), ('HIGH', 36.0, 50.0, 170, 195))
           x(level, lo, hi, hlo, hhi)
     WHERE x.level = s.intensity_level;
    dist := round(COALESCE(s.target_distance_meters, 1000) * pg_temp.rnd(p_seed || 'd', 0.95, 1.05), 1);
    dur := round(s.duration_minutes * pg_temp.rnd(p_seed || 't', 0.92, 1.03), 1);
    avg := round(v_lo + (v_hi - v_lo) * (0.2 + 0.8 * a), 1);
    top := round(avg * pg_temp.rnd(p_seed || 's', 1.15, 1.30), 1);
    hr := round(v_hr_hi - (v_hr_hi - v_hr_lo) * a * pg_temp.rnd(p_seed || 'h', 0.8, 1.0));
    hr_max := LEAST(220, hr + pg_temp.rndi(p_seed || 'm', 15, 30));
    hr_rec := CASE WHEN p_fatigue THEN LEAST(hr - 9, pg_temp.rndi(p_seed || 'f', 118, 130))
                   ELSE LEAST(hr - 9, round(120 - 45 * a * pg_temp.rnd(p_seed || 'r', 0.85, 1.0))::INT) END;
    rating := pg_temp.rating(dist, dur, top, avg, hr, hr_max, hr_rec);
END $$ LANGUAGE plpgsql;

-- Một kế hoạch cho một ngựa (createPlan). Trạng thái từng buổi, lot và plan suy ra từ ngày, giờ hiện tại và các khoảng
-- nghỉ trong seed_block. p_unrecorded: buổi đã qua gần nhất để trống kết quả (Trainer cần ghi).
-- (One plan for one horse. Session, lot and plan states follow from the dates, now and the rest windows.
-- p_unrecorded leaves the latest past session without a result for the trainer to record.)
CREATE FUNCTION pg_temp.make_plan(p_horse_k TEXT, p_course BIGINT, p_start DATE, p_days TEXT,
                                  p_unrecorded BOOLEAN DEFAULT FALSE, p_fatigue_from DATE DEFAULT NULL) RETURNS BIGINT AS $$
DECLARE
    v_horse BIGINT := pg_temp.hid(p_horse_k);
    v_trainer BIGINT; v_groom BIGINT; v_total INT; v_subjects BIGINT[]; v_dates DATE[];
    v_created TIMESTAMP; v_block TIMESTAMP; v_plan BIGINT; v_lot BIGINT; v_lot_end TIME; v_rec TIMESTAMP;
    v_status TEXT; v_last_past INT := 0; v_ability NUMERIC; v_approved TIMESTAMP; r RECORD;
    v_done INT := 0; v_cancelled INT := 0;
BEGIN
    SELECT a.trainer_id, ss.groom_id INTO v_trainer, v_groom
      FROM horses h JOIN stable_stalls ss ON ss.id = h.current_stall_id JOIN areas a ON a.id = ss.area_id WHERE h.id = v_horse;
    SELECT total_sessions INTO v_total FROM courses WHERE id = p_course;
    IF (SELECT trainer_id FROM courses WHERE id = p_course) <> v_trainer THEN
        RAISE EXCEPTION 'Course % does not belong to the trainer of horse %', p_course, p_horse_k;
    END IF;
    SELECT array_agg(subject_id ORDER BY order_index) INTO v_subjects FROM course_subjects WHERE course_id = p_course;
    v_dates := pg_temp.session_dates(p_start, p_days, v_total);
    v_created := LEAST((p_start - 2) + TIME '16:00', pg_temp.clock() - INTERVAL '2 hours');
    SELECT manager_reviewed_at INTO v_approved FROM admission_applications
     WHERE horse_id = v_horse AND status = 'APPROVED';
    IF v_approved IS NULL OR v_created < v_approved THEN
        RAISE EXCEPTION 'Plan for % is created on % before the horse was admitted (%)', p_horse_k, v_created, v_approved;
    END IF;
    IF pg_temp.blocked_at(v_horse, v_created) THEN
        RAISE EXCEPTION 'Plan for % is created on % while the horse is resting', p_horse_k, v_created;
    END IF;
    IF EXISTS (SELECT 1 FROM horse_training_plans p WHERE p.horse_id = v_horse AND p.status <> 'CANCELLED'
                AND p.start_date <= v_dates[v_total] AND p.end_date >= v_dates[1]) THEN
        RAISE EXCEPTION 'Plan for % (% - %) overlaps another plan of the horse', p_horse_k, v_dates[1], v_dates[v_total];
    END IF;
    -- Lần chặn tập đầu tiên sau khi tạo plan. (The first block after the plan was created.)
    SELECT min(from_ts) INTO v_block FROM seed_block WHERE horse_id = v_horse AND from_ts > v_created;
    v_ability := pg_temp.rnd('ability' || p_horse_k, 0.15, 1.0);

    INSERT INTO horse_training_plans (horse_id, course_id, trainer_id, start_date, end_date, status, created_at, updated_at,
                                      training_days, groom_id)
    VALUES (v_horse, p_course, v_trainer, v_dates[1], v_dates[v_total], 'ACTIVE', v_created, v_created,
            pg_temp.sorted_days(p_days), v_groom)
    RETURNING id INTO v_plan;

    FOR i IN 1..v_total LOOP
        IF v_dates[i] < pg_temp.today() THEN v_last_past := i; END IF;
    END LOOP;
    FOR i IN 1..v_total LOOP
        v_lot := pg_temp.place(v_trainer, v_dates[i], v_subjects[((i - 1) % array_length(v_subjects, 1)) + 1], v_groom, v_created);
        SELECT end_time INTO v_lot_end FROM training_lots WHERE id = v_lot;
        v_rec := v_dates[i] + v_lot_end + make_interval(mins => pg_temp.rndi('rec' || v_plan || i, 5, 35));
        IF v_block IS NOT NULL AND v_dates[i] >= v_block::DATE AND v_rec >= v_block THEN
            v_status := 'CANCELLED';
        ELSIF v_rec <= pg_temp.clock() AND NOT (p_unrecorded AND i = v_last_past) THEN
            v_status := 'COMPLETED';
        ELSE
            v_status := 'SCHEDULED';
        END IF;
        IF v_status = 'COMPLETED' THEN
            r := pg_temp.workout_result(v_plan || '-' || i, (SELECT subject_id FROM training_lots WHERE id = v_lot),
                                        v_ability + 0.015 * i, p_fatigue_from IS NOT NULL AND v_dates[i] >= p_fatigue_from);
            INSERT INTO training_workouts (plan_id, horse_id, assigned_to_id, status, actual_distance_meters,
                                           actual_duration_minutes, top_speed_kmh, average_speed_kmh, average_heart_rate,
                                           max_heart_rate, recovery_heart_rate, performance_rating, trainer_feedback,
                                           created_at, updated_at, lot_id)
            VALUES (v_plan, v_horse, v_groom, 'COMPLETED', r.dist, r.dur, r.top, r.avg, r.hr, r.hr_max, r.hr_rec, r.rating,
                    CASE WHEN r.rating >= 8 THEN 'Strong, even rhythm; recovered quickly after the final furlong.'
                         WHEN r.rating >= 6 THEN 'Solid session; pace dropped slightly in the second half.'
                         WHEN r.rating >= 4 THEN 'Laboured in the last part; heart rate slow to come down.'
                         ELSE 'Tired early and slow to recover. Rest day advised; ask the vet if it continues.' END,
                    v_created, v_rec, v_lot);
            v_done := v_done + 1;
        ELSE
            INSERT INTO training_workouts (plan_id, horse_id, assigned_to_id, status, created_at, updated_at, lot_id)
            VALUES (v_plan, v_horse, v_groom, v_status, v_created,
                    CASE WHEN v_status = 'CANCELLED' THEN v_block ELSE v_created END, v_lot);
            IF v_status = 'CANCELLED' THEN v_cancelled := v_cancelled + 1; END IF;
        END IF;
    END LOOP;

    UPDATE horse_training_plans
       SET status = CASE WHEN v_cancelled > 0 THEN 'CANCELLED'
                         WHEN v_done = v_total THEN 'COMPLETED'
                         WHEN v_dates[1] > pg_temp.today() THEN 'UPCOMING'
                         ELSE 'ACTIVE' END,
           updated_at = CASE WHEN v_cancelled > 0 THEN v_block
                             WHEN v_done > 0 THEN (SELECT max(updated_at) FROM training_workouts WHERE plan_id = v_plan)
                             ELSE v_created END
     WHERE id = v_plan;
    PERFORM pg_temp.audit(v_trainer, 'Create Training Plan', 'HorseTrainingPlan', NULL,
                          'Trainer created a new training plan for horse', v_created, 'POST', '/api/training-plans');
    RETURN v_plan;
END $$ LANGUAGE plpgsql;

-- Một nhóm ngựa ghi danh cùng lúc vào một khóa (một request createPlan nhiều ngựa).
-- (A group of horses enrolled together in one course: one createPlan request with several horses.)
CREATE FUNCTION pg_temp.cohort(p_trainer TEXT, p_course TEXT, p_horses TEXT[], p_start_offset INT, p_days TEXT,
                               p_unrecorded TEXT DEFAULT NULL, p_fatigue TEXT DEFAULT NULL) RETURNS VOID AS $$
DECLARE
    v_course BIGINT;
    v_k TEXT;
BEGIN
    SELECT id INTO v_course FROM courses WHERE name = p_course AND trainer_id = pg_temp.uid(p_trainer);
    IF v_course IS NULL THEN RAISE EXCEPTION 'Unknown course % of %', p_course, p_trainer; END IF;
    FOREACH v_k IN ARRAY p_horses LOOP
        PERFORM pg_temp.make_plan(v_k, v_course, pg_temp.today() + p_start_offset, p_days, COALESCE(v_k = p_unrecorded, FALSE),
                                  CASE WHEN v_k = p_fatigue THEN pg_temp.today() - 7 END);
    END LOOP;
END $$ LANGUAGE plpgsql;

-- Bài tập và khóa học. Khóa có nhiều kiểu: một bài lặp lại, nhiều bài xoay vòng, toàn bài khác nhau, khóa chưa dùng.
-- (Subjects and courses. Course kinds: one subject repeated, several subjects rotated, all distinct, an unused one.)
CREATE FUNCTION pg_temp.subject(p_trainer TEXT, p_name TEXT, p_desc TEXT, p_surface TEXT, p_distance INT, p_intensity TEXT,
                                p_category TEXT, p_minutes INT, p_created_days_ago INT) RETURNS VOID AS $$
    INSERT INTO subjects (trainer_id, name, description, surface_type, target_distance_meters, intensity_level, category,
                          duration_minutes, created_at, updated_at)
    VALUES (pg_temp.uid(p_trainer), p_name, p_desc, p_surface, p_distance, p_intensity, p_category, p_minutes,
            pg_temp.at(-p_created_days_ago, TIME '09:00'), pg_temp.at(-p_created_days_ago, TIME '09:00'))
$$ LANGUAGE sql;

CREATE FUNCTION pg_temp.course(p_trainer TEXT, p_name TEXT, p_goal TEXT, p_sessions INT, p_subjects TEXT[],
                               p_created_days_ago INT) RETURNS VOID AS $$
DECLARE
    v_course BIGINT;
    v_trainer BIGINT := pg_temp.uid(p_trainer);
    v_at TIMESTAMP := pg_temp.at(-p_created_days_ago, TIME '10:00');
BEGIN
    INSERT INTO courses (name, target_goal, total_sessions, created_by_id, created_at, updated_at, trainer_id)
    VALUES (p_name, p_goal, p_sessions, v_trainer, v_at, v_at, v_trainer) RETURNING id INTO v_course;
    FOR i IN 1..array_length(p_subjects, 1) LOOP
        INSERT INTO course_subjects (course_id, subject_id, order_index, created_at)
        SELECT v_course, s.id, i, v_at FROM subjects s WHERE s.trainer_id = v_trainer AND s.name = p_subjects[i];
        IF NOT FOUND THEN RAISE EXCEPTION 'Unknown subject % of %', p_subjects[i], p_trainer; END IF;
    END LOOP;
    PERFORM pg_temp.audit(v_trainer, 'Create Training Course', 'Course', NULL, 'Trainer registered a new curriculum course',
                          v_at, 'POST', '/api/courses');
END $$ LANGUAGE plpgsql;

DO $$
DECLARE
    v_lot BIGINT;
BEGIN
    -- ----------------------------------------------------------------- trainer1 (khu A, B)
    PERFORM pg_temp.subject('TR01', 'Aerobic Base Canter', 'Steady canter on turf to build the aerobic base.', 'TURF', 1600, 'MEDIUM', 'ENDURANCE', 60, 120);
    PERFORM pg_temp.subject('TR01', 'Long Stride Gallop', 'Sustained gallop to condition long-stride rhythm.', 'DIRT', 2000, 'MEDIUM', 'ENDURANCE', 60, 120);
    PERFORM pg_temp.subject('TR01', 'Uphill Power Gallop', 'Incline work for hindquarter drive and cardiac economy.', 'DIRT', 1600, 'HIGH', 'ENDURANCE', 60, 118);
    PERFORM pg_temp.subject('TR01', 'Gate Breakout Sprint', 'Explosive gate opening and a 400 m initial drive.', 'DIRT', 400, 'HIGH', 'GATE_START', 45, 118);
    PERFORM pg_temp.subject('TR01', 'Turn and Straight Sprint', 'Hold the line through the turn, accelerate on the straight.', 'SYNTHETIC', 1000, 'HIGH', 'SPEED', 60, 117);
    PERFORM pg_temp.subject('TR01', 'Furlong Interval Bursts', 'Short bursts with 2-minute recovery between furlongs.', 'TURF', 1200, 'HIGH', 'SPEED', 60, 117);
    PERFORM pg_temp.subject('TR01', 'Walk and Hydro Cooling', 'Active walking and cold-water hosing to clear lactate.', 'SYNTHETIC', 800, 'LOW', 'RECOVERY', 45, 116);
    PERFORM pg_temp.subject('TR01', 'Trot Balance Work', 'Balance, stride symmetry and joint mobility at trot.', 'TURF', 1200, 'LOW', 'RECOVERY', 45, 116);
    PERFORM pg_temp.subject('TR01', 'Gate Loading Composure', 'Calm loading into the starting stall and a quiet stand.', 'DIRT', 200, 'LOW', 'GATE_START', 45, 115);
    PERFORM pg_temp.subject('TR01', 'Official Speed Trial', 'Full mock trial with electronic timing.', 'TURF', 1200, 'HIGH', 'SPEED', 60, 115);
    PERFORM pg_temp.subject('TR01', 'Sand Track Stamina', 'Deep sand work for stamina; not in any course yet.', 'DIRT', 1800, 'MEDIUM', 'ENDURANCE', 60, 20);

    PERFORM pg_temp.course('TR01', 'Gate Confidence Drill', 'Calm, confident loading and stand in the starting gate.', 6,
                           ARRAY['Gate Loading Composure'], 110);
    PERFORM pg_temp.course('TR01', 'Novice Foundation', 'Base fitness and steady rhythm for newly admitted horses.', 10,
                           ARRAY['Aerobic Base Canter', 'Trot Balance Work', 'Gate Loading Composure', 'Long Stride Gallop', 'Walk and Hydro Cooling'], 110);
    PERFORM pg_temp.course('TR01', 'Sprint Acceleration', 'Top speed and acceleration out of the gate and the turn.', 8,
                           ARRAY['Gate Breakout Sprint', 'Turn and Straight Sprint', 'Furlong Interval Bursts', 'Walk and Hydro Cooling'], 108);
    PERFORM pg_temp.course('TR01', 'Classic Endurance', 'Stamina for 1,800-2,400 m races.', 12,
                           ARRAY['Aerobic Base Canter', 'Long Stride Gallop', 'Uphill Power Gallop', 'Walk and Hydro Cooling'], 108);
    PERFORM pg_temp.course('TR01', 'Championship Trial Protocol', 'Six different race-specific sessions ending with a timed trial.', 6,
                           ARRAY['Turn and Straight Sprint', 'Furlong Interval Bursts', 'Uphill Power Gallop', 'Gate Breakout Sprint', 'Long Stride Gallop', 'Official Speed Trial'], 100);
    PERFORM pg_temp.course('TR01', 'Recovery Maintenance', 'Light work after a heavy block or a rest period.', 4,
                           ARRAY['Trot Balance Work', 'Walk and Hydro Cooling'], 100);
    PERFORM pg_temp.course('TR01', 'Winter Base Block', 'Planned for the winter break; no horse enrolled yet.', 20,
                           ARRAY['Aerobic Base Canter', 'Sand Track Stamina', 'Trot Balance Work'], 10);

    -- Đợt 1 (khoảng 13 tuần trước). (Block 1, about 13 weeks ago.)
    PERFORM pg_temp.cohort('TR01', 'Novice Foundation', ARRAY['H001', 'H004', 'H007', 'H010', 'H013', 'H016'], -91, 'MONDAY,WEDNESDAY,FRIDAY');
    PERFORM pg_temp.cohort('TR01', 'Gate Confidence Drill', ARRAY['H002', 'H005', 'H008', 'H011', 'H014', 'H017'], -90, 'TUESDAY,THURSDAY');
    PERFORM pg_temp.cohort('TR01', 'Classic Endurance', ARRAY['H003', 'H006', 'H009', 'H012', 'H015'], -89, 'TUESDAY,THURSDAY,SATURDAY');
    PERFORM pg_temp.cohort('TR01', 'Recovery Maintenance', ARRAY['H018'], -88, 'SUNDAY');
    -- Đợt 2 (khoảng 2 tháng trước). (Block 2, about two months ago.)
    PERFORM pg_temp.cohort('TR01', 'Championship Trial Protocol', ARRAY['H001', 'H005', 'H009', 'H011', 'H015', 'H016'], -62, 'MONDAY,WEDNESDAY,FRIDAY');
    PERFORM pg_temp.cohort('TR01', 'Sprint Acceleration', ARRAY['H002', 'H004', 'H008', 'H012', 'H013', 'H017'], -61, 'TUESDAY,THURSDAY,SATURDAY');
    PERFORM pg_temp.cohort('TR01', 'Classic Endurance', ARRAY['H003', 'H006', 'H010', 'H014', 'H018'], -56, 'MONDAY,THURSDAY');
    PERFORM pg_temp.cohort('TR01', 'Recovery Maintenance', ARRAY['H007'], -40, 'WEDNESDAY,SATURDAY');
    -- Đợt hiện tại và sắp tới. (Current and upcoming block.)
    PERFORM pg_temp.cohort('TR01', 'Sprint Acceleration', ARRAY['H001', 'H004', 'H010'], -11, 'MONDAY,WEDNESDAY,FRIDAY', NULL, 'H004');
    PERFORM pg_temp.cohort('TR01', 'Classic Endurance', ARRAY['H002', 'H007', 'H013'], -12, 'TUESDAY,THURSDAY,SATURDAY');
    PERFORM pg_temp.cohort('TR01', 'Championship Trial Protocol', ARRAY['H005', 'H008', 'H011', 'H016'], -6, 'MONDAY,WEDNESDAY,FRIDAY');
    PERFORM pg_temp.cohort('TR01', 'Novice Foundation', ARRAY['H003', 'H006', 'H015'], -11, 'TUESDAY,FRIDAY', 'H003');
    PERFORM pg_temp.cohort('TR01', 'Gate Confidence Drill', ARRAY['H009'], 3, 'TUESDAY,THURSDAY');
    PERFORM pg_temp.cohort('TR01', 'Sprint Acceleration', ARRAY['H012', 'H017'], 5, 'MONDAY,WEDNESDAY,FRIDAY');

    -- ----------------------------------------------------------------- trainer2 (khu C, D)
    PERFORM pg_temp.subject('TR02', 'Distance Canter', 'Even canter over a long distance.', 'TURF', 2000, 'MEDIUM', 'ENDURANCE', 60, 90);
    PERFORM pg_temp.subject('TR02', 'Hill Repeats', 'Repeated short climbs at a strong canter.', 'DIRT', 1400, 'HIGH', 'ENDURANCE', 60, 90);
    PERFORM pg_temp.subject('TR02', 'Breeze Work', 'Fast work over 800 m at race pace.', 'TURF', 800, 'HIGH', 'SPEED', 45, 90);
    PERFORM pg_temp.subject('TR02', 'Pace Change Drill', 'Switch between canter and gallop on command.', 'SYNTHETIC', 1200, 'MEDIUM', 'SPEED', 60, 88);
    PERFORM pg_temp.subject('TR02', 'Starting Stall Practice', 'Walk-in, stand and walk-out of the starting stalls.', 'DIRT', 200, 'LOW', 'GATE_START', 45, 88);
    PERFORM pg_temp.subject('TR02', 'Pool Recovery Swim', 'Low-impact swimming after hard work.', 'SYNTHETIC', 400, 'LOW', 'RECOVERY', 45, 88);
    PERFORM pg_temp.course('TR02', 'Distance Builder', 'Build stamina for 2,000 m and longer.', 9,
                           ARRAY['Distance Canter', 'Hill Repeats', 'Pool Recovery Swim'], 85);
    PERFORM pg_temp.course('TR02', 'Speed Sharpening', 'Race pace and quick changes of speed.', 8,
                           ARRAY['Breeze Work', 'Pace Change Drill', 'Hill Repeats', 'Pool Recovery Swim'], 85);
    PERFORM pg_temp.course('TR02', 'Starting Stall Basics', 'Stall manners for horses new to racing.', 4,
                           ARRAY['Starting Stall Practice'], 85);
    PERFORM pg_temp.cohort('TR02', 'Distance Builder', ARRAY['H019', 'H022', 'H025'], -70, 'MONDAY,WEDNESDAY,FRIDAY');
    PERFORM pg_temp.cohort('TR02', 'Speed Sharpening', ARRAY['H020', 'H023', 'H026'], -69, 'TUESDAY,THURSDAY');
    PERFORM pg_temp.cohort('TR02', 'Starting Stall Basics', ARRAY['H021', 'H024', 'H027'], -68, 'SATURDAY');
    PERFORM pg_temp.cohort('TR02', 'Speed Sharpening', ARRAY['H028', 'H031', 'H034'], -40, 'MONDAY,WEDNESDAY,FRIDAY');
    PERFORM pg_temp.cohort('TR02', 'Distance Builder', ARRAY['H029', 'H032', 'H035'], -38, 'TUESDAY,THURSDAY,SATURDAY');
    PERFORM pg_temp.cohort('TR02', 'Starting Stall Basics', ARRAY['H030', 'H033', 'H036'], -30, 'SATURDAY,SUNDAY');
    PERFORM pg_temp.cohort('TR02', 'Speed Sharpening', ARRAY['H019', 'H023', 'H031'], -9, 'MONDAY,WEDNESDAY,FRIDAY');
    PERFORM pg_temp.cohort('TR02', 'Distance Builder', ARRAY['H020', 'H029', 'H035'], -2, 'TUESDAY,THURSDAY,SATURDAY');
    PERFORM pg_temp.cohort('TR02', 'Starting Stall Basics', ARRAY['H025', 'H032'], 4, 'MONDAY,THURSDAY');

    -- ----------------------------------------------------------------- trainer3 (khu E)
    PERFORM pg_temp.subject('TR03', 'Desert Endurance Trot', 'Long, even trot for Arabian endurance.', 'DIRT', 3000, 'LOW', 'ENDURANCE', 60, 50);
    PERFORM pg_temp.subject('TR03', 'Tempo Canter', 'Canter at a fixed tempo with heart-rate targets.', 'TURF', 1800, 'MEDIUM', 'ENDURANCE', 60, 50);
    PERFORM pg_temp.subject('TR03', 'Short Sprint Repeats', '3 x 300 m sprints with walk recovery.', 'TURF', 900, 'HIGH', 'SPEED', 45, 50);
    PERFORM pg_temp.subject('TR03', 'Stretch and Walk', 'Hand walking and stretching.', 'SYNTHETIC', 600, 'LOW', 'RECOVERY', 45, 50);
    PERFORM pg_temp.course('TR03', 'Arabian Endurance Base', 'Aerobic base for endurance racing.', 6,
                           ARRAY['Desert Endurance Trot', 'Tempo Canter'], 45);
    PERFORM pg_temp.course('TR03', 'Pace Variation', 'Tempo, sprints and recovery in one block.', 6,
                           ARRAY['Tempo Canter', 'Short Sprint Repeats', 'Stretch and Walk'], 45);
    PERFORM pg_temp.cohort('TR03', 'Arabian Endurance Base', ARRAY['H037', 'H040'], -25, 'MONDAY,WEDNESDAY,FRIDAY');
    PERFORM pg_temp.cohort('TR03', 'Pace Variation', ARRAY['H038', 'H041'], -24, 'TUESDAY,THURSDAY,SATURDAY');
    PERFORM pg_temp.cohort('TR03', 'Arabian Endurance Base', ARRAY['H039', 'H042'], -8, 'MONDAY,WEDNESDAY,FRIDAY');
    PERFORM pg_temp.cohort('TR03', 'Pace Variation', ARRAY['H037'], 2, 'TUESDAY,THURSDAY,SATURDAY');

    -- Trainer1 hủy một lot đã lên lịch vì mưa lớn (TrainingLotService.cancelLot): các buổi trong lot thành CANCELLED,
    -- plan vẫn hoàn thành vì buổi hủy được coi là đã xử lý. (trainer1 cancelled one lot for heavy rain.)
    SELECT l.id INTO v_lot FROM training_lots l
     WHERE l.trainer_id = pg_temp.uid('TR01') AND l.lot_date = pg_temp.today() - 80 AND l.status <> 'CANCELLED'
     ORDER BY l.start_time LIMIT 1;
    UPDATE training_workouts SET status = 'CANCELLED', actual_distance_meters = NULL, actual_duration_minutes = NULL,
                                 top_speed_kmh = NULL, average_speed_kmh = NULL, average_heart_rate = NULL, max_heart_rate = NULL,
                                 recovery_heart_rate = NULL, performance_rating = NULL, trainer_feedback = NULL,
                                 updated_at = (pg_temp.today() - 81) + TIME '18:00'
     WHERE lot_id = v_lot;
    UPDATE training_lots SET status = 'CANCELLED', updated_at = (pg_temp.today() - 81) + TIME '18:00' WHERE id = v_lot;

    -- Trạng thái lot theo các buổi trong lot (updateLotProgress / cancelLotIfEmpty).
    -- (Lot status from its workouts, like updateLotProgress / cancelLotIfEmpty.)
    UPDATE training_lots l
       SET status = CASE WHEN NOT EXISTS (SELECT 1 FROM training_workouts w WHERE w.lot_id = l.id AND w.status <> 'CANCELLED')
                         THEN 'CANCELLED'
                         WHEN NOT EXISTS (SELECT 1 FROM training_workouts w WHERE w.lot_id = l.id AND w.status = 'SCHEDULED')
                         THEN 'COMPLETED' ELSE 'SCHEDULED' END,
           updated_at = GREATEST(l.updated_at, (SELECT max(w.updated_at) FROM training_workouts w WHERE w.lot_id = l.id))
     WHERE l.status <> 'CANCELLED';
END $$;
