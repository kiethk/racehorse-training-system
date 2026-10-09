ALTER TABLE care_schedule
    ADD COLUMN IF NOT EXISTS source_schedule_id BIGINT REFERENCES care_schedule(id),
    ADD COLUMN IF NOT EXISTS requested_by_id BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100),
    ADD COLUMN IF NOT EXISTS request_fingerprint VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uq_care_schedule_source_schedule
    ON care_schedule(source_schedule_id)
    WHERE source_schedule_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_care_schedule_request_idempotency
    ON care_schedule(requested_by_id, idempotency_key)
    WHERE requested_by_id IS NOT NULL AND idempotency_key IS NOT NULL;
