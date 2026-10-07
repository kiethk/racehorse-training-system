-- V71: Separate Trainer Schedule from Vet Care Schedule
-- Domain separation:
--   care_schedule: exclusively the Veterinarian task schedule
--   trainer_schedule: exclusively the Head Trainer readiness review task schedule

-- 1. Create trainer_schedule table
CREATE TABLE IF NOT EXISTS trainer_schedule (
    id                      BIGSERIAL PRIMARY KEY,
    horse_id                BIGINT NOT NULL REFERENCES horses(id),
    admission_id            BIGINT NOT NULL REFERENCES admission_applications(id),
    source_care_schedule_id BIGINT NOT NULL REFERENCES care_schedule(id),
    trainer_id              BIGINT NOT NULL REFERENCES users(id),
    status                  VARCHAR(50) NOT NULL,
    scheduled_at            TIMESTAMP NOT NULL,
    duration_minutes        INTEGER NOT NULL DEFAULT 30,
    completed_at            TIMESTAMP,
    created_at              TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP NOT NULL,
    CONSTRAINT uq_trainer_schedule_admission UNIQUE (admission_id),
    CONSTRAINT uq_trainer_schedule_care_schedule UNIQUE (source_care_schedule_id),
    CONSTRAINT chk_trainer_schedule_duration CHECK (duration_minutes > 0),
    CONSTRAINT chk_trainer_schedule_status CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED'))
);

CREATE INDEX IF NOT EXISTS idx_trainer_schedule_trainer
    ON trainer_schedule(trainer_id, status, scheduled_at);

CREATE INDEX IF NOT EXISTS idx_trainer_schedule_horse
    ON trainer_schedule(horse_id, scheduled_at);

-- 2. Add trainer_schedule_id to racing_readiness_assessments
ALTER TABLE racing_readiness_assessments
    ADD COLUMN IF NOT EXISTS trainer_schedule_id BIGINT UNIQUE REFERENCES trainer_schedule(id);

-- 3. Fail safely if any CARE_SCHEDULE rows have obsolete TRAINER_ASSIGNMENT care_type
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM care_schedule WHERE care_type = 'TRAINER_ASSIGNMENT') THEN
        RAISE EXCEPTION 'care_schedule contains rows with care_type = TRAINER_ASSIGNMENT which must be resolved manually';
    END IF;
END $$;

-- 4. Backfill historical completed Trainer work only when there is a completed Initial Care Schedule
INSERT INTO trainer_schedule (
    horse_id,
    admission_id,
    source_care_schedule_id,
    trainer_id,
    status,
    scheduled_at,
    duration_minutes,
    completed_at,
    created_at,
    updated_at
)
SELECT
    cs.horse_id,
    cs.admission_id,
    cs.id,
    COALESCE(rra.trainer_id, cs.trainer_id, a.trainer_id),
    'COMPLETED',
    COALESCE(cs.completed_at, cs.scheduled_at, NOW()),
    30,
    COALESCE(a.trainer_reviewed_at, rra.created_at, cs.completed_at, NOW()),
    COALESCE(cs.completed_at, cs.created_at, NOW()),
    COALESCE(a.trainer_reviewed_at, cs.updated_at, NOW())
FROM care_schedule cs
JOIN admission_applications a ON cs.admission_id = a.id
LEFT JOIN LATERAL (
    SELECT id, trainer_id, created_at
    FROM racing_readiness_assessments
    WHERE admission_id = a.id
    ORDER BY id ASC
    LIMIT 1
) rra ON true
WHERE cs.care_type = 'INITIAL'
  AND cs.status = 'COMPLETED'
  AND COALESCE(rra.trainer_id, cs.trainer_id, a.trainer_id) IS NOT NULL
  AND (rra.id IS NOT NULL OR a.trainer_reviewed_at IS NOT NULL OR a.status IN ('MANAGER_REVIEW', 'APPROVED', 'REJECTED'))
ON CONFLICT (admission_id) DO NOTHING;

-- 5. Backfill pending post-Vet reviews for Admissions in TRAINER_REVIEW with completed INITIAL care schedule
INSERT INTO trainer_schedule (
    horse_id,
    admission_id,
    source_care_schedule_id,
    trainer_id,
    status,
    scheduled_at,
    duration_minutes,
    completed_at,
    created_at,
    updated_at
)
SELECT
    cs.horse_id,
    cs.admission_id,
    cs.id,
    COALESCE(cs.trainer_id, a.trainer_id),
    'SCHEDULED',
    COALESCE(cs.completed_at, cs.scheduled_at, NOW()),
    30,
    NULL,
    COALESCE(cs.completed_at, cs.created_at, NOW()),
    NOW()
FROM care_schedule cs
JOIN admission_applications a ON cs.admission_id = a.id
WHERE cs.care_type = 'INITIAL'
  AND cs.status = 'COMPLETED'
  AND a.status = 'TRAINER_REVIEW'
  AND a.trainer_reviewed_at IS NULL
  AND COALESCE(cs.trainer_id, a.trainer_id) IS NOT NULL
ON CONFLICT (admission_id) DO NOTHING;

-- 6. Link racing_readiness_assessments.trainer_schedule_id where deterministic
UPDATE racing_readiness_assessments rra
SET trainer_schedule_id = ts.id
FROM trainer_schedule ts
WHERE rra.id = (
    SELECT id FROM racing_readiness_assessments
    WHERE admission_id = ts.admission_id
    ORDER BY id DESC LIMIT 1
)
  AND rra.trainer_schedule_id IS NULL;

-- 7. Remove obsolete trainer_id on care_schedule
DROP INDEX IF EXISTS idx_care_schedule_trainer;
ALTER TABLE care_schedule DROP COLUMN IF EXISTS trainer_id;

