-- =====================================================================
-- 50_training.sql — bài tập, khóa học, Training Group, Group Plan, Session và Workout của 3 Head Trainer (~13 tuần + sắp tới).
-- (Subjects, courses, training groups, group plans, sessions and workouts of the 3 head trainers: ~13 weeks of history plus upcoming.)
--
-- Luật đang giữ (TrainingGroupService / GroupPlanService / SessionSettlementService, migration V202610311000):
--   - Nhóm tối đa 6 ngựa, mỗi ngựa một nhóm, không hai ngựa cùng Groom trong một nhóm (BR-10, BR-09); ngựa ELIGIBLE, không bị
--     chặn tập, trong khu của Trainer. Ngựa đang bị chặn tập không nằm trong nhóm nào.
--   - Các Group Plan chưa đóng của một nhóm không chồng ngày; mỗi plan có đúng total_sessions buổi, xoay vòng bài của khóa,
--     chỉ vào các thứ đã chọn.
--   - Session trong khung 06:00-10:00, bắt đầu :00/:15/:30/:45, không chồng Session khác của Trainer, xếp first-fit theo ngày.
--   - Trạng thái theo ĐỒNG HỒ: Session hết giờ là HELD (kể cả chưa ghi kết quả), plan đóng khi không còn Session chờ chạy
--     (COMPLETED nếu có Session HELD, ngược lại CANCELLED). Hủy chỉ trước giờ bắt đầu.
--   - Ngựa bị chặn tập ra khỏi nhóm: Workout của buổi chưa bắt đầu bị hủy.
--   - Buổi đã ghi kết quả có đủ 7 chỉ số; điểm phong độ tính đúng công thức WorkoutPerformanceCalculator.
-- Kịch bản demo có sẵn: Session đã HELD còn thiếu kết quả (nhãn "Cần ghi kết quả"), ngựa vào giữa khóa, ngựa bị bỏ riêng một buổi,
-- ngựa "không chạy", Session bị hủy vì mưa, plan bị hủy giữa chừng, plan xếp hàng chờ sau plan đang chạy.
-- (Rules kept: group of at most 6 with one horse per groom and one group per horse, no overlapping unfinished plans per group,
-- Sessions on quarter hours inside the golden window placed first-fit per day, statuses follow the clock, blocking removes a horse
-- from its group and cancels what has not started. Demo scenarios: a Held Session missing results, a mid-course joiner, one horse
-- cancelled for a single Session, a horse that did not run, a rainy Session cancelled, a plan dropped part-way, a queued plan.)
-- =====================================================================

CREATE TEMP TABLE seed_plan_cancel (plan_id BIGINT PRIMARY KEY);

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
    -- Nhịp tối đa còn xa 220; hồi phục 60-98 bpm theo năng lực (dưới ngưỡng 100). Chỉ buổi mệt mới vượt ngưỡng hồi phục,
    -- để cảnh báo trên màn hình là trường hợp có chủ đích. (Max HR stays well under 220; recovery is 60-98 bpm by
    -- ability, under the 100 limit. Only tiring sessions break the recovery limit, so on-screen alerts are deliberate.)
    hr_max := LEAST(220, hr + pg_temp.rndi(p_seed || 'm', 8, 18));
    hr_rec := CASE WHEN p_fatigue THEN LEAST(hr - 9, pg_temp.rndi(p_seed || 'f', 118, 130))
                   ELSE round(60 + 38 * (1 - a) * pg_temp.rnd(p_seed || 'r', 0.9, 1.0))::INT END;
    rating := pg_temp.rating(dist, dur, top, avg, hr, hr_max, hr_rec);
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


-- Khe giờ đầu tiên còn trống trong khung giờ vàng cho một buổi dài p_minutes phút (first-fit, mốc 15 phút).
-- (The first free slot of the golden window for a Session of p_minutes minutes: first-fit, 15-minute steps.)
CREATE FUNCTION pg_temp.place_session(p_trainer BIGINT, p_date DATE, p_minutes INT) RETURNS TIME AS $$
DECLARE
    v_start TIME := TIME '06:00';
    v_end TIME;
BEGIN
    LOOP
        v_end := v_start + make_interval(mins => p_minutes);
        IF v_end > TIME '10:00' OR v_end < v_start THEN
            RAISE EXCEPTION 'Schedule full on % for trainer % (too many sessions that day; move a group to other weekdays)',
                p_date, p_trainer;
        END IF;
        EXIT WHEN NOT EXISTS (SELECT 1 FROM training_sessions o
                               WHERE o.trainer_id = p_trainer AND o.session_date = p_date AND o.status <> 'CANCELLED'
                                 AND o.start_time < v_end AND o.end_time > v_start);
        v_start := v_start + INTERVAL '15 minutes';
    END LOOP;
    RETURN v_start;
