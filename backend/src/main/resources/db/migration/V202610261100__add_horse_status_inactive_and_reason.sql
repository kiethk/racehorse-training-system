-- =====================================================================
-- V202610261100: ADD HORSE STATUS 'INACTIVE', STATUS REASON, AND PERMISSIONS
-- =====================================================================

-- 1. Thêm cột status_reason
ALTER TABLE horses ADD COLUMN IF NOT EXISTS status_reason TEXT;

-- 2. Cập nhật CHECK constraint cho current_status
ALTER TABLE horses DROP CONSTRAINT IF EXISTS chk_horses_current_status;
ALTER TABLE horses ADD CONSTRAINT chk_horses_current_status 
    CHECK (current_status IN ('CANDIDATE', 'ELIGIBLE', 'REJECTED', 'INACTIVE'));

-- 3. Tạo permission và gán quyền
INSERT INTO permissions (code, description) 
VALUES ('HORSE_STATUS_UPDATE', 'Cập nhật trạng thái vòng đời của ngựa (ELIGIBLE/INACTIVE)')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name IN ('CLUB_MANAGER', 'GROOM') AND p.code = 'HORSE_STATUS_UPDATE'
ON CONFLICT DO NOTHING;
