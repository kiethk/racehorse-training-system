-- =====================================================================
-- SEED TRAINING DEMO DATA (Idempotent & Dynamic - Leader Compliant)
-- =====================================================================
-- Quy tac cua Leader:
-- 1. Chi ghi phan nghiep vu Trainer (courses, subjects, plans, lots, workouts, metrics)
-- 2. Tim nhan vien bang EMAIL (trainer1@rtms.com, grooms)
-- 3. Tim Area/Stall bang MA (A, B, A01..A09, B01..B09)
-- 4. Tim ngua bang UELN (horses.registration_number)
-- 5. Tuyet doi KHONG dung ID so cung (local ID != cloud ID)
-- 6. Idempotent: kiem tra / xoa sach du lieu cu truoc khi nap
-- 7. Toi uu mui gio 30 ngay va diem phong do (>= 7.0) de suy ra Specialty!
-- 8. Moi plan di qua pg_temp.seed_plan: ngay buoi tap, bai tap, lot, groom deu theo dung luat cua app
--    (BR-01/02/03/09/10), nen du lieu seed luon qua duoc cac rang buoc DB (migration V202610251100).
-- =====================================================================

-- Ngay cac buoi tap: duyet tung ngay tu p_start, lay ngay co thu nam trong p_days, du p_total buoi.
-- (Session dates: walk day by day from p_start, keep days whose weekday is in p_days, until p_total sessions.)
CREATE FUNCTION pg_temp.seed_dates(p_start DATE, p_days TEXT, p_total INT) RETURNS DATE[] AS $fn$
DECLARE
    v_dates DATE[] := '{}';
    v_day DATE := p_start;
BEGIN
    WHILE COALESCE(ARRAY_LENGTH(v_dates, 1), 0) < p_total LOOP
        IF position(upper(to_char(v_day, 'FMDAY')) IN p_days) > 0 THEN
            v_dates := v_dates || v_day;
        END IF;
        v_day := v_day + 1;
    END LOOP;
    RETURN v_dates;
END;
$fn$ LANGUAGE plpgsql;

-- Xep mot buoi tap vao lot: dung lai lot cung ngay + cung bai con cho va chua co groom nay (BR-10, BR-09), neu khong thi
-- mo lot moi o khe 15 phut dau tien con trong khung gio vang 06:00-10:00 khong dung lot khac (BR-02, BR-03).
-- (Place a session in a lot: reuse a lot of the same day + subject that has room and not this groom (BR-10, BR-09),
-- otherwise open a new lot in the first free 15-minute slot of the 06:00-10:00 window overlapping no other lot (BR-02, BR-03).)
CREATE FUNCTION pg_temp.seed_place(p_trainer BIGINT, p_date DATE, p_subject BIGINT, p_groom BIGINT, p_lot_status TEXT)
RETURNS BIGINT AS $fn$
DECLARE
    v_minutes INT;
    v_lot BIGINT;
    v_start TIME := TIME '06:00';
    v_end TIME;
BEGIN
    SELECT duration_minutes INTO v_minutes FROM subjects WHERE id = p_subject;

    IF p_lot_status <> 'CANCELLED' THEN
        SELECT l.id INTO v_lot
        FROM training_lots l
        WHERE l.trainer_id = p_trainer AND l.lot_date = p_date AND l.subject_id = p_subject AND l.status = p_lot_status
          AND (SELECT count(*) FROM training_workouts w WHERE w.lot_id = l.id AND w.status <> 'CANCELLED') < l.max_capacity
          AND NOT EXISTS (SELECT 1 FROM training_workouts w
                          WHERE w.lot_id = l.id AND w.status <> 'CANCELLED' AND w.assigned_to_id = p_groom)
        ORDER BY l.start_time LIMIT 1;
        IF v_lot IS NOT NULL THEN
            RETURN v_lot;
        END IF;

        LOOP
            v_end := v_start + make_interval(mins => v_minutes);
            IF v_end > TIME '10:00' OR v_end < v_start THEN
                RAISE EXCEPTION 'No free slot in the golden window on % for subject % (seed has too many lots that day)',
                    p_date, p_subject;
            END IF;
            EXIT WHEN NOT EXISTS (SELECT 1 FROM training_lots o
                                  WHERE o.trainer_id = p_trainer AND o.lot_date = p_date AND o.status <> 'CANCELLED'
                                    AND o.start_time < v_end AND o.end_time > v_start);
            v_start := v_start + INTERVAL '15 minutes';
        END LOOP;
    END IF;

    v_end := v_start + make_interval(mins => v_minutes);
    INSERT INTO training_lots (trainer_id, subject_id, lot_date, start_time, end_time, max_capacity, status, created_at, updated_at)
    VALUES (p_trainer, p_subject, p_date, v_start, v_end, 6, p_lot_status, NOW(), NOW())
    RETURNING id INTO v_lot;
    RETURN v_lot;