END $$ LANGUAGE plpgsql;

-- Chia các ngựa đã cho thành các nhóm tối đa 6 ngựa, không hai ngựa cùng Groom trong một nhóm. Ngựa đang bị chặn tập bỏ qua.
-- (Splits the horses into groups of at most 6 with no two horses of one groom. Horses blocked right now are skipped.)
CREATE FUNCTION pg_temp.make_groups(p_trainer TEXT, p_prefix TEXT, p_horses TEXT[]) RETURNS BIGINT[] AS $$
DECLARE
    v_trainer BIGINT := pg_temp.uid(p_trainer);
    v_groups BIGINT[] := '{}';
    v_created TIMESTAMP := pg_temp.at(-92, TIME '09:00');
    v_k TEXT; v_horse BIGINT; v_groom BIGINT; v_group BIGINT; v_placed BOOLEAN;
BEGIN
    FOREACH v_k IN ARRAY p_horses LOOP
        v_horse := pg_temp.hid(v_k);
        IF (SELECT training_decision FROM horses WHERE id = v_horse) = 'BLOCKED' THEN
            RAISE NOTICE 'Horse % is resting, so it is in no group', v_k;
            CONTINUE;
        END IF;
        SELECT ss.groom_id INTO v_groom FROM horses h JOIN stable_stalls ss ON ss.id = h.current_stall_id WHERE h.id = v_horse;
        v_placed := FALSE;
        IF array_length(v_groups, 1) IS NOT NULL THEN
            FOREACH v_group IN ARRAY v_groups LOOP
                IF (SELECT count(*) FROM training_group_members WHERE group_id = v_group) < 6
                   AND NOT EXISTS (SELECT 1 FROM training_group_members m WHERE m.group_id = v_group AND m.groom_id = v_groom) THEN
                    INSERT INTO training_group_members (group_id, horse_id, groom_id, joined_at) VALUES (v_group, v_horse, v_groom, v_created);
                    v_placed := TRUE;
                    EXIT;
                END IF;
            END LOOP;
        END IF;
        IF NOT v_placed THEN
            INSERT INTO training_groups (trainer_id, name, created_at, updated_at)
            VALUES (v_trainer, p_prefix || ' ' || (COALESCE(array_length(v_groups, 1), 0) + 1), v_created, v_created)
            RETURNING id INTO v_group;
            v_groups := v_groups || v_group;
            INSERT INTO training_group_members (group_id, horse_id, groom_id, joined_at) VALUES (v_group, v_horse, v_groom, v_created);
            PERFORM pg_temp.audit(v_trainer, 'Create Training Group', 'TrainingGroup', v_group,
                                  'Trainer created a new training group', v_created, 'POST', '/api/training-groups');
        END IF;
    END LOOP;
    RETURN v_groups;
END $$ LANGUAGE plpgsql;

-- Ghi một Workout kèm kết quả (hoặc không). (Writes one Workout, with a result or without.)
CREATE FUNCTION pg_temp.put_workout(p_session BIGINT, p_plan BIGINT, p_horse BIGINT, p_groom BIGINT, p_status TEXT,
                                    p_recorded BOOLEAN, p_ability NUMERIC, p_fatigue BOOLEAN, p_created TIMESTAMP,
                                    p_updated TIMESTAMP) RETURNS VOID AS $$
DECLARE
    s training_sessions;
    r RECORD;
BEGIN
    SELECT * INTO s FROM training_sessions WHERE id = p_session;
    IF p_recorded THEN
        r := pg_temp.workout_result(p_plan || '-' || p_horse || '-' || p_session, s.subject_id, p_ability, p_fatigue);
        INSERT INTO training_workouts (session_id, horse_id, assigned_to_id, status, actual_distance_meters, actual_duration_minutes,
                                       top_speed_kmh, average_speed_kmh, average_heart_rate, max_heart_rate, recovery_heart_rate,
                                       performance_rating, trainer_feedback, created_at, updated_at)
        VALUES (p_session, p_horse, p_groom, p_status, r.dist, r.dur, r.top, r.avg, r.hr, r.hr_max, r.hr_rec, r.rating,
                CASE WHEN r.rating >= 8 THEN 'Strong, even rhythm; recovered quickly after the final furlong.'
                     WHEN r.rating >= 6 THEN 'Solid session; pace dropped slightly in the second half.'
                     WHEN r.rating >= 4 THEN 'Laboured in the last part; heart rate slow to come down.'
                     ELSE 'Tired early and slow to recover. Rest day advised; ask the vet if it continues.' END,
                p_created, p_updated);
    ELSE
        INSERT INTO training_workouts (session_id, horse_id, assigned_to_id, status, created_at, updated_at)
        VALUES (p_session, p_horse, p_groom, p_status, p_created, p_updated);
    END IF;
