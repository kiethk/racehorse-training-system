-- Permissions for HorsePedigreeController
INSERT INTO permissions (code, description) VALUES
                                                ('HORSE_PEDIGREE_VIEW', 'View horse pedigree information'),
                                                ('HORSE_PEDIGREE_UPDATE', 'Create or update horse pedigree information');

-- Grant HORSE_PEDIGREE_VIEW to HORSE_OWNER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE p.code = 'HORSE_PEDIGREE_VIEW'
  AND r.name = 'HORSE_OWNER';

-- Grant HORSE_PEDIGREE_UPDATE to HORSE_OWNER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE p.code = 'HORSE_PEDIGREE_UPDATE'
  AND r.name = 'HORSE_OWNER';