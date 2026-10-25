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
-- =====================================================================

DO $$
DECLARE
    -- Trainer & Staff IDs (resolved via email)
    v_trainer1 BIGINT;
    v_groom_daniel BIGINT;
    v_groom_olivia BIGINT;
    v_groom_alex BIGINT;
    v_groom_chris BIGINT;
    v_groom_david BIGINT;
    v_groom_emma BIGINT;

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
    v_plan_id BIGINT;
    v_lot_id BIGINT;
    v_lot_id2 BIGINT;
    v_lot_id3 BIGINT;
    v_lot_date DATE;
    v_horse_id BIGINT;
    v_groom_id BIGINT;
    v_i INT;
    v_base_date DATE := CURRENT_DATE;

BEGIN
    -- 1. RESOLVE STAFF BY EMAIL (No hardcoded IDs)
    SELECT id INTO v_trainer1 FROM users WHERE email = 'trainer1@rtms.com';
    IF v_trainer1 IS NULL THEN
        RAISE EXCEPTION 'Trainer account trainer1@rtms.com not found! Ensure baseline seed is loaded.';
    END IF;

    SELECT id INTO v_groom_daniel FROM users WHERE email = 'daniel.brooks@demo.rtms.example';
    SELECT id INTO v_groom_olivia FROM users WHERE email = 'olivia.parker@demo.rtms.example';
    SELECT id INTO v_groom_alex FROM users WHERE email = 'alex.carter@demo.rtms.example';
    SELECT id INTO v_groom_chris FROM users WHERE email = 'chris.allen@demo.rtms.example';
    SELECT id INTO v_groom_david FROM users WHERE email = 'david.moore@demo.rtms.example';
    SELECT id INTO v_groom_emma FROM users WHERE email = 'emma.reed@demo.rtms.example';

    IF v_groom_daniel IS NULL THEN SELECT id INTO v_groom_daniel FROM users WHERE role_id = (SELECT id FROM roles WHERE name = 'GROOM') ORDER BY id LIMIT 1; END IF;
    IF v_groom_olivia IS NULL THEN v_groom_olivia := v_groom_daniel; END IF;
    IF v_groom_alex IS NULL THEN v_groom_alex := v_groom_daniel; END IF;
    IF v_groom_chris IS NULL THEN v_groom_chris := v_groom_daniel; END IF;
    IF v_groom_david IS NULL THEN v_groom_david := v_groom_daniel; END IF;
    IF v_groom_emma IS NULL THEN v_groom_emma := v_groom_daniel; END IF;

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

    -- A. COMPLETED PLANS (Hoan thanh trong vong 8 - 22 ngay truoc, nam tron trong 30 ngay!)
    -- Horses 1 to 16 each receive 1 completed plan with HIGH performance ratings (8, 9, 10)
    -- để đủ điều kiện suy ra SPECIALTY (>= 7.0)!
    FOR v_i IN 1..LEAST(16, v_horse_count) LOOP
        v_horse_id := v_horses[v_i];
        v_groom_id := CASE (v_i % 6)
            WHEN 1 THEN v_groom_daniel
            WHEN 2 THEN v_groom_olivia
            WHEN 3 THEN v_groom_alex
            WHEN 4 THEN v_groom_chris
            WHEN 5 THEN v_groom_david
            ELSE v_groom_emma
        END;

        INSERT INTO horse_training_plans (
            horse_id, course_id, trainer_id, start_date, end_date, status,
            created_at, updated_at, training_days, groom_id
        ) VALUES (
            v_horse_id,
            CASE (v_i % 3) WHEN 1 THEN v_crs_foundation WHEN 2 THEN v_crs_endurance ELSE v_crs_sprint END,
            v_trainer1,
            v_base_date - INTERVAL '22 days',
            v_base_date - INTERVAL '9 days',
            'COMPLETED',
            v_base_date - INTERVAL '25 days',
            v_base_date - INTERVAL '9 days',
            'FRIDAY,MONDAY,WEDNESDAY',
            v_groom_id
        ) RETURNING id INTO v_plan_id;

        FOR k IN 0..5 LOOP
            v_lot_date := (v_base_date - INTERVAL '21 days')::DATE + (k * 2);

            SELECT id INTO v_lot_id FROM training_lots
            WHERE trainer_id = v_trainer1 AND lot_date = v_lot_date AND start_time = '06:00:00';

            IF v_lot_id IS NULL THEN
                INSERT INTO training_lots (trainer_id, subject_id, lot_date, start_time, end_time, max_capacity, status, created_at, updated_at)
                VALUES (
                    v_trainer1, 
                    CASE (v_i % 3) WHEN 0 THEN v_sub_interval WHEN 1 THEN v_sub_aerobic ELSE v_sub_stride END,
                    v_lot_date, '06:00:00', '07:00:00', 6, 'COMPLETED', NOW(), NOW()
                ) RETURNING id INTO v_lot_id;
            END IF;

            -- High performance rating: 8, 9, 10 so average rating is ~8.5 (>= 7.0 to trigger Specialty!)
            INSERT INTO training_workouts (
                plan_id, horse_id, assigned_to_id, status, actual_distance_meters, actual_duration_minutes,
                top_speed_kmh, average_speed_kmh, average_heart_rate, max_heart_rate, recovery_heart_rate,
                performance_rating, trainer_feedback, created_at, updated_at, lot_id
            ) VALUES (
                v_plan_id, v_horse_id, v_groom_id, 'COMPLETED',
                CASE (v_i % 3) WHEN 0 THEN 1200 WHEN 1 THEN 1600 ELSE 2000 END,
                56, 
                61.5 + (k * 0.7), 53.0 + (k * 0.5), 142 + (k * 2), 175 + (k * 2), 85 - k,
                CASE WHEN k >= 4 THEN 9 WHEN k >= 2 THEN 8 ELSE 8 END,
                'Outstanding burst and recovery rhythm. Stride efficiency at competition standard.',
                v_lot_date + TIME '07:10:00', v_lot_date + TIME '07:10:00', v_lot_id
            );

            INSERT INTO horse_health_metrics (
                horse_id, recorded_at, heart_rate, temperature, weight, respiratory_rate,
                hydration_status, body_condition_score, notes, created_at
            ) VALUES (
                v_horse_id, v_lot_date + TIME '07:30:00',
                37.5 + (k * 0.3), 38.0, 490.0 + (k * 1.0), 14,
                'OPTIMAL', 6.0, '[DEMO-SEED] Post-workout recovery check.', NOW()
            );
        END LOOP;
    END LOOP;

    -- Secondary completed plans for Top 3 horses (completed 40 days ago, ensuring deep historical depth)
    FOR v_i IN 1..LEAST(3, v_horse_count) LOOP
        v_horse_id := v_horses[v_i];
        INSERT INTO horse_training_plans (
            horse_id, course_id, trainer_id, start_date, end_date, status,
            created_at, updated_at, training_days, groom_id
        ) VALUES (
            v_horse_id, v_crs_gate, v_trainer1,
            v_base_date - INTERVAL '45 days', v_base_date - INTERVAL '37 days',
            'COMPLETED',
            v_base_date - INTERVAL '48 days', v_base_date - INTERVAL '37 days',
            'THURSDAY,TUESDAY',
            v_groom_daniel
        ) RETURNING id INTO v_plan_id;

        FOR k IN 0..3 LOOP
            v_lot_date := (v_base_date - INTERVAL '45 days')::DATE + (k * 2);
            SELECT id INTO v_lot_id FROM training_lots
            WHERE trainer_id = v_trainer1 AND lot_date = v_lot_date AND start_time = '07:00:00';

            IF v_lot_id IS NULL THEN
                INSERT INTO training_lots (trainer_id, subject_id, lot_date, start_time, end_time, max_capacity, status, created_at, updated_at)
                VALUES (v_trainer1, v_sub_gate_sprint, v_lot_date, '07:00:00', '08:00:00', 4, 'COMPLETED', NOW(), NOW())
                RETURNING id INTO v_lot_id;
            END IF;

            INSERT INTO training_workouts (
                plan_id, horse_id, assigned_to_id, status, actual_distance_meters, actual_duration_minutes,
                top_speed_kmh, average_speed_kmh, average_heart_rate, max_heart_rate, recovery_heart_rate,
                performance_rating, trainer_feedback, created_at, updated_at, lot_id
            ) VALUES (
                v_plan_id, v_horse_id, v_groom_daniel, 'COMPLETED',
                400, 40, 59.0, 48.5, 138, 168, 85,
                8, 'Sub-1.5s barrier breakout achieved.',
                v_lot_date + TIME '08:10:00', v_lot_date + TIME '08:10:00', v_lot_id
            );
        END LOOP;
    END LOOP;

    -- B. ACTIVE PLANS (4 plans: Nausicaa, Eagle Atena, Si Renesme, Enuma)
    -- Group Lot: Nausicaa & Eagle Atena paired in Slot 1 (06:00 - 07:00)
    -- Si Renesme in Slot 2 (07:00 - 08:00)
    -- Enuma in Slot 3 (08:00 - 09:00) with Fatigue Risk Alert
    -- ZERO OVERLAPS!
    -- =========================================================================
    DECLARE
        v_plan_id_h1 BIGINT;
        v_plan_id_h2 BIGINT;
        v_plan_id_h3 BIGINT;
        v_plan_id_h4 BIGINT;
    BEGIN
        INSERT INTO horse_training_plans (
            horse_id, course_id, trainer_id, start_date, end_date, status,
            created_at, updated_at, training_days, groom_id
        ) VALUES (
            v_h_nausicaa, v_crs_sprint, v_trainer1,
            v_base_date - INTERVAL '7 days', v_base_date + INTERVAL '14 days',
            'ACTIVE',
            v_base_date - INTERVAL '10 days', v_base_date,
            'FRIDAY,MONDAY,WEDNESDAY', v_groom_daniel
        ) RETURNING id INTO v_plan_id_h1;

        INSERT INTO horse_training_plans (
            horse_id, course_id, trainer_id, start_date, end_date, status,
            created_at, updated_at, training_days, groom_id
        ) VALUES (
            v_h_eagle, v_crs_sprint, v_trainer1,
            v_base_date - INTERVAL '7 days', v_base_date + INTERVAL '14 days',
            'ACTIVE',
            v_base_date - INTERVAL '10 days', v_base_date,
            'FRIDAY,MONDAY,WEDNESDAY', v_groom_daniel
        ) RETURNING id INTO v_plan_id_h2;

        INSERT INTO horse_training_plans (
            horse_id, course_id, trainer_id, start_date, end_date, status,
            created_at, updated_at, training_days, groom_id
        ) VALUES (
            v_h_si_renesme, v_crs_endurance, v_trainer1,
            v_base_date - INTERVAL '5 days', v_base_date + INTERVAL '16 days',
            'ACTIVE',
            v_base_date - INTERVAL '8 days', v_base_date,
            'FRIDAY,MONDAY,WEDNESDAY', v_groom_olivia
        ) RETURNING id INTO v_plan_id_h3;

        INSERT INTO horse_training_plans (
            horse_id, course_id, trainer_id, start_date, end_date, status,
            created_at, updated_at, training_days, groom_id
        ) VALUES (
            v_h_enuma, v_crs_championship, v_trainer1,
            v_base_date - INTERVAL '6 days', v_base_date + INTERVAL '15 days',
            'ACTIVE',
            v_base_date - INTERVAL '9 days', v_base_date,
            'FRIDAY,MONDAY,WEDNESDAY', v_groom_olivia
        ) RETURNING id INTO v_plan_id_h4;

        FOR k IN 0..5 LOOP
            v_lot_date := (v_base_date - INTERVAL '4 days')::DATE + (k * 2);

            -- Slot 1: Shared Lot for Nausicaa & Eagle Atena
            INSERT INTO training_lots (
                trainer_id, subject_id, lot_date, start_time, end_time, max_capacity, status, created_at, updated_at
            ) VALUES (
                v_trainer1, v_sub_turn_sprint, v_lot_date, '06:00:00', '07:00:00', 4,
                CASE WHEN v_lot_date < v_base_date THEN 'COMPLETED' ELSE 'SCHEDULED' END,
                NOW(), NOW()
            ) RETURNING id INTO v_lot_id;

            -- Slot 2: Lot for Si Renesme
            INSERT INTO training_lots (
                trainer_id, subject_id, lot_date, start_time, end_time, max_capacity, status, created_at, updated_at
            ) VALUES (
                v_trainer1, v_sub_stride, v_lot_date, '07:00:00', '08:00:00', 4,
                CASE WHEN v_lot_date < v_base_date THEN 'COMPLETED' ELSE 'SCHEDULED' END,
                NOW(), NOW()
            ) RETURNING id INTO v_lot_id2;

            -- Slot 3: Lot for Enuma (Fatigue Risk Alert)
            INSERT INTO training_lots (
                trainer_id, subject_id, lot_date, start_time, end_time, max_capacity, status, created_at, updated_at
            ) VALUES (
                v_trainer1, v_sub_uphill, v_lot_date, '08:00:00', '09:00:00', 4,
                CASE WHEN v_lot_date < v_base_date THEN 'COMPLETED' ELSE 'SCHEDULED' END,
                NOW(), NOW()
            ) RETURNING id INTO v_lot_id3;

            -- Workouts for Nausicaa (Rating 9/10!)
            INSERT INTO training_workouts (
                plan_id, horse_id, assigned_to_id, status, actual_distance_meters, actual_duration_minutes,
                top_speed_kmh, average_speed_kmh, average_heart_rate, max_heart_rate, recovery_heart_rate,
                performance_rating, trainer_feedback, created_at, updated_at, lot_id
            ) VALUES (
                v_plan_id_h1, v_h_nausicaa, v_groom_daniel,
                CASE WHEN v_lot_date < v_base_date THEN 'COMPLETED' ELSE 'SCHEDULED' END,
                CASE WHEN v_lot_date < v_base_date THEN 1000 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 55 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 64.4 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 55.1 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 145 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 178 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 88 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 9 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 'Exceptional bend exit and explosive acceleration.' ELSE NULL END,
                NOW(), NOW(), v_lot_id
            );

            -- Workouts for Eagle Atena (Rating 8/10!)
            INSERT INTO training_workouts (
                plan_id, horse_id, assigned_to_id, status, actual_distance_meters, actual_duration_minutes,
                top_speed_kmh, average_speed_kmh, average_heart_rate, max_heart_rate, recovery_heart_rate,
                performance_rating, trainer_feedback, created_at, updated_at, lot_id
            ) VALUES (
                v_plan_id_h2, v_h_eagle, v_groom_daniel,
                CASE WHEN v_lot_date < v_base_date THEN 'COMPLETED' ELSE 'SCHEDULED' END,
                CASE WHEN v_lot_date < v_base_date THEN 1000 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 56 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 63.8 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 54.5 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 148 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 180 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 90 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 8 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 'Kept up very well with Nausicaa on turn 3.' ELSE NULL END,
                NOW(), NOW(), v_lot_id
            );

            -- Workouts for Si Renesme (Rating 8/10!)
            INSERT INTO training_workouts (
                plan_id, horse_id, assigned_to_id, status, actual_distance_meters, actual_duration_minutes,
                top_speed_kmh, average_speed_kmh, average_heart_rate, max_heart_rate, recovery_heart_rate,
                performance_rating, trainer_feedback, created_at, updated_at, lot_id
            ) VALUES (
                v_plan_id_h3, v_h_si_renesme, v_groom_olivia,
                CASE WHEN v_lot_date < v_base_date THEN 'COMPLETED' ELSE 'SCHEDULED' END,
                CASE WHEN v_lot_date < v_base_date THEN 2000 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 60 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 58.0 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 49.8 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 140 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 170 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 88 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 8 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 'Steady cadence throughout 2000m.' ELSE NULL END,
                NOW(), NOW(), v_lot_id2
            );

            -- Workouts for Enuma (Fatigue Risk Alert: rating 3/10)
            INSERT INTO training_workouts (
                plan_id, horse_id, assigned_to_id, status, actual_distance_meters, actual_duration_minutes,
                top_speed_kmh, average_speed_kmh, average_heart_rate, max_heart_rate, recovery_heart_rate,
                performance_rating, trainer_feedback, created_at, updated_at, lot_id
            ) VALUES (
                v_plan_id_h4, v_h_enuma, v_groom_olivia,
                CASE WHEN v_lot_date < v_base_date THEN 'COMPLETED' ELSE 'SCHEDULED' END,
                CASE WHEN v_lot_date < v_base_date THEN 1600 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 65 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 54.2 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 45.0 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 162 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 198 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 124 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 3 ELSE NULL END,
                CASE WHEN v_lot_date < v_base_date THEN 'Showed sign of hind leg fatigue on final incline. Advised vet evaluation.' ELSE NULL END,
                NOW(), NOW(), v_lot_id3
            );

            IF v_lot_date < v_base_date THEN
                INSERT INTO horse_health_metrics (
                    horse_id, recorded_at, heart_rate, temperature, weight, respiratory_rate,
                    hydration_status, body_condition_score, notes, created_at
                ) VALUES (
                    v_h_enuma, v_lot_date + TIME '09:45:00',
                    46.0, 38.6, 492.0, 22,
                    'MILD_DEHYDRATION', 5.0,
                    '[DEMO-SEED] High recovery heart rate (124 bpm) observed post-uphill gallop. Potential fatigue alert.',
                    NOW()
                );
            END IF;

        END LOOP;
    END;

    -- C. UPCOMING PLANS (2 plans: Douglas & Imperium)
    -- =========================================================================
    INSERT INTO horse_training_plans (
        horse_id, course_id, trainer_id, start_date, end_date, status,
        created_at, updated_at, training_days, groom_id
    ) VALUES (
        v_h_douglas, v_crs_foundation, v_trainer1,
        v_base_date + INTERVAL '5 days', v_base_date + INTERVAL '26 days',
        'UPCOMING',
        NOW(), NOW(), 'THURSDAY,TUESDAY', v_groom_alex
    ) RETURNING id INTO v_plan_id;

    FOR k IN 0..3 LOOP
        v_lot_date := (v_base_date + INTERVAL '5 days')::DATE + (k * 2);
        SELECT id INTO v_lot_id FROM training_lots
        WHERE trainer_id = v_trainer1 AND lot_date = v_lot_date AND start_time = '06:00:00';

        IF v_lot_id IS NULL THEN
            INSERT INTO training_lots (trainer_id, subject_id, lot_date, start_time, end_time, max_capacity, status, created_at, updated_at)
            VALUES (v_trainer1, v_sub_aerobic, v_lot_date, '06:00:00', '07:00:00', 6, 'SCHEDULED', NOW(), NOW())
            RETURNING id INTO v_lot_id;
        END IF;

        INSERT INTO training_workouts (
            plan_id, horse_id, assigned_to_id, status, created_at, updated_at, lot_id
        ) VALUES (
            v_plan_id, v_h_douglas, v_groom_alex, 'SCHEDULED', NOW(), NOW(), v_lot_id
        );
    END LOOP;

    INSERT INTO horse_training_plans (
        horse_id, course_id, trainer_id, start_date, end_date, status,
        created_at, updated_at, training_days, groom_id
    ) VALUES (
        v_h_imperium, v_crs_endurance, v_trainer1,
        v_base_date + INTERVAL '7 days', v_base_date + INTERVAL '28 days',
        'UPCOMING',
        NOW(), NOW(), 'FRIDAY,MONDAY,WEDNESDAY', v_groom_alex
    ) RETURNING id INTO v_plan_id;

    FOR k IN 0..3 LOOP
        v_lot_date := (v_base_date + INTERVAL '7 days')::DATE + (k * 2);
        SELECT id INTO v_lot_id FROM training_lots
        WHERE trainer_id = v_trainer1 AND lot_date = v_lot_date AND start_time = '07:00:00';

        IF v_lot_id IS NULL THEN
            INSERT INTO training_lots (trainer_id, subject_id, lot_date, start_time, end_time, max_capacity, status, created_at, updated_at)
            VALUES (v_trainer1, v_sub_stride, v_lot_date, '07:00:00', '08:00:00', 4, 'SCHEDULED', NOW(), NOW())
            RETURNING id INTO v_lot_id;
        END IF;

        INSERT INTO training_workouts (
            plan_id, horse_id, assigned_to_id, status, created_at, updated_at, lot_id
        ) VALUES (
            v_plan_id, v_h_imperium, v_groom_alex, 'SCHEDULED', NOW(), NOW(), v_lot_id
        );
    END LOOP;

    -- D. CANCELLED PLAN (1 plan: AG Nobel Emir)
    -- =========================================================================
    INSERT INTO horse_training_plans (
        horse_id, course_id, trainer_id, start_date, end_date, status,
        created_at, updated_at, training_days, groom_id
    ) VALUES (
        v_h_nobel, v_crs_sprint, v_trainer1,
        v_base_date - INTERVAL '15 days', v_base_date + INTERVAL '5 days',
        'CANCELLED',
        v_base_date - INTERVAL '18 days', v_base_date - INTERVAL '10 days',
        'THURSDAY,TUESDAY', v_groom_alex
    ) RETURNING id INTO v_plan_id;

    INSERT INTO training_lots (
        trainer_id, subject_id, lot_date, start_time, end_time, max_capacity, status, created_at, updated_at
    ) VALUES (
        v_trainer1, v_sub_gate_sprint, (v_base_date - INTERVAL '14 days')::DATE, '08:00:00', '09:00:00', 4,
        'CANCELLED', NOW(), NOW()
    ) RETURNING id INTO v_lot_id;

    INSERT INTO training_workouts (
        plan_id, horse_id, assigned_to_id, status, trainer_feedback, created_at, updated_at, lot_id
    ) VALUES (
        v_plan_id, v_h_nobel, v_groom_alex, 'CANCELLED', 'Session cancelled before commencement.', NOW(), NOW(), v_lot_id
    );

    RAISE NOTICE 'TRAINING DEMO SEED COMPLETED SUCCESSFULLY FOR TRAINER ID %!', v_trainer1;
END $$;
