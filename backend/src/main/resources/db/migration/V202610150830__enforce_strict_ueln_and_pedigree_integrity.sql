-- =====================================================================
-- V202610150830: ENFORCE STRICT UELN AND PEDIGREE INTEGRITY
--
-- 1. Standardizes UELN format to: ^[0-9]{6}[A-Z0-9]{9}$
--    (6-digit country/database code + 9 alphanumeric characters).
-- 2. Nullifies invalid/legacy mock identifiers such as 'VNM22SC00000001'
--    or non-standard UELNs. Unknown UELNs remain NULL.
-- 3. Enforces pedigree integrity:
--    - child UELN != sire UELN, child UELN != dam UELN, sire UELN != dam UELN.
--    - horse cannot be its own sire or dam; sire != dam.
--    - keeps horse.registration_number unique while allowing multiple
--      children to share the same sire/dam (no global unique on sire/dam).
-- =====================================================================

-- Step 1: Clean and normalize data in horses
UPDATE horses
SET registration_number = NULLIF(UPPER(TRIM(registration_number)), '')
WHERE registration_number IS NOT NULL;

UPDATE horses
SET registration_number = NULL
WHERE registration_number IS NOT NULL
  AND registration_number !~ '^[0-9]{6}[A-Z0-9]{9}$';

-- Step 2: Clean and normalize data in candidate_horse_profiles
-- Temporarily disable snapshot immutability trigger for migration cleanup
ALTER TABLE candidate_horse_profiles
    DISABLE TRIGGER trg_candidate_snapshot_immutable;

UPDATE candidate_horse_profiles
SET registration_number = NULLIF(UPPER(TRIM(registration_number)), ''),
    sire_registration_number = NULLIF(UPPER(TRIM(sire_registration_number)), ''),
    dam_registration_number = NULLIF(UPPER(TRIM(dam_registration_number)), '');

UPDATE candidate_horse_profiles
SET registration_number = NULL
WHERE registration_number IS NOT NULL
  AND registration_number !~ '^[0-9]{6}[A-Z0-9]{9}$';

UPDATE candidate_horse_profiles
SET sire_registration_number = NULL
WHERE sire_registration_number IS NOT NULL
  AND sire_registration_number !~ '^[0-9]{6}[A-Z0-9]{9}$';

UPDATE candidate_horse_profiles
SET dam_registration_number = NULL
WHERE dam_registration_number IS NOT NULL
  AND dam_registration_number !~ '^[0-9]{6}[A-Z0-9]{9}$';

-- Clean any conflicting candidate parent UELNs
UPDATE candidate_horse_profiles
SET sire_registration_number = NULL
WHERE registration_number IS NOT NULL
  AND sire_registration_number IS NOT NULL
  AND registration_number = sire_registration_number;

UPDATE candidate_horse_profiles
SET dam_registration_number = NULL
WHERE (registration_number IS NOT NULL AND dam_registration_number IS NOT NULL AND registration_number = dam_registration_number)
   OR (sire_registration_number IS NOT NULL AND dam_registration_number IS NOT NULL AND sire_registration_number = dam_registration_number);

-- Re-enable snapshot immutability trigger immediately after normalization
ALTER TABLE candidate_horse_profiles
    ENABLE TRIGGER trg_candidate_snapshot_immutable;

-- Step 3: Clean and normalize data in horse_pedigrees
UPDATE horse_pedigrees
SET sire_registration_number = NULLIF(UPPER(TRIM(sire_registration_number)), ''),
    dam_registration_number = NULLIF(UPPER(TRIM(dam_registration_number)), '');

UPDATE horse_pedigrees
SET sire_registration_number = NULL
WHERE sire_registration_number IS NOT NULL
  AND sire_registration_number !~ '^[0-9]{6}[A-Z0-9]{9}$';

UPDATE horse_pedigrees
SET dam_registration_number = NULL
WHERE dam_registration_number IS NOT NULL
  AND dam_registration_number !~ '^[0-9]{6}[A-Z0-9]{9}$';

-- Clean conflicting pedigree parent relationships
UPDATE horse_pedigrees
SET sire_id = NULL
WHERE sire_id IS NOT NULL AND horse_id = sire_id;

