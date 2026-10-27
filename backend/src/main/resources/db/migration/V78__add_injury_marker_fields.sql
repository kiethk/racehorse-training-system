-- Add durable marker fields to injury_records (T4)
ALTER TABLE injury_records
    ADD COLUMN IF NOT EXISTS zone_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS local_x NUMERIC(10,4),
    ADD COLUMN IF NOT EXISTS local_y NUMERIC(10,4),
    ADD COLUMN IF NOT EXISTS local_z NUMERIC(10,4),
    ADD COLUMN IF NOT EXISTS normal_x NUMERIC(10,4),
    ADD COLUMN IF NOT EXISTS normal_y NUMERIC(10,4),
    ADD COLUMN IF NOT EXISTS normal_z NUMERIC(10,4),
    ADD COLUMN IF NOT EXISTS mesh_name VARCHAR(128);

CREATE INDEX IF NOT EXISTS idx_injury_records_zone ON injury_records(zone_id);
