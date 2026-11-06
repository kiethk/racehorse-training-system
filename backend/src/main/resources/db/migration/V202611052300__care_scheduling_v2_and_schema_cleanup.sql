-- V202611052300__care_scheduling_v2_and_schema_cleanup.sql
-- Care Scheduling V2.0 & Schema Cleanup
-- 1. Add scheduled_date and rollover_count to care_schedule
-- 2. Backfill scheduled_date from scheduled_at / requested_at
-- 3. Drop dead columns: request_fingerprint, paused_from
-- 4. Extend reschedule_reason constraint with URGENT_PREEMPTION, TIME_EXCEEDED, PEER_OFFLOAD

DROP VIEW IF EXISTS care_schedules CASCADE;

ALTER TABLE care_schedule
    ADD COLUMN IF NOT EXISTS scheduled_date DATE NULL,
    ADD COLUMN IF NOT EXISTS rollover_count INT NOT NULL DEFAULT 0;

-- Backfill scheduled_date
UPDATE care_schedule
   SET scheduled_date = CAST(scheduled_at AS DATE)
 WHERE scheduled_date IS NULL AND scheduled_at IS NOT NULL;

UPDATE care_schedule
   SET scheduled_date = CAST(requested_at AS DATE)
 WHERE scheduled_date IS NULL AND requested_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_care_schedule_vet_scheduled_date
    ON care_schedule(veterinarian_id, scheduled_date);

-- Drop dead columns
ALTER TABLE care_schedule
    DROP COLUMN IF EXISTS request_fingerprint,
    DROP COLUMN IF EXISTS paused_from;

-- Recreate compatibility view
CREATE OR REPLACE VIEW care_schedules AS SELECT * FROM care_schedule;

-- Extend reschedule reason check constraint
ALTER TABLE care_schedule DROP CONSTRAINT IF EXISTS chk_care_schedule_reschedule_reason;
ALTER TABLE care_schedule ADD CONSTRAINT chk_care_schedule_reschedule_reason
    CHECK (reschedule_reason IS NULL OR reschedule_reason IN (
        'RACE_CONFLICT', 'UNDER_TREATMENT', 'OWNER_REQUEST', 'WEATHER', 'EOD_ROLLOVER', 'OTHER',
        'URGENT_PREEMPTION', 'TIME_EXCEEDED', 'PEER_OFFLOAD'));

COMMENT ON COLUMN care_schedule.scheduled_date IS 'Target calendar date for examination (V2 date-based bucket)';
COMMENT ON COLUMN care_schedule.rollover_count IS 'Number of times this exam has been rolled over (FIFO priority tracking)';
