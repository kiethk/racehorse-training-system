-- =====================================================================
-- Training Groups foundation (expand step)
-- =====================================================================
--
-- Adds the Training Group model BESIDE the lot / per-horse plan tables so nothing existing breaks:
--   training_groups, training_group_members, group_plans, training_sessions, and a session link on training_workouts.
-- The old tables are dropped by a later migration once nothing reads them any more.
--
-- Rules guarded here as well as in the services:
--   BR-10  a group holds at most 6 horses
--   BR-09  no two members of one group share a Groom
--   one group per horse
--   unfinished Group Plans of one group never overlap in dates
--   non-cancelled Sessions of one Trainer never overlap in time
--   Session times sit on a 15-minute step inside the Golden Window (06:00 to 10:00)

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ---------------------------------------------------------------------
-- Training groups and their members
-- ---------------------------------------------------------------------
CREATE TABLE training_groups (
    id         BIGSERIAL PRIMARY KEY,
    trainer_id BIGINT       NOT NULL REFERENCES users (id),
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    CONSTRAINT chk_training_groups_name CHECK (length(btrim(name)) > 0),
    CONSTRAINT uq_training_groups_trainer_name UNIQUE (trainer_id, name)
);

CREATE TABLE training_group_members (
    id        BIGSERIAL PRIMARY KEY,
    group_id  BIGINT    NOT NULL REFERENCES training_groups (id) ON DELETE CASCADE,
    horse_id  BIGINT    NOT NULL REFERENCES horses (id),
    groom_id  BIGINT REFERENCES users (id) ON DELETE SET NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_group_members_horse UNIQUE (horse_id)
);

CREATE INDEX idx_group_members_group ON training_group_members (group_id);

-- BR-09: one Groom leads at most one horse of a group.
CREATE UNIQUE INDEX uq_group_members_groom ON training_group_members (group_id, groom_id) WHERE groom_id IS NOT NULL;

-- BR-10: at most 6 horses in a group. Deferred so a move (leave then join) inside one transaction is checked at commit.
CREATE FUNCTION trg_group_members_capacity() RETURNS trigger AS $$
BEGIN
    IF (SELECT count(*) FROM training_group_members WHERE group_id = NEW.group_id) > 6 THEN
        RAISE EXCEPTION 'Group % is full: more than 6 horses', NEW.group_id USING ERRCODE = '23514';
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER group_members_capacity
    AFTER INSERT OR UPDATE OF group_id ON training_group_members
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION trg_group_members_capacity();

