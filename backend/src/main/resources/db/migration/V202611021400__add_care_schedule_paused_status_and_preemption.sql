-- V202611021400__add_care_schedule_paused_status_and_preemption.sql
-- Add paused_from timestamp to care_schedule for preemption SLA freezing (WF-02 §3.2 / D2)

ALTER TABLE care_schedule
    ADD COLUMN IF NOT EXISTS paused_from TIMESTAMP NULL;

COMMENT ON COLUMN care_schedule.paused_from IS 'Timestamp when an in-progress examination was paused for emergency preemption';
