-- Soft exit for training group members: preserves membership history when horses leave or when a group is disbanded.
-- Replaces the hard unique constraint on horse_id with a partial unique index on active members (left_at IS NULL).
-- Capacity (BR-10) and unique groom (BR-09) also apply only to active members.

ALTER TABLE training_group_members ADD COLUMN left_at TIMESTAMP;

ALTER TABLE training_group_members DROP CONSTRAINT uq_group_members_horse;

CREATE UNIQUE INDEX uq_group_members_active_horse ON training_group_members (horse_id) WHERE left_at IS NULL;

DROP INDEX IF EXISTS uq_group_members_groom;

CREATE UNIQUE INDEX uq_group_members_active_groom ON training_group_members (group_id, groom_id) WHERE groom_id IS NOT NULL AND left_at IS NULL;

CREATE OR REPLACE FUNCTION trg_group_members_capacity() RETURNS trigger AS $$
BEGIN
    IF (SELECT count(*) FROM training_group_members WHERE group_id = NEW.group_id AND left_at IS NULL) > 6 THEN
        RAISE EXCEPTION 'Group % is full: more than 6 horses', NEW.group_id USING ERRCODE = '23514';
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;
