-- =====================================================================
-- V202611071500: DROP LEGACY FINANCIAL REPORTS TABLE
--
-- The financial_reports table was originally created in V9__horse_owner.sql.
-- Financial/revenue tracking was subsequently decommissioned from RTMS scope.
-- The table has no application entities, repositories, or services mapping to it,
-- and contains no data. Dropping it cleans up orphan schema legacy.
-- =====================================================================

DROP TABLE IF EXISTS financial_reports CASCADE;

