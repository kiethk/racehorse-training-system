-- =====================================================================
-- V202610251000: READINESS ASSESSMENT IS ADMISSION-ONLY
--
-- The racing readiness assessment is written once, when the head trainer
-- reviews an admission. The periodic assessment it was also meant to hold
-- never had a flow that creates it, so the two columns only that kind used
-- are dropped: fitness_score and valid_until.
--
-- chk_rra_scores also covers fitness_score, and dropping the column would
-- drop the whole check, so it is recreated for the three remaining scores.
-- =====================================================================

ALTER TABLE racing_readiness_assessments DROP CONSTRAINT IF EXISTS chk_rra_valid_until;
ALTER TABLE racing_readiness_assessments DROP CONSTRAINT IF EXISTS chk_rra_scores;

ALTER TABLE racing_readiness_assessments DROP COLUMN IF EXISTS fitness_score;
ALTER TABLE racing_readiness_assessments DROP COLUMN IF EXISTS valid_until;

ALTER TABLE racing_readiness_assessments
    ADD CONSTRAINT chk_rra_scores CHECK (
        (conformation_score IS NULL OR conformation_score BETWEEN 0 AND 10)
        AND (temperament_score IS NULL OR temperament_score BETWEEN 0 AND 10)
        AND (gait_quality_score IS NULL OR gait_quality_score BETWEEN 0 AND 10));
