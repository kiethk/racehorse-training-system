-- =====================================================================
-- V47: HOÀN THIỆN racing_readiness_assessments CHO BƯỚC TRAINER REVIEW
--
-- Bảng tạo từ V9 (phục vụ màn hình Horse Owner) nhưng chưa có entity Java,
-- 0 dòng dữ liệu. Nay dùng cho CẢ HAI giai đoạn:
--
--   admission_id NOT NULL -> đánh giá trong luồng nhập học.
--                            Ngựa đang CÁCH LY nên Trainer chỉ quan sát và
--                            dắt tay -> KHÔNG đo được fitness_score (để NULL).
--   admission_id NULL     -> đánh giá định kỳ, fitness suy từ số liệu tập thật.
--
-- Biểu đồ tiến bộ chỉ lấy loại định kỳ. Điểm nhập học hiển thị như mốc khởi
-- đầu riêng, KHÔNG nối liền đường — hai thang đo khác nhau.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Ai đánh giá + thuộc đơn nào
-- ---------------------------------------------------------------------
ALTER TABLE racing_readiness_assessments
    ADD COLUMN trainer_id   BIGINT REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN admission_id BIGINT REFERENCES admission_applications(id) ON DELETE SET NULL;

COMMENT ON COLUMN racing_readiness_assessments.admission_id IS
    'NULL = đánh giá định kỳ. NOT NULL = đánh giá trong luồng nhập học. '
    'Dùng FK thay vì cờ boolean vì một ngựa có thể có nhiều đơn nhập học '
    '(bị từ chối rồi nộp lại cùng UELN) — cần biết đánh giá thuộc đơn nào.';

-- Mỗi đơn nhập học chỉ có đúng MỘT bản đánh giá
CREATE UNIQUE INDEX uq_rra_admission
    ON racing_readiness_assessments(admission_id)
    WHERE admission_id IS NOT NULL;

CREATE INDEX idx_rra_horse_date
    ON racing_readiness_assessments(horse_id, assessment_date DESC);

-- ---------------------------------------------------------------------
-- 2. Bỏ health_score
--
--    Sức khoẻ thuộc chuyên môn Thú y, và horse_health_metrics đã lưu dữ liệu
--    ĐO ĐƯỢC (nhịp tim, thân nhiệt, cân nặng, nhịp thở, điểm thể trạng).
--    Để Trainer tự chấm điểm sức khoẻ sẽ tạo nguồn sự thật thứ hai mâu thuẫn
--    với Vet. Cột này từ V9, chưa từng có dòng nào và không có entity Java.
-- ---------------------------------------------------------------------
ALTER TABLE racing_readiness_assessments
    DROP COLUMN IF EXISTS health_score;

-- ---------------------------------------------------------------------
-- 3. Các trường Trainer ĐÁNH GIÁ ĐƯỢC trong khu cách ly
--    (chỉ cần quan sát trong chuồng và dắt tay đi bộ)
-- ---------------------------------------------------------------------
ALTER TABLE racing_readiness_assessments
    ADD COLUMN conformation_score       NUMERIC(3,1),
    ADD COLUMN temperament_score        NUMERIC(3,1),
    ADD COLUMN gait_quality_score       NUMERIC(3,1),
    ADD COLUMN estimated_months_to_race INT;

COMMENT ON COLUMN racing_readiness_assessments.fitness_score IS
    'Thang 0-10. NULL ở bước nhập học vì ngựa đang cách ly, không đưa ra '
    'đường chạy chung được nên không đo được thể lực.';
COMMENT ON COLUMN racing_readiness_assessments.estimated_months_to_race IS
    'Ước tính số tháng nữa mới đủ điều kiện đăng ký giải. Đây mới là con số '
    'Manager dùng để cân nhắc chi phí nuôi — readiness_status gần như luôn là '
    'NEEDS_MORE_TRAINING với ngựa mới nhập.';

-- ---------------------------------------------------------------------
-- 4. Ràng buộc giá trị
--    Trước đây readiness_status là VARCHAR(50) TỰ DO — nhập 'abc' cũng lọt.
-- ---------------------------------------------------------------------
ALTER TABLE racing_readiness_assessments
    ADD CONSTRAINT chk_rra_status
        CHECK (readiness_status IN ('READY', 'NEEDS_MORE_TRAINING', 'UNSUITABLE'));

ALTER TABLE racing_readiness_assessments
    ADD CONSTRAINT chk_rra_scores CHECK (
        (fitness_score       IS NULL OR fitness_score       BETWEEN 0 AND 10) AND
        (conformation_score  IS NULL OR conformation_score  BETWEEN 0 AND 10) AND
        (temperament_score   IS NULL OR temperament_score   BETWEEN 0 AND 10) AND
        (gait_quality_score  IS NULL OR gait_quality_score  BETWEEN 0 AND 10)
    );

ALTER TABLE racing_readiness_assessments
    ADD CONSTRAINT chk_rra_months
        CHECK (estimated_months_to_race IS NULL
               OR estimated_months_to_race BETWEEN 0 AND 60);

-- Đánh giá nhập học KHÔNG có hạn dùng; đánh giá định kỳ thì có
ALTER TABLE racing_readiness_assessments
    ADD CONSTRAINT chk_rra_valid_until
        CHECK (admission_id IS NULL OR valid_until IS NULL);

-- ---------------------------------------------------------------------
-- 5. Permission
-- ---------------------------------------------------------------------
INSERT INTO permissions (code, description) VALUES
    ('RACING_READINESS_ASSESSMENT_CREATE', 'Tạo bản đánh giá mức độ sẵn sàng thi đấu'),
    ('RACING_READINESS_ASSESSMENT_VIEW',   'Xem lịch sử đánh giá mức độ sẵn sàng thi đấu'),
    ('HEALTH_RECORD_VIEW',                 'Xem hồ sơ sức khoẻ của chiến mã'),
    ('HORSE_HEALTH_METRIC_VIEW',           'Xem chỉ số sinh tồn của chiến mã')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'HEAD_TRAINER'
  AND p.code IN ('RACING_READINESS_ASSESSMENT_CREATE', 'RACING_READINESS_ASSESSMENT_VIEW',
                 'HEALTH_RECORD_VIEW', 'HORSE_HEALTH_METRIC_VIEW')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name IN ('CLUB_MANAGER', 'VETERINARIAN', 'HORSE_OWNER')
  AND p.code IN ('RACING_READINESS_ASSESSMENT_VIEW', 'HEALTH_RECORD_VIEW',
                 'HORSE_HEALTH_METRIC_VIEW')
ON CONFLICT DO NOTHING;