END;
$fn$ LANGUAGE plpgsql;

-- Tao mot plan tron ven: dung total_sessions buoi, xoay vong bai cua khoa, chi vao cac thu da chon, groom la groom cua chuong.
-- p_status: COMPLETED (moi buoi da qua, co ket qua) | ACTIVE (buoi truoc hom nay co ket qua, tu hom nay tro di SCHEDULED)
--           | UPCOMING (moi buoi SCHEDULED) | CANCELLED (moi buoi CANCELLED).
-- p_profile: elite | good | steady | fatigue = kieu chi so cua con ngua. p_unrecorded: buoi da qua gan nhat chua ghi ket qua.
-- (Creates a whole plan: exactly total_sessions sessions, rotating the course subjects, only on the chosen weekdays, with
-- the stall groom. p_profile picks the horse metrics; p_unrecorded leaves the latest past session without a result.)
CREATE FUNCTION pg_temp.seed_plan(p_horse BIGINT, p_course BIGINT, p_trainer BIGINT, p_fallback_groom BIGINT,
                                  p_start DATE, p_days TEXT, p_status TEXT, p_profile TEXT, p_unrecorded BOOLEAN)
RETURNS BIGINT AS $fn$
DECLARE
    v_total INT;
    v_subjects BIGINT[];
    v_dates DATE[];
    v_groom BIGINT;
    v_plan BIGINT;
    v_lot BIGINT;
    v_subject RECORD;
    v_last_past INT := 0;
    v_done BOOLEAN;
    v_lot_status TEXT;
    v_top NUMERIC; v_avg NUMERIC; v_hr INT; v_max_hr INT; v_rec_hr INT; v_rating INT;
    i INT;
