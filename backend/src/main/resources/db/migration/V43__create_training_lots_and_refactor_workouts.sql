-- =====================================================================
-- V43: CHUYỂN SANG CƠ CHẾ LOT (NHÓM NGỰA TẬP CHUNG MỘT BÀI)
--
-- Nguyên tắc:
--   1 Lot = 1 Trainer + 1 Ngày + 1 Bài tập + 1 Khung giờ
--   Ngựa trong cùng lot bắt buộc cùng bài (đúng theo cấu trúc, không cần validate)
--   TrainingWorkout chỉ còn là "con ngựa X tham gia lot Y + kết quả đo được"
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. SUBJECT: thời lượng bài tập + loại buổi tập
-- ---------------------------------------------------------------------
ALTER TABLE subjects
    ADD COLUMN duration_minutes INT         NOT NULL DEFAULT 60,
    ADD COLUMN workout_type     VARCHAR(20) NOT NULL DEFAULT 'REGULAR';

ALTER TABLE subjects
    ADD CONSTRAINT chk_subjects_duration
        CHECK (duration_minutes BETWEEN 15 AND 240);

ALTER TABLE subjects
    ADD CONSTRAINT chk_subjects_workout_type
        CHECK (workout_type IN ('REGULAR', 'TRIAL_RUN'));

COMMENT ON COLUMN subjects.duration_minutes IS
    'Thời lượng buổi tập (phút). Quyết định độ dài của lot chạy bài này.';
COMMENT ON COLUMN subjects.workout_type IS
    'Thay cho việc suy WorkoutType bằng cách so chuỗi tên bài (rất dễ vỡ).';

-- Gán thời lượng cho 4 bài mẫu đã seed ở V23.
-- Tổng 3 bài lớn nhất = 90+60+60 = 210 phút <= 240 phút khung giờ vàng.
UPDATE subjects SET duration_minutes = 90
    WHERE name = 'Khởi động & Chạy bền nhịp đều';
UPDATE subjects SET duration_minutes = 60
    WHERE name = 'Biến tốc tăng tốc nước rút';
UPDATE subjects SET duration_minutes = 60, workout_type = 'TRIAL_RUN'
    WHERE name = 'Chạy thử tính giờ (Trial Run)';
UPDATE subjects SET duration_minutes = 45
    WHERE name = 'Đi bộ và thả lỏng cơ sau tập';

-- ---------------------------------------------------------------------
-- 2. BẢNG TRAINING_LOTS
-- ---------------------------------------------------------------------
CREATE TABLE training_lots (
    id           BIGSERIAL PRIMARY KEY,

    trainer_id   BIGINT NOT NULL REFERENCES users(id),
    subject_id   BIGINT NOT NULL REFERENCES subjects(id) ON DELETE RESTRICT,

    lot_date     DATE NOT NULL,
    start_time   TIME NOT NULL,
    end_time     TIME NOT NULL,

    max_capacity INT NOT NULL DEFAULT 6,
    status       VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',

    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP,

    CONSTRAINT chk_training_lots_time
        CHECK (start_time < end_time),
    CONSTRAINT chk_training_lots_capacity
        CHECK (max_capacity BETWEEN 1 AND 20),
    CONSTRAINT chk_training_lots_status
        CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

-- Index cho thuật toán quét khe (lấy mọi lot của Trainer trong 1 ngày)
CREATE INDEX idx_training_lots_trainer_date
    ON training_lots(trainer_id, lot_date);

-- Index cho bước "tìm lot cùng bài" (BƯỚC 1 của thuật toán)
CREATE INDEX idx_training_lots_lookup
    ON training_lots(trainer_id, lot_date, subject_id, status);

-- ---------------------------------------------------------------------
-- 3. HORSE_TRAINING_PLANS: lưu KHUÔN MẪU của khoá
--
--    Vì sao cần: khi lot bị huỷ hoặc có buổi lệch chuẩn, không thể suy
--    ngược "khoá này tập thứ mấy / Groom nào" từ các workout còn lại.
--    KHÔNG lưu start_time/end_time — giờ giấc giờ thuộc về lot.
-- ---------------------------------------------------------------------
ALTER TABLE horse_training_plans
    ADD COLUMN training_days VARCHAR(100),
    ADD COLUMN groom_id      BIGINT REFERENCES users(id) ON DELETE SET NULL;

COMMENT ON COLUMN horse_training_plans.training_days IS
    'Các thứ trong tuần, lưu dạng "MONDAY,WEDNESDAY,FRIDAY". Đọc/ghi qua TrainingDaySetConverter.';

CREATE INDEX idx_plans_groom
    ON horse_training_plans(groom_id);

-- Index cho API gợi ý nhóm (joinable-cohorts): GROUP BY bộ ba này
CREATE INDEX idx_plans_cohort
    ON horse_training_plans(trainer_id, course_id, start_date);

-- ---------------------------------------------------------------------
-- 4. TRAINING_WORKOUTS: bỏ giờ giấc, gắn vào lot
--
--    Dữ liệu workout cũ không có lot nên không migrate được -> xoá.
--    DÙNG DELETE, KHÔNG DÙNG "TRUNCATE ... CASCADE":
--    health_records có FK trỏ tới training_workouts, TRUNCATE CASCADE sẽ
--    xoá sạch cả bảng bệnh án. DELETE thì FK ON DELETE SET NULL chỉ gỡ liên kết.
-- ---------------------------------------------------------------------
DELETE FROM training_workouts;

ALTER TABLE training_workouts
    ADD COLUMN lot_id BIGINT NOT NULL
        REFERENCES training_lots(id) ON DELETE CASCADE;

ALTER TABLE training_workouts
    DROP COLUMN subject_id,
    DROP COLUMN workout_date,
    DROP COLUMN start_time,
    DROP COLUMN end_time,
    DROP COLUMN workout_type;

CREATE INDEX idx_training_workouts_lot
    ON training_workouts(lot_id);

-- Một con ngựa không thể xuất hiện 2 lần trong cùng một lot
ALTER TABLE training_workouts
    ADD CONSTRAINT uq_workout_lot_horse UNIQUE (lot_id, horse_id);

-- Index cũ idx_training_workouts_horse dùng workout_date đã bị xoá cột -> dựng lại
DROP INDEX IF EXISTS idx_training_workouts_horse;
DROP INDEX IF EXISTS idx_training_workouts_plan;

CREATE INDEX idx_training_workouts_horse ON training_workouts(horse_id);
CREATE INDEX idx_training_workouts_plan  ON training_workouts(plan_id);
CREATE INDEX idx_training_workouts_groom ON training_workouts(assigned_to_id);
