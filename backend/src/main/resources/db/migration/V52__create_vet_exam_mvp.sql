-- Vet MVP: an examination request owns scheduling state; HealthRecord owns the
-- immutable clinical result. The veterinarian is final for Admission.

ALTER TABLE admission_applications
    DROP CONSTRAINT IF EXISTS chk_admission_status,
    DROP CONSTRAINT IF EXISTS chk_admission_vet_decision,
    DROP CONSTRAINT IF EXISTS chk_admission_q_stall_required,
    DROP CONSTRAINT IF EXISTS chk_admission_post_groom_has_horse;

ALTER TABLE admission_applications
    ADD CONSTRAINT chk_admission_status CHECK (status IN (
        'GROOM_REVIEW', 'WAITING_FOR_STALL', 'VET_REVIEW', 'PENDING_RECHECK',
        'TRAINER_REVIEW', 'MANAGER_REVIEW', 'APPROVED', 'REJECTED'
    )),
    ADD CONSTRAINT chk_admission_vet_decision CHECK (
        vet_decision IS NULL OR vet_decision IN ('APPROVED', 'RECHECK_REQUIRED', 'REJECTED')
    ),
    ADD CONSTRAINT chk_admission_q_stall_required CHECK (
        status NOT IN ('VET_REVIEW', 'PENDING_RECHECK', 'TRAINER_REVIEW', 'MANAGER_REVIEW', 'APPROVED')
        OR quarantine_stall_id IS NOT NULL
    ),
    ADD CONSTRAINT chk_admission_post_groom_has_horse CHECK (
        status NOT IN ('VET_REVIEW', 'PENDING_RECHECK', 'TRAINER_REVIEW', 'MANAGER_REVIEW', 'APPROVED')
        OR horse_id IS NOT NULL
    ) NOT VALID;

ALTER TABLE horses
    ADD COLUMN training_locked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN training_lock_reason TEXT,
    ADD COLUMN training_lock_review_date DATE,
    ADD COLUMN training_lock_vet_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN training_lock_updated_at TIMESTAMP;

UPDATE horses
SET training_locked = TRUE,
    training_lock_reason = 'Existing non-eligible horse state',
    training_lock_updated_at = NOW()
WHERE current_status <> 'ELIGIBLE';