UPDATE horse_pedigrees
SET dam_id = NULL
WHERE dam_id IS NOT NULL AND (horse_id = dam_id OR (sire_id IS NOT NULL AND sire_id = dam_id));

UPDATE horse_pedigrees
SET dam_registration_number = NULL
WHERE sire_registration_number IS NOT NULL
  AND dam_registration_number IS NOT NULL
  AND sire_registration_number = dam_registration_number;

-- Step 4: Drop old check constraints
ALTER TABLE horses
    DROP CONSTRAINT IF EXISTS chk_horses_registration_number_ueln;

ALTER TABLE candidate_horse_profiles
    DROP CONSTRAINT IF EXISTS chk_candidate_registration_number_ueln,
    DROP CONSTRAINT IF EXISTS chk_candidate_sire_registration_number_ueln,
    DROP CONSTRAINT IF EXISTS chk_candidate_dam_registration_number_ueln,
    DROP CONSTRAINT IF EXISTS chk_candidate_pedigree_ueln_distinct;

ALTER TABLE horse_pedigrees
    DROP CONSTRAINT IF EXISTS chk_pedigree_sire_registration_number_ueln,
    DROP CONSTRAINT IF EXISTS chk_pedigree_dam_registration_number_ueln,
    DROP CONSTRAINT IF EXISTS chk_pedigree_sire_dam_ueln_distinct,
    DROP CONSTRAINT IF EXISTS chk_pedigree_sire_dam_id_distinct,
    DROP CONSTRAINT IF EXISTS chk_pedigree_not_self_parent;

-- Step 5: Add strict format and relationship constraints
ALTER TABLE horses
    ADD CONSTRAINT chk_horses_registration_number_ueln
        CHECK (
            registration_number IS NULL
            OR registration_number ~ '^[0-9]{6}[A-Z0-9]{9}$'
        );

ALTER TABLE candidate_horse_profiles
    ADD CONSTRAINT chk_candidate_registration_number_ueln
        CHECK (
            registration_number IS NULL
            OR registration_number ~ '^[0-9]{6}[A-Z0-9]{9}$'
        ),
    ADD CONSTRAINT chk_candidate_sire_registration_number_ueln
        CHECK (
            sire_registration_number IS NULL
            OR sire_registration_number ~ '^[0-9]{6}[A-Z0-9]{9}$'
        ),
    ADD CONSTRAINT chk_candidate_dam_registration_number_ueln
        CHECK (
            dam_registration_number IS NULL
            OR dam_registration_number ~ '^[0-9]{6}[A-Z0-9]{9}$'
        ),
    ADD CONSTRAINT chk_candidate_pedigree_ueln_distinct
        CHECK (
            (registration_number IS NULL OR sire_registration_number IS NULL OR registration_number <> sire_registration_number)
            AND (registration_number IS NULL OR dam_registration_number IS NULL OR registration_number <> dam_registration_number)
            AND (sire_registration_number IS NULL OR dam_registration_number IS NULL OR sire_registration_number <> dam_registration_number)
        );

ALTER TABLE horse_pedigrees
    ADD CONSTRAINT chk_pedigree_sire_registration_number_ueln
        CHECK (
            sire_registration_number IS NULL
            OR sire_registration_number ~ '^[0-9]{6}[A-Z0-9]{9}$'
        ),
    ADD CONSTRAINT chk_pedigree_dam_registration_number_ueln
        CHECK (
            dam_registration_number IS NULL
            OR dam_registration_number ~ '^[0-9]{6}[A-Z0-9]{9}$'
        ),
    ADD CONSTRAINT chk_pedigree_sire_dam_ueln_distinct
        CHECK (
            sire_registration_number IS NULL
            OR dam_registration_number IS NULL
            OR sire_registration_number <> dam_registration_number
        ),
    ADD CONSTRAINT chk_pedigree_sire_dam_id_distinct
        CHECK (
            sire_id IS NULL
            OR dam_id IS NULL
            OR sire_id <> dam_id
        ),
    ADD CONSTRAINT chk_pedigree_not_self_parent
        CHECK (
            (sire_id IS NULL OR horse_id <> sire_id)
            AND (dam_id IS NULL OR horse_id <> dam_id)
        );