END $$ LANGUAGE plpgsql;

-- Một khóa giao cho một nhóm (GroupPlanService.createPlan): các buổi xếp first-fit, mỗi thành viên có một Workout mỗi buổi.
-- Trạng thái suy ra từ đồng hồ và các khoảng nghỉ trong seed_block. Chỉ buổi của các ngày TRƯỚC hôm nay mới có kết quả; buổi
-- đã hết giờ hôm nay luôn chưa ghi để demo việc ghi kết quả. p_unrecorded: buổi đã qua gần nhất cũng để trống kết quả.
-- p_exclude: ngựa chưa vào nhóm lúc tạo plan (vào giữa khóa bằng pg_temp.join_plan).
-- (One course assigned to a group. Sessions are placed first-fit and every member gets a Workout per Session. Statuses follow the
-- clock and the rest windows in seed_block. Only Sessions before today have results; Sessions that ended today are left to
-- record. p_unrecorded leaves the latest past Session without results too. p_exclude: horses not yet in the group when the plan
-- was created (they join mid-course with pg_temp.join_plan).)
CREATE FUNCTION pg_temp.make_group_plan(p_group BIGINT, p_course BIGINT, p_start DATE, p_days TEXT,
                                        p_unrecorded BOOLEAN DEFAULT FALSE, p_exclude BIGINT[] DEFAULT '{}',
                                        p_fatigue BIGINT DEFAULT NULL) RETURNS BIGINT AS $$
DECLARE
    v_trainer BIGINT; v_total INT; v_subjects BIGINT[]; v_dates DATE[]; v_created TIMESTAMP;
    v_plan BIGINT; v_subject BIGINT; v_minutes INT; v_start TIME; v_end TIME; v_last_past INT := 0;
    s RECORD; m RECORD; v_block TIMESTAMP; v_approved TIMESTAMP; v_ability NUMERIC; v_end_ts TIMESTAMP;
    v_rec TIMESTAMP; v_status TEXT; v_recorded BOOLEAN;
