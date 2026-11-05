-- Existing horses and admission snapshots retain NULL until their sex is known.
ALTER TABLE horses ADD COLUMN sex VARCHAR(10);
ALTER TABLE candidate_horse_profiles ADD COLUMN sex VARCHAR(10);

ALTER TABLE horses
    ADD CONSTRAINT chk_horses_sex CHECK (sex IN ('MALE', 'FEMALE'));
ALTER TABLE candidate_horse_profiles
    ADD CONSTRAINT chk_candidate_horse_profiles_sex CHECK (sex IN ('MALE', 'FEMALE'));
