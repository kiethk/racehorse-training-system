-- RTMS manual mock data seed for local demo/review.
--
-- Usage:
--   psql "postgresql://rtms_user:rtms_password@localhost:5433/rtms_db" -f backend/scripts/seed_mock_data.sql
--
-- Notes:
--   - This is intentionally NOT a Flyway migration.
--   - It is safe to run more than once on a normal local development database.
--   - Demo login password for seeded users is the same hash used by existing test accounts: admin123.

BEGIN;

-- ---------------------------------------------------------------------
-- 1. Demo users and operational profiles
-- ---------------------------------------------------------------------

INSERT INTO users (full_name, email, password_hash, role_id, phone, address, is_active)
SELECT seed.full_name,
       seed.email,
       '$2a$10$2rXcS5LHx7wVYOkClxQ1YuIvg4myh7UgloaGcwcgvnahK9pTWVEJ.',
       r.id,
       seed.phone,
       seed.address,
       true
FROM (
    VALUES
        ('Demo Owner', 'mock.owner@rtms.local', '+84 900 000 101', 'Mock owner address'),
        ('Demo Groom', 'mock.groom@rtms.local', '+84 900 000 102', 'Mock groom dormitory'),
        ('Demo Veterinarian', 'mock.vet@rtms.local', '+84 900 000 103', 'Mock veterinary office'),
        ('Demo Head Trainer', 'mock.trainer@rtms.local', '+84 900 000 104', 'Mock training office'),
        ('Demo Club Manager', 'mock.manager@rtms.local', '+84 900 000 105', 'Mock manager office')
) AS seed(full_name, email, phone, address)
JOIN roles r ON r.name = CASE seed.email
    WHEN 'mock.owner@rtms.local' THEN 'HORSE_OWNER'
    WHEN 'mock.groom@rtms.local' THEN 'GROOM'
    WHEN 'mock.vet@rtms.local' THEN 'VETERINARIAN'
    WHEN 'mock.trainer@rtms.local' THEN 'HEAD_TRAINER'
    WHEN 'mock.manager@rtms.local' THEN 'CLUB_MANAGER'
END
WHERE NOT EXISTS (
    SELECT 1 FROM users u WHERE u.email = seed.email
);

INSERT INTO groom_profiles (user_id, trainer_id)
SELECT groom.id, trainer.id
FROM users groom
JOIN users trainer ON trainer.email = 'mock.trainer@rtms.local'
WHERE groom.email = 'mock.groom@rtms.local'
ON CONFLICT (user_id) DO UPDATE
SET trainer_id = EXCLUDED.trainer_id;

INSERT INTO veterinarian_profiles (user_id, license_number, license_issued_date, specialization)
SELECT u.id, 'MOCK-VET-001', DATE '2022-01-15', 'Equine admission screening'
FROM users u
WHERE u.email = 'mock.vet@rtms.local'
ON CONFLICT (user_id) DO UPDATE
SET license_number = EXCLUDED.license_number,
    license_issued_date = EXCLUDED.license_issued_date,
    specialization = EXCLUDED.specialization;

INSERT INTO trainer_profiles (user_id, certification_number, certification_issued_date, years_of_experience)
SELECT u.id, 'MOCK-TRAINER-001', DATE '2020-05-10', 8
FROM users u
WHERE u.email = 'mock.trainer@rtms.local'
ON CONFLICT (user_id) DO UPDATE
SET certification_number = EXCLUDED.certification_number,
    certification_issued_date = EXCLUDED.certification_issued_date,
    years_of_experience = EXCLUDED.years_of_experience;

-- Assign a small, visible care area to the mock groom.
UPDATE stable_stalls
SET groom_id = (SELECT id FROM users WHERE email = 'mock.groom@rtms.local'),
    updated_at = NOW()
WHERE stall_code IN ('Q2', 'Q3', 'Q4', 'A3', 'A4', 'A5');

-- ---------------------------------------------------------------------
-- 2. Admission applications in every important business state
-- ---------------------------------------------------------------------

