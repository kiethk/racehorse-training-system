-- =====================================================================
-- V72: TỐI GIẢN "NGỰA CÓ ĐƯỢC TẬP KHÔNG" + DỌN CÁC TRẠNG THÁI KHÔNG DÙNG
-- =====================================================================
--
-- Trước V72, cùng một câu hỏi "ngựa có được tập không" được lưu ở 5 nơi:
--   horses.training_locked, horses.training_status, health_records.vet_decision,
--   admission_applications.vet_decision, horses.current_status (INJURED...).
-- Sau V72 chỉ còn:
--   horses.training_decision        ALLOWED | BLOCKED  (đang có hiệu lực)
--   horses.training_decision_reason vì sao bị chặn
--   health_records.training_decision  kết luận của từng lần khám (lịch sử)
--   admission_applications.vet_training_decision  kết luận lần khám nhập học
-- "Tạm nghỉ đến" suy ra từ lần khám kế tiếp trong care_schedule, không lưu.
--
-- Bỏ RESTRICTED: về hành vi nó giống hệt BLOCKED (Trainer đều bị chặn lập
-- lịch). Thương nhẹ = BLOCKED + lịch khám lại gần.
--
-- Ngựa CANDIDATE không cần cờ khóa: Horse.canTrain() đòi currentStatus =
-- ELIGIBLE, nên khóa "hành chính" bằng chuỗi cố định cũng bỏ.
-- ---------------------------------------------------------------------

-- =====================================================================
-- 1. HORSES
-- =====================================================================

-- 1a. Khóa hành chính cũ (ngựa chờ khám nhập học / chờ duyệt) -> không còn lưu.
UPDATE horses
SET training_status = 'ALLOWED',
    training_locked = FALSE,
    training_lock_reason = NULL
WHERE training_lock_reason IN ('Admission pending trainer and manager review',
                               'Initial admission examination is pending');

-- 1b. Ngựa INJURED (trạng thái cũ) -> giữ đúng ý nghĩa: bị chặn tập.
UPDATE horses
SET training_status = 'BLOCKED',
    training_lock_reason = COALESCE(training_lock_reason, 'Chấn thương (ghi nhận trước khi đổi cấu trúc trạng thái)')
WHERE current_status = 'INJURED';

-- 1c. RESTRICTED, và mọi khóa y tế còn lại chỉ thể hiện qua cờ -> BLOCKED.
UPDATE horses
SET training_status = 'BLOCKED'
WHERE training_status = 'RESTRICTED'
   OR (training_locked = TRUE AND training_status = 'ALLOWED');

-- 1d. Sức khỏe không còn nằm trong vòng đời: các trạng thái này về ELIGIBLE.
UPDATE horses
SET current_status = 'ELIGIBLE'
WHERE current_status IN ('INJURED', 'MONITORING', 'QUARANTINED');

ALTER TABLE horses DROP CONSTRAINT IF EXISTS chk_horses_current_status;
ALTER TABLE horses
    ADD CONSTRAINT chk_horses_current_status
        CHECK (current_status IN ('CANDIDATE', 'ELIGIBLE', 'REJECTED'));

-- 1e. Một cột quyết định + một cột lý do. Các cột còn lại không ai đọc:
--     review_date -> thay bằng lịch khám kế tiếp; vet_id -> đã có ở bệnh án;
--     updated_at -> horses.updated_at.
ALTER TABLE horses
    DROP COLUMN training_locked,
    DROP COLUMN training_lock_review_date,
    DROP COLUMN training_lock_vet_id,
    DROP COLUMN training_lock_updated_at;

ALTER TABLE horses RENAME COLUMN training_status TO training_decision;
ALTER TABLE horses RENAME COLUMN training_lock_reason TO training_decision_reason;

ALTER TABLE horses
    ADD CONSTRAINT chk_horses_training_decision
        CHECK (training_decision IN ('ALLOWED', 'BLOCKED'));

-- =====================================================================
-- 2. HEALTH_RECORDS
-- =====================================================================
UPDATE health_records SET training_decision = 'BLOCKED' WHERE training_decision = 'RESTRICTED';

-- vet_decision: được TÍNH từ training_decision, không phải ý kiến riêng.
-- rejection_reason: Thú y không còn từ chối đơn. follow_up_date: thay bằng nextSchedule.
ALTER TABLE health_records
    DROP COLUMN vet_decision,
    DROP COLUMN rejection_reason,
    DROP COLUMN follow_up_date;