-- ---------------------------------------------------------------------
-- Group plans (a group running a course)
-- ---------------------------------------------------------------------
CREATE TABLE group_plans (
    id            BIGSERIAL PRIMARY KEY,
    group_id      BIGINT REFERENCES training_groups (id) ON DELETE SET NULL,
    course_id     BIGINT      NOT NULL REFERENCES courses (id),
    trainer_id    BIGINT      NOT NULL REFERENCES users (id),
    start_date    DATE        NOT NULL,
    end_date      DATE        NOT NULL,
    training_days VARCHAR(100) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
    created_at    TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP,
    CONSTRAINT chk_group_plans_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_group_plans_status CHECK (status IN ('UPCOMING', 'ACTIVE', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_group_plans_training_days CHECK (training_days ~
        '^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)(,(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY))*$'),
    CONSTRAINT excl_group_plans_dates EXCLUDE USING gist (
        group_id WITH =,
        daterange(start_date, end_date, '[]') WITH &&
    ) WHERE (status IN ('UPCOMING', 'ACTIVE'))
);

CREATE INDEX idx_group_plans_group ON group_plans (group_id, status);
CREATE INDEX idx_group_plans_trainer ON group_plans (trainer_id, status);

-- ---------------------------------------------------------------------
-- Sessions (one outing of a group on one day)
-- ---------------------------------------------------------------------
CREATE TABLE training_sessions (
    id            BIGSERIAL PRIMARY KEY,
    group_plan_id BIGINT      NOT NULL REFERENCES group_plans (id) ON DELETE CASCADE,
    subject_id    BIGINT      NOT NULL REFERENCES subjects (id) ON DELETE RESTRICT,
    trainer_id    BIGINT      NOT NULL REFERENCES users (id),
    session_date  DATE        NOT NULL,
    start_time    TIME        NOT NULL,
    end_time      TIME        NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at    TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP,
    CONSTRAINT chk_training_sessions_status CHECK (status IN ('SCHEDULED', 'HELD', 'CANCELLED')),
    CONSTRAINT chk_training_sessions_time CHECK (start_time < end_time),
    CONSTRAINT chk_training_sessions_golden_window CHECK (status = 'CANCELLED'
        OR (start_time >= TIME '06:00' AND end_time <= TIME '10:00')),
    CONSTRAINT chk_training_sessions_quarter_hour CHECK (status = 'CANCELLED'
        OR (EXTRACT(MINUTE FROM start_time)::int % 15 = 0 AND EXTRACT(SECOND FROM start_time) = 0)),
    CONSTRAINT excl_training_sessions_trainer_time EXCLUDE USING gist (
        trainer_id WITH =,
        tsrange(session_date + start_time, session_date + end_time) WITH &&
    ) WHERE (status <> 'CANCELLED')
);

CREATE INDEX idx_training_sessions_plan ON training_sessions (group_plan_id, status);
CREATE INDEX idx_training_sessions_trainer_date ON training_sessions (trainer_id, session_date);

-- ---------------------------------------------------------------------
-- Workouts now belong to a Session (the lot / plan link stays until the old model is dropped)
-- ---------------------------------------------------------------------
ALTER TABLE training_workouts
    ADD COLUMN session_id BIGINT REFERENCES training_sessions (id) ON DELETE CASCADE,
    ALTER COLUMN plan_id DROP NOT NULL,
    ALTER COLUMN lot_id DROP NOT NULL;

ALTER TABLE training_workouts DROP CONSTRAINT chk_workouts_status;
ALTER TABLE training_workouts
    ADD CONSTRAINT chk_workouts_status CHECK (status IN ('SCHEDULED', 'COMPLETED', 'HELD', 'CANCELLED')) NOT VALID,
    ADD CONSTRAINT chk_workouts_one_model CHECK (
        (session_id IS NOT NULL AND lot_id IS NULL AND plan_id IS NULL)
        OR (session_id IS NULL AND lot_id IS NOT NULL AND plan_id IS NOT NULL)) NOT VALID,
    -- A Held Workout either has a full recorded result or none at all.
    ADD CONSTRAINT chk_workouts_held_result_all_or_none CHECK (status <> 'HELD' OR (
        (actual_distance_meters IS NULL AND actual_duration_minutes IS NULL AND top_speed_kmh IS NULL
            AND average_speed_kmh IS NULL AND average_heart_rate IS NULL AND max_heart_rate IS NULL
            AND recovery_heart_rate IS NULL AND performance_rating IS NULL)
        OR (actual_distance_meters IS NOT NULL AND actual_duration_minutes IS NOT NULL
            AND top_speed_kmh IS NOT NULL AND average_speed_kmh IS NOT NULL
            AND average_heart_rate IS NOT NULL AND max_heart_rate IS NOT NULL
            AND recovery_heart_rate IS NOT NULL AND performance_rating IS NOT NULL)));

CREATE INDEX idx_training_workouts_session ON training_workouts (session_id);
CREATE UNIQUE INDEX uq_workout_session_horse_active ON training_workouts (session_id, horse_id)
    WHERE session_id IS NOT NULL AND status <> 'CANCELLED';

-- ---------------------------------------------------------------------
-- Authorities (same role grants as the lot / plan authorities they sit beside)
-- ---------------------------------------------------------------------
INSERT INTO permissions (code, description)
SELECT v.code, v.description
FROM (VALUES
    ('TRAINING_GROUP_VIEW', 'View training groups'),
    ('TRAINING_GROUP_MANAGE', 'Create, change and delete training groups and their horses'),
    ('GROUP_PLAN_VIEW', 'Open a group plan'),
    ('GROUP_PLAN_LIST', 'List all group plans (trainer and club manager only)'),
    ('GROUP_PLAN_CREATE', 'Assign a course to a training group and cancel a group plan'),
    ('SESSION_VIEW', 'View training sessions'),
    ('SESSION_UPDATE', 'Move or cancel training sessions and workouts')
) AS v(code, description)
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.code = v.code);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM (VALUES
    ('TRAINING_GROUP_VIEW', 'HEAD_TRAINER'), ('TRAINING_GROUP_VIEW', 'CLUB_MANAGER'),
    ('TRAINING_GROUP_MANAGE', 'HEAD_TRAINER'),
    ('GROUP_PLAN_VIEW', 'HEAD_TRAINER'), ('GROUP_PLAN_VIEW', 'CLUB_MANAGER'),
    ('GROUP_PLAN_VIEW', 'HORSE_OWNER'), ('GROUP_PLAN_VIEW', 'VETERINARIAN'),
    ('GROUP_PLAN_LIST', 'HEAD_TRAINER'), ('GROUP_PLAN_LIST', 'CLUB_MANAGER'),
    ('GROUP_PLAN_CREATE', 'HEAD_TRAINER'),
    ('SESSION_VIEW', 'HEAD_TRAINER'), ('SESSION_VIEW', 'CLUB_MANAGER'),
    ('SESSION_VIEW', 'GROOM'), ('SESSION_VIEW', 'HORSE_OWNER'),
    ('SESSION_UPDATE', 'HEAD_TRAINER')
) AS g(permission_code, role_name)
JOIN permissions p ON p.code = g.permission_code
JOIN roles r ON r.name = g.role_name
WHERE NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
