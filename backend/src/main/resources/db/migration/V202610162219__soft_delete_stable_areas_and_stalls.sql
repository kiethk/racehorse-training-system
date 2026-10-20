ALTER TABLE areas ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE stable_stalls ADD COLUMN deleted_at TIMESTAMP;

CREATE INDEX idx_areas_deleted_type ON areas(type, deleted_at);
CREATE INDEX idx_stalls_active_area ON stable_stalls(area_id) WHERE deleted_at IS NULL;

INSERT INTO permissions (code, description)
VALUES ('AREA_DELETE', 'Soft-delete unused stable areas and their stalls')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'CLUB_MANAGER' AND p.code = 'AREA_DELETE'
ON CONFLICT DO NOTHING;
