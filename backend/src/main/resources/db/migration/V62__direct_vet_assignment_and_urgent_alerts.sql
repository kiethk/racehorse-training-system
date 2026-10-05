-- Replace the offer/acceptance workflow with direct, transactional assignment.
-- Historical offer rows are preserved under a deprecated table name for audit/export.

UPDATE care_schedule
SET status = 'REQUESTED', veterinarian_id = NULL, scheduled_at = NULL, updated_at = NOW()
WHERE status = 'AWAITING_VET_CONFIRMATION';

-- New REQUESTED rows are unassigned by definition. Normalize historical rows
-- before the application begins enforcing the direct-assignment invariant.
UPDATE care_schedule
SET veterinarian_id = NULL, scheduled_at = NULL, updated_at = NOW()
WHERE status = 'REQUESTED'
  AND (veterinarian_id IS NOT NULL OR scheduled_at IS NOT NULL);

ALTER TABLE vet_offers RENAME TO deprecated_vet_offers;
COMMENT ON TABLE deprecated_vet_offers IS
    'Read-only history from the removed Vet offer workflow. Application code must not use this table.';

ALTER INDEX IF EXISTS uq_vet_offer_pending_schedule RENAME TO deprecated_vet_offer_pending_schedule;
ALTER INDEX IF EXISTS uq_vet_offer_accepted_schedule RENAME TO deprecated_vet_offer_accepted_schedule;
ALTER INDEX IF EXISTS idx_vet_offers_vet_status RENAME TO deprecated_vet_offers_vet_status;

ALTER TABLE groom_incident_reports
    ADD COLUMN care_schedule_id BIGINT REFERENCES care_schedule(id);

UPDATE groom_incident_reports report
SET care_schedule_id = schedule.id
FROM care_schedule schedule
WHERE schedule.source_incident_id = report.id
  AND report.care_schedule_id IS NULL;

CREATE INDEX idx_incident_care_schedule
    ON groom_incident_reports(care_schedule_id)
    WHERE care_schedule_id IS NOT NULL;

CREATE INDEX idx_care_schedule_scheduler_queue
    ON care_schedule(status, care_type, created_at, id);
CREATE INDEX idx_care_schedule_vet_slot
    ON care_schedule(veterinarian_id, scheduled_at)
    WHERE status IN ('SCHEDULED', 'IN_PROGRESS');
CREATE INDEX idx_care_schedule_horse_slot
    ON care_schedule(horse_id, scheduled_at)
    WHERE status IN ('SCHEDULED', 'IN_PROGRESS');

-- Preserve every historical row while resolving any pre-existing duplicate
-- active urgent cases deterministically. The most-progressed, oldest case stays active.
WITH ranked_urgent AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY horse_id
               ORDER BY CASE status
                            WHEN 'IN_PROGRESS' THEN 0
                            WHEN 'SCHEDULED' THEN 1
                            ELSE 2
                        END,
                        created_at ASC,
                        id ASC
           ) AS position
    FROM care_schedule
    WHERE care_type = 'URGENT'
      AND status IN ('REQUESTED', 'SCHEDULED', 'IN_PROGRESS')
)
UPDATE care_schedule schedule
SET status = 'CANCELLED',
    cancel_reason = COALESCE(schedule.cancel_reason || '; ', '')
        || 'Superseded by active urgent-case invariant during V62 migration',
    updated_at = NOW()
FROM ranked_urgent ranked
WHERE schedule.id = ranked.id
  AND ranked.position > 1;

CREATE UNIQUE INDEX uq_care_schedule_one_active_urgent_per_horse
    ON care_schedule(horse_id)
    WHERE care_type = 'URGENT' AND status IN ('REQUESTED', 'SCHEDULED', 'IN_PROGRESS');
