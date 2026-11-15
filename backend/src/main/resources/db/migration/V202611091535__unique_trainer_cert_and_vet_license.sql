-- Enforce unique certification number for head trainers and unique license number for veterinarians
CREATE UNIQUE INDEX IF NOT EXISTS uq_trainer_profiles_certification_number
    ON trainer_profiles (certification_number);

CREATE UNIQUE INDEX IF NOT EXISTS uq_veterinarian_profiles_license_number
    ON veterinarian_profiles (license_number);
