-- =====================================================================
-- Migration: V202611071200__simplify_care_schedule_status_to_four_states.sql
-- Description: Simplify CareSchedule status to exactly 4 states:
--              SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED.
--              Abolishes REQUESTED, OVERDUE, DELAYED, PAUSED.
-- =====================================================================

-- 1. Migrate any existing legacy statuses to SCHEDULED
UPDATE care_schedule
   SET status = 'SCHEDULED',
       rollover_count = GREATEST(rollover_count, 1)
 WHERE status IN ('OVERDUE', 'DELAYED', 'PAUSED');

UPDATE care_schedule
   SET status = 'SCHEDULED'
 WHERE status = 'REQUESTED';

-- 2. Drop and recreate check constraint with only the 4 canonical states
ALTER TABLE care_schedule DROP CONSTRAINT IF EXISTS chk_care_schedules_status;
ALTER TABLE care_schedule ADD CONSTRAINT chk_care_schedules_status
    CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'));

-- 3. Update column default to SCHEDULED and add cancel_reason column
ALTER TABLE care_schedule ALTER COLUMN status SET DEFAULT 'SCHEDULED';
ALTER TABLE care_schedule ADD COLUMN IF NOT EXISTS cancel_reason TEXT;

-- 4. Recreate partial indexes for the simplified active statuses (SCHEDULED, IN_PROGRESS)
DROP INDEX IF EXISTS uq_care_schedule_one_active_urgent_per_horse;
CREATE UNIQUE INDEX uq_care_schedule_one_active_urgent_per_horse
    ON care_schedule(horse_id)
    WHERE care_type = 'URGENT' AND status IN ('SCHEDULED', 'IN_PROGRESS');

DROP INDEX IF EXISTS uq_care_schedule_one_active_preventive_per_horse;
CREATE UNIQUE INDEX uq_care_schedule_one_active_preventive_per_horse
    ON care_schedule(horse_id)
    WHERE purpose = 'PREVENTIVE' AND status IN ('SCHEDULED', 'IN_PROGRESS');

DROP INDEX IF EXISTS idx_care_schedule_vet_slot;
CREATE INDEX idx_care_schedule_vet_slot
    ON care_schedule(veterinarian_id, scheduled_at)
    WHERE status IN ('SCHEDULED', 'IN_PROGRESS');

DROP INDEX IF EXISTS idx_care_schedule_horse_slot;
CREATE INDEX idx_care_schedule_horse_slot
    ON care_schedule(horse_id, scheduled_at)
    WHERE status IN ('SCHEDULED', 'IN_PROGRESS');

-- 5. Refresh view if exists
DROP VIEW IF EXISTS care_schedules CASCADE;
CREATE OR REPLACE VIEW care_schedules AS SELECT * FROM care_schedule;
