-- =====================================================================
-- V49: QUYỀN XEM DANH SÁCH NHÂN SỰ
--
-- Trainer cần danh sách Groom để gán vào chuồng. Toàn dự án chưa có
-- endpoint nào liệt kê người dùng, nên màn hình gán Groom hiện buộc
-- phải gõ tay id.
--
-- CHỈ quyền XEM. Tạo/sửa/xoá tài khoản vẫn thuộc Club Manager và
-- chưa nằm trong phạm vi đồ án.
-- =====================================================================

INSERT INTO permissions (code, description) VALUES
    ('USER_VIEW', 'Xem danh sách nhân sự để phân công công việc')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE p.code = 'USER_VIEW'
  AND r.name IN ('HEAD_TRAINER', 'CLUB_MANAGER')
ON CONFLICT DO NOTHING;