WITH demo_owner AS (
    SELECT id FROM users WHERE email = 'mock.owner@rtms.local'
),
seed AS (
    SELECT *
    FROM (
        VALUES
            ('RTMSMOCK0000001', 'Azure Comet', 'Thoroughbred', DATE '2021-03-14', 'Vietnam Studbook',
             'Sire Atlas', 'RTMSSIRE0000001', 'Dam Aurora', 'RTMSDAM00000001',
             'Fresh owner submission. Groom has not started review yet.',
             NOW() - INTERVAL '6 days'),
            ('RTMSMOCK0000002', 'Quarantine Bolt', 'Arabian', DATE '2020-08-08', 'Vietnam Studbook',
             'Sire Boreal', 'RTMSSIRE0000002', 'Dam Breeze', 'RTMSDAM00000002',
             'Groom approved. Waiting for initial veterinarian review.',
             NOW() - INTERVAL '5 days'),
            ('RTMSMOCK0000003', 'Trainer Meadow', 'Thoroughbred', DATE '2019-11-21', 'Vietnam Studbook',
             'Sire Cobalt', 'RTMSSIRE0000003', 'Dam Clover', 'RTMSDAM00000003',
             'Vet approved. Trainer needs to complete racing readiness assessment.',
             NOW() - INTERVAL '4 days'),
            ('RTMSMOCK0000004', 'Manager Star', 'Quarter Horse', DATE '2020-01-30', 'Vietnam Studbook',
             'Sire Dakota', 'RTMSSIRE0000004', 'Dam Dawn', 'RTMSDAM00000004',
             'Ready for final manager decision and regular stall assignment.',
             NOW() - INTERVAL '3 days'),
            ('RTMSMOCK0000005', 'Approved Harbor', 'Thoroughbred', DATE '2018-06-12', 'Vietnam Studbook',
             'Sire Echo', 'RTMSSIRE0000005', 'Dam Ember', 'RTMSDAM00000005',
             'Fully approved horse for downstream stable, health, grooming, and training screens.',
             NOW() - INTERVAL '2 days'),
            ('RTMSMOCK0000006', 'Rejected Ember', 'Warmblood', DATE '2022-02-18', 'Vietnam Studbook',
             'Sire Falcon', 'RTMSSIRE0000006', 'Dam Fern', 'RTMSDAM00000006',
             'Rejected example with clear feedback for owner view.',
             NOW() - INTERVAL '1 day')
    ) AS v(registration_number, name, breed, date_of_birth, registry_name,
           sire_name, sire_registration_number, dam_name, dam_registration_number,
           pedigree_notes, submitted_at)
)
INSERT INTO admission_applications (owner_id, status, submitted_at, created_at, updated_at)
SELECT demo_owner.id, 'GROOM_REVIEW', seed.submitted_at, seed.submitted_at, NOW()
FROM seed
CROSS JOIN demo_owner
WHERE NOT EXISTS (
    SELECT 1
    FROM candidate_horse_profiles existing
    WHERE existing.registration_number = seed.registration_number
);

