ALTER TABLE admission_applications
    ADD COLUMN IF NOT EXISTS vet_training_decision VARCHAR(30);

-- Backfill from vet_decision for existing records
UPDATE admission_applications
SET vet_training_decision = CASE
    WHEN vet_decision = 'APPROVED' THEN 'ALLOWED'
    WHEN vet_decision = 'RECHECK_REQUIRED' THEN 'RESTRICTED'
    WHEN vet_decision = 'REJECTED' THEN 'BLOCKED'
    ELSE NULL
END
WHERE vet_training_decision IS NULL AND vet_decision IS NOT NULL;
