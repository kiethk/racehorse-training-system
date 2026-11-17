-- ============================================================================
-- RTMS M02 Head Trainer — additional test data seed
-- Target: the same database the app uses (rtms_system_test, rtms_db, ...).
-- Run AFTER the official demo seed. This script is idempotent: it adds/restores
-- only its own M02 fixtures and does not delete unrelated data.
--
-- What it adds
--   1. Area F for trainer3 + 12 stalls + 7 eligible horses + 1 no-groom horse
--      + 1 no-stall horse + 1 blocked horse (TC18–TC20, TC19).
--   2. Reset the two seed admissions EROIKA and ARC KAABA to TRAINER_REVIEW
--      (TC50–TC52), and add one fresh M02_ADM_VALIDATION admission
--      (TC53–TC54; TC54 reuses it after TC53). Groom/vet reviews and documents are copied from a complete
--      seed admission; any prior readiness assessment is deleted so TC50–TC54
--      can be replayed.
--   3. One foreign TRAINER_REVIEW admission for trainer2: M02_ADM_FOREIGN
--      (TC55).
--
-- It also removes only M02UI_* groups and M02FIX_* memberships left by a
-- previous UI run so the fixture horses return to the candidate list.
--
-- After running, use the verification queries at the bottom to get the
-- admission IDs for TC50–TC55.
-- ============================================================================

BEGIN;

-- Target guard: this seed is for the test database rtms_system_test.
-- rtms_db is the main DB and has Flyway drift; do not run this there.
-- If you intentionally use a smoke database, name it rtms_smoke.
DO $$
BEGIN
    IF current_database() NOT IN ('rtms_system_test', 'rtms_smoke') THEN
        RAISE EXCEPTION 'M02 seed target is rtms_system_test (or rtms_smoke). Current database: %', current_database();
    END IF;
END $$;

DO $$
DECLARE
    v_trainer1      bigint;
    v_trainer2      bigint;
    v_trainer3      bigint;
    v_groom         bigint;
    v_template      bigint;
    v_area_f        bigint;
    v_stall_id      bigint;
    v_horse_id      bigint;
    v_admission_id  bigint;
    i               int;
    v_names         text[] := ARRAY['EROIKA', 'ARC KAABA', 'M02_ADM_VALIDATION'];
    v_regs          text[] := ARRAY['380005262952020', '380005260892020', '999100000000003'];
    v_fix_names     text[] := ARRAY['M02FIX_ALPHA', 'M02FIX_BRAVO', 'M02FIX_CHARLIE', 'M02FIX_DELTA', 'M02FIX_ECHO', 'M02FIX_FOXTROT', 'M02FIX_GOLF'];
    v_fix_regs      text[] := ARRAY['999000000000001', '999000000000002', '999000000000003', '999000000000004', '999000000000005', '999000000000006', '999000000000007'];
