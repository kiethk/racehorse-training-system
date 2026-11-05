-- =====================================================================
-- Migration: V202611042100__add_delayed_status_and_refine_overdue.sql
-- Description: AD-026: Add DELAYED status, refine OVERDUE detection, and cleanup legacy columns
-- =====================================================================

-- 1. Nới rộng CHECK constraint cho care_schedule.status (thêm DELAYED)
ALTER TABLE care_schedule DROP CONSTRAINT IF EXISTS chk_care_schedules_status;
ALTER TABLE care_schedule ADD CONSTRAINT chk_care_schedules_status
    CHECK (status IN ('REQUESTED', 'SCHEDULED', 'IN_PROGRESS', 'PAUSED', 'DELAYED', 'OVERDUE', 'COMPLETED'));

-- 2. Cập nhật các Partial Indexes bao gồm DELAYED
DROP INDEX IF EXISTS uq_care_schedule_one_active_urgent_per_horse;
CREATE UNIQUE INDEX uq_care_schedule_one_active_urgent_per_horse
    ON care_schedule(horse_id)
    WHERE care_type = 'URGENT' AND status IN ('REQUESTED', 'SCHEDULED', 'IN_PROGRESS', 'PAUSED', 'DELAYED', 'OVERDUE');

DROP INDEX IF EXISTS uq_care_schedule_one_active_preventive_per_horse;
CREATE UNIQUE INDEX uq_care_schedule_one_active_preventive_per_horse
    ON care_schedule(horse_id)
    WHERE purpose = 'PREVENTIVE' AND status IN ('REQUESTED', 'SCHEDULED', 'IN_PROGRESS', 'PAUSED', 'DELAYED', 'OVERDUE');

DROP INDEX IF EXISTS idx_care_schedule_vet_slot;
CREATE INDEX idx_care_schedule_vet_slot
    ON care_schedule(veterinarian_id, scheduled_at)
    WHERE status IN ('SCHEDULED', 'IN_PROGRESS', 'PAUSED', 'DELAYED', 'OVERDUE');

DROP INDEX IF EXISTS idx_care_schedule_horse_slot;
CREATE INDEX idx_care_schedule_horse_slot
    ON care_schedule(horse_id, scheduled_at)
    WHERE status IN ('SCHEDULED', 'IN_PROGRESS', 'PAUSED', 'DELAYED', 'OVERDUE');

-- 3. Dọn dẹp an toàn cột rác: care_schedule.cancel_reason và health_records.source_training_workout_id
DROP VIEW IF EXISTS care_schedules CASCADE;
ALTER TABLE care_schedule DROP COLUMN IF EXISTS cancel_reason;
CREATE OR REPLACE VIEW care_schedules AS SELECT * FROM care_schedule;

ALTER TABLE health_records DROP COLUMN IF EXISTS source_training_workout_id;