WITH seed AS (
    SELECT *
    FROM (
        VALUES
            ('RTMSMOCK0000001', 'Azure Comet', 'Thoroughbred', DATE '2021-03-14', 'Vietnam Studbook',
             'Sire Atlas', 'RTMSSIRE0000001', 'Dam Aurora', 'RTMSDAM00000001',
             'Fresh owner submission. Groom has not started review yet.', NOW() - INTERVAL '6 days'),
            ('RTMSMOCK0000002', 'Quarantine Bolt', 'Arabian', DATE '2020-08-08', 'Vietnam Studbook',
             'Sire Boreal', 'RTMSSIRE0000002', 'Dam Breeze', 'RTMSDAM00000002',
             'Groom approved. Waiting for initial veterinarian review.', NOW() - INTERVAL '5 days'),
            ('RTMSMOCK0000003', 'Trainer Meadow', 'Thoroughbred', DATE '2019-11-21', 'Vietnam Studbook',
             'Sire Cobalt', 'RTMSSIRE0000003', 'Dam Clover', 'RTMSDAM00000003',
             'Vet approved. Trainer needs to complete racing readiness assessment.', NOW() - INTERVAL '4 days'),
            ('RTMSMOCK0000004', 'Manager Star', 'Quarter Horse', DATE '2020-01-30', 'Vietnam Studbook',
             'Sire Dakota', 'RTMSSIRE0000004', 'Dam Dawn', 'RTMSDAM00000004',
             'Ready for final manager decision and regular stall assignment.', NOW() - INTERVAL '3 days'),
            ('RTMSMOCK0000005', 'Approved Harbor', 'Thoroughbred', DATE '2018-06-12', 'Vietnam Studbook',
             'Sire Echo', 'RTMSSIRE0000005', 'Dam Ember', 'RTMSDAM00000005',
             'Fully approved horse for downstream stable, health, grooming, and training screens.', NOW() - INTERVAL '2 days'),
            ('RTMSMOCK0000006', 'Rejected Ember', 'Warmblood', DATE '2022-02-18', 'Vietnam Studbook',
             'Sire Falcon', 'RTMSSIRE0000006', 'Dam Fern', 'RTMSDAM00000006',
             'Rejected example with clear feedback for owner view.', NOW() - INTERVAL '1 day')
    ) AS v(registration_number, name, breed, date_of_birth, registry_name,
           sire_name, sire_registration_number, dam_name, dam_registration_number,
           pedigree_notes, submitted_at)
),
newest_unprofiled_admission AS (
    SELECT seed.*,
           a.id AS admission_id,
           ROW_NUMBER() OVER (PARTITION BY seed.registration_number ORDER BY a.id DESC) AS rn
    FROM seed
    JOIN admission_applications a
      ON a.status = 'GROOM_REVIEW'
     AND a.submitted_at = seed.submitted_at
    WHERE NOT EXISTS (
        SELECT 1
        FROM candidate_horse_profiles existing
        WHERE existing.registration_number = seed.registration_number
    )
    AND NOT EXISTS (
        SELECT 1
        FROM candidate_horse_profiles profile
        WHERE profile.admission_id = a.id
    )
)
INSERT INTO candidate_horse_profiles (
    admission_id,
    name,
    breed,
    date_of_birth,
    registration_number,
    registry_name,
    sire_name,
    sire_registration_number,
    dam_name,
    dam_registration_number,
    pedigree_notes
)
SELECT admission_id,
       name,
       breed,
       date_of_birth,
       registration_number,
       registry_name,
       sire_name,
       sire_registration_number,
       dam_name,
       dam_registration_number,
       pedigree_notes
FROM newest_unprofiled_admission
WHERE rn = 1;

INSERT INTO admission_documents (admission_id, document_type, file_url, record_date, note)
SELECT c.admission_id,
       doc.document_type,
       doc.file_url,
       doc.record_date,
       doc.note
FROM candidate_horse_profiles c
JOIN (
    VALUES
        ('RTMSMOCK0000001', 'REGISTRATION_DOCUMENT', '/mock/admissions/azure-comet-registration.pdf', DATE '2026-09-01', 'Mock registration document'),
        ('RTMSMOCK0000002', 'HEALTH_CERTIFICATE', '/mock/admissions/quarantine-bolt-health.pdf', DATE '2026-09-02', 'Mock health certificate'),
        ('RTMSMOCK0000003', 'VACCINATION_RECORD', '/mock/admissions/trainer-meadow-vaccination.pdf', DATE '2026-09-03', 'Mock vaccination record'),
        ('RTMSMOCK0000004', 'PEDIGREE_CERTIFICATE', '/mock/admissions/manager-star-pedigree.pdf', DATE '2026-09-04', 'Mock pedigree certificate'),
        ('RTMSMOCK0000005', 'HORSE_PHOTO', '/mock/admissions/approved-harbor-photo.jpg', DATE '2026-09-05', 'Mock horse photo'),
        ('RTMSMOCK0000006', 'PREVIOUS_MEDICAL_RECORD', '/mock/admissions/rejected-ember-medical.pdf', DATE '2026-09-06', 'Mock previous medical record')
) AS doc(registration_number, document_type, file_url, record_date, note)
    ON doc.registration_number = c.registration_number
