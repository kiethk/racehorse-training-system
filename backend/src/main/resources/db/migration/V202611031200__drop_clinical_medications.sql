-- V202611031200 — Track 0 (ROADMAP v3.0): bỏ bảng clinical_medications thừa.
-- Đơn thuốc Vet kê giờ lưu JSON snake_case trong treatment_plans.instructions.
-- Bảng prescriptions giữ lại (dữ liệu lịch sử) nhưng không ghi mới.
DROP TABLE IF EXISTS clinical_medications;