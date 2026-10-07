-- =====================================================================
-- V64: EXTEND REGULAR AREAS FROM 10 TO 12 STALLS
-- =====================================================================
-- Each REGULAR area must contain exactly 12 stalls.
-- V28 seeded stalls 1–10. This migration adds stalls 11 and 12
-- only where they don't already exist (idempotent INSERT).
-- Quarantine area stalls remain unchanged.
-- =====================================================================

INSERT INTO stable_stalls (area_id, stall_number, stall_code, status)
SELECT a.id, n.stall_number, a.code || n.stall_number, 'AVAILABLE'
FROM areas a
CROSS JOIN generate_series(11, 12) AS n(stall_number)
WHERE a.type = 'REGULAR'
  AND NOT EXISTS (
      SELECT 1 FROM stable_stalls ss
      WHERE ss.area_id = a.id AND ss.stall_number = n.stall_number
  );