WHERE NOT EXISTS (
    SELECT 1
    FROM admission_documents existing
    WHERE existing.admission_id = c.admission_id
      AND existing.document_type = doc.document_type
      AND existing.file_url = doc.file_url
);

-- ---------------------------------------------------------------------
-- 3. Horses created by the post-Groom admission flow
-- ---------------------------------------------------------------------

INSERT INTO horses (
    name,
    breed,
    date_of_birth,
    current_status,
    owner_id,
    registry_name,
    registration_number,
    created_at,
    updated_at
)
SELECT c.name,
       c.breed,
       c.date_of_birth,
       CASE c.registration_number
           WHEN 'RTMSMOCK0000005' THEN 'ELIGIBLE'
           WHEN 'RTMSMOCK0000006' THEN 'REJECTED'
           ELSE 'CANDIDATE'
       END,
       a.owner_id,
       c.registry_name,
       c.registration_number,
       NOW(),
       NOW()
FROM candidate_horse_profiles c
JOIN admission_applications a ON a.id = c.admission_id
WHERE c.registration_number IN (
    'RTMSMOCK0000002',
    'RTMSMOCK0000003',
    'RTMSMOCK0000004',
    'RTMSMOCK0000005',
    'RTMSMOCK0000006'
)
ON CONFLICT (registration_number) DO NOTHING;

INSERT INTO horse_pedigrees (
    horse_id,
    sire_name,
    sire_registration_number,
    dam_name,
    dam_registration_number,
    pedigree_notes
)
SELECT h.id,
       c.sire_name,
       c.sire_registration_number,
       c.dam_name,
       c.dam_registration_number,
       c.pedigree_notes
FROM horses h
JOIN candidate_horse_profiles c ON c.registration_number = h.registration_number
WHERE c.registration_number LIKE 'RTMSMOCK00000%'
ON CONFLICT (horse_id) DO UPDATE
SET sire_name = EXCLUDED.sire_name,
    sire_registration_number = EXCLUDED.sire_registration_number,
    dam_name = EXCLUDED.dam_name,
    dam_registration_number = EXCLUDED.dam_registration_number,
    pedigree_notes = EXCLUDED.pedigree_notes;

-- ---------------------------------------------------------------------
-- 4. Advance admissions after snapshots/documents are in place
-- ---------------------------------------------------------------------

UPDATE stable_stalls
SET status = 'OCCUPIED',
    updated_at = NOW()
WHERE stall_code IN ('Q2', 'Q3', 'Q4')
  AND EXISTS (
      SELECT 1
      FROM candidate_horse_profiles c
      WHERE c.registration_number IN ('RTMSMOCK0000002', 'RTMSMOCK0000003', 'RTMSMOCK0000004')
  );

UPDATE stable_stalls
SET status = 'OCCUPIED',
    updated_at = NOW()
WHERE stall_code = 'A3'
  AND EXISTS (
      SELECT 1
      FROM candidate_horse_profiles c
      WHERE c.registration_number = 'RTMSMOCK0000005'
  );

UPDATE admission_applications a
SET status = state.status,
    quarantine_stall_id = q.id,
    groom_id = groom.id,
    groom_decision = state.groom_decision,
    groom_feedback = state.groom_feedback,
    groom_reviewed_at = state.groom_reviewed_at,
    veterinarian_id = vet.id,
    vet_decision = state.vet_decision,
    vet_feedback = state.vet_feedback,
    vet_reviewed_at = state.vet_reviewed_at,
    trainer_id = trainer.id,
    trainer_feedback = state.trainer_feedback,
    trainer_reviewed_at = state.trainer_reviewed_at,
    manager_id = manager_user.id,
    manager_decision = state.manager_decision,
    manager_feedback = state.manager_feedback,
    manager_reviewed_at = state.manager_reviewed_at,
    horse_id = h.id,
    updated_at = NOW()
