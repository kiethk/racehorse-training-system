-- =====================================================================
-- V202610241000: INDEX HORSE TRAINING PLANS BY HORSE AND STATUS
--
-- The plan preview and the BR-01 overlap check look up the running
-- (ACTIVE / UPCOMING) plans of a set of horses on every change of the
-- enrollment form. Without an index this is a full scan of the table.
-- =====================================================================

CREATE INDEX IF NOT EXISTS idx_horse_training_plans_horse_status
    ON horse_training_plans (horse_id, status);
