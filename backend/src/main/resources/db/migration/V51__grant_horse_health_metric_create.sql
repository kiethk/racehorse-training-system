-- Vet records measurements during an examination.
INSERT INTO permissions (code, description) VALUES
    ('HORSE_HEALTH_METRIC_CREATE', 'Ghi chỉ số sức khỏe của chiến mã')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'VETERINARIAN' AND p.code = 'HORSE_HEALTH_METRIC_CREATE'
ON CONFLICT DO NOTHING;
