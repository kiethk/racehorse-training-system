-- =====================================================================
-- V62: INDEX CHO HÀNG CHỜ ĐÁNH GIÁ CỦA TỪNG HUẤN LUYỆN VIÊN
-- =====================================================================
--
-- Hàng chờ của Trainer đổi từ "mọi đơn TRAINER_REVIEW" sang "đơn
-- TRAINER_REVIEW được phân cho chính tôi", nên câu truy vấn mới có dạng:
--     WHERE status = ? AND (trainer_id = ? OR trainer_id IS NULL)
--     WHERE trainer_id = ? AND trainer_reviewed_at IS NOT NULL
--
-- admission_applications.trainer_id đã có khoá ngoại từ V25, nhưng Postgres
-- KHÔNG tự tạo index cho cột khoá ngoại (khác với khoá chính và UNIQUE).
-- Hiện chỉ có idx_admission_status, nên hai câu trên phải quét toàn bảng.
--
-- trainer_id đứng trước status vì nó lọc mạnh hơn: số Trainer nhiều, còn
-- status chỉ có 8 giá trị. Index này cũng phục vụ được câu thứ hai, vì
-- trainer_id là cột đầu tiên (nguyên tắc leftmost prefix).
-- ---------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_admission_trainer_status
    ON admission_applications (trainer_id, status);
