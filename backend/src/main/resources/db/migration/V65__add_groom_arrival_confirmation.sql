-- V65: Groom arrival gate and least-loaded veterinarian scheduling.
--
-- The V58-V61 refactor consolidated legacy vet_exams and
-- preventive_care_schedules into care_schedule. This migration intentionally
-- only references the consolidated table.

ALTER TABLE admission_applications
    ADD COLUMN IF NOT EXISTS arrival_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS arrival_confirmed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS arrival_confirmed_by BIGINT REFERENCES users(id) ON DELETE SET NULL;

-- Existing admissions that already have a Horse have already passed the
-- physical-arrival gate before this migration was introduced.
UPDATE admission_applications
SET arrival_status = 'CONFIRMED',
    arrival_confirmed_at = COALESCE(arrival_confirmed_at, groom_reviewed_at, updated_at, NOW()),
    arrival_confirmed_by = COALESCE(arrival_confirmed_by, groom_id)
WHERE horse_id IS NOT NULL
  AND status IN ('VET_REVIEW', 'PENDING_RECHECK', 'TRAINER_REVIEW', 'MANAGER_REVIEW', 'APPROVED');

ALTER TABLE admission_applications
    DROP CONSTRAINT IF EXISTS chk_admission_status,
    DROP CONSTRAINT IF EXISTS chk_admission_vet_decision,
    DROP CONSTRAINT IF EXISTS chk_admission_q_stall_required,
    DROP CONSTRAINT IF EXISTS chk_admission_post_groom_has_horse;

ALTER TABLE admission_applications
    ADD CONSTRAINT chk_admission_status CHECK (status IN (
        'GROOM_REVIEW', 'WAITING_FOR_STALL', 'WAITING_FOR_ARRIVAL',
        'VET_REVIEW', 'PENDING_RECHECK', 'TRAINER_REVIEW',
        'MANAGER_REVIEW', 'APPROVED', 'REJECTED'
    )),
    ADD CONSTRAINT chk_admission_vet_decision CHECK (
        vet_decision IS NULL OR vet_decision IN ('APPROVED', 'RECHECK_REQUIRED', 'REJECTED')
    ),
    ADD CONSTRAINT chk_admission_q_stall_required CHECK (
        status NOT IN (
            'WAITING_FOR_ARRIVAL', 'VET_REVIEW', 'PENDING_RECHECK',
            'TRAINER_REVIEW', 'MANAGER_REVIEW', 'APPROVED'
        )
        OR quarantine_stall_id IS NOT NULL
    ),
    ADD CONSTRAINT chk_admission_post_groom_has_horse CHECK (
        status NOT IN (
            'VET_REVIEW', 'PENDING_RECHECK', 'TRAINER_REVIEW',
            'MANAGER_REVIEW', 'APPROVED'
        )
        OR horse_id IS NOT NULL
    ) NOT VALID,
    ADD CONSTRAINT chk_admission_arrival_status CHECK (
        arrival_status IN ('PENDING', 'CONFIRMED')
    ),
    ADD CONSTRAINT chk_admission_confirmed_arrival_timestamp CHECK (
        arrival_status <> 'CONFIRMED' OR arrival_confirmed_at IS NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_admission_arrival_status
    ON admission_applications(arrival_status, status);

-- V60 preserved the original legacy rows in JSONB but normalized OVERDUE
-- preventive schedules to REQUESTED. Restore the overdue meaning and the
-- original Vet assignment where that information is available.
WITH overdue_legacy AS (
    SELECT cs.id,
           NULLIF(entry->'row'->>'veterinarian_id', '')::BIGINT AS legacy_vet_id,
           CASE
               WHEN (entry->'row'->>'scheduled_date') ~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}$'
               THEN ((entry->'row'->>'scheduled_date')::DATE + TIME '14:00')::TIMESTAMP
           END AS legacy_scheduled_at
    FROM care_schedule cs
    CROSS JOIN LATERAL jsonb_array_elements(cs.legacy_data) AS source(entry)
    WHERE source.entry->>'source' = 'preventive_care_schedules'
      AND source.entry->'row'->>'status' = 'OVERDUE'
)
UPDATE care_schedule cs
SET status = 'OVERDUE',
    veterinarian_id = COALESCE(cs.veterinarian_id, overdue_legacy.legacy_vet_id),
    scheduled_at = COALESCE(cs.scheduled_at, overdue_legacy.legacy_scheduled_at),
    updated_at = NOW()
FROM overdue_legacy
WHERE cs.id = overdue_legacy.id
  AND cs.status = 'REQUESTED';

ALTER TABLE care_schedule
    DROP CONSTRAINT IF EXISTS chk_care_schedule_status,
    ADD CONSTRAINT chk_care_schedule_status CHECK (status IN (
        'REQUESTED', 'AWAITING_VET_CONFIRMATION', 'SCHEDULED',
        'IN_PROGRESS', 'OVERDUE', 'COMPLETED', 'CANCELLED'
    ));

CREATE INDEX IF NOT EXISTS idx_care_schedule_vet_load
    ON care_schedule(veterinarian_id, status, horse_id);
