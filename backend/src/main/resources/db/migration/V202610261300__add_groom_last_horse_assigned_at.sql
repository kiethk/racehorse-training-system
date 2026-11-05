-- =====================================================================
-- V202610261100: GROOM LAST HORSE ASSIGNMENT TIME
--
-- Fair stall allocation picks the groom with the fewest horses and, on a tie,
-- the groom who has waited longest for a new horse. The horse count is
-- derived from stall status; the waiting time cannot be: stable_stalls.updated_at
-- also moves on groom reassignment and other edits. NULL means the groom never
-- received a horse and therefore goes first.
-- =====================================================================

ALTER TABLE groom_profiles
    ADD COLUMN last_horse_assigned_at TIMESTAMP;
