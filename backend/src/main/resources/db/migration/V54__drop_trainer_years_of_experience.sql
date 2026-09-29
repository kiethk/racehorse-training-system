-- =====================================================================
-- V54: BỎ CỘT years_of_experience KHỎI trainer_profiles
--
-- Cột có từ V5 nhưng chỉ có ĐƯỜNG GHI, không có đường đọc:
--   - StaffManagementService ghi giá trị khi Quản lý tạo tài khoản Trainer
--   - KHÔNG API nào trả nó về, KHÔNG màn hình nào hiển thị,
--     KHÔNG logic nghiệp vụ nào dùng tới
--
-- Dữ liệu chỉ vào mà không bao giờ ra thì không mang lại giá trị, lại khiến
-- người dùng mất công nhập. Bỏ hẳn thay vì để đó chờ một màn hình có thể
-- không bao giờ được làm.
--
-- LƯU Ý: spring.jpa.hibernate.ddl-auto=validate, nên migration này PHẢI đi
-- kèm việc xoá trường yearsOfExperience trong entity TrainerProfile. Xoá cột
-- mà giữ trường Java sẽ làm ứng dụng không khởi động được.
-- =====================================================================

ALTER TABLE trainer_profiles
    DROP COLUMN IF EXISTS years_of_experience;
