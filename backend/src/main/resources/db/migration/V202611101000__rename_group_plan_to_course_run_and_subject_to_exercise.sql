-- =====================================================================
-- Đổi thuật ngữ: Group Plan -> Course Run, Subject -> Exercise.
-- (Terminology change: Group Plan -> Course Run, Subject -> Exercise.)
--
-- Chỉ đổi tên, không đổi dữ liệu hay luật. Tên ràng buộc, chỉ mục, dãy số và hàm trigger đổi theo để không còn tên cũ trên bảng
-- mới. RENAME bảng KHÔNG sửa thân hàm trigger, nên hàm được tạo lại với tên bảng và cột mới.
-- (Names only, no data or rule change. Constraint, index, sequence and trigger-function names follow so no old name stays on a
-- renamed table. RENAME does not rewrite a trigger function body, so the function is recreated with the new table and column.)
-- =====================================================================

-- ---------------------------------------------------------------------
-- Course Run (trước là group_plans)
-- ---------------------------------------------------------------------
ALTER TABLE group_plans RENAME TO course_runs;
ALTER SEQUENCE group_plans_id_seq RENAME TO course_runs_id_seq;
ALTER INDEX group_plans_pkey RENAME TO course_runs_pkey;
ALTER TABLE course_runs RENAME CONSTRAINT group_plans_course_id_fkey TO course_runs_course_id_fkey;
ALTER TABLE course_runs RENAME CONSTRAINT group_plans_group_id_fkey TO course_runs_group_id_fkey;
ALTER TABLE course_runs RENAME CONSTRAINT group_plans_trainer_id_fkey TO course_runs_trainer_id_fkey;
ALTER TABLE course_runs RENAME CONSTRAINT chk_group_plans_dates TO chk_course_runs_dates;
ALTER TABLE course_runs RENAME CONSTRAINT chk_group_plans_status TO chk_course_runs_status;
ALTER TABLE course_runs RENAME CONSTRAINT chk_group_plans_training_days TO chk_course_runs_training_days;
ALTER TABLE course_runs RENAME CONSTRAINT excl_group_plans_dates TO excl_course_runs_dates;
ALTER INDEX idx_group_plans_group RENAME TO idx_course_runs_group;
ALTER INDEX idx_group_plans_trainer RENAME TO idx_course_runs_trainer;

ALTER TABLE training_sessions RENAME COLUMN group_plan_id TO course_run_id;
ALTER TABLE training_sessions RENAME CONSTRAINT training_sessions_group_plan_id_fkey TO training_sessions_course_run_id_fkey;
ALTER INDEX idx_training_sessions_plan RENAME TO idx_training_sessions_course_run;

-- ---------------------------------------------------------------------
-- Exercise (trước là subjects) và CourseExercise (trước là course_subjects)
-- ---------------------------------------------------------------------
ALTER TABLE subjects RENAME TO exercises;
ALTER SEQUENCE subjects_id_seq RENAME TO exercises_id_seq;
ALTER INDEX subjects_pkey RENAME TO exercises_pkey;
ALTER TABLE exercises RENAME CONSTRAINT subjects_trainer_id_fkey TO exercises_trainer_id_fkey;
ALTER TABLE exercises RENAME CONSTRAINT chk_subjects_category TO chk_exercises_category;
ALTER TABLE exercises RENAME CONSTRAINT chk_subjects_distance TO chk_exercises_distance;
ALTER TABLE exercises RENAME CONSTRAINT chk_subjects_duration TO chk_exercises_duration;
ALTER TABLE exercises RENAME CONSTRAINT chk_subjects_intensity TO chk_exercises_intensity;
ALTER TABLE exercises RENAME CONSTRAINT chk_subjects_name_not_blank TO chk_exercises_name_not_blank;
ALTER TABLE exercises RENAME CONSTRAINT chk_subjects_surface TO chk_exercises_surface;
ALTER INDEX idx_subjects_trainer_id RENAME TO idx_exercises_trainer_id;

ALTER TABLE course_subjects RENAME TO course_exercises;
ALTER SEQUENCE course_subjects_id_seq RENAME TO course_exercises_id_seq;
ALTER INDEX course_subjects_pkey RENAME TO course_exercises_pkey;
ALTER TABLE course_exercises RENAME COLUMN subject_id TO exercise_id;
ALTER TABLE course_exercises RENAME CONSTRAINT course_subjects_course_id_fkey TO course_exercises_course_id_fkey;
ALTER TABLE course_exercises RENAME CONSTRAINT course_subjects_subject_id_fkey TO course_exercises_exercise_id_fkey;
ALTER TABLE course_exercises RENAME CONSTRAINT chk_course_subjects_order TO chk_course_exercises_order;
ALTER TABLE course_exercises RENAME CONSTRAINT uq_course_subject_order TO uq_course_exercise_order;
ALTER INDEX idx_course_subjects_course RENAME TO idx_course_exercises_course;

ALTER TABLE training_sessions RENAME COLUMN subject_id TO exercise_id;
ALTER TABLE training_sessions RENAME CONSTRAINT training_sessions_subject_id_fkey TO training_sessions_exercise_id_fkey;

-- Một bài tập chỉ xuất hiện một lần trong mỗi khóa: tạo lại hàm và trigger với tên mới.
-- (An exercise appears once per course: the function and trigger are recreated with the new names.)
DROP TRIGGER course_subject_once ON course_exercises;
DROP FUNCTION trg_course_subject_once();

CREATE FUNCTION trg_course_exercise_once() RETURNS trigger AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM course_exercises ce
               WHERE ce.course_id = NEW.course_id AND ce.exercise_id = NEW.exercise_id AND ce.id <> NEW.id) THEN
        RAISE EXCEPTION 'Exercise % is already in course %', NEW.exercise_id, NEW.course_id
            USING ERRCODE = '23505';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER course_exercise_once
    BEFORE INSERT OR UPDATE OF course_id, exercise_id ON course_exercises
    FOR EACH ROW EXECUTE FUNCTION trg_course_exercise_once();

-- ---------------------------------------------------------------------
-- Quyền: đổi mã, giữ nguyên cấp quyền theo vai trò (role_permissions trỏ tới id nên không đổi).
-- (Authorities: the code changes, the role grants stay because role_permissions points at the id.)
-- ---------------------------------------------------------------------
UPDATE permissions SET code = 'COURSE_RUN_VIEW', description = 'Open a course run' WHERE code = 'GROUP_PLAN_VIEW';
UPDATE permissions SET code = 'COURSE_RUN_LIST', description = 'List all course runs (trainer and club manager only)' WHERE code = 'GROUP_PLAN_LIST';
UPDATE permissions SET code = 'COURSE_RUN_MANAGE', description = 'Assign a course to a training group and cancel a course run' WHERE code = 'GROUP_PLAN_CREATE';
UPDATE permissions SET code = 'EXERCISE_VIEW', description = 'View the exercise library' WHERE code = 'SUBJECT_VIEW';
UPDATE permissions SET code = 'EXERCISE_MANAGE', description = 'Create and edit exercises in the exercise library' WHERE code = 'SUBJECT_MANAGE';
