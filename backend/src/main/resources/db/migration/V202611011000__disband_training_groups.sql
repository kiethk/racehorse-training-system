-- A Training Group can be Disbanded: the row stays so past plans keep the group's name, but it holds no members, takes no new plan
-- and its name may be reused. Name uniqueness therefore applies only among live groups of one Trainer.

ALTER TABLE training_groups ADD COLUMN disbanded_at TIMESTAMP;

ALTER TABLE training_groups DROP CONSTRAINT uq_training_groups_trainer_name;

CREATE UNIQUE INDEX uq_training_groups_live_trainer_name ON training_groups (trainer_id, name) WHERE disbanded_at IS NULL;
