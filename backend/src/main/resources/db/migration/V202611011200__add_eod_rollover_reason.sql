-- V202611011200 — Track 1 (ROADMAP v2.2 §4.5 / D1): EOD rollover dùng reschedule_reason mới.
-- Chỉ nới CHECK constraint — không đổi dữ liệu, không ảnh seed (seed chỉ ghi UNDER_TREATMENT).
ALTER TABLE care_schedule DROP CONSTRAINT IF EXISTS chk_care_schedule_reschedule_reason;
ALTER TABLE care_schedule ADD CONSTRAINT chk_care_schedule_reschedule_reason
    CHECK (reschedule_reason IS NULL OR reschedule_reason IN (
        'RACE_CONFLICT', 'UNDER_TREATMENT', 'OWNER_REQUEST', 'WEATHER', 'EOD_ROLLOVER', 'OTHER'));