BEGIN
    SELECT total_sessions INTO v_total FROM courses WHERE id = p_course;
    SELECT ARRAY_AGG(cs.subject_id ORDER BY cs.order_index) INTO v_subjects FROM course_subjects cs WHERE cs.course_id = p_course;
    v_dates := pg_temp.seed_dates(p_start, p_days, v_total);

    -- Groom cua chuong cua con ngua (BR: groom cua workout = groom chuong). (The groom of the horse stall.)
    SELECT COALESCE(ss.groom_id, p_fallback_groom) INTO v_groom
    FROM horses h LEFT JOIN stable_stalls ss ON ss.id = h.current_stall_id WHERE h.id = p_horse;

    -- Trang thai plan phai khop voi ngay. (The plan status must match the dates.)
    IF (p_status = 'COMPLETED' AND v_dates[v_total] >= CURRENT_DATE)
       OR (p_status = 'ACTIVE' AND (v_dates[1] > CURRENT_DATE OR v_dates[v_total] < CURRENT_DATE))
       OR (p_status = 'UPCOMING' AND v_dates[1] <= CURRENT_DATE) THEN
        RAISE EXCEPTION 'Seed plan % of horse % does not fit its dates %..%', p_status, p_horse, v_dates[1], v_dates[v_total];
    END IF;

    INSERT INTO horse_training_plans (horse_id, course_id, trainer_id, start_date, end_date, status,
                                      created_at, updated_at, training_days, groom_id)
    VALUES (p_horse, p_course, p_trainer, v_dates[1], v_dates[v_total], p_status,
            LEAST(v_dates[1], CURRENT_DATE)::TIMESTAMP - INTERVAL '3 days', NOW(), p_days, v_groom)
    RETURNING id INTO v_plan;

    FOR i IN 1..v_total LOOP
        IF v_dates[i] < CURRENT_DATE THEN v_last_past := i; END IF;
    END LOOP;

    FOR i IN 1..v_total LOOP
        SELECT s.id, s.target_distance_meters, s.duration_minutes INTO v_subject
        FROM subjects s WHERE s.id = v_subjects[((i - 1) % ARRAY_LENGTH(v_subjects, 1)) + 1];

        v_done := p_status = 'COMPLETED' OR (p_status = 'ACTIVE' AND v_dates[i] < CURRENT_DATE
                                             AND NOT (p_unrecorded AND i = v_last_past));
        v_lot_status := CASE WHEN p_status = 'CANCELLED' THEN 'CANCELLED'
                             WHEN v_dates[i] < CURRENT_DATE AND v_done THEN 'COMPLETED'
                             ELSE 'SCHEDULED' END;
        v_lot := pg_temp.seed_place(p_trainer, v_dates[i], v_subject.id, v_groom, v_lot_status);

        IF v_done THEN
            -- Chi so theo kieu ngua; moi gia tri nam trong khoang app cho phep (HR 30-240, avg <= top, diem 1-10).
            -- (Metrics by horse profile; every value is inside the range the app accepts.)
            CASE p_profile
                WHEN 'elite'   THEN v_top := 64.4 + (i % 3) * 0.4; v_avg := 55.1 + (i % 3) * 0.3;
                                    v_hr := 145; v_max_hr := 178; v_rec_hr := 88; v_rating := 9 + (i % 2);
                WHEN 'good'    THEN v_top := 61.5 + i * 0.2;       v_avg := 53.0 + i * 0.15;
                                    v_hr := 142; v_max_hr := 175; v_rec_hr := 85; v_rating := 8 + (i % 2);
                WHEN 'steady'  THEN v_top := 58.0;                 v_avg := 49.8;
                                    v_hr := 140; v_max_hr := 170; v_rec_hr := 88; v_rating := 8;
                ELSE                v_top := 54.2;                 v_avg := 45.0;
                                    v_hr := 162; v_max_hr := 198; v_rec_hr := 124; v_rating := 3;
            END CASE;

            INSERT INTO training_workouts (
                plan_id, horse_id, assigned_to_id, status, actual_distance_meters, actual_duration_minutes,
                top_speed_kmh, average_speed_kmh, average_heart_rate, max_heart_rate, recovery_heart_rate,
                performance_rating, trainer_feedback, created_at, updated_at, lot_id
            ) VALUES (
                v_plan, p_horse, v_groom, 'COMPLETED', v_subject.target_distance_meters, v_subject.duration_minutes - 4,
                v_top, v_avg, v_hr, v_max_hr, v_rec_hr, v_rating,
                CASE p_profile
                    WHEN 'elite' THEN 'Outstanding burst and recovery rhythm. Stride efficiency at competition standard.'
                    WHEN 'fatigue' THEN 'Showed sign of hind leg fatigue on final incline. Advised vet evaluation.'
                    ELSE 'Steady cadence and good recovery throughout the session.' END,
                v_dates[i] + TIME '10:10:00', v_dates[i] + TIME '10:10:00', v_lot
            );

            IF p_profile = 'fatigue' THEN
                INSERT INTO horse_health_metrics (horse_id, recorded_at, heart_rate, temperature, weight, respiratory_rate,
                                                  hydration_status, body_condition_score, notes, created_at)
                VALUES (p_horse, v_dates[i] + TIME '10:30:00', 46.0, 38.6, 492.0, 22, 'MILD_DEHYDRATION', 5.0,
                        '[DEMO-SEED] High recovery heart rate (124 bpm) observed post-uphill gallop. Potential fatigue alert.', NOW());
            ELSE
                INSERT INTO horse_health_metrics (horse_id, recorded_at, heart_rate, temperature, weight, respiratory_rate,
                                                  hydration_status, body_condition_score, notes, created_at)
                VALUES (p_horse, v_dates[i] + TIME '10:30:00', 37.5 + (i - 1) * 0.3, 38.0, 490.0 + (i - 1), 14,
                        'OPTIMAL', 6.0, '[DEMO-SEED] Post-workout recovery check.', NOW());
            END IF;
        ELSE
            INSERT INTO training_workouts (plan_id, horse_id, assigned_to_id, status, trainer_feedback, created_at, updated_at, lot_id)
            VALUES (v_plan, p_horse, v_groom,
                    CASE WHEN p_status = 'CANCELLED' THEN 'CANCELLED' ELSE 'SCHEDULED' END,
                    CASE WHEN p_status = 'CANCELLED' THEN 'Session cancelled before commencement.' END,
                    NOW(), NOW(), v_lot);
        END IF;
    END LOOP;
    RETURN v_plan;
