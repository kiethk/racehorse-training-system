-- =====================================================================
-- 20_accounts_stable.sql — tài khoản, hồ sơ nhân viên, khu chuồng, chuồng và phân công Groom.
-- (Accounts, staff profiles, areas, stalls and groom assignments.)
--
-- Luật đang giữ (StableAssignmentService / StaffManagementService):
--   - Mỗi khu 12 chuồng đánh số 1-12; khu thường có đúng một Head Trainer, khu cách ly không có Trainer.
--   - Mỗi Groom tối đa 3 chuồng (BR-06); Groom khu thường báo cáo cho Trainer của khu, Groom khu Q không có Trainer.
--   - Trạng thái chuồng (AVAILABLE/RESERVED/OCCUPIED) do luồng nhập học trong 30_admissions.sql đặt.
-- (Rules kept: 12 stalls per area; one Head Trainer per regular area, none in quarantine; a groom has at most 3 stalls;
-- stall status is set by the admission flow in 30_admissions.sql.)
-- =====================================================================

-- Mật khẩu admin123 (BCrypt cost 10, cùng hash migration V77 dùng). (Password admin123, the hash migration V77 uses.)
INSERT INTO users (full_name, email, password_hash, role_id, phone, address, is_active, created_at, updated_at)
SELECT a.full_name, a.email, '$2a$10$1weRcbnyUXb6/Gth/W0yGegtpwVyTju1ZCVL0qZYpEWirPNlrKLiS', r.id, a.phone, a.address,
       a.is_active, pg_temp.d(a.created_on) + TIME '09:00', pg_temp.d(a.created_on) + TIME '09:00'
  FROM seed_account a JOIN roles r ON r.name = a.role
 ORDER BY CASE a.role WHEN 'CLUB_MANAGER' THEN 1 WHEN 'HEAD_TRAINER' THEN 2 WHEN 'VETERINARIAN' THEN 3
                      WHEN 'GROOM' THEN 4 ELSE 5 END, a.k;

INSERT INTO seed_user (k, id) SELECT a.k, u.id FROM seed_account a JOIN users u ON u.email = a.email;

-- Tài khoản Groom đã nghỉ việc: bị khóa, không còn chuồng. (A former groom: deactivated, no stalls.)
UPDATE users SET updated_at = pg_temp.at(-40, TIME '16:00') WHERE id = pg_temp.uid('GR25');

INSERT INTO trainer_profiles (user_id, certification_number, certification_issued_date)
SELECT pg_temp.uid(k), credential, credential_issued FROM seed_account WHERE role = 'HEAD_TRAINER';

INSERT INTO veterinarian_profiles (user_id, license_number, license_issued_date, specialization)
SELECT pg_temp.uid(k), credential, credential_issued, specialization FROM seed_account WHERE role = 'VETERINARIAN';

INSERT INTO areas (code, name, type, trainer_id, created_at, updated_at)
SELECT code, name, type, CASE WHEN trainer_k IS NULL THEN NULL ELSE pg_temp.uid(trainer_k) END,
       pg_temp.d(DATE '2026-02-20') + TIME '08:00', pg_temp.d(DATE '2026-02-20') + TIME '08:00'
  FROM seed_area ORDER BY type DESC, code;

INSERT INTO stable_stalls (area_id, stall_number, stall_code, status, groom_id, created_at, updated_at)
SELECT a.id, n, a.code || lpad(n::TEXT, 2, '0'), 'AVAILABLE',
       (SELECT pg_temp.uid(b.groom_k) FROM seed_groom_block b
         WHERE b.area_code = a.code AND n BETWEEN b.first_stall AND b.first_stall + 2),
       a.created_at, a.created_at
  FROM areas a CROSS JOIN generate_series(1, 12) n
 ORDER BY a.id, n;

INSERT INTO groom_profiles (user_id, trainer_id)
SELECT pg_temp.uid(a.k),
       (SELECT ar.trainer_id FROM seed_groom_block b JOIN areas ar ON ar.code = b.area_code WHERE b.groom_k = a.k)
  FROM seed_account a WHERE a.role = 'GROOM';
