-- =====================================================================
-- V202610261200: ADD STATUS_REASON_CODE TO HORSES TABLE
-- =====================================================================

ALTER TABLE horses ADD COLUMN IF NOT EXISTS status_reason_code VARCHAR(50);
