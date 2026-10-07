ALTER TABLE audit_logs
    ADD COLUMN http_method VARCHAR(10),
    ADD COLUMN request_path VARCHAR(500),
    ADD COLUMN status_code INTEGER;

ALTER TABLE audit_logs
    ALTER COLUMN action DROP NOT NULL,
    ALTER COLUMN entity_name DROP NOT NULL;
