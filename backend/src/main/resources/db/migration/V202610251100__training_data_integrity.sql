-- Ràng buộc toàn vẹn cho luồng training: DB tự từ chối dữ liệu sai nghiệp vụ dù đi qua API, seed hay SQL tay.
-- (Integrity rules for the training flow: the database itself rejects business-invalid data, whether it comes from the API,
-- a seed or hand-written SQL.)
--
-- Chỉ thay đổi schema, KHÔNG sửa dữ liệu. Các CHECK thêm bằng NOT VALID: dòng cũ không bị kiểm lại, nhưng mọi INSERT/UPDATE
-- mới đều bị kiểm. Ràng buộc chéo nhiều dòng dùng trigger chỉ chạy khi dòng được thêm hoặc đổi chỗ, nên dòng cũ không chặn
-- các cập nhật không liên quan (ví dụ ghi kết quả buổi tập).
-- (Schema changes only, NO data changes. CHECKs are added NOT VALID: old rows are not re-checked, but every new INSERT/UPDATE
-- is. Cross-row rules use triggers that fire only when a row is added or moved, so legacy rows never block unrelated updates
-- such as recording a workout result.)

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ---------------------------------------------------------------
-- subjects / courses / course_subjects
-- ---------------------------------------------------------------
ALTER TABLE subjects
    ADD CONSTRAINT chk_subjects_name_not_blank CHECK (length(btrim(name)) > 0) NOT VALID,
    ADD CONSTRAINT chk_subjects_surface CHECK (surface_type IN ('TURF', 'DIRT', 'SYNTHETIC')) NOT VALID,
    ADD CONSTRAINT chk_subjects_intensity CHECK (intensity_level IN ('LOW', 'MEDIUM', 'HIGH')) NOT VALID,
    ADD CONSTRAINT chk_subjects_distance CHECK (target_distance_meters IS NULL
        OR (target_distance_meters >= 100 AND target_distance_meters <= 10000)) NOT VALID;

ALTER TABLE courses
    ADD CONSTRAINT chk_courses_name_not_blank CHECK (length(btrim(name)) > 0) NOT VALID,
    ADD CONSTRAINT chk_courses_total_sessions CHECK (total_sessions >= 1 AND total_sessions <= 60) NOT VALID;

ALTER TABLE course_subjects
    ADD CONSTRAINT chk_course_subjects_order CHECK (order_index >= 1) NOT VALID;

-- Một bài chỉ xuất hiện một lần trong khóa. Dùng trigger (không phải UNIQUE) vì dữ liệu cũ có thể đã trùng.
-- (A subject appears once per course. A trigger rather than UNIQUE because legacy data may already contain duplicates.)
CREATE FUNCTION trg_course_subject_once() RETURNS trigger AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM course_subjects cs
               WHERE cs.course_id = NEW.course_id AND cs.subject_id = NEW.subject_id AND cs.id <> NEW.id) THEN
        RAISE EXCEPTION 'Subject % is already in course %', NEW.subject_id, NEW.course_id
            USING ERRCODE = '23505';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER course_subject_once
    BEFORE INSERT OR UPDATE OF course_id, subject_id ON course_subjects
    FOR EACH ROW EXECUTE FUNCTION trg_course_subject_once();

-- ---------------------------------------------------------------
-- horse_training_plans
-- ---------------------------------------------------------------
ALTER TABLE horse_training_plans
    ADD CONSTRAINT chk_plans_dates CHECK (end_date >= start_date) NOT VALID,
    ADD CONSTRAINT chk_plans_status CHECK (status IN ('UPCOMING', 'ACTIVE', 'COMPLETED', 'CANCELLED')) NOT VALID,
    ADD CONSTRAINT chk_plans_training_days CHECK (training_days IS NULL OR training_days ~
        '^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)(,(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY))*$')
        NOT VALID;

-- BR-01: một con ngựa không có hai kế hoạch ACTIVE/UPCOMING chồng khoảng ngày (kể cả khi hai request chạy cùng lúc).
-- (BR-01: a horse never has two ACTIVE/UPCOMING plans with overlapping dates, even when two requests run at once.)
ALTER TABLE horse_training_plans
    ADD CONSTRAINT excl_plans_horse_dates EXCLUDE USING gist (
        horse_id WITH =,
        daterange(start_date, end_date, '[]') WITH &&
    ) WHERE (status IN ('ACTIVE', 'UPCOMING'));

-- ---------------------------------------------------------------
-- training_lots
-- ---------------------------------------------------------------
-- BR-03: lot nằm trong khung giờ vàng 06:00-10:00 và bắt đầu ở phút :00/:15/:30/:45 (lot đã hủy được miễn).
-- (BR-03: a lot sits in the 06:00-10:00 golden window and starts at :00/:15/:30/:45 (cancelled lots are exempt).)
ALTER TABLE training_lots
    ADD CONSTRAINT chk_training_lots_golden_window CHECK (status = 'CANCELLED'
        OR (start_time >= TIME '06:00' AND end_time <= TIME '10:00')) NOT VALID,
    ADD CONSTRAINT chk_training_lots_quarter_hour CHECK (status = 'CANCELLED'
        OR (EXTRACT(MINUTE FROM start_time)::int % 15 = 0 AND EXTRACT(SECOND FROM start_time) = 0)) NOT VALID;