BEGIN
    SELECT trainer_id INTO v_trainer FROM training_groups WHERE id = p_group;
    SELECT total_sessions INTO v_total FROM courses WHERE id = p_course;
    IF (SELECT trainer_id FROM courses WHERE id = p_course) <> v_trainer THEN
        RAISE EXCEPTION 'Course % does not belong to the trainer of group %', p_course, p_group;
    END IF;
    SELECT array_agg(subject_id ORDER BY order_index) INTO v_subjects FROM course_subjects WHERE course_id = p_course;
    v_dates := pg_temp.session_dates(p_start, p_days, v_total);
    v_created := LEAST((p_start - 2) + TIME '16:00', pg_temp.clock() - INTERVAL '2 hours');

    -- Chèn với trạng thái đã đóng để ràng buộc loại trừ chỉ xét plan chưa đóng khi trạng thái cuối được đặt.
    -- (Inserted as closed so the exclusion constraint only looks at unfinished plans once the final status is set.)
    INSERT INTO group_plans (group_id, course_id, trainer_id, start_date, end_date, training_days, status, created_at, updated_at)
    VALUES (p_group, p_course, v_trainer, v_dates[1], v_dates[v_total], pg_temp.sorted_days(p_days), 'COMPLETED', v_created, v_created)
    RETURNING id INTO v_plan;

    FOR i IN 1..v_total LOOP
        v_subject := v_subjects[((i - 1) % array_length(v_subjects, 1)) + 1];
        SELECT duration_minutes INTO v_minutes FROM subjects WHERE id = v_subject;
        v_start := pg_temp.place_session(v_trainer, v_dates[i], v_minutes);
        v_end := v_start + make_interval(mins => v_minutes);
        INSERT INTO training_sessions (group_plan_id, subject_id, trainer_id, session_date, start_time, end_time, status, created_at, updated_at)
        VALUES (v_plan, v_subject, v_trainer, v_dates[i], v_start, v_end,
                CASE WHEN v_dates[i] + v_end <= pg_temp.clock() THEN 'HELD' ELSE 'SCHEDULED' END, v_created,
                CASE WHEN v_dates[i] + v_end <= pg_temp.clock() THEN v_dates[i] + v_end ELSE v_created END);
    END LOOP;

    -- v_last_past counts only Sessions on days before today.
    SELECT COALESCE(max(idx), 0) INTO v_last_past FROM (
        SELECT row_number() OVER (ORDER BY session_date, start_time) idx, status, session_date
          FROM training_sessions WHERE group_plan_id = v_plan) x
     WHERE x.status = 'HELD' AND x.session_date < pg_temp.today();

    FOR m IN SELECT hm.horse_id, hm.groom_id FROM training_group_members hm
              WHERE hm.group_id = p_group AND NOT (hm.horse_id = ANY (p_exclude)) ORDER BY hm.id LOOP
        SELECT manager_reviewed_at INTO v_approved FROM admission_applications WHERE horse_id = m.horse_id AND status = 'APPROVED';
        IF v_approved IS NULL OR v_created < v_approved OR pg_temp.blocked_at(m.horse_id, v_created) THEN
            CONTINUE;      -- not admitted yet, or resting when the plan was made: not part of this plan
        END IF;
        SELECT min(from_ts) INTO v_block FROM seed_block WHERE horse_id = m.horse_id AND from_ts > v_created;
        v_ability := pg_temp.rnd('ability' || m.horse_id, 0.0, 1.0);
        FOR s IN SELECT id, idx, session_date, end_time, status FROM (
                     SELECT id, row_number() OVER (ORDER BY session_date, start_time) idx, session_date, end_time, status
                       FROM training_sessions WHERE group_plan_id = v_plan) q ORDER BY idx LOOP
            v_end_ts := s.session_date + s.end_time;
            v_rec := v_end_ts + make_interval(mins => pg_temp.rndi('rec' || v_plan || m.horse_id || s.idx, 5, 35));
            IF v_block IS NOT NULL AND v_end_ts > v_block THEN
                -- Bị chặn tập: ra khỏi nhóm, mọi buổi chưa kết thúc lúc đó bị hủy. (Blocked: leaves the group, what had not ended is cancelled.)
                PERFORM pg_temp.put_workout(s.id, v_plan, m.horse_id, m.groom_id, 'CANCELLED', FALSE, v_ability, FALSE, v_created, v_block);
            ELSIF s.status = 'HELD' THEN
                v_recorded := s.session_date < pg_temp.today() AND NOT (p_unrecorded AND s.idx = v_last_past);
                PERFORM pg_temp.put_workout(s.id, v_plan, m.horse_id, m.groom_id, 'HELD', v_recorded, v_ability + 0.015 * s.idx,
                                            p_fatigue = m.horse_id AND s.session_date >= pg_temp.today() - 7, v_created,
                                            CASE WHEN v_recorded THEN v_rec ELSE v_end_ts END);
            ELSE
                PERFORM pg_temp.put_workout(s.id, v_plan, m.horse_id, m.groom_id, 'SCHEDULED', FALSE, v_ability, FALSE, v_created, v_created);
            END IF;
        END LOOP;
    END LOOP;

    PERFORM pg_temp.audit(v_trainer, 'Create Training Plan', 'GroupPlan', v_plan,
                          'Trainer created a new training plan for a group', v_created, 'POST', '/api/group-plans');
    RETURN v_plan;
END $$ LANGUAGE plpgsql;

-- Một ngựa vào nhóm giữa khóa: chỉ có Workout từ ngày p_from trở đi (như GroupWorkoutSyncService.createFutureWorkouts).
-- (A horse joins the group mid-course: Workouts only from p_from on.)
CREATE FUNCTION pg_temp.join_plan(p_plan BIGINT, p_horse BIGINT, p_from DATE) RETURNS VOID AS $$
DECLARE
    v_groom BIGINT; v_ability NUMERIC := pg_temp.rnd('ability' || p_horse, 0.0, 1.0);
    s RECORD; v_rec TIMESTAMP; v_recorded BOOLEAN;
BEGIN
    SELECT hm.groom_id INTO v_groom FROM training_group_members hm JOIN group_plans p ON p.group_id = hm.group_id
     WHERE p.id = p_plan AND hm.horse_id = p_horse;
    IF v_groom IS NULL THEN RAISE EXCEPTION 'Horse % is not a member of the group of plan %', p_horse, p_plan; END IF;
    FOR s IN SELECT id, session_date, end_time, status, row_number() OVER (ORDER BY session_date, start_time) idx
               FROM training_sessions WHERE group_plan_id = p_plan ORDER BY session_date, start_time LOOP
        CONTINUE WHEN s.session_date < p_from OR s.status = 'CANCELLED';
        v_rec := s.session_date + s.end_time + make_interval(mins => pg_temp.rndi('rec' || p_plan || p_horse || s.idx, 5, 35));
        v_recorded := s.status = 'HELD' AND s.session_date < pg_temp.today();
        PERFORM pg_temp.put_workout(s.id, p_plan, p_horse, v_groom, s.status, v_recorded, v_ability + 0.015 * s.idx, FALSE,
                                    (p_from - 1) + TIME '18:00', CASE WHEN v_recorded THEN v_rec ELSE (p_from - 1) + TIME '18:00' END);
    END LOOP;
