-- Groom duyệt đơn là nhận ngựa vào q-stall của chính mình: bỏ hàng đợi WAITING_FOR_STALL.
-- groom_review_started_at là mốc FIFO và mốc mở rộng pool (N giờ); ghi mỗi lần đơn vào GROOM_REVIEW.
-- groom_pool_widened_at đánh dấu đơn đã được báo cho cả nhóm groom còn chỗ, để job chỉ báo một lần.
-- (A groom who approves an admission takes the horse into one of their own quarantine stalls, so the
-- WAITING_FOR_STALL queue goes away. groom_review_started_at orders the pool and starts the widening clock;
-- groom_pool_widened_at records that the widened pool was notified, so the job notifies once.)

ALTER TABLE admission_applications
    ADD COLUMN groom_review_started_at TIMESTAMP,
    ADD COLUMN groom_pool_widened_at TIMESTAMP;

UPDATE admission_applications
SET groom_review_started_at = COALESCE(submitted_at, created_at)
WHERE status = 'GROOM_REVIEW';

-- Đơn đang chờ chuồng quay lại GROOM_REVIEW để một groom còn chỗ duyệt và nhận.
-- (Admissions waiting for a stall return to GROOM_REVIEW so a groom with a free stall can claim them.)
UPDATE admission_applications
SET status = 'GROOM_REVIEW',
    groom_id = NULL,
    groom_decision = NULL,
    groom_feedback = NULL,
    groom_reviewed_at = NULL,
    groom_review_started_at = NOW(),
    updated_at = NOW()
WHERE status = 'WAITING_FOR_STALL';

ALTER TABLE admission_applications
    DROP CONSTRAINT IF EXISTS chk_admission_status;

ALTER TABLE admission_applications
    ADD CONSTRAINT chk_admission_status CHECK (status IN (
        'GROOM_REVIEW', 'WAITING_FOR_ARRIVAL', 'ARRIVAL_EXPIRED',
        'VET_REVIEW', 'TRAINER_REVIEW', 'MANAGER_REVIEW', 'APPROVED', 'REJECTED'
    )),
    ADD CONSTRAINT chk_admission_groom_review_started CHECK (
        status <> 'GROOM_REVIEW' OR groom_review_started_at IS NOT NULL
    );

CREATE INDEX idx_admission_groom_pool
    ON admission_applications (groom_review_started_at, id)
    WHERE status = 'GROOM_REVIEW';
