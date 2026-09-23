-- =====================================================================
-- V46: XOÁ CỘT groom_profiles.shift (ENUM CHẾT)
--
-- Vì sao xoá:
--   - Không code nào đọc hay ghi cột này kể từ khi tạo ở V8.
--   - Thiết kế hiện tại mâu thuẫn với khái niệm ca: SOP Generator sinh đủ 5
--     việc từ 05:00 tới 16:30 cho MỌI Groom, tức mô hình FULL_DAY. Một Groom
--     ca MORNING vẫn bị giao việc cho ăn 16:30.
--   - Xử lý ca đúng nghĩa sẽ phải lọc SopSlot theo ca VÀ quyết định ai làm
--     những việc ngoài ca -> đẻ thêm nghiệp vụ bàn giao ca, ngoài phạm vi.
--   - Ràng buộc BR-06 (tối đa 3 chuồng/Groom) đã đủ để giới hạn khối lượng
--     công việc, không cần thêm chiều "ca".
--
-- Nếu sau này cần lại, thêm cột mới trong migration sau — rẻ hơn nhiều so với
-- việc duy trì một cột không ai dùng mà ai đọc code cũng tưởng là có ý nghĩa.
-- =====================================================================

ALTER TABLE groom_profiles
    DROP COLUMN IF EXISTS shift;