END;
$fn$ LANGUAGE plpgsql;

DO $$
DECLARE
    -- Trainer & Staff IDs (resolved via email)
    v_trainer1 BIGINT;
    v_groom_daniel BIGINT;

    -- Horses resolved via UELN (registration_number)
    v_horses BIGINT[];
    v_horse_count INT;
    v_h_nausicaa BIGINT;
    v_h_eagle BIGINT;
    v_h_si_renesme BIGINT;
    v_h_enuma BIGINT;
    v_h_douglas BIGINT;
    v_h_imperium BIGINT;
    v_h_nobel BIGINT;

    -- Subject IDs
    v_sub_aerobic BIGINT;
    v_sub_stride BIGINT;
    v_sub_uphill BIGINT;
    v_sub_gate_sprint BIGINT;
    v_sub_turn_sprint BIGINT;
    v_sub_interval BIGINT;
    v_sub_cooling BIGINT;
    v_sub_trot BIGINT;
    v_sub_gate_load BIGINT;
    v_sub_trial BIGINT;

    -- Course IDs
    v_crs_foundation BIGINT;
    v_crs_sprint BIGINT;
    v_crs_endurance BIGINT;
    v_crs_gate BIGINT;
    v_crs_championship BIGINT;
    v_crs_full_season BIGINT;

    -- Iteration variables
    v_i INT;
    v_group INT;
    v_base_date DATE := CURRENT_DATE;

