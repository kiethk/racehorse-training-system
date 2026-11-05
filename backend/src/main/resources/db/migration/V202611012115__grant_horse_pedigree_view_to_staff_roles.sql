-- Grant HORSE_PEDIGREE_VIEW to club staff roles (CLUB_MANAGER, HEAD_TRAINER, VETERINARIAN, GROOM)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE p.code = 'HORSE_PEDIGREE_VIEW'
  AND r.name IN ('CLUB_MANAGER', 'HEAD_TRAINER', 'VETERINARIAN', 'GROOM')
ON CONFLICT DO NOTHING;

