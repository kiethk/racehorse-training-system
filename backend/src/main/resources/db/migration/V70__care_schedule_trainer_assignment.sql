-- Spec §3.3: a CareSchedule carries both responsible parties.
--
-- veterinarianId: assigned directly by the scheduler (Vet Flow MVP §3.3).
-- trainerId:       assigned in the SAME transaction by the same scheduler pass,
--                   choosing the Head Trainer with the fewest assigned horses.
--                   Only the ID is written — no separate TrainingSession is created.
--
-- NULL until a Trainer is available, mirroring veterinarianId semantics.

ALTER TABLE care_schedule
    ADD COLUMN IF NOT EXISTS trainer_id BIGINT REFERENCES users(id);

-- Scheduler ranks Head Trainers by current assignment load; the queue index
-- keeps that ranking from degrading into a sequential scan.
CREATE INDEX IF NOT EXISTS idx_care_schedule_trainer
    ON care_schedule(trainer_id, scheduled_at)
    WHERE trainer_id IS NOT NULL;

-- Backfill INITIAL schedules that already have a scheduler-assigned Vet from the
-- admission's trainer so historical rows are not trainer-less by accident.
UPDATE care_schedule cs
SET trainer_id = a.trainer_id
FROM admission_applications a
WHERE cs.admission_id = a.id
  AND cs.trainer_id IS NULL
  AND a.trainer_id IS NOT NULL
  AND cs.care_type = 'INITIAL';

CREATE INDEX IF NOT EXISTS idx_care_schedule_source_incident
    ON care_schedule(source_incident_id)
    WHERE source_incident_id IS NOT NULL;