CREATE TABLE vet_exams (
    id BIGSERIAL PRIMARY KEY,
    horse_id BIGINT NOT NULL REFERENCES horses(id) ON DELETE RESTRICT,
    admission_id BIGINT REFERENCES admission_applications(id) ON DELETE RESTRICT,
    exam_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    priority INTEGER NOT NULL,
    reason TEXT,
    created_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    assigned_vet_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    preferred_vet_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    requested_for_date DATE,
    scheduled_at TIMESTAMP,
    duration_minutes INTEGER NOT NULL DEFAULT 30,
    health_record_id BIGINT UNIQUE REFERENCES health_records(id) ON DELETE RESTRICT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_vet_exam_type CHECK (exam_type IN ('INITIAL', 'ROUTINE', 'URGENT', 'FOLLOW_UP')),
    CONSTRAINT chk_vet_exam_status CHECK (status IN ('REQUESTED', 'SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_vet_exam_duration CHECK (duration_minutes > 0),
    CONSTRAINT chk_vet_exam_scheduled_fields CHECK (
        status NOT IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED')
        OR (assigned_vet_id IS NOT NULL AND scheduled_at IS NOT NULL)
    ),
    CONSTRAINT chk_vet_exam_completed_record CHECK (
        status <> 'COMPLETED' OR health_record_id IS NOT NULL
    ),
    CONSTRAINT chk_vet_exam_initial_admission CHECK (
        exam_type <> 'INITIAL' OR admission_id IS NOT NULL
    )
);

CREATE INDEX idx_vet_exam_queue ON vet_exams(status, priority DESC, created_at ASC);
CREATE INDEX idx_vet_exam_vet_schedule ON vet_exams(assigned_vet_id, scheduled_at);
CREATE INDEX idx_vet_exam_horse_schedule ON vet_exams(horse_id, scheduled_at);
CREATE INDEX idx_vet_exam_admission ON vet_exams(admission_id, created_at DESC);
CREATE UNIQUE INDEX uq_vet_exam_active_admission_type
    ON vet_exams(admission_id, exam_type)
    WHERE admission_id IS NOT NULL
      AND status IN ('REQUESTED', 'SCHEDULED', 'IN_PROGRESS');

ALTER TABLE health_records
    ADD COLUMN treatment TEXT,
    ADD COLUMN vet_decision VARCHAR(30),
    ADD COLUMN rejection_reason TEXT,
    ADD CONSTRAINT chk_health_record_vet_decision CHECK (
        vet_decision IS NULL OR vet_decision IN ('APPROVED', 'RECHECK_REQUIRED', 'REJECTED')
    ),
    ADD CONSTRAINT chk_health_record_rejection_reason CHECK (
        vet_decision <> 'REJECTED' OR NULLIF(BTRIM(rejection_reason), '') IS NOT NULL
    ),
    ADD CONSTRAINT chk_health_record_follow_up CHECK (
        vet_decision <> 'RECHECK_REQUIRED' OR follow_up_date IS NOT NULL
    );

-- Convert pending Admission initial work into VetExam requests. Historical
-- completed preventive records remain untouched.
INSERT INTO vet_exams (
    horse_id, admission_id, exam_type, status, priority, reason,
    assigned_vet_id, scheduled_at, duration_minutes, created_at, updated_at
)
SELECT a.horse_id, a.id, 'INITIAL',
       CASE WHEN pcs.veterinarian_id IS NOT NULL AND pcs.scheduled_date IS NOT NULL
            THEN 'SCHEDULED' ELSE 'REQUESTED' END,
       300, COALESCE(pcs.description, 'Initial admission examination'),
       pcs.veterinarian_id,
       CASE WHEN pcs.scheduled_date IS NULL THEN NULL
            ELSE pcs.scheduled_date + TIME '13:30' END,
       30, COALESCE(pcs.created_at, NOW()), NOW()
FROM admission_applications a
LEFT JOIN LATERAL (
    SELECT p.* FROM preventive_care_schedules p
    WHERE p.horse_id = a.horse_id
      AND p.care_type = 'INITIAL_EXAM'
      AND p.status IN ('PENDING', 'OVERDUE')
    ORDER BY p.id DESC LIMIT 1
) pcs ON TRUE
WHERE a.status = 'VET_REVIEW'
  AND a.horse_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM vet_exams e
      WHERE e.admission_id = a.id AND e.exam_type = 'INITIAL'
  );

UPDATE preventive_care_schedules pcs
SET status = 'CANCELLED', updated_at = NOW()
WHERE pcs.care_type = 'INITIAL_EXAM'
  AND pcs.status IN ('PENDING', 'OVERDUE')
  AND EXISTS (
      SELECT 1 FROM vet_exams e
      WHERE e.horse_id = pcs.horse_id AND e.exam_type = 'INITIAL'
  );

INSERT INTO permissions (code, description) VALUES
    ('VET_EXAM_VIEW', 'View veterinary examination requests and results'),
    ('VET_EXAM_CREATE', 'Report an urgent veterinary examination request'),
    ('VET_EXAM_MANAGE', 'Start and complete veterinary examinations')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name IN ('GROOM', 'HEAD_TRAINER', 'VETERINARIAN', 'CLUB_MANAGER')
  AND p.code = 'VET_EXAM_VIEW'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name IN ('GROOM', 'HEAD_TRAINER', 'VETERINARIAN')
  AND p.code = 'VET_EXAM_CREATE'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'VETERINARIAN' AND p.code = 'VET_EXAM_MANAGE'
ON CONFLICT DO NOTHING;

-- Manager receives the report but cannot handle the clinical incident.
DELETE FROM role_permissions rp
USING roles r, permissions p
WHERE rp.role_id = r.id AND rp.permission_id = p.id
  AND r.name = 'CLUB_MANAGER' AND p.code = 'GROOM_INCIDENT_REPORT_HANDLE';
