-- =====================================================================
-- V45: VÒNG ĐỜI BÁO CÁO SỰ CỐ (GROOM -> VET) + FIX UNIQUE WORKOUT ACTIVE
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Sửa ràng buộc UNIQUE của training_workouts:
--    Chỉ chặn trùng với các buổi CÒN HIỆU LỰC (status <> 'CANCELLED').
--    Buổi đã huỷ là lịch sử, không cản trở khi ngựa hồi phục quay lại lot.
-- ---------------------------------------------------------------------
ALTER TABLE training_workouts
    DROP CONSTRAINT IF EXISTS uq_workout_lot_horse;

CREATE UNIQUE INDEX IF NOT EXISTS uq_workout_lot_horse_active
    ON training_workouts(lot_id, horse_id)
    WHERE status <> 'CANCELLED';

-- ---------------------------------------------------------------------
-- 2. Vòng đời báo cáo sự cố (groom_incident_reports)
--
-- Luồng:
--   Groom tạo sự cố                  -> REPORTED
--   Vet bấm "Tiếp nhận"              -> IN_REVIEW  (+ handled_by_id, handled_at)
--   Vet khám xong, đã lập bệnh án    -> RESOLVED
--   Vet xác định báo động giả        -> DISMISSED
-- ---------------------------------------------------------------------
ALTER TABLE groom_incident_reports
    ADD COLUMN status        VARCHAR(30) NOT NULL DEFAULT 'REPORTED',
    ADD COLUMN handled_by_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN handled_at    TIMESTAMP,
    ADD COLUMN handler_note  TEXT;

ALTER TABLE groom_incident_reports
    ADD CONSTRAINT chk_incident_status
        CHECK (status IN ('REPORTED', 'IN_REVIEW', 'RESOLVED', 'DISMISSED'));

-- Dashboard Thú y: "sự cố chờ khám", mới nhất lên đầu
CREATE INDEX idx_incident_status
    ON groom_incident_reports(status, reported_at DESC);

CREATE INDEX idx_incident_horse
    ON groom_incident_reports(horse_id, reported_at DESC);

-- ---------------------------------------------------------------------
-- 3. Permission xử lý sự cố
-- ---------------------------------------------------------------------
INSERT INTO permissions (code, description) VALUES
    ('GROOM_INCIDENT_REPORT_HANDLE',
     'Tiếp nhận và kết luận báo cáo sự cố do Groom gửi')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name IN ('VETERINARIAN', 'CLUB_MANAGER')
  AND p.code = 'GROOM_INCIDENT_REPORT_HANDLE'
ON CONFLICT DO NOTHING;
