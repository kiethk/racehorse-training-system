-- V202610200816__add_details_to_audit_logs.sql
-- Thêm cột details vào audit_logs để lưu mô tả nghiệp vụ thân thiện cho Manager

ALTER TABLE audit_logs
    ADD COLUMN details TEXT;

CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_name, entity_id);
