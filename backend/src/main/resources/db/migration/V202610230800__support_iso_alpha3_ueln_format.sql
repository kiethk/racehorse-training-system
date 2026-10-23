-- =====================================================================
-- V202610230800: SUPPORT ISO ALPHA-3 UELN FORMAT
--
-- Expands UELN check constraints to allow both:
-- 1. Numeric ISO code prefix: ^[0-9]{6}[A-Z0-9]{9}$ (e.g. 704001SC0000001)
-- 2. Alpha-3 ISO country code prefix: ^[A-Z]{3}[A-Z0-9]{12}$ (e.g. VNM22SC00000001, VNM17MS00000002)
-- Combined regex: ^([0-9]{6}[A-Z0-9]{9}|[A-Z]{3}[A-Z0-9]{12})$
-- =====================================================================

ALTER TABLE horses
    DROP CONSTRAINT IF EXISTS chk_horses_registration_number_ueln;

ALTER TABLE horses
    ADD CONSTRAINT chk_horses_registration_number_ueln
        CHECK (
            registration_number IS NULL
            OR registration_number ~ '^([0-9]{6}[A-Z0-9]{9}|[A-Z]{3}[A-Z0-9]{12})$'
        );

ALTER TABLE candidate_horse_profiles
    DROP CONSTRAINT IF EXISTS chk_candidate_registration_number_ueln,
    DROP CONSTRAINT IF EXISTS chk_candidate_sire_registration_number_ueln,
    DROP CONSTRAINT IF EXISTS chk_candidate_dam_registration_number_ueln;

ALTER TABLE candidate_horse_profiles
    ADD CONSTRAINT chk_candidate_registration_number_ueln
        CHECK (
            registration_number IS NULL
            OR registration_number ~ '^([0-9]{6}[A-Z0-9]{9}|[A-Z]{3}[A-Z0-9]{12})$'
        ),
    ADD CONSTRAINT chk_candidate_sire_registration_number_ueln
        CHECK (
            sire_registration_number IS NULL
            OR sire_registration_number ~ '^([0-9]{6}[A-Z0-9]{9}|[A-Z]{3}[A-Z0-9]{12})$'
        ),
    ADD CONSTRAINT chk_candidate_dam_registration_number_ueln
        CHECK (
            dam_registration_number IS NULL
            OR dam_registration_number ~ '^([0-9]{6}[A-Z0-9]{9}|[A-Z]{3}[A-Z0-9]{12})$'
        );

ALTER TABLE horse_pedigrees
    DROP CONSTRAINT IF EXISTS chk_pedigree_sire_registration_number_ueln,
    DROP CONSTRAINT IF EXISTS chk_pedigree_dam_registration_number_ueln;

ALTER TABLE horse_pedigrees
    ADD CONSTRAINT chk_pedigree_sire_registration_number_ueln
        CHECK (
            sire_registration_number IS NULL
            OR sire_registration_number ~ '^([0-9]{6}[A-Z0-9]{9}|[A-Z]{3}[A-Z0-9]{12})$'
        ),
    ADD CONSTRAINT chk_pedigree_dam_registration_number_ueln
        CHECK (
            dam_registration_number IS NULL
            OR dam_registration_number ~ '^([0-9]{6}[A-Z0-9]{9}|[A-Z]{3}[A-Z0-9]{12})$'
        );