END $$ LANGUAGE plpgsql;

-- Trainer hủy Workout của riêng một ngựa trước khi buổi bắt đầu. (The Trainer cancels one horse's Workout before it starts.)
CREATE FUNCTION pg_temp.cancel_workout(p_plan BIGINT, p_horse BIGINT, p_idx INT) RETURNS VOID AS $$
    UPDATE training_workouts w SET status = 'CANCELLED', updated_at = (s.session_date - 1) + TIME '18:00',
           actual_distance_meters = NULL, actual_duration_minutes = NULL, top_speed_kmh = NULL, average_speed_kmh = NULL,
           average_heart_rate = NULL, max_heart_rate = NULL, recovery_heart_rate = NULL, performance_rating = NULL, trainer_feedback = NULL
      FROM (SELECT id, session_date FROM (SELECT id, session_date, row_number() OVER (ORDER BY session_date, start_time) idx
                                            FROM training_sessions WHERE group_plan_id = p_plan) q WHERE idx = p_idx) s
     WHERE w.session_id = s.id AND w.horse_id = p_horse
$$ LANGUAGE sql;

-- Ghi kết quả xong mới biết ngựa "không chạy": Workout thành CANCELLED, không có số đo. ("Did not run" while recording.)
CREATE FUNCTION pg_temp.did_not_run(p_plan BIGINT, p_horse BIGINT, p_idx INT) RETURNS VOID AS $$
    UPDATE training_workouts w SET status = 'CANCELLED', updated_at = s.session_date + s.end_time + INTERVAL '20 minutes',
           actual_distance_meters = NULL, actual_duration_minutes = NULL, top_speed_kmh = NULL, average_speed_kmh = NULL,
           average_heart_rate = NULL, max_heart_rate = NULL, recovery_heart_rate = NULL, performance_rating = NULL, trainer_feedback = NULL
      FROM (SELECT id, session_date, end_time FROM (SELECT id, session_date, end_time, row_number() OVER (ORDER BY session_date, start_time) idx
                                                      FROM training_sessions WHERE group_plan_id = p_plan) q WHERE idx = p_idx) s
     WHERE w.session_id = s.id AND w.horse_id = p_horse
$$ LANGUAGE sql;

-- Trời mưa: Trainer hủy cả buổi trước giờ bắt đầu. (Rain: the Trainer cancels a whole Session before it starts.)
CREATE FUNCTION pg_temp.cancel_session(p_plan BIGINT, p_idx INT) RETURNS VOID AS $$
DECLARE
    v_session BIGINT; v_at TIMESTAMP;
BEGIN
    SELECT id, (session_date - 1) + TIME '18:00' INTO v_session, v_at FROM (
        SELECT id, session_date, row_number() OVER (ORDER BY session_date, start_time) idx
          FROM training_sessions WHERE group_plan_id = p_plan) q WHERE idx = p_idx;
    UPDATE training_workouts SET status = 'CANCELLED', updated_at = v_at, actual_distance_meters = NULL, actual_duration_minutes = NULL,
           top_speed_kmh = NULL, average_speed_kmh = NULL, average_heart_rate = NULL, max_heart_rate = NULL,
           recovery_heart_rate = NULL, performance_rating = NULL, trainer_feedback = NULL
     WHERE session_id = v_session;
    UPDATE training_sessions SET status = 'CANCELLED', updated_at = v_at WHERE id = v_session;
END $$ LANGUAGE plpgsql;

-- Trainer bỏ khóa tại thời điểm p_at: mọi buổi CHƯA bắt đầu bị hủy, plan thành CANCELLED. (The Trainer drops the course at p_at.)
CREATE FUNCTION pg_temp.cancel_plan(p_plan BIGINT, p_at TIMESTAMP) RETURNS VOID AS $$
BEGIN
    UPDATE training_workouts SET status = 'CANCELLED', updated_at = p_at, actual_distance_meters = NULL, actual_duration_minutes = NULL,
           top_speed_kmh = NULL, average_speed_kmh = NULL, average_heart_rate = NULL, max_heart_rate = NULL,
           recovery_heart_rate = NULL, performance_rating = NULL, trainer_feedback = NULL
     WHERE session_id IN (SELECT id FROM training_sessions WHERE group_plan_id = p_plan AND session_date + start_time > p_at);
    UPDATE training_sessions SET status = 'CANCELLED', updated_at = p_at
     WHERE group_plan_id = p_plan AND session_date + start_time > p_at;
    INSERT INTO seed_plan_cancel VALUES (p_plan);
    UPDATE group_plans SET status = 'CANCELLED', updated_at = p_at WHERE id = p_plan;
END $$ LANGUAGE plpgsql;

-- Chuỗi khóa của các nhóm: mỗi nhóm chạy lần lượt các khóa, khóa sau bắt đầu ngay sau khóa trước, đến khi có một khóa xếp hàng chờ
-- (bắt đầu sau hôm nay). p_unrecorded_group: nhóm có buổi gần nhất chưa ghi kết quả; p_late_group: nhóm có ngựa vào giữa khóa.
-- (Each group runs its courses back to back until one is queued after today.)
CREATE FUNCTION pg_temp.run_timeline(p_trainer TEXT, p_groups BIGINT[], p_courses TEXT[], p_unrecorded_group INT DEFAULT NULL,
                                     p_late_group INT DEFAULT NULL) RETURNS VOID AS $$
DECLARE
    v_week TEXT[] := ARRAY['MONDAY,WEDNESDAY,FRIDAY', 'TUESDAY,THURSDAY,SATURDAY', 'MONDAY,THURSDAY', 'WEDNESDAY,SATURDAY', 'SUNDAY'];
    v_days TEXT; v_start DATE; v_course BIGINT; v_total INT; v_dates DATE[]; v_plan BIGINT; v_ci INT; v_late BIGINT;
    v_current BOOLEAN;
BEGIN
    FOR gi IN 1..COALESCE(array_length(p_groups, 1), 0) LOOP
        v_days := v_week[((gi - 1) % array_length(v_week, 1)) + 1];
        v_start := pg_temp.today() - 91 + gi;
        v_ci := gi;
        LOOP
            SELECT id, total_sessions INTO v_course, v_total FROM courses
             WHERE name = p_courses[((v_ci - 1) % array_length(p_courses, 1)) + 1] AND trainer_id = pg_temp.uid(p_trainer);
            v_dates := pg_temp.session_dates(v_start, v_days, v_total);
            v_current := v_dates[1] <= pg_temp.today() AND v_dates[v_total] >= pg_temp.today();
            v_late := NULL;
            IF v_current AND p_late_group = gi THEN
                SELECT horse_id INTO v_late FROM training_group_members WHERE group_id = p_groups[gi] ORDER BY id DESC LIMIT 1;
            END IF;
            v_plan := pg_temp.make_group_plan(p_groups[gi], v_course, v_start, v_days,
                                              v_current AND p_unrecorded_group = gi,
                                              CASE WHEN v_late IS NULL THEN '{}'::BIGINT[] ELSE ARRAY[v_late] END);
            IF v_late IS NOT NULL THEN
                PERFORM pg_temp.join_plan(v_plan, v_late, v_dates[3]);
            END IF;
            EXIT WHEN v_dates[1] > pg_temp.today();
            v_start := v_dates[v_total] + 1;
            v_ci := v_ci + 1;
        END LOOP;
    END LOOP;
END $$ LANGUAGE plpgsql;

-- Đồng bộ cuối, giống SessionSettlementService: buổi chưa hết giờ mà không còn Workout nào thì tự hủy, rồi mỗi plan đóng khi không
-- còn buổi chờ chạy. (Final settle, like SessionSettlementService.)
CREATE FUNCTION pg_temp.settle_plans() RETURNS VOID AS $$
BEGIN
    UPDATE training_sessions s SET status = 'CANCELLED', updated_at = s.created_at
     WHERE s.status = 'SCHEDULED' AND NOT EXISTS (SELECT 1 FROM training_workouts w WHERE w.session_id = s.id AND w.status <> 'CANCELLED');
    UPDATE group_plans p
       SET status = CASE WHEN EXISTS (SELECT 1 FROM training_sessions s WHERE s.group_plan_id = p.id AND s.status = 'SCHEDULED')
                         THEN CASE WHEN p.start_date > pg_temp.today() THEN 'UPCOMING' ELSE 'ACTIVE' END
                         WHEN EXISTS (SELECT 1 FROM training_sessions s WHERE s.group_plan_id = p.id AND s.status = 'HELD')
                         THEN 'COMPLETED' ELSE 'CANCELLED' END,
           updated_at = GREATEST(p.created_at, COALESCE((SELECT max(s.updated_at) FROM training_sessions s WHERE s.group_plan_id = p.id), p.created_at))
     WHERE NOT EXISTS (SELECT 1 FROM seed_plan_cancel c WHERE c.plan_id = p.id);
END $$ LANGUAGE plpgsql;


DO $$
DECLARE
    v_groups BIGINT[];
    v_plan BIGINT;
    v_horse BIGINT;
BEGIN
    -- ----------------------------------------------------------------- subjects and courses
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
    PERFORM pg_temp.subject('TR03', 'Desert Endurance Trot', 'Long, even trot for Arabian endurance.', 'DIRT', 3000, 'LOW', 'ENDURANCE', 60, 50);
    PERFORM pg_temp.subject('TR03', 'Tempo Canter', 'Canter at a fixed tempo with heart-rate targets.', 'TURF', 1800, 'MEDIUM', 'ENDURANCE', 60, 50);
    PERFORM pg_temp.subject('TR03', 'Short Sprint Repeats', '3 x 300 m sprints with walk recovery.', 'TURF', 900, 'HIGH', 'SPEED', 45, 50);
    PERFORM pg_temp.subject('TR03', 'Stretch and Walk', 'Hand walking and stretching.', 'SYNTHETIC', 600, 'LOW', 'RECOVERY', 45, 50);
    PERFORM pg_temp.course('TR03', 'Arabian Endurance Base', 'Aerobic base for endurance racing.', 6,
                           ARRAY['Desert Endurance Trot', 'Tempo Canter'], 45);
    PERFORM pg_temp.course('TR03', 'Pace Variation', 'Tempo, sprints and recovery in one block.', 6,
                           ARRAY['Tempo Canter', 'Short Sprint Repeats', 'Stretch and Walk'], 45);


    -- Trainer1, an early group that ran two plans and was then Disbanded; its horses go into the Morning groups below on 3 Aug.
    -- (Disbanded group: no members, no unfinished plan, past plans keep the group. Built first because a horse is in one group only.)
    DECLARE
        v_old BIGINT; v_k TEXT; v_h BIGINT; v_g BIGINT; v_gone TIMESTAMP := pg_temp.at(-91, TIME '09:30');
        v_trainer BIGINT := pg_temp.uid('TR01');
    BEGIN
        INSERT INTO training_groups (trainer_id, name, created_at, updated_at)
        VALUES (v_trainer, 'Foundation Group', pg_temp.at(-125, TIME '09:00'), pg_temp.at(-125, TIME '09:00')) RETURNING id INTO v_old;
        PERFORM pg_temp.audit(v_trainer, 'Create Training Group', 'TrainingGroup', v_old,
                              'Trainer created a new training group', pg_temp.at(-125, TIME '09:00'), 'POST', '/api/training-groups');
        FOREACH v_k IN ARRAY ARRAY['H001','H004','H007'] LOOP
            v_h := pg_temp.hid(v_k);
            SELECT ss.groom_id INTO v_g FROM horses h JOIN stable_stalls ss ON ss.id = h.current_stall_id WHERE h.id = v_h;
            INSERT INTO training_group_members (group_id, horse_id, groom_id, joined_at) VALUES (v_old, v_h, v_g, pg_temp.at(-125, TIME '09:00'));
        END LOOP;
        PERFORM pg_temp.make_group_plan(v_old, (SELECT id FROM courses WHERE name = 'Gate Confidence Drill' AND trainer_id = v_trainer),
                                        pg_temp.today() - 118, 'MONDAY,WEDNESDAY,FRIDAY');
        v_plan := pg_temp.make_group_plan(v_old, (SELECT id FROM courses WHERE name = 'Novice Foundation' AND trainer_id = v_trainer),
                                          pg_temp.today() - 104, 'MONDAY,WEDNESDAY,FRIDAY');
        PERFORM pg_temp.cancel_plan(v_plan, (SELECT session_date + end_time FROM (
            SELECT session_date, end_time, row_number() OVER (ORDER BY session_date, start_time) idx
              FROM training_sessions WHERE group_plan_id = v_plan) q WHERE idx = 4) + INTERVAL '30 minutes');
        -- Disband (TrainingGroupService.disband): the horses leave, the group row stays with its disbanded_at.
        DELETE FROM training_group_members WHERE group_id = v_old;
        UPDATE training_groups SET disbanded_at = v_gone, updated_at = v_gone WHERE id = v_old;
        PERFORM pg_temp.audit(v_trainer, 'Disband Training Group', 'TrainingGroup', v_old,
                              'Trainer disbanded a training group', v_gone, 'POST', '/api/training-groups/' || v_old || '/disband');
    END;

    -- ----------------------------------------------------------------- groups and their courses
    v_groups := pg_temp.make_groups('TR01', 'Morning Group',
        ARRAY['H001','H002','H003','H004','H005','H006','H007','H008','H009','H010','H011','H012','H013','H014','H015','H016','H017','H018']);
    PERFORM pg_temp.run_timeline('TR01', v_groups,
        ARRAY['Novice Foundation', 'Sprint Acceleration', 'Classic Endurance', 'Championship Trial Protocol', 'Recovery Maintenance'],
        1, 2);

    v_groups := pg_temp.make_groups('TR02', 'Track Group',
        ARRAY['H019','H020','H021','H022','H023','H024','H025','H026','H027','H028','H029','H030','H031','H032','H033','H034','H035','H036']);
    PERFORM pg_temp.run_timeline('TR02', v_groups, ARRAY['Distance Builder', 'Speed Sharpening', 'Starting Stall Basics'], 2, NULL);

    v_groups := pg_temp.make_groups('TR03', 'Arabian Group', ARRAY['H037','H038','H039','H040','H041','H042']);
    PERFORM pg_temp.run_timeline('TR03', v_groups, ARRAY['Arabian Endurance Base', 'Pace Variation'], NULL, NULL);

    -- ----------------------------------------------------------------- demo scenarios
    -- Trainer1, group 1, second course: one horse did not actually run in the third Session.
    SELECT p.id INTO v_plan FROM group_plans p JOIN training_groups g ON g.id = p.group_id
     WHERE g.trainer_id = pg_temp.uid('TR01') AND g.name = 'Morning Group 1' ORDER BY p.start_date OFFSET 1 LIMIT 1;
    SELECT horse_id INTO v_horse FROM training_group_members m JOIN training_groups g ON g.id = m.group_id
     WHERE g.trainer_id = pg_temp.uid('TR01') AND g.name = 'Morning Group 1' ORDER BY m.id LIMIT 1;
    IF v_plan IS NOT NULL AND v_horse IS NOT NULL THEN PERFORM pg_temp.did_not_run(v_plan, v_horse, 3); END IF;

    -- Trainer1, group 2, second course: heavy rain cancelled the fourth Session for the whole group.
    SELECT p.id INTO v_plan FROM group_plans p JOIN training_groups g ON g.id = p.group_id
     WHERE g.trainer_id = pg_temp.uid('TR01') AND g.name = 'Morning Group 2' ORDER BY p.start_date OFFSET 1 LIMIT 1;
    IF v_plan IS NOT NULL THEN PERFORM pg_temp.cancel_session(v_plan, 4); END IF;

    -- Trainer1, group 3, running course: the Trainer rests one horse for its next Session only.
    SELECT p.id INTO v_plan FROM group_plans p JOIN training_groups g ON g.id = p.group_id
     WHERE g.trainer_id = pg_temp.uid('TR01') AND g.name = 'Morning Group 3' AND p.status = 'ACTIVE' ORDER BY p.start_date LIMIT 1;
    SELECT horse_id INTO v_horse FROM training_group_members m JOIN training_groups g ON g.id = m.group_id
     WHERE g.trainer_id = pg_temp.uid('TR01') AND g.name = 'Morning Group 3' ORDER BY m.id LIMIT 1;
    IF v_plan IS NOT NULL AND v_horse IS NOT NULL THEN
        PERFORM pg_temp.cancel_workout(v_plan, v_horse, (SELECT idx FROM (
            SELECT row_number() OVER (ORDER BY session_date, start_time) idx, session_date, start_time, status
              FROM training_sessions WHERE group_plan_id = v_plan) q
            WHERE q.status = 'SCHEDULED' ORDER BY idx LIMIT 1));
    END IF;

    -- Trainer2, group 1, second course: dropped part-way (Cancel plan after the fourth Session).
    SELECT p.id INTO v_plan FROM group_plans p JOIN training_groups g ON g.id = p.group_id
     WHERE g.trainer_id = pg_temp.uid('TR02') AND g.name = 'Track Group 1' ORDER BY p.start_date OFFSET 1 LIMIT 1;
    IF v_plan IS NOT NULL THEN
        PERFORM pg_temp.cancel_plan(v_plan, (SELECT session_date + end_time FROM (
            SELECT session_date, end_time, row_number() OVER (ORDER BY session_date, start_time) idx
              FROM training_sessions WHERE group_plan_id = v_plan) q WHERE idx = 4) + INTERVAL '30 minutes');
    END IF;

    -- Trainer1, a live group nobody has joined yet (no members, no plan).
    DECLARE
        v_trainer BIGINT := pg_temp.uid('TR01'); v_g BIGINT;
    BEGIN
        INSERT INTO training_groups (trainer_id, name, created_at, updated_at)
        VALUES (v_trainer, 'Reserve Group', pg_temp.at(-30, TIME '09:00'), pg_temp.at(-30, TIME '09:00')) RETURNING id INTO v_g;
        PERFORM pg_temp.audit(v_trainer, 'Create Training Group', 'TrainingGroup', v_g,
                              'Trainer created a new training group', pg_temp.at(-30, TIME '09:00'), 'POST', '/api/training-groups');
    END;

    PERFORM pg_temp.settle_plans();
END $$;
