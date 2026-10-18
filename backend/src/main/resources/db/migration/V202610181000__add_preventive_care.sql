-- Preventive care (docs/preventive-care-flow.md):
--   preventive_protocols    danh mục phác đồ tiêm phòng / tẩy giun, chỉ đọc trong MVP
--   prescriptions           gắn mũi tiêm với phác đồ để tính hạn tiêm kế tiếp
--   care_schedule.purpose   tách mốc tiêm phòng khỏi lịch tái khám thường
--   care_schedule.reschedule_*  lý do dời lịch

CREATE TABLE preventive_protocols (
    id             BIGSERIAL PRIMARY KEY,
    code           VARCHAR(30)  NOT NULL UNIQUE,
    name           VARCHAR(100) NOT NULL,
    category       VARCHAR(20)  NOT NULL,
    interval_days  INT          NOT NULL,
    warning_days   INT          NOT NULL,
    mandatory      BOOLEAN      NOT NULL DEFAULT FALSE,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    default_dosage VARCHAR(100),
    default_route  VARCHAR(100),
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_preventive_protocols_category CHECK (category IN ('VACCINE', 'DEWORMING')),
    CONSTRAINT chk_preventive_protocols_interval CHECK (interval_days > 0),
    CONSTRAINT chk_preventive_protocols_warning CHECK (warning_days >= 0 AND warning_days < interval_days)
);

INSERT INTO preventive_protocols (code, name, category, interval_days, warning_days, mandatory, default_dosage, default_route)
VALUES
    ('EI',        'Cúm ngựa (Equine Influenza)', 'VACCINE',   180, 14, TRUE,  '1 liều',          'IM'),
    ('TETANUS',   'Uốn ván (Tetanus)',           'VACCINE',   365, 30, TRUE,  '1 liều',          'IM'),
    ('RABIES',    'Bệnh dại (Rabies)',           'VACCINE',   365, 30, FALSE, '1 liều',          'IM'),
    ('EEE_WEE',   'Viêm não ngựa (EEE/WEE)',     'VACCINE',   365, 30, TRUE,  '1 liều',          'IM'),
    ('WEST_NILE', 'West Nile',                   'VACCINE',   365, 30, FALSE, '1 liều',          'IM'),
    ('DEWORM',    'Tẩy giun định kỳ',            'DEWORMING',  90,  7, TRUE,  'Theo cân nặng',   'Oral');

ALTER TABLE prescriptions
    ADD COLUMN preventive_protocol_id BIGINT REFERENCES preventive_protocols(id);

CREATE INDEX idx_prescriptions_preventive_protocol
    ON prescriptions(preventive_protocol_id)
    WHERE preventive_protocol_id IS NOT NULL;

ALTER TABLE care_schedule
    ADD COLUMN purpose VARCHAR(20) NOT NULL DEFAULT 'FOLLOW_UP',
    ADD COLUMN reschedule_reason VARCHAR(30),
    ADD COLUMN reschedule_note TEXT,
    ADD CONSTRAINT chk_care_schedule_purpose CHECK (purpose IN ('FOLLOW_UP', 'PREVENTIVE')),
    ADD CONSTRAINT chk_care_schedule_reschedule_reason CHECK (reschedule_reason IS NULL
        OR reschedule_reason IN ('RACE_CONFLICT', 'UNDER_TREATMENT', 'OWNER_REQUEST', 'WEATHER', 'OTHER'));

-- Mỗi ngựa chỉ có một mốc tiêm phòng đang chờ; upsert dời mốc này thay vì sinh thêm.
CREATE UNIQUE INDEX uq_care_schedule_one_active_preventive_per_horse
    ON care_schedule(horse_id)
    WHERE purpose = 'PREVENTIVE' AND status IN ('REQUESTED', 'SCHEDULED', 'IN_PROGRESS');
