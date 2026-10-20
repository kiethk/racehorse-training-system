ALTER TABLE stable_stalls
    DROP CONSTRAINT IF EXISTS chk_stable_stalls_status;

ALTER TABLE stable_stalls
    ADD CONSTRAINT chk_stable_stalls_status
        CHECK (status IN ('AVAILABLE', 'RESERVED', 'OCCUPIED'));

ALTER TABLE admission_applications
    ADD COLUMN arrival_deadline_at TIMESTAMP,
    ADD COLUMN arrived_at TIMESTAMP;

ALTER TABLE admission_applications
    DROP CONSTRAINT IF EXISTS chk_admission_status,
    DROP CONSTRAINT IF EXISTS chk_admission_q_stall_required,
    DROP CONSTRAINT IF EXISTS chk_admission_post_groom_has_horse;

ALTER TABLE admission_applications
    ADD CONSTRAINT chk_admission_status CHECK (status IN (
        'GROOM_REVIEW', 'WAITING_FOR_STALL', 'WAITING_FOR_ARRIVAL', 'ARRIVAL_EXPIRED',
        'VET_REVIEW', 'TRAINER_REVIEW', 'MANAGER_REVIEW', 'APPROVED', 'REJECTED'
    )),
    ADD CONSTRAINT chk_admission_q_stall_required CHECK (
        status NOT IN ('WAITING_FOR_ARRIVAL', 'VET_REVIEW', 'TRAINER_REVIEW', 'MANAGER_REVIEW', 'APPROVED')
        OR quarantine_stall_id IS NOT NULL
    ),
    ADD CONSTRAINT chk_admission_post_groom_has_horse CHECK (
        status NOT IN ('VET_REVIEW', 'TRAINER_REVIEW', 'MANAGER_REVIEW', 'APPROVED')
        OR horse_id IS NOT NULL
    ) NOT VALID,
    ADD CONSTRAINT chk_admission_arrival_deadline CHECK (
        status <> 'WAITING_FOR_ARRIVAL' OR arrival_deadline_at IS NOT NULL
    ),
    ADD CONSTRAINT chk_admission_waiting_arrival_has_no_horse CHECK (
        status <> 'WAITING_FOR_ARRIVAL' OR horse_id IS NULL
    ),
    ADD CONSTRAINT chk_admission_arrival_expired_released CHECK (
        status <> 'ARRIVAL_EXPIRED' OR (quarantine_stall_id IS NULL AND horse_id IS NULL)
    );

INSERT INTO permissions (code, description)
VALUES ('ADMISSION_ARRIVAL_REOPEN', 'Reopen an expired horse arrival reservation');