-- BR-02: hai lot chưa hủy của cùng một Trainer không chồng giờ trong ngày.
-- (BR-02: two non-cancelled lots of one trainer never overlap in time.)
ALTER TABLE training_lots
    ADD CONSTRAINT excl_lots_trainer_time EXCLUDE USING gist (
        trainer_id WITH =,
        tsrange(lot_date + start_time, lot_date + end_time) WITH &&
    ) WHERE (status <> 'CANCELLED');

-- ---------------------------------------------------------------
-- training_workouts
-- ---------------------------------------------------------------
ALTER TABLE training_workouts
    ADD CONSTRAINT chk_workouts_status CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED')) NOT VALID,
    ADD CONSTRAINT chk_workouts_measurements CHECK (
        (actual_distance_meters IS NULL OR actual_distance_meters > 0)
        AND (actual_duration_minutes IS NULL OR (actual_duration_minutes > 0 AND actual_duration_minutes <= 1440))
        AND (top_speed_kmh IS NULL OR (top_speed_kmh > 0 AND top_speed_kmh <= 100))
        AND (average_speed_kmh IS NULL OR (average_speed_kmh > 0 AND average_speed_kmh <= 100))
        AND (average_heart_rate IS NULL OR average_heart_rate BETWEEN 30 AND 240)
        AND (max_heart_rate IS NULL OR max_heart_rate BETWEEN 30 AND 240)
        AND (recovery_heart_rate IS NULL OR recovery_heart_rate BETWEEN 30 AND 240)
        AND (performance_rating IS NULL OR performance_rating BETWEEN 1 AND 10)
        AND (average_speed_kmh IS NULL OR top_speed_kmh IS NULL OR average_speed_kmh <= top_speed_kmh)
        AND (average_heart_rate IS NULL OR max_heart_rate IS NULL OR average_heart_rate <= max_heart_rate)
    ) NOT VALID,
    ADD CONSTRAINT chk_workouts_completed_has_result CHECK (status <> 'COMPLETED' OR (
        actual_distance_meters IS NOT NULL AND actual_duration_minutes IS NOT NULL
        AND top_speed_kmh IS NOT NULL AND average_speed_kmh IS NOT NULL
        AND average_heart_rate IS NOT NULL AND max_heart_rate IS NOT NULL
        AND recovery_heart_rate IS NOT NULL AND performance_rating IS NOT NULL)) NOT VALID;

-- BR-10: một lot không quá max_capacity ngựa; BR-09: một Groom chỉ dắt một ngựa trong cùng lot.
-- Kiểm cuối transaction (DEFERRABLE INITIALLY DEFERRED) để một lệnh có thể thêm cả nhóm ngựa rồi mới bị kiểm một lần.
-- (BR-10: a lot holds at most max_capacity horses; BR-09: a groom leads one horse per lot. Checked at the end of the
-- transaction (DEFERRABLE INITIALLY DEFERRED) so a batch of horses is validated once, after the whole batch is in.)
CREATE FUNCTION trg_lot_workouts_rules() RETURNS trigger AS $$
DECLARE
    capacity integer;
    active_count integer;
BEGIN
    SELECT max_capacity INTO capacity FROM training_lots WHERE id = NEW.lot_id;
    SELECT count(*) INTO active_count FROM training_workouts WHERE lot_id = NEW.lot_id AND status <> 'CANCELLED';
    IF capacity IS NOT NULL AND active_count > capacity THEN
        RAISE EXCEPTION 'Lot % is full: % horses for capacity %', NEW.lot_id, active_count, capacity
            USING ERRCODE = '23514';
    END IF;

    IF NEW.assigned_to_id IS NOT NULL AND (
        SELECT count(*) FROM training_workouts
        WHERE lot_id = NEW.lot_id AND assigned_to_id = NEW.assigned_to_id AND status <> 'CANCELLED') > 1 THEN
        RAISE EXCEPTION 'Groom % already leads another horse in lot %', NEW.assigned_to_id, NEW.lot_id
            USING ERRCODE = '23505';
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER lot_workouts_rules_insert
    AFTER INSERT ON training_workouts
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW WHEN (NEW.status <> 'CANCELLED')
    EXECUTE FUNCTION trg_lot_workouts_rules();

-- UPDATE chỉ kiểm khi ngựa bị chuyển lot, đổi Groom, hoặc được "hồi sinh" từ CANCELLED.
-- (UPDATE is checked only when a horse moves lot, changes groom, or is revived from CANCELLED.)
CREATE CONSTRAINT TRIGGER lot_workouts_rules_update
    AFTER UPDATE ON training_workouts
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW WHEN (NEW.status <> 'CANCELLED' AND (
        OLD.lot_id IS DISTINCT FROM NEW.lot_id
        OR OLD.assigned_to_id IS DISTINCT FROM NEW.assigned_to_id
        OR OLD.status = 'CANCELLED'))
    EXECUTE FUNCTION trg_lot_workouts_rules();