FROM candidate_horse_profiles c
JOIN (
    VALUES
        ('RTMSMOCK0000002', 'VET_REVIEW', 'Q2', 'APPROVED', 'Pedigree and documents are complete. Move to quarantine.', NOW() - INTERVAL '4 days', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
        ('RTMSMOCK0000003', 'TRAINER_REVIEW', 'Q3', 'APPROVED', 'Physical identity verified. Quarantine stall assigned.', NOW() - INTERVAL '3 days', 'APPROVED', 'Initial exam is normal. Candidate may proceed to trainer assessment.', NOW() - INTERVAL '2 days', NULL, NULL, NULL, NULL, NULL),
        ('RTMSMOCK0000004', 'MANAGER_REVIEW', 'Q4', 'APPROVED', 'Documents and identity look good.', NOW() - INTERVAL '3 days', 'APPROVED', 'Vet screening passed. No blocking conditions.', NOW() - INTERVAL '2 days', 'Trainer assessment complete. Good temperament, needs conditioning plan.', NOW() - INTERVAL '1 day', NULL, NULL, NULL),
        ('RTMSMOCK0000005', 'APPROVED', 'Q5', 'APPROVED', 'Candidate matched submitted records.', NOW() - INTERVAL '5 days', 'APPROVED', 'Health screening passed.', NOW() - INTERVAL '4 days', 'Ready for regular training after a short conditioning period.', NOW() - INTERVAL '3 days', 'APPROVED', 'Approved for regular stable intake.', NOW() - INTERVAL '2 days'),
        ('RTMSMOCK0000006', 'REJECTED', NULL, 'REJECTED', 'Registration evidence is inconsistent. Owner must resubmit corrected documents.', NOW() - INTERVAL '1 day', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
) AS state(registration_number, status, quarantine_code, groom_decision, groom_feedback, groom_reviewed_at,
           vet_decision, vet_feedback, vet_reviewed_at,
           trainer_feedback, trainer_reviewed_at,
           manager_decision, manager_feedback, manager_reviewed_at)
    ON state.registration_number = c.registration_number
LEFT JOIN stable_stalls q ON q.stall_code = state.quarantine_code
LEFT JOIN horses h ON h.registration_number = c.registration_number
LEFT JOIN users groom ON groom.email = 'mock.groom@rtms.local'
LEFT JOIN users vet ON vet.email = 'mock.vet@rtms.local'
LEFT JOIN users trainer ON trainer.email = 'mock.trainer@rtms.local'
LEFT JOIN users manager_user ON manager_user.email = 'mock.manager@rtms.local'
WHERE a.id = c.admission_id;

UPDATE horses h
SET current_status = CASE h.registration_number
        WHEN 'RTMSMOCK0000005' THEN 'ELIGIBLE'
        WHEN 'RTMSMOCK0000006' THEN 'REJECTED'
        ELSE 'CANDIDATE'
    END,
    current_stall_id = CASE h.registration_number
        WHEN 'RTMSMOCK0000002' THEN (SELECT id FROM stable_stalls WHERE stall_code = 'Q2')
        WHEN 'RTMSMOCK0000003' THEN (SELECT id FROM stable_stalls WHERE stall_code = 'Q3')
        WHEN 'RTMSMOCK0000004' THEN (SELECT id FROM stable_stalls WHERE stall_code = 'Q4')
        WHEN 'RTMSMOCK0000005' THEN (SELECT id FROM stable_stalls WHERE stall_code = 'A3')
        ELSE NULL
    END,
    updated_at = NOW()
WHERE h.registration_number IN (
    'RTMSMOCK0000002',
    'RTMSMOCK0000003',
    'RTMSMOCK0000004',
    'RTMSMOCK0000005',
    'RTMSMOCK0000006'
);

-- ---------------------------------------------------------------------
-- 5. Supporting health, trainer, groom, and audit records
-- ---------------------------------------------------------------------

DELETE FROM preventive_care_schedules pcs
USING horses h
WHERE pcs.horse_id = h.id
  AND h.registration_number LIKE 'RTMSMOCK00000%'
  AND pcs.care_type = 'INITIAL_EXAM'
  AND pcs.id NOT IN (
      SELECT MIN(keep_pcs.id)
      FROM preventive_care_schedules keep_pcs
      JOIN horses keep_h ON keep_h.id = keep_pcs.horse_id
      WHERE keep_h.registration_number LIKE 'RTMSMOCK00000%'
        AND keep_pcs.care_type = 'INITIAL_EXAM'
      GROUP BY keep_pcs.horse_id, keep_pcs.care_type
  );

INSERT INTO preventive_care_schedules (
    horse_id,
    veterinarian_id,
    care_type,
    scheduled_date,
    description,
    status,
    created_at,
    updated_at
)
SELECT h.id,
       NULL,
       'INITIAL_EXAM',
       NULL,
       'Initial admission physical examination in quarantine area',
       CASE h.registration_number
           WHEN 'RTMSMOCK0000006' THEN 'CANCELLED'
           ELSE 'PENDING'
       END,
       NOW(),
       NOW()
FROM horses h
WHERE h.registration_number IN (
    'RTMSMOCK0000002',
    'RTMSMOCK0000003',
    'RTMSMOCK0000004',
    'RTMSMOCK0000005',
    'RTMSMOCK0000006'
)
AND NOT EXISTS (
    SELECT 1
    FROM preventive_care_schedules pcs
    WHERE pcs.horse_id = h.id
      AND pcs.care_type = 'INITIAL_EXAM'
);

INSERT INTO health_records (
    horse_id,
    veterinarian_id,
    examined_at,
    symptoms,
    findings,
    diagnosis,
    record_type,
    notes,
    follow_up_date,
    created_at,
    updated_at
)
SELECT h.id,
       vet.id,
       NOW() - INTERVAL '2 days',
       'Admission screening',
       CASE h.registration_number
           WHEN 'RTMSMOCK0000003' THEN 'Vitals stable. Mild stiffness after transport.'
           WHEN 'RTMSMOCK0000004' THEN 'Vitals stable. No contagious symptoms observed.'
           ELSE 'Vitals stable. Cleared after quarantine exam.'
       END,
       CASE h.registration_number
           WHEN 'RTMSMOCK0000003' THEN 'Transport fatigue'
           WHEN 'RTMSMOCK0000004' THEN 'Healthy admission candidate'
           ELSE 'Healthy'
       END,
       'INITIAL_EXAM',
       'Mock admission health record for demo screens.',
       CURRENT_DATE + 14,
       NOW(),
       NOW()
FROM horses h
JOIN users vet ON vet.email = 'mock.vet@rtms.local'
WHERE h.registration_number IN ('RTMSMOCK0000003', 'RTMSMOCK0000004', 'RTMSMOCK0000005')
AND NOT EXISTS (
    SELECT 1
    FROM health_records existing
    WHERE existing.horse_id = h.id
      AND existing.record_type = 'INITIAL_EXAM'
      AND existing.notes = 'Mock admission health record for demo screens.'
);

INSERT INTO racing_readiness_assessments (
    horse_id,
    readiness_status,
    fitness_score,
    conformation_score,
    temperament_score,
    gait_quality_score,
    estimated_months_to_race,
    assessment_date,
    valid_until,
    remarks,
    trainer_id,
    admission_id,
    created_at
)
SELECT h.id,
       'NEEDS_MORE_TRAINING',
       NULL,
       7.5,
       8.0,
       7.0,
       4,
       CURRENT_DATE - 1,
       NULL,
       'Mock trainer admission assessment: promising candidate, needs conditioning before race registration.',
       trainer.id,
       a.id,
       NOW()
FROM horses h
JOIN candidate_horse_profiles c ON c.registration_number = h.registration_number
JOIN admission_applications a ON a.id = c.admission_id
JOIN users trainer ON trainer.email = 'mock.trainer@rtms.local'
WHERE h.registration_number IN ('RTMSMOCK0000004', 'RTMSMOCK0000005')
ON CONFLICT DO NOTHING;

INSERT INTO groom_daily_tasks (
    groom_id,
    horse_id,
    task_type,
    scheduled_time,
    completed_at,
    is_completed,
    notes
)
SELECT groom.id,
       h.id,
       task.task_type,
       task.scheduled_time,
       task.completed_at,
       task.is_completed,
       task.notes
FROM users groom
JOIN horses h ON h.registration_number = 'RTMSMOCK0000005'
JOIN (
    VALUES
        ('FEEDING', NOW() - INTERVAL '3 hours', NOW() - INTERVAL '2 hours 45 minutes', true, 'Morning feed completed for approved demo horse.'),
        ('STALL_CLEANING', NOW() + INTERVAL '2 hours', NULL, false, 'Afternoon stall cleaning pending.')
) AS task(task_type, scheduled_time, completed_at, is_completed, notes) ON TRUE
WHERE groom.email = 'mock.groom@rtms.local'
AND NOT EXISTS (
    SELECT 1
    FROM groom_daily_tasks existing
    WHERE existing.groom_id = groom.id
      AND existing.horse_id = h.id
      AND existing.task_type = task.task_type
      AND existing.notes = task.notes
);

INSERT INTO groom_incident_reports (
    groom_id,
    horse_id,
    title,
    description,
    image_url,
    severity,
    reported_at,
    status,
    handled_by_id,
    handled_at,
    handler_note
)
SELECT groom.id,
       h.id,
       'Reduced appetite during morning check',
       'Horse left part of the morning feed. No visible injury, but should be monitored by Vet.',
       NULL,
       'MEDIUM',
       NOW() - INTERVAL '6 hours',
       'IN_REVIEW',
       vet.id,
       NOW() - INTERVAL '5 hours',
       'Vet accepted the incident for follow-up observation.'
FROM users groom
JOIN users vet ON vet.email = 'mock.vet@rtms.local'
JOIN horses h ON h.registration_number = 'RTMSMOCK0000005'
WHERE groom.email = 'mock.groom@rtms.local'
AND NOT EXISTS (
    SELECT 1
    FROM groom_incident_reports existing
    WHERE existing.horse_id = h.id
      AND existing.title = 'Reduced appetite during morning check'
);

INSERT INTO audit_logs (user_id, action, entity_name, entity_id, created_at)
SELECT manager_user.id,
       'MOCK_DATA_SEEDED',
       'admission_applications',
       a.id,
       NOW()
FROM users manager_user
JOIN candidate_horse_profiles c ON c.registration_number = 'RTMSMOCK0000004'
JOIN admission_applications a ON a.id = c.admission_id
WHERE manager_user.email = 'mock.manager@rtms.local'
AND NOT EXISTS (
    SELECT 1
    FROM audit_logs existing
    WHERE existing.action = 'MOCK_DATA_SEEDED'
      AND existing.entity_name = 'admission_applications'
      AND existing.entity_id = a.id
);

COMMIT;

-- Quick check:
-- SELECT a.id, c.name, a.status, h.current_status, qs.stall_code AS quarantine_stall
-- FROM admission_applications a
-- JOIN candidate_horse_profiles c ON c.admission_id = a.id
-- LEFT JOIN horses h ON h.id = a.horse_id
-- LEFT JOIN stable_stalls qs ON qs.id = a.quarantine_stall_id
-- WHERE c.registration_number LIKE 'RTMSMOCK00000%'
-- ORDER BY c.registration_number;
