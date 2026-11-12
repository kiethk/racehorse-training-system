ALTER TABLE admission_applications
    ADD COLUMN assigned_groom_id BIGINT REFERENCES users(id),
    ADD COLUMN groom_assigned_at TIMESTAMP,
    ADD COLUMN groom_review_due_at TIMESTAMP;

CREATE INDEX idx_admission_assigned_groom_review ON admission_applications(assigned_groom_id)
    WHERE status = 'GROOM_REVIEW';

CREATE TABLE admission_groom_assignments (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL REFERENCES admission_applications(id),
    groom_id BIGINT NOT NULL REFERENCES users(id),
    assigned_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    end_reason VARCHAR(30) CHECK (end_reason IN
        ('DECIDED', 'OVERDUE', 'FULL', 'LOST_QUARANTINE', 'MANUAL', 'REOPENED')),
    assigned_by_id BIGINT REFERENCES users(id)
);
CREATE INDEX idx_groom_assignment_admission ON admission_groom_assignments(admission_id);
CREATE INDEX idx_groom_assignment_open_groom ON admission_groom_assignments(groom_id) WHERE ended_at IS NULL;
CREATE UNIQUE INDEX uq_groom_assignment_open_admission ON admission_groom_assignments(admission_id)
    WHERE ended_at IS NULL;

ALTER TABLE groom_profiles ADD COLUMN last_arrival_confirmed_at TIMESTAMP;
INSERT INTO groom_profiles (user_id, last_arrival_confirmed_at)
SELECT ss.groom_id, MAX(a.arrived_at)
FROM admission_applications a
JOIN stable_stalls ss ON ss.id = a.quarantine_stall_id
WHERE ss.groom_id IS NOT NULL AND a.arrived_at IS NOT NULL
GROUP BY ss.groom_id
ON CONFLICT (user_id) DO UPDATE SET last_arrival_confirmed_at = EXCLUDED.last_arrival_confirmed_at;

-- V48 guards candidate snapshots and documents, not updates of admission_applications.
-- V25 and V73 allow GROOM_REVIEW without a decision, horse or quarantine stall.
UPDATE stable_stalls ss SET status = 'AVAILABLE'
WHERE ss.status = 'RESERVED' AND EXISTS (
    SELECT 1 FROM admission_applications a
    WHERE a.status = 'WAITING_FOR_STALL' AND a.quarantine_stall_id = ss.id
);
UPDATE admission_applications
SET status = 'GROOM_REVIEW', groom_decision = NULL, groom_feedback = NULL,
    groom_reviewed_at = NULL, groom_id = NULL, quarantine_stall_id = NULL, updated_at = NOW()
WHERE status = 'WAITING_FOR_STALL';
