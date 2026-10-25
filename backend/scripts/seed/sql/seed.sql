-- =====================================================================
-- seed.sql — chạy toàn bộ seed theo thứ tự. seed-database.ps1 gọi file này trong MỘT transaction (psql -1):
-- lỗi ở bất kỳ bước nào thì DB được giữ nguyên như trước khi chạy.
-- (Runs the whole seed in order. seed-database.ps1 runs it in ONE transaction: any error leaves the DB unchanged.)
--
-- Thêm phần seed mới: tạo file NN_ten.sql trong thư mục này và thêm một dòng \ir đúng vị trí bên dưới.
-- (To add a part: create NN_name.sql here and add an \ir line at the right place below.)
-- =====================================================================
\set ON_ERROR_STOP 1
\ir 00_helpers.sql
\ir 01_cleanup.sql
\ir 10_data.sql
\ir 20_accounts_stable.sql
\ir 30_admissions.sql
\ir 40_vet_cases.sql
\ir 45_preventive.sql
\ir 50_training.sql
\ir 60_groom.sql
\ir 65_vitals.sql
\ir 70_racing.sql
\ir 99_verify.sql
