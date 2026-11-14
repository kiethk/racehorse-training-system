-- A Groom may lead several horses of the same training group (grooms look after 2-4 horses in practice).
-- Drops the unique (group_id, groom_id) index that held the former rule BR-09.
DROP INDEX IF EXISTS uq_group_members_active_groom;
DROP INDEX IF EXISTS uq_group_members_groom;
