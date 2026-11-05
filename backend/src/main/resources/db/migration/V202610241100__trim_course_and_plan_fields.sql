-- =====================================================================
-- V202610241100: DROP COURSE DESCRIPTION AND PLAN NOTES
--
-- 1. courses.description: the target goal already says what a course is
--    for; the second free-text field only added noise to the screens.
-- 2. horse_training_plans.notes: never shown anywhere; the only writer was
--    the auto-cancel cascade appending its reason, which the horse already
--    keeps in training_decision_reason.
-- =====================================================================

ALTER TABLE courses DROP COLUMN IF EXISTS description;

ALTER TABLE horse_training_plans DROP COLUMN IF EXISTS notes;
