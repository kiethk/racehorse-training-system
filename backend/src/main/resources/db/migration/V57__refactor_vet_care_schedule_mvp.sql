-- V57__refactor_vet_care_schedule_mvp.sql
-- S2 Care Schedule and Vet Offer MVP schema refactoring

-- 1. Care Schedules table
CREATE TABLE IF NOT EXISTS care_schedules (
    id BIGSERIAL PRIMARY KEY,
    horse_id BIGINT NOT NULL REFERENCES horses(id),
    veterinarian_id BIGINT REFERENCES users(id),
    admission_id BIGINT REFERENCES admission_applications(id),
    source_incident_id BIGINT,
    care_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'REQUESTED',
    scheduled_at TIMESTAMP,
    duration_minutes INT NOT NULL DEFAULT 30,
    description TEXT,
    completed_at TIMESTAMP,
    cancel_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_care_schedules_horse_status ON care_schedules(horse_id, status);
CREATE INDEX IF NOT EXISTS idx_care_schedules_vet_status ON care_schedules(veterinarian_id, status);
CREATE INDEX IF NOT EXISTS idx_care_schedules_admission ON care_schedules(admission_id);

-- 2. Vet Offers table
CREATE TABLE IF NOT EXISTS vet_offers (
    id BIGSERIAL PRIMARY KEY,
    care_schedule_id BIGINT NOT NULL REFERENCES care_schedules(id) ON DELETE CASCADE,
    veterinarian_id BIGINT NOT NULL REFERENCES users(id),
    proposed_scheduled_at TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    round INT NOT NULL DEFAULT 1,
    offered_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP,
    responded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_vet_offer_schedule_vet_round UNIQUE (care_schedule_id, veterinarian_id, round)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_vet_offer_pending_schedule ON vet_offers (care_schedule_id) WHERE status = 'PENDING';
CREATE UNIQUE INDEX IF NOT EXISTS uq_vet_offer_accepted_schedule ON vet_offers (care_schedule_id) WHERE status = 'ACCEPTED';
CREATE INDEX IF NOT EXISTS idx_vet_offers_vet_status ON vet_offers(veterinarian_id, status);

-- 3. Update health_records table
ALTER TABLE health_records ADD COLUMN IF NOT EXISTS care_schedule_id BIGINT REFERENCES care_schedules(id);
ALTER TABLE health_records ADD COLUMN IF NOT EXISTS training_decision VARCHAR(50);
ALTER TABLE health_records ADD COLUMN IF NOT EXISTS restriction_details TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS uq_health_records_care_schedule ON health_records (care_schedule_id) WHERE care_schedule_id IS NOT NULL;

-- 4. Update horses table
ALTER TABLE horses ADD COLUMN IF NOT EXISTS training_status VARCHAR(50) NOT NULL DEFAULT 'ALLOWED';
UPDATE horses SET training_status = CASE WHEN training_locked = TRUE THEN 'BLOCKED' ELSE 'ALLOWED' END WHERE training_status IS NULL OR training_status = 'ALLOWED';