ALTER TABLE health_records
    ADD CONSTRAINT chk_health_records_training_decision
        CHECK (training_decision IS NULL OR training_decision IN ('ALLOWED', 'BLOCKED'));

-- =====================================================================
-- 3. ADMISSION_APPLICATIONS
-- =====================================================================
UPDATE admission_applications
SET vet_training_decision = 'BLOCKED'
WHERE vet_training_decision = 'RESTRICTED';

-- V68 đã sao vet_decision sang vet_training_decision, không mất thông tin.
ALTER TABLE admission_applications DROP COLUMN vet_decision;

ALTER TABLE admission_applications
    ADD CONSTRAINT chk_admission_vet_training_decision
        CHECK (vet_training_decision IS NULL OR vet_training_decision IN ('ALLOWED', 'BLOCKED'));

-- PENDING_RECHECK chưa từng được gán; chuyển phòng hờ rồi bỏ khỏi các CHECK.
UPDATE admission_applications SET status = 'VET_REVIEW' WHERE status = 'PENDING_RECHECK';

ALTER TABLE admission_applications
    DROP CONSTRAINT IF EXISTS chk_admission_status,
    DROP CONSTRAINT IF EXISTS chk_admission_q_stall_required,
    DROP CONSTRAINT IF EXISTS chk_admission_post_groom_has_horse;

ALTER TABLE admission_applications
    ADD CONSTRAINT chk_admission_status CHECK (status IN (
        'GROOM_REVIEW', 'WAITING_FOR_STALL', 'VET_REVIEW',
        'TRAINER_REVIEW', 'MANAGER_REVIEW', 'APPROVED', 'REJECTED'
    )),
    ADD CONSTRAINT chk_admission_q_stall_required CHECK (
        status NOT IN ('VET_REVIEW', 'TRAINER_REVIEW', 'MANAGER_REVIEW', 'APPROVED')
        OR quarantine_stall_id IS NOT NULL
    ),
    ADD CONSTRAINT chk_admission_post_groom_has_horse CHECK (
        status NOT IN ('VET_REVIEW', 'TRAINER_REVIEW', 'MANAGER_REVIEW', 'APPROVED')
        OR horse_id IS NOT NULL
    ) NOT VALID;

-- =====================================================================
-- 4. KẾ HOẠCH / LOT / BUỔI TẬP — giá trị không bao giờ được gán
-- =====================================================================
UPDATE horse_training_plans SET status = 'CANCELLED' WHERE status = 'PAUSED';
UPDATE training_lots        SET status = 'SCHEDULED' WHERE status = 'IN_PROGRESS';
UPDATE training_workouts    SET status = 'SCHEDULED' WHERE status = 'IN_PROGRESS';

ALTER TABLE training_lots DROP CONSTRAINT IF EXISTS chk_training_lots_status;
ALTER TABLE training_lots
    ADD CONSTRAINT chk_training_lots_status
        CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED'));

-- =====================================================================
-- 5. SUBJECT / COURSE — cột không có logic nào đọc
-- =====================================================================
-- workout_type: frontend còn gửi 4/5 giá trị backend không nhận.
ALTER TABLE subjects DROP CONSTRAINT IF EXISTS chk_subjects_workout_type;
ALTER TABLE subjects DROP COLUMN workout_type;

-- status: chỉ còn một giá trị ACTIVE sau khi bỏ ARCHIVED (chưa từng được gán).
ALTER TABLE courses DROP COLUMN status;

-- =====================================================================
-- 6. QUYỀN CỦA CÁC ENDPOINT ĐÃ BỎ
-- =====================================================================
-- PUT /api/horses/{id}/status, PUT /api/training-plans/{id}/status,
-- POST /api/injury-records, GET /api/horses/{id}/training-lock-status:
-- không màn hình nào gọi. Xóa quyền để trang phân quyền không còn mục mồ côi.
DELETE FROM role_permissions
WHERE permission_id IN (
    SELECT id FROM permissions
    WHERE code IN ('HORSE_STATUS_EDIT', 'TRAINING_PLAN_UPDATE',
                   'INJURY_RECORD_CREATE', 'HORSE_TRAINING_LOCK_VIEW')
);

DELETE FROM permissions
WHERE code IN ('HORSE_STATUS_EDIT', 'TRAINING_PLAN_UPDATE',
               'INJURY_RECORD_CREATE', 'HORSE_TRAINING_LOCK_VIEW');
