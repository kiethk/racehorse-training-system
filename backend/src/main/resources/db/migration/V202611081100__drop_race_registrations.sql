-- The Racing Registration module is dropped and will not be built.
DELETE FROM role_permissions
WHERE permission_id IN (SELECT id FROM permissions WHERE code LIKE 'RACE\_REGISTRATION\_%');

DELETE FROM permissions WHERE code LIKE 'RACE\_REGISTRATION\_%';

DROP TABLE IF EXISTS race_registrations;
