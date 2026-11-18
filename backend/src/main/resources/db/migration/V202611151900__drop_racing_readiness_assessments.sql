-- =====================================================================
-- V202611151900: BỎ BẢNG ĐÁNH GIÁ SẴN SÀNG THI ĐẤU
-- (Drop the racing readiness assessment table.)
--
-- Bước Trainer của đơn nhập học nay chỉ nhập một nhận xét bằng chữ, lưu ở
-- admission_applications.trainer_feedback (cột này đã có và đã được ghi cùng
-- nội dung với remarks của bảng này). Điểm số, mức sẵn sàng và số tháng ước
-- tính không còn được lưu.
-- (The Trainer step now only hands in written feedback, kept in
-- admission_applications.trainer_feedback. Scores, readiness status and the
-- estimated months are no longer stored.)
--
-- Xem docs/adr/0002-trainer-review-is-free-text.md.
-- =====================================================================

DROP TABLE IF EXISTS racing_readiness_assessments;

-- Hai quyền chỉ phục vụ bảng này. (The two permissions only served this table.)
DELETE FROM role_permissions
 WHERE permission_id IN (SELECT id FROM permissions
                          WHERE code IN ('RACING_READINESS_ASSESSMENT_CREATE', 'RACING_READINESS_ASSESSMENT_VIEW'));

DELETE FROM permissions
 WHERE code IN ('RACING_READINESS_ASSESSMENT_CREATE', 'RACING_READINESS_ASSESSMENT_VIEW');
