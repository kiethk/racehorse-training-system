ALTER TABLE race_registrations
    ADD COLUMN race_category VARCHAR(255),
    ADD COLUMN organizer VARCHAR(255),
    ADD COLUMN source_url VARCHAR(1000),
    ADD COLUMN nomination_deadline DATE,
    ADD COLUMN event_time TIME,
    ADD COLUMN prize_details TEXT,
    ADD COLUMN selection_reason TEXT;

-- Giữ giờ thật của dữ liệu cũ; 00:00 thường là giá trị ngày không có giờ.
UPDATE race_registrations
SET event_time = event_date::time
WHERE event_date::time <> TIME '00:00';

ALTER TABLE race_registrations
    ALTER COLUMN event_date TYPE DATE USING event_date::date;

CREATE INDEX idx_race_registrations_trainer_created
    ON race_registrations (trainer_id, created_at DESC);
