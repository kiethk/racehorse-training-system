-- =====================================================================
-- V202610151505: REFACTOR INCIDENT REPORTS TABLE AND COLUMNS
-- Rename table groom_incident_reports -> horse_incident_reports
-- Rename column groom_id -> reported_by_id
-- Create backward compatibility view groom_incident_reports
-- =====================================================================

-- 1. Rename table if exists
ALTER TABLE IF EXISTS groom_incident_reports RENAME TO horse_incident_reports;

-- 2. Rename column groom_id to reported_by_id if exists
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'horse_incident_reports' AND column_name = 'groom_id'
    ) THEN
        ALTER TABLE horse_incident_reports RENAME COLUMN groom_id TO reported_by_id;
    END IF;
END $$;

-- 3. Create view for backward compatibility
CREATE OR REPLACE VIEW groom_incident_reports AS
SELECT 
    id,
    reported_by_id AS groom_id,
    horse_id,
    care_schedule_id,
    title,
    description,
    image_url,
    severity,
    reported_at,
    status,
    handled_by_id,
    handled_at,
    handler_note
FROM horse_incident_reports;
