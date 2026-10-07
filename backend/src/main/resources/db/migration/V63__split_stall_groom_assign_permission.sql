-- =====================================================================
-- V63: TÁCH QUYỀN PHÂN CÔNG GROOM CHO CHUỒNG → CHỈ QUẢN LÝ CÂU LẠC BỘ
-- =====================================================================
--
-- VẤN ĐỀ
-- -------
-- Phân công Groom cho chuồng chuyển từ Huấn luyện viên sang Quản lý câu lạc
-- bộ. Nhưng KHÔNG thể thu hồi STABLE_STALL_UPDATE khỏi HEAD_TRAINER, vì một
-- quyền duy nhất đó đang gác HAI endpoint khác nhau:
--
--     PUT /api/stalls/{id}/assign-groom   -> chuyển cho Quản lý
--     PUT /api/horses/{id}/assign-stall   -> Trainer PHẢI giữ (đổi chuồng
--                                            cho ngựa, kèm kiểm tra xung đột
--                                            BR-09 khi dời sang chuồng của
--                                            Groom khác)
--
-- V35 cấp đúng quyền đó cho HEAD_TRAINER, và comment của nó nói rõ là cho
-- cả hai việc: "quản lý xếp chuồng cho ngựa VÀ phân công Groom".
-- DELETE khỏi role_permissions sẽ làm Trainer mất luôn khả năng đổi chuồng.
--
-- CÁCH LÀM
-- --------
-- Tạo quyền MỚI, chỉ cấp cho CLUB_MANAGER, rồi đổi @PreAuthorize của
-- /assign-groom sang quyền đó. STABLE_STALL_UPDATE giữ nguyên y như cũ cho
-- mọi vai trò — không DELETE dòng nào, nên không có rủi ro gỡ nhầm quyền
-- khác. Trainer tự động mất khả năng gán Groom chỉ vì endpoint đó bắt đầu
-- đòi một quyền mà Trainer không có.
--
-- V35 KHÔNG được sửa: Flyway lưu checksum của migration đã chạy, sửa một ký
-- tự là app ném FlywayValidateException và không khởi động được.
-- ---------------------------------------------------------------------

INSERT INTO permissions (code, description) VALUES
    ('STALL_GROOM_ASSIGN',
     'Phân công hoặc gỡ Groom phụ trách một chuồng (thuộc Quản lý câu lạc bộ)')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'STALL_GROOM_ASSIGN'
  AND r.name = 'CLUB_MANAGER'
ON CONFLICT DO NOTHING;