BEGIN
    SELECT id INTO v_trainer1 FROM users WHERE email = 'trainer1@rtms.com';
    SELECT id INTO v_trainer2 FROM users WHERE email = 'trainer2@rtms.com';
    SELECT id INTO v_trainer3 FROM users WHERE email = 'trainer3@rtms.com';
    IF v_trainer1 IS NULL OR v_trainer2 IS NULL OR v_trainer3 IS NULL THEN
        RAISE EXCEPTION 'Missing demo trainer accounts (trainer1/trainer2/trainer3@rtms.com). Run the official demo seed first.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM horses WHERE name = 'EROIKA')
       OR NOT EXISTS (SELECT 1 FROM horses WHERE name = 'ARC KAABA') THEN
        RAISE EXCEPTION 'Seed horses EROIKA/ARC KAABA not found. Run the official demo seed first.';
    END IF;

    -- A complete admission with groom/vet reviews, profile and documents.
    SELECT a.id INTO v_template
    FROM admission_applications a
    WHERE a.groom_id IS NOT NULL
      AND a.groom_decision = 'APPROVED'
      AND a.veterinarian_id IS NOT NULL
      AND a.vet_reviewed_at IS NOT NULL
      AND a.horse_id IS NOT NULL
      AND EXISTS (SELECT 1 FROM candidate_horse_profiles p WHERE p.admission_id = a.id)
    ORDER BY (SELECT count(*) FROM admission_documents d WHERE d.admission_id = a.id) DESC, a.id
    LIMIT 1;
    IF v_template IS NULL THEN
        RAISE EXCEPTION 'No complete admission template found. Run the official demo seed first.';
    END IF;

    -- ------------------------------------------------------------------
    -- 1. Area F group fixtures for trainer3 (TC18–TC20)
    -- ------------------------------------------------------------------
    SELECT id INTO v_area_f FROM areas WHERE code = 'F';
    IF v_area_f IS NULL THEN
        INSERT INTO areas (code, name, type, trainer_id, created_at, updated_at)
        VALUES ('F', 'Area F - M02 UI fixtures', 'REGULAR', v_trainer3, now(), now())
        RETURNING id INTO v_area_f;
    ELSE
        IF (SELECT trainer_id FROM areas WHERE id = v_area_f) IS DISTINCT FROM v_trainer3 THEN
            RAISE EXCEPTION 'Area F already exists for another trainer. Rename or remove it before running this seed.';
        END IF;
        UPDATE areas SET deleted_at = NULL, updated_at = now() WHERE id = v_area_f;
    END IF;

    SELECT gp.user_id INTO v_groom
    FROM groom_profiles gp
    WHERE gp.trainer_id = v_trainer3
    ORDER BY gp.user_id
    LIMIT 1;
    IF v_groom IS NULL THEN
        RAISE EXCEPTION 'No groom is assigned to trainer3. Run the official demo seed first.';
    END IF;

    FOR i IN 1..12 LOOP
        INSERT INTO stable_stalls (area_id, stall_number, stall_code, status, groom_id, created_at, updated_at)
        SELECT v_area_f, i, 'F' || lpad(i::text, 2, '0'),
               CASE WHEN i IN (1,2,3,4,5,6,7,8,10) THEN 'OCCUPIED' ELSE 'AVAILABLE' END,
               CASE WHEN i = 8 THEN NULL ELSE v_groom END,
               now(), now()
        WHERE NOT EXISTS (SELECT 1 FROM stable_stalls WHERE stall_code = 'F' || lpad(i::text, 2, '0'));
    END LOOP;

    FOR i IN 1..7 LOOP
        INSERT INTO horses (name, breed, date_of_birth, current_status, stable_location, owner_id,
                            current_stall_id, registry_name, registration_number, training_decision,
                            sex, created_at, updated_at)
        SELECT v_fix_names[i], 'Arabian', DATE '2021-01-01', 'ELIGIBLE', 'Area F', 33,
               (SELECT id FROM stable_stalls WHERE stall_code = 'F' || lpad(i::text, 2, '0')),
               'M02 UI fixture', v_fix_regs[i], 'ALLOWED', 'FEMALE', now(), now()
        WHERE NOT EXISTS (SELECT 1 FROM horses WHERE name = v_fix_names[i]);
    END LOOP;

    INSERT INTO horses (name, breed, date_of_birth, current_status, stable_location, owner_id,
                        current_stall_id, registry_name, registration_number, training_decision,
                        sex, created_at, updated_at)
    SELECT 'M02FIX_NO_GROOM', 'Arabian', DATE '2021-01-01', 'ELIGIBLE', 'Area F', 33,
           (SELECT id FROM stable_stalls WHERE stall_code = 'F08'),
           'M02 UI fixture', '999000000000008', 'ALLOWED', 'FEMALE', now(), now()
    WHERE NOT EXISTS (SELECT 1 FROM horses WHERE name = 'M02FIX_NO_GROOM');

    INSERT INTO horses (name, breed, date_of_birth, current_status, stable_location, owner_id,
                        current_stall_id, registry_name, registration_number, training_decision,
                        sex, created_at, updated_at)
    SELECT 'M02FIX_NO_STALL', 'Arabian', DATE '2021-01-01', 'ELIGIBLE', NULL, 33,
           NULL, 'M02 UI fixture', '999000000000009', 'ALLOWED', 'FEMALE', now(), now()
    WHERE NOT EXISTS (SELECT 1 FROM horses WHERE name = 'M02FIX_NO_STALL');

    INSERT INTO horses (name, breed, date_of_birth, current_status, stable_location, owner_id,
                        current_stall_id, registry_name, registration_number, training_decision,
                        training_decision_reason, sex, created_at, updated_at)
    SELECT 'M02FIX_BLOCKED', 'Arabian', DATE '2021-01-01', 'ELIGIBLE', 'Area F', 33,
           (SELECT id FROM stable_stalls WHERE stall_code = 'F10'),
           'M02 UI fixture', '999000000000010', 'BLOCKED',
           'M02 UI fixture: blocked for the negative group test', 'FEMALE', now(), now()
    WHERE NOT EXISTS (SELECT 1 FROM horses WHERE name = 'M02FIX_BLOCKED');

    -- Free the fixture horses if a previous UI run left them inside M02UI_* groups.
    DELETE FROM training_group_members
    WHERE group_id IN (SELECT id FROM training_groups WHERE name LIKE 'M02UI_%');
    DELETE FROM training_groups WHERE name LIKE 'M02UI_%';

    -- ------------------------------------------------------------------
    -- 2. Three fresh TRAINER_REVIEW admissions for trainer1 (TC50–TC54)
    -- ------------------------------------------------------------------
    FOR i IN 1..3 LOOP
        SELECT id INTO v_horse_id FROM horses WHERE name = v_names[i];
        IF v_horse_id IS NULL THEN
            INSERT INTO horses (name, breed, date_of_birth, current_status, stable_location, owner_id,
                                current_stall_id, registry_name, registration_number, training_decision,
                                sex, created_at, updated_at)
            SELECT v_names[i], 'Arabian', DATE '2020-01-01', 'CANDIDATE', NULL,
                   t.owner_id, NULL, 'M02 test fixture', v_regs[i], 'ALLOWED', 'FEMALE', now(), now()
            FROM admission_applications t WHERE t.id = v_template
            RETURNING id INTO v_horse_id;
        END IF;

        SELECT id INTO v_admission_id
        FROM admission_applications
        WHERE horse_id = v_horse_id
        ORDER BY id DESC
        LIMIT 1;

        IF v_admission_id IS NULL THEN
            SELECT ss.id INTO v_stall_id
            FROM stable_stalls ss
            JOIN areas a ON ss.area_id = a.id
            WHERE a.code = 'Q-A' AND ss.status = 'AVAILABLE'
            ORDER BY ss.id
            LIMIT 1;
            IF v_stall_id IS NULL THEN
                RAISE EXCEPTION 'No available Q-A quarantine stall for %.', v_names[i];
            END IF;

            -- Create in GROOM_REVIEW first: the candidate/document snapshot is
            -- still open, so the seed can copy the profile and documents.
            INSERT INTO admission_applications
                (owner_id, status, quarantine_stall_id, groom_id, groom_decision, groom_feedback,
                 groom_reviewed_at, veterinarian_id, vet_feedback, vet_reviewed_at, trainer_id,
                 horse_id, submitted_at, created_at, updated_at, vet_training_decision)
            SELECT t.owner_id, 'GROOM_REVIEW', v_stall_id, NULL, NULL, NULL,
                   NULL, NULL, NULL, NULL, v_trainer1,
                   v_horse_id, now() - interval '2 days', now(), now(), NULL
            FROM admission_applications t
            WHERE t.id = v_template
            RETURNING id INTO v_admission_id;

            INSERT INTO candidate_horse_profiles
                (admission_id, name, breed, date_of_birth, registration_number, registry_name,
                 sire_name, sire_registration_number, dam_name, dam_registration_number,
                 pedigree_notes, created_at, updated_at, sex)
            SELECT v_admission_id, v_names[i], p.breed, p.date_of_birth, v_regs[i], p.registry_name,
                   p.sire_name, p.sire_registration_number, p.dam_name, p.dam_registration_number,
                   p.pedigree_notes, now(), now(), p.sex
            FROM candidate_horse_profiles p
            WHERE p.admission_id = v_template
              AND NOT EXISTS (SELECT 1 FROM candidate_horse_profiles x WHERE x.admission_id = v_admission_id);

            INSERT INTO admission_documents
                (admission_id, document_type, file_url, record_date, note, uploaded_at, original_file_name)
            SELECT v_admission_id, d.document_type, d.file_url, d.record_date, d.note, now(), d.original_file_name
            FROM admission_documents d
            WHERE d.admission_id = v_template
              AND NOT EXISTS (SELECT 1 FROM admission_documents x WHERE x.admission_id = v_admission_id);

            -- Complete the copied groom/vet reviews and move to TRAINER_REVIEW.
            UPDATE admission_applications a
            SET status = 'TRAINER_REVIEW',
                groom_id = t.groom_id,
                groom_decision = t.groom_decision,
                groom_feedback = t.groom_feedback,
                groom_reviewed_at = t.groom_reviewed_at,
                veterinarian_id = t.veterinarian_id,
                vet_feedback = t.vet_feedback,
                vet_reviewed_at = t.vet_reviewed_at,
                vet_training_decision = 'ALLOWED',
                updated_at = now()
            FROM admission_applications t
            WHERE a.id = v_admission_id AND t.id = v_template;

            UPDATE stable_stalls
            SET status = 'RESERVED',
                groom_id = (SELECT groom_id FROM admission_applications WHERE id = v_admission_id)
            WHERE id = v_stall_id;
        ELSE
            UPDATE admission_applications
            SET status = 'TRAINER_REVIEW',
                trainer_id = v_trainer1,
                trainer_feedback = NULL,
                trainer_reviewed_at = NULL,
                manager_id = NULL,
                manager_decision = NULL,
                manager_feedback = NULL,
                manager_reviewed_at = NULL,
                vet_training_decision = 'ALLOWED',
                updated_at = now()
            WHERE id = v_admission_id;
            DELETE FROM racing_readiness_assessments
            WHERE admission_id = v_admission_id OR horse_id = v_horse_id;
        END IF;
    END LOOP;

    -- ------------------------------------------------------------------
    -- 3. One foreign TRAINER_REVIEW admission for trainer2 (TC55)
    -- ------------------------------------------------------------------
    SELECT id INTO v_horse_id FROM horses WHERE name = 'M02_ADM_FOREIGN';
    IF v_horse_id IS NULL THEN
        INSERT INTO horses (name, breed, date_of_birth, current_status, stable_location, owner_id,
                            current_stall_id, registry_name, registration_number, training_decision,
                            sex, created_at, updated_at)
        SELECT 'M02_ADM_FOREIGN', 'Arabian', DATE '2020-01-01', 'CANDIDATE', NULL,
               t.owner_id, NULL, 'M02 test fixture', '999100000000005', 'ALLOWED', 'FEMALE', now(), now()
        FROM admission_applications t WHERE t.id = v_template
        RETURNING id INTO v_horse_id;
    END IF;

    SELECT id INTO v_admission_id
    FROM admission_applications
    WHERE horse_id = v_horse_id
    ORDER BY id DESC
    LIMIT 1;

    IF v_admission_id IS NULL THEN
        SELECT ss.id INTO v_stall_id
        FROM stable_stalls ss
        JOIN areas a ON ss.area_id = a.id
        WHERE a.code = 'Q-A' AND ss.status = 'AVAILABLE'
        ORDER BY ss.id
        LIMIT 1;
        IF v_stall_id IS NULL THEN
            RAISE EXCEPTION 'No available Q-A quarantine stall for M02_ADM_FOREIGN.';
        END IF;

        INSERT INTO admission_applications
            (owner_id, status, quarantine_stall_id, groom_id, groom_decision, groom_feedback,
             groom_reviewed_at, veterinarian_id, vet_feedback, vet_reviewed_at, trainer_id,
             horse_id, submitted_at, created_at, updated_at, vet_training_decision)
        SELECT t.owner_id, 'GROOM_REVIEW', v_stall_id, NULL, NULL, NULL,
               NULL, NULL, NULL, NULL, v_trainer2,
               v_horse_id, now() - interval '2 days', now(), now(), NULL
        FROM admission_applications t
        WHERE t.id = v_template
        RETURNING id INTO v_admission_id;

        INSERT INTO candidate_horse_profiles
            (admission_id, name, breed, date_of_birth, registration_number, registry_name,
             sire_name, sire_registration_number, dam_name, dam_registration_number,
             pedigree_notes, created_at, updated_at, sex)
        SELECT v_admission_id, 'M02_ADM_FOREIGN', p.breed, p.date_of_birth, '999100000000005', p.registry_name,
               p.sire_name, p.sire_registration_number, p.dam_name, p.dam_registration_number,
               p.pedigree_notes, now(), now(), p.sex
        FROM candidate_horse_profiles p
        WHERE p.admission_id = v_template
          AND NOT EXISTS (SELECT 1 FROM candidate_horse_profiles x WHERE x.admission_id = v_admission_id);

        INSERT INTO admission_documents
            (admission_id, document_type, file_url, record_date, note, uploaded_at, original_file_name)
        SELECT v_admission_id, d.document_type, d.file_url, d.record_date, d.note, now(), d.original_file_name
        FROM admission_documents d
        WHERE d.admission_id = v_template
          AND NOT EXISTS (SELECT 1 FROM admission_documents x WHERE x.admission_id = v_admission_id);

        UPDATE admission_applications a
        SET status = 'TRAINER_REVIEW',
            groom_id = t.groom_id,
            groom_decision = t.groom_decision,
            groom_feedback = t.groom_feedback,
            groom_reviewed_at = t.groom_reviewed_at,
            veterinarian_id = t.veterinarian_id,
            vet_feedback = t.vet_feedback,
            vet_reviewed_at = t.vet_reviewed_at,
            vet_training_decision = 'ALLOWED',
            updated_at = now()
        FROM admission_applications t
        WHERE a.id = v_admission_id AND t.id = v_template;

        UPDATE stable_stalls
        SET status = 'RESERVED',
            groom_id = (SELECT groom_id FROM admission_applications WHERE id = v_admission_id)
        WHERE id = v_stall_id;
    ELSE
        UPDATE admission_applications
        SET status = 'TRAINER_REVIEW',
            trainer_id = v_trainer2,
            trainer_feedback = NULL,
            trainer_reviewed_at = NULL,
            manager_id = NULL,
            manager_decision = NULL,
            manager_feedback = NULL,
            manager_reviewed_at = NULL,
            vet_training_decision = 'ALLOWED',
            updated_at = now()
        WHERE id = v_admission_id;
        DELETE FROM racing_readiness_assessments
        WHERE admission_id = v_admission_id OR horse_id = v_horse_id;
    END IF;

    RAISE NOTICE 'M02 seed complete: Area F group fixtures + EROIKA/ARC KAABA reset + 1 validation admission + 1 foreign admission.';
