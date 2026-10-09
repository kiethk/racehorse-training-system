-- =====================================================================
-- V44: PERMISSION CHO LOT + GÁN KHU VỰC CHO TRAINER
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Permission mới
-- ---------------------------------------------------------------------
INSERT INTO permissions (code, description) VALUES
    ('TRAINING_LOT_VIEW',   'Xem lịch lot huấn luyện theo ngày hoặc theo tuần'),
    ('TRAINING_LOT_UPDATE', 'Dời giờ hoặc huỷ lot huấn luyện')
ON CONFLICT (code) DO NOTHING;

-- HEAD_TRAINER: toàn quyền trên lot + đóng buổi tập
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'HEAD_TRAINER'
  AND p.code IN (
      'TRAINING_LOT_VIEW',
      'TRAINING_LOT_UPDATE',
      'TRAINING_WORKOUT_VIEW',
      'TRAINING_WORKOUT_UPDATE'   -- đã tồn tại từ V22 nhưng chưa endpoint nào dùng
  )
ON CONFLICT DO NOTHING;

-- GROOM / CLUB_MANAGER / HORSE_OWNER: chỉ xem
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name IN ('GROOM', 'CLUB_MANAGER', 'HORSE_OWNER')
  AND p.code IN ('TRAINING_LOT_VIEW', 'TRAINING_WORKOUT_VIEW')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- 2. Gán khu vực cho Trainer
--
--    areas.trainer_id tồn tại từ V24 nhưng chưa code nào ghi/đọc.
--    Module Trainer dùng chuỗi:
--        Horse.current_stall_id -> StableStall.area_id -> Area.trainer_id
--    để xác định "con ngựa này thuộc Trainer nào".
--
--    Seed dưới đây gán TẤT CẢ khu REGULAR cho HEAD_TRAINER đầu tiên,
--    đủ để dev/demo chạy. Trong vận hành thật, Club Manager sẽ gán qua
--    màn hình quản lý khu vực (thuộc phạm vi actor Club Manager).
-- ---------------------------------------------------------------------
UPDATE areas a
SET trainer_id = (
        SELECT u.id
        FROM users u
        JOIN roles r ON r.id = u.role_id
        WHERE r.name = 'HEAD_TRAINER'
          AND u.is_active = true
        ORDER BY u.id
        LIMIT 1
    )
WHERE a.type = 'REGULAR'
  AND a.trainer_id IS NULL
  AND EXISTS (
        SELECT 1 FROM users u
        JOIN roles r ON r.id = u.role_id
        WHERE r.name = 'HEAD_TRAINER' AND u.is_active = true
  );
