-- Seed permissions for Audit Log viewing and maintenance (Archive & Purge)
INSERT INTO permissions (code, description) VALUES
    ('AUDIT_LOG_VIEW', 'View system audit and activity logs'),
    ('AUDIT_LOG_MANAGE', 'Archive, export, and purge system audit logs')
ON CONFLICT (code) DO NOTHING;

-- Grant AUDIT_LOG_VIEW and AUDIT_LOG_MANAGE to CLUB_MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'CLUB_MANAGER' AND p.code IN ('AUDIT_LOG_VIEW', 'AUDIT_LOG_MANAGE')
ON CONFLICT DO NOTHING;
