-- =====================================================================
-- V202611091435: REVOKE HORSE_CREATE FROM GROOM
-- =====================================================================
-- Groom creates/reuses horses during arrival confirmation directly via
-- AdmissionGroomReviewService (protected by ADMISSION_APPLICATION_GROOM_REVIEW).
-- HORSE_CREATE exposed the standalone POST /api/horses endpoint, allowing
-- ELIGIBLE horses to be created directly without an admission application.
-- Revoking HORSE_CREATE from GROOM prevents bypassing admission workflow.

DELETE FROM role_permissions rp
USING roles r, permissions p
WHERE rp.role_id = r.id
  AND rp.permission_id = p.id
  AND r.name = 'GROOM'
  AND p.code = 'HORSE_CREATE';

-- Re-assign HORSE_CREATE to CLUB_MANAGER for administrative/direct creation if needed
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'CLUB_MANAGER'
  AND p.code = 'HORSE_CREATE'
ON CONFLICT DO NOTHING;
