-- Local/manual seed for testing Groom admission -> arrival -> Vet assignment.
-- Run after all Flyway migrations, against a test database only.
-- Existing login accounts: owner@rtms.com / groom@rtms.com / vet@rtms.com (admin123).
--
-- Test flow:
--   1. Log in as owner@rtms.com and confirm the admission is in Groom review.
--   2. Log in as groom@rtms.com and approve it. It should become WAITING_FOR_ARRIVAL
--      with a quarantine stall reserved and a 14-day arrival deadline.
--   3. Confirm the horse has arrived. It should become VET_REVIEW and receive an
--      INITIAL care schedule with veterinarian_id assigned immediately to the
--      least-loaded eligible Vet (even if no appointment slot is currently free).
--   4. Run the verification query at the end to inspect the assigned Vet.

BEGIN;

-- SQL-only preflight (avoids a procedural DO block in SQL clients that split it).
-- If an account is missing or there is not enough stable capacity, this INSERT
-- fails its CHECK constraint and safely aborts the transaction.
CREATE TEMP TABLE groom_arrival_seed_preflight (
    requirements_met BOOLEAN NOT NULL CHECK (requirements_met)
) ON COMMIT DROP;

INSERT INTO groom_arrival_seed_preflight (requirements_met)
SELECT
    EXISTS (
        SELECT 1 FROM users u JOIN roles r ON r.id = u.role_id
        WHERE u.email = 'owner@rtms.com' AND r.name = 'HORSE_OWNER' AND u.is_active
    )
    AND EXISTS (
        SELECT 1 FROM users u JOIN roles r ON r.id = u.role_id
        WHERE u.email = 'groom@rtms.com' AND r.name = 'GROOM' AND u.is_active
    )
    AND EXISTS (
        SELECT 1 FROM users u JOIN roles r ON r.id = u.role_id
        WHERE u.email = 'vet@rtms.com' AND r.name = 'VETERINARIAN' AND u.is_active
    )
    AND (
        SELECT COUNT(*)
        FROM stable_stalls ss JOIN areas a ON a.id = ss.area_id
        WHERE a.type = 'QUARANTINE' AND ss.status = 'AVAILABLE'
    ) >= 1
    AND (
        SELECT COUNT(*)
        FROM stable_stalls ss JOIN areas a ON a.id = ss.area_id
        WHERE a.type = 'REGULAR' AND ss.status = 'AVAILABLE'
    ) >= (
        SELECT COUNT(*) + 1
        FROM stable_stalls ss JOIN areas a ON a.id = ss.area_id
        WHERE a.type = 'QUARANTINE' AND ss.status = 'OCCUPIED'
    );

-- Make the existing test Vet eligible for auto-assignment without overwriting
-- any profile that already exists.
INSERT INTO veterinarian_profiles (user_id, license_number, license_issued_date, specialization)
SELECT u.id, 'TEST-VET-ADMISSION-001', DATE '2024-01-01', 'Admission screening test'
FROM users u
WHERE u.email = 'vet@rtms.com'
ON CONFLICT (user_id) DO NOTHING;

CREATE TEMP TABLE groom_arrival_seed_target (
    admission_id BIGINT PRIMARY KEY
) ON COMMIT DROP;

-- Stable registration number makes the seed safe to re-run. If this admission
-- has already advanced in the workflow, keep that state instead of creating a duplicate.
WITH inserted_admission AS (
    INSERT INTO admission_applications (owner_id, status, submitted_at, created_at, updated_at)
    SELECT owner.id, 'GROOM_REVIEW', NOW(), NOW(), NOW()
    FROM users owner
    WHERE owner.email = 'owner@rtms.com'
      AND NOT EXISTS (
          SELECT 1
          FROM candidate_horse_profiles existing
          WHERE existing.registration_number = 'TSTARRIVAL00001'
      )
    RETURNING id
)
INSERT INTO groom_arrival_seed_target (admission_id)
SELECT id FROM inserted_admission;

INSERT INTO groom_arrival_seed_target (admission_id)
SELECT a.id
FROM candidate_horse_profiles c
JOIN admission_applications a ON a.id = c.admission_id
WHERE c.registration_number = 'TSTARRIVAL00001'
ORDER BY a.id
LIMIT 1
ON CONFLICT (admission_id) DO NOTHING;

INSERT INTO candidate_horse_profiles (
    admission_id, name, breed, date_of_birth, registration_number,
    registry_name, sire_name, dam_name, pedigree_notes, created_at, updated_at
)
SELECT target.admission_id,
       'TEST ARRIVAL AUTO ASSIGN',
       'Thoroughbred',
       DATE '2022-04-15',
       'TSTARRIVAL00001',
       'RTMS TEST REGISTRY',
       'TEST SIRE',
       'TEST DAM',
       'Seed for Groom arrival confirmation and immediate least-loaded Vet assignment.',
       NOW(), NOW()
FROM groom_arrival_seed_target target
WHERE NOT EXISTS (
    SELECT 1 FROM candidate_horse_profiles existing
    WHERE existing.admission_id = target.admission_id
);

INSERT INTO admission_documents (admission_id, document_type, file_url, record_date, note)
SELECT target.admission_id, 'REGISTRATION_DOCUMENT',
       '/test-data/groom-arrival-assignment/registration.pdf', CURRENT_DATE,
       'Test registration document for Groom admission review.'
FROM groom_arrival_seed_target target
WHERE NOT EXISTS (
    SELECT 1 FROM admission_documents existing
    WHERE existing.admission_id = target.admission_id
      AND existing.document_type = 'REGISTRATION_DOCUMENT'
      AND existing.file_url = '/test-data/groom-arrival-assignment/registration.pdf'
);

COMMIT;

-- Initial verification: admission must be GROOM_REVIEW and have no assigned stall yet.
SELECT a.id AS admission_id,
       c.name AS candidate_name,
       c.registration_number,
       a.status,
       a.veterinarian_id,
       a.groom_id,
       a.quarantine_stall_id,
       a.arrival_deadline_at,
       a.horse_id,
       a.arrived_at
FROM admission_applications a
JOIN candidate_horse_profiles c ON c.admission_id = a.id
WHERE c.registration_number = 'TSTARRIVAL00001';

-- Run again after Groom confirms arrival; veterinarian_id must be non-NULL.
SELECT a.id AS admission_id,
       c.name AS candidate_name,
       a.status AS admission_status,
       a.arrived_at,
       a.veterinarian_id AS admission_veterinarian_id,
       admission_vet.full_name AS admission_assigned_vet,
       cs.id AS initial_schedule_id,
       cs.status AS schedule_status,
       cs.veterinarian_id AS schedule_veterinarian_id,
       schedule_vet.full_name AS schedule_assigned_vet,
       cs.scheduled_at,
       cs.duration_minutes
FROM admission_applications a
JOIN candidate_horse_profiles c ON c.admission_id = a.id
LEFT JOIN care_schedule cs ON cs.admission_id = a.id AND cs.care_type = 'INITIAL'
LEFT JOIN users admission_vet ON admission_vet.id = a.veterinarian_id
LEFT JOIN users schedule_vet ON schedule_vet.id = cs.veterinarian_id
WHERE c.registration_number = 'TSTARRIVAL00001';