END $$;

COMMIT;

-- ============================================================================
-- Verification queries (run after COMMIT)
-- ============================================================================

-- 1. Group fixture horses (TC18–TC20)
SELECT h.name, ss.stall_code, ss.groom_id, h.training_decision
FROM horses h
LEFT JOIN stable_stalls ss ON h.current_stall_id = ss.id
WHERE h.name LIKE 'M02FIX_%'
ORDER BY h.name;

-- 2. Fresh admissions for trainer1 (TC50–TC54) and trainer2 (TC55)
SELECT a.id AS admission_id, h.name AS horse_name, a.status, a.trainer_id,
       a.groom_id, a.veterinarian_id, a.quarantine_stall_id
FROM admission_applications a
JOIN horses h ON h.id = a.horse_id
WHERE h.name IN ('EROIKA', 'ARC KAABA', 'M02_ADM_VALIDATION', 'M02_ADM_FOREIGN')
ORDER BY h.name;

-- 3. No readiness assessment should exist for the fresh admissions
SELECT a.id AS admission_id, h.name AS horse_name,
       (SELECT count(*) FROM racing_readiness_assessments r
        WHERE r.admission_id = a.id OR r.horse_id = a.horse_id) AS assessments
FROM admission_applications a
JOIN horses h ON h.id = a.horse_id
WHERE h.name IN ('EROIKA', 'ARC KAABA', 'M02_ADM_VALIDATION', 'M02_ADM_FOREIGN')
ORDER BY h.name;

-- 4. Trainer1 pending queue should contain the M02 admissions
SELECT a.id AS admission_id, h.name AS horse_name, a.status
FROM admission_applications a
JOIN horses h ON h.id = a.horse_id
WHERE a.trainer_id = (SELECT id FROM users WHERE email = 'trainer1@rtms.com')
  AND a.status = 'TRAINER_REVIEW'
ORDER BY a.id;