BEGIN
    -- 1. RESOLVE STAFF BY EMAIL (No hardcoded IDs)
    SELECT id INTO v_trainer1 FROM users WHERE email = 'trainer1@rtms.com';
    IF v_trainer1 IS NULL THEN
        RAISE EXCEPTION 'Trainer account trainer1@rtms.com not found! Ensure baseline seed is loaded.';
    END IF;

    SELECT id INTO v_groom_daniel FROM users WHERE email = 'daniel.brooks@demo.rtms.example';

    IF v_groom_daniel IS NULL THEN SELECT id INTO v_groom_daniel FROM users WHERE role_id = (SELECT id FROM roles WHERE name = 'GROOM') ORDER BY id LIMIT 1; END IF;

    -- 2. RESOLVE HORSES BY AREA CODE ('A', 'B') & UELN (registration_number)
    SELECT ARRAY_AGG(h.id ORDER BY a.code, ss.stall_number)
    INTO v_horses
    FROM horses h
    JOIN stable_stalls ss ON ss.id = h.current_stall_id
    JOIN areas a ON a.id = ss.area_id
    WHERE a.code IN ('A', 'B')
      AND h.registration_number IS NOT NULL;

    v_horse_count := COALESCE(ARRAY_LENGTH(v_horses, 1), 0);
    RAISE NOTICE 'Trainer: trainer1@rtms.com (ID %), Horses resolved in Area A & B: %', v_trainer1, v_horse_count;

    IF v_horse_count < 18 THEN
        RAISE NOTICE 'Area A & B has % horses. Attempting lookup by standard demo UELNs...', v_horse_count;
        SELECT ARRAY_AGG(id ORDER BY registration_number)
        INTO v_horses
        FROM horses
        WHERE registration_number IN (
            '380005278762021', '380005253762019', '380005301322023', '380005286802022',
            '380005280282019', '380005255802019', '380005238902018', '380005245052018',
            '380005238622018', '380005286322022', '380005245932018', '380005245662018',
            '380005254462019', '380005248392018', '380005243522018', '380005294272023',
            '380005252572019', '380005257812019'
        );
        v_horse_count := COALESCE(ARRAY_LENGTH(v_horses, 1), 0);
        RAISE NOTICE 'Horses resolved via UELN fallback: %', v_horse_count;
    END IF;

    IF v_horse_count = 0 THEN
        RAISE EXCEPTION 'No horses found in Area A/B or matching demo UELNs! Please run baseline demo seed first.';
    END IF;

    -- Resolve key individual horses by UELN
    SELECT id INTO v_h_nausicaa   FROM horses WHERE registration_number = '380005278762021'; -- A01
    SELECT id INTO v_h_eagle      FROM horses WHERE registration_number = '380005253762019'; -- A02 (Shared Lot)
    SELECT id INTO v_h_si_renesme FROM horses WHERE registration_number = '380005301322023'; -- A03
    SELECT id INTO v_h_enuma      FROM horses WHERE registration_number = '380005286802022'; -- A04 (Risk Alert)
    SELECT id INTO v_h_douglas    FROM horses WHERE registration_number = '380005280282019'; -- A05 (Upcoming)
    SELECT id INTO v_h_imperium   FROM horses WHERE registration_number = '380005255802019'; -- A06 (Upcoming)
    SELECT id INTO v_h_nobel      FROM horses WHERE registration_number = '380005238902018'; -- A07 (Cancelled)

    IF v_h_nausicaa   IS NULL THEN v_h_nausicaa := v_horses[1]; END IF;
    IF v_h_eagle      IS NULL THEN v_h_eagle := v_horses[2]; END IF;
    IF v_h_si_renesme IS NULL THEN v_h_si_renesme := v_horses[3]; END IF;
    IF v_h_enuma      IS NULL THEN v_h_enuma := v_horses[4]; END IF;
    IF v_h_douglas    IS NULL THEN v_h_douglas := v_horses[5]; END IF;
    IF v_h_imperium   IS NULL THEN v_h_imperium := v_horses[6]; END IF;
    IF v_h_nobel      IS NULL THEN v_h_nobel := v_horses[7]; END IF;

    -- 3. CLEANUP PREVIOUS TRAINING DATA IDEMPOTENTLY
    DELETE FROM horse_health_metrics WHERE notes LIKE '[DEMO-SEED]%';

    DELETE FROM training_workouts WHERE plan_id IN (
        SELECT id FROM horse_training_plans WHERE trainer_id = v_trainer1 OR horse_id = ANY(v_horses)
    );

    DELETE FROM training_lots WHERE trainer_id = v_trainer1;

    DELETE FROM horse_training_plans WHERE trainer_id = v_trainer1 OR horse_id = ANY(v_horses);

    DELETE FROM course_subjects WHERE course_id IN (
        SELECT id FROM courses WHERE trainer_id = v_trainer1 OR created_by_id = v_trainer1
    );
    DELETE FROM courses WHERE trainer_id = v_trainer1 OR created_by_id = v_trainer1;

    DELETE FROM subjects WHERE trainer_id = v_trainer1;

    -- 4. INSERT 10 SPECIALIZED SUBJECTS FOR TRAINER1
    INSERT INTO subjects (trainer_id, name, description, surface_type, target_distance_meters, intensity_level, category, duration_minutes, created_at, updated_at)
    VALUES (v_trainer1, 'Aerobic Base & Steady Canter', 'Develops cardiovascular foundation and lung capacity with steady cadence on turf.', 'TURF', 1600, 'MEDIUM', 'ENDURANCE', 60, NOW(), NOW())
    RETURNING id INTO v_sub_aerobic;

    INSERT INTO subjects (trainer_id, name, description, surface_type, target_distance_meters, intensity_level, category, duration_minutes, created_at, updated_at)
    VALUES (v_trainer1, 'Long-Distance Stride Conditioning', 'Sustained gallop to condition long-stride rhythm and muscular endurance.', 'DIRT', 2000, 'MEDIUM', 'ENDURANCE', 60, NOW(), NOW())
    RETURNING id INTO v_sub_stride;

    INSERT INTO subjects (trainer_id, name, description, surface_type, target_distance_meters, intensity_level, category, duration_minutes, created_at, updated_at)
    VALUES (v_trainer1, 'Uphill Power & Stamina Gallop', 'Incline resistance training to strengthen hindquarter drive and cardiac economy.', 'DIRT', 1600, 'HIGH', 'ENDURANCE', 60, NOW(), NOW())
    RETURNING id INTO v_sub_uphill;

    INSERT INTO subjects (trainer_id, name, description, surface_type, target_distance_meters, intensity_level, category, duration_minutes, created_at, updated_at)
    VALUES (v_trainer1, 'Starting Gate Breakout & Sprint', 'Explosive gate opening drills, rapid reaction focus, and 400m initial drive.', 'DIRT', 400, 'HIGH', 'GATE_START', 60, NOW(), NOW())
    RETURNING id INTO v_sub_gate_sprint;

    INSERT INTO subjects (trainer_id, name, description, surface_type, target_distance_meters, intensity_level, category, duration_minutes, created_at, updated_at)
    VALUES (v_trainer1, 'Turn Acceleration & Straight Sprint', 'Mastering the apex turn and unleashing rapid top-end acceleration on the home stretch.', 'SYNTHETIC', 1000, 'HIGH', 'SPEED', 60, NOW(), NOW())
    RETURNING id INTO v_sub_turn_sprint;

    INSERT INTO subjects (trainer_id, name, description, surface_type, target_distance_meters, intensity_level, category, duration_minutes, created_at, updated_at)
    VALUES (v_trainer1, 'High-Speed Furlong Interval Burst', 'Short explosive bursts with 2-minute active recovery intervals between furlongs.', 'TURF', 1200, 'HIGH', 'SPEED', 60, NOW(), NOW())
    RETURNING id INTO v_sub_interval;

    INSERT INTO subjects (trainer_id, name, description, surface_type, target_distance_meters, intensity_level, category, duration_minutes, created_at, updated_at)
    VALUES (v_trainer1, 'Post-Workout Walk & Hydro-Cooling', 'Low-impact active walking followed by cold-water hydrotherapy to accelerate lactic clearance.', 'SYNTHETIC', 800, 'LOW', 'RECOVERY', 45, NOW(), NOW())
    RETURNING id INTO v_sub_cooling;

    INSERT INTO subjects (trainer_id, name, description, surface_type, target_distance_meters, intensity_level, category, duration_minutes, created_at, updated_at)
    VALUES (v_trainer1, 'Cadence Rhythm & Trot Balance', 'Fine-tuning balance, stride symmetry, and joint mobilization.', 'TURF', 1200, 'LOW', 'RECOVERY', 60, NOW(), NOW())
    RETURNING id INTO v_sub_trot;

    INSERT INTO subjects (trainer_id, name, description, surface_type, target_distance_meters, intensity_level, category, duration_minutes, created_at, updated_at)
    VALUES (v_trainer1, 'Gate Loading & Barrier Composure', 'Desensitization to starting stall noise and pressure to keep heart rate calm before release.', 'DIRT', 200, 'LOW', 'GATE_START', 45, NOW(), NOW())
    RETURNING id INTO v_sub_gate_load;

    INSERT INTO subjects (trainer_id, name, description, surface_type, target_distance_meters, intensity_level, category, duration_minutes, created_at, updated_at)
    VALUES (v_trainer1, 'Maximum Velocity Official Trial', 'Full-course mock trial under race conditions with electronic timing and heart rate telemetries.', 'TURF', 1200, 'HIGH', 'SPEED', 60, NOW(), NOW())
    RETURNING id INTO v_sub_trial;

    -- 5. INSERT 6 COURSES FOR TRAINER1
    INSERT INTO courses (trainer_id, created_by_id, name, target_goal, total_sessions, created_at, updated_at)
    VALUES (v_trainer1, v_trainer1, 'Novice Thoroughbred Foundation', 'Establish consistent stride cadence and calm gate behavior.', 6, NOW(), NOW())
    RETURNING id INTO v_crs_foundation;

    INSERT INTO courses (trainer_id, created_by_id, name, target_goal, total_sessions, created_at, updated_at)
    VALUES (v_trainer1, v_trainer1, 'Advanced Sprint Acceleration', 'Maximize top speed and explosive acceleration under race pressure.', 6, NOW(), NOW())
    RETURNING id INTO v_crs_sprint;

    INSERT INTO courses (trainer_id, created_by_id, name, target_goal, total_sessions, created_at, updated_at)
    VALUES (v_trainer1, v_trainer1, 'Classic Cup Endurance Mastery', 'Sustain top-tier aerobic stamina through distance races.', 6, NOW(), NOW())
    RETURNING id INTO v_crs_endurance;

    INSERT INTO courses (trainer_id, created_by_id, name, target_goal, total_sessions, created_at, updated_at)
    VALUES (v_trainer1, v_trainer1, 'Barrier Discipline & Gate Mastery', 'Eliminate hesitation and achieve sub-1.5s gate reaction time.', 4, NOW(), NOW())
    RETURNING id INTO v_crs_gate;

    INSERT INTO courses (trainer_id, created_by_id, name, target_goal, total_sessions, created_at, updated_at)
    VALUES (v_trainer1, v_trainer1, 'Championship Speed & Trial Protocol', 'Peak fitness and official readiness score above 85.', 6, NOW(), NOW())
    RETURNING id INTO v_crs_championship;

    INSERT INTO courses (trainer_id, created_by_id, name, target_goal, total_sessions, created_at, updated_at)
    VALUES (v_trainer1, v_trainer1, 'Full-Season Athletic Conditioning', 'Maintain prime condition across the active racing season.', 8, NOW(), NOW())
    RETURNING id INTO v_crs_full_season;

    -- Link Subjects to Courses (course_subjects): each subject once; the course repeats the list until total_sessions
    INSERT INTO course_subjects (course_id, subject_id, order_index, created_at) VALUES
    (v_crs_foundation, v_sub_aerobic, 1, NOW()),
    (v_crs_foundation, v_sub_gate_load, 2, NOW()),
    (v_crs_foundation, v_sub_trot, 3, NOW()),
    (v_crs_foundation, v_sub_stride, 4, NOW()),
    (v_crs_foundation, v_sub_cooling, 5, NOW());

    INSERT INTO course_subjects (course_id, subject_id, order_index, created_at) VALUES
    (v_crs_sprint, v_sub_gate_sprint, 1, NOW()),
    (v_crs_sprint, v_sub_turn_sprint, 2, NOW()),
    (v_crs_sprint, v_sub_interval, 3, NOW()),
    (v_crs_sprint, v_sub_trial, 4, NOW()),
    (v_crs_sprint, v_sub_cooling, 5, NOW());

    INSERT INTO course_subjects (course_id, subject_id, order_index, created_at) VALUES
    (v_crs_endurance, v_sub_aerobic, 1, NOW()),
    (v_crs_endurance, v_sub_stride, 2, NOW()),
    (v_crs_endurance, v_sub_uphill, 3, NOW()),
    (v_crs_endurance, v_sub_cooling, 4, NOW());

    INSERT INTO course_subjects (course_id, subject_id, order_index, created_at) VALUES
    (v_crs_gate, v_sub_gate_load, 1, NOW()),
    (v_crs_gate, v_sub_gate_sprint, 2, NOW()),
    (v_crs_gate, v_sub_turn_sprint, 3, NOW());

    INSERT INTO course_subjects (course_id, subject_id, order_index, created_at) VALUES
    (v_crs_championship, v_sub_turn_sprint, 1, NOW()),
    (v_crs_championship, v_sub_interval, 2, NOW()),
    (v_crs_championship, v_sub_uphill, 3, NOW()),
    (v_crs_championship, v_sub_trial, 4, NOW()),
    (v_crs_championship, v_sub_cooling, 5, NOW());

    INSERT INTO course_subjects (course_id, subject_id, order_index, created_at) VALUES
    (v_crs_full_season, v_sub_aerobic, 1, NOW()),
    (v_crs_full_season, v_sub_stride, 2, NOW()),
    (v_crs_full_season, v_sub_gate_sprint, 3, NOW()),
    (v_crs_full_season, v_sub_uphill, 4, NOW()),
    (v_crs_full_season, v_sub_interval, 5, NOW()),
    (v_crs_full_season, v_sub_turn_sprint, 6, NOW()),
    (v_crs_full_season, v_sub_trial, 7, NOW()),
    (v_crs_full_season, v_sub_cooling, 8, NOW());

    -- 6. SEED TRAINING PLANS
    -- 16 Horses enrolled, leaving the last 2 horses (B08 & B09) NOT enrolled!
    -- =========================================================================

    -- A. COMPLETED PLANS: 16 horses in 4 groups (horse i is in group (i-1) % 4), each group on its own course and weekdays.
    -- Groups start 30..27 days ago so every plan has ended before the ACTIVE/UPCOMING plans below begin, and the high ratings
    -- (8-10) fall inside the 30-day window needed to infer SPECIALTY (>= 7.0).
    FOR v_i IN 1..LEAST(16, v_horse_count) LOOP
        v_group := (v_i - 1) % 4;
        PERFORM pg_temp.seed_plan(
            v_horses[v_i],
            CASE v_group WHEN 0 THEN v_crs_foundation WHEN 1 THEN v_crs_endurance WHEN 2 THEN v_crs_sprint ELSE v_crs_championship END,
            v_trainer1, v_groom_daniel,
            v_base_date - (30 - v_group),
            CASE v_group WHEN 0 THEN 'FRIDAY,MONDAY,WEDNESDAY' WHEN 1 THEN 'THURSDAY,TUESDAY'
                         WHEN 2 THEN 'MONDAY,THURSDAY' ELSE 'FRIDAY,TUESDAY' END,
            'COMPLETED', CASE WHEN v_i % 2 = 0 THEN 'elite' ELSE 'good' END, false);
    END LOOP;

    -- Secondary completed plans for the top 3 horses (45 days ago, historical depth)
    FOR v_i IN 1..LEAST(3, v_horse_count) LOOP
        PERFORM pg_temp.seed_plan(v_horses[v_i], v_crs_gate, v_trainer1, v_groom_daniel,
                                  v_base_date - 45, 'THURSDAY,TUESDAY', 'COMPLETED', 'good', false);
    END LOOP;

    -- B. ACTIVE PLANS (4 plans: Nausicaa, Eagle Atena, Si Renesme, Enuma)
    -- Nausicaa & Enuma take the same course and weekdays, so their sessions share a lot (they have different grooms).
    -- Si Renesme keeps her latest past session unrecorded, so the "record result" flow can be demoed.
    -- Enuma carries the fatigue risk alert.
    PERFORM pg_temp.seed_plan(v_h_nausicaa, v_crs_sprint, v_trainer1, v_groom_daniel,
                              v_base_date - 4, 'FRIDAY,MONDAY,WEDNESDAY', 'ACTIVE', 'elite', false);
    PERFORM pg_temp.seed_plan(v_h_enuma, v_crs_sprint, v_trainer1, v_groom_daniel,
                              v_base_date - 4, 'FRIDAY,MONDAY,WEDNESDAY', 'ACTIVE', 'fatigue', false);
    PERFORM pg_temp.seed_plan(v_h_eagle, v_crs_championship, v_trainer1, v_groom_daniel,
                              v_base_date - 4, 'FRIDAY,MONDAY,WEDNESDAY', 'ACTIVE', 'good', false);
    PERFORM pg_temp.seed_plan(v_h_si_renesme, v_crs_endurance, v_trainer1, v_groom_daniel,
                              v_base_date - 4, 'FRIDAY,MONDAY,WEDNESDAY', 'ACTIVE', 'steady', true);

    -- C. UPCOMING PLANS (2 plans: Douglas & Imperium)
    PERFORM pg_temp.seed_plan(v_h_douglas, v_crs_foundation, v_trainer1, v_groom_daniel,
                              v_base_date + 5, 'THURSDAY,TUESDAY', 'UPCOMING', 'good', false);
    PERFORM pg_temp.seed_plan(v_h_imperium, v_crs_endurance, v_trainer1, v_groom_daniel,
                              v_base_date + 7, 'FRIDAY,MONDAY,WEDNESDAY', 'UPCOMING', 'good', false);

    -- D. CANCELLED PLAN (1 plan: AG Nobel Emir): every session and lot is cancelled
    PERFORM pg_temp.seed_plan(v_h_nobel, v_crs_sprint, v_trainer1, v_groom_daniel,
                              v_base_date - 3, 'THURSDAY,TUESDAY', 'CANCELLED', 'good', false);

    RAISE NOTICE 'TRAINING DEMO SEED COMPLETED SUCCESSFULLY FOR TRAINER ID %!', v_trainer1;
END $$;
