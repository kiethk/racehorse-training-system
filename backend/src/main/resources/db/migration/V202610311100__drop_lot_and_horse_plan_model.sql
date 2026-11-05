-- =====================================================================
-- Contract step: remove the lot and per-horse plan model
-- =====================================================================
--
-- The Training Group model (V202610311000) replaced it. There is NO data conversion: workouts of the old model are deleted, then
-- training_lots and horse_training_plans are dropped together with their triggers and constraints. Workouts of the new model
-- (session_id set) are untouched. Health records that pointed at a deleted workout keep their row (the FK sets the column to NULL).

DELETE FROM training_workouts WHERE session_id IS NULL;

DROP TRIGGER IF EXISTS lot_workouts_rules_insert ON training_workouts;
DROP TRIGGER IF EXISTS lot_workouts_rules_update ON training_workouts;
DROP FUNCTION IF EXISTS trg_lot_workouts_rules();

ALTER TABLE training_workouts
    DROP CONSTRAINT IF EXISTS chk_workouts_one_model,
    DROP CONSTRAINT IF EXISTS chk_workouts_completed_has_result,
    DROP CONSTRAINT IF EXISTS chk_workouts_status;

DROP INDEX IF EXISTS uq_workout_lot_horse_active;

ALTER TABLE training_workouts
    DROP COLUMN lot_id,
    DROP COLUMN plan_id,
    ALTER COLUMN session_id SET NOT NULL,
    ADD CONSTRAINT chk_workouts_status CHECK (status IN ('SCHEDULED', 'HELD', 'CANCELLED'));

DROP TABLE training_lots;
DROP TABLE horse_training_plans;
