-- V60 completes the CareSchedule refactor introduced in V58. Flyway runs this
-- in one transaction; unexpected FK or clinical link conflicts abort the merge.
ALTER TABLE care_schedules RENAME TO care_schedule;

-- Preserve original subtypes, request dates, authors and preferred vets.
ALTER TABLE care_schedule ADD COLUMN legacy_data JSONB NOT NULL DEFAULT '[]'::jsonb;
CREATE TEMP TABLE legacy_exam_map (old_id BIGINT PRIMARY KEY, new_id BIGINT NOT NULL) ON COMMIT DROP;
CREATE TEMP TABLE legacy_preventive_map (old_id BIGINT PRIMARY KEY, new_id BIGINT NOT NULL) ON COMMIT DROP;

DO $$
DECLARE
    e RECORD;
    p RECORD;
    target_id BIGINT;
BEGIN
    FOR e IN SELECT * FROM vet_exams ORDER BY id LOOP
        target_id := NULL;
        SELECT care_schedule_id INTO target_id FROM health_records WHERE id = e.health_record_id;
        IF target_id IS NULL AND e.exam_type = 'INITIAL'
           AND e.status IN ('REQUESTED', 'SCHEDULED', 'IN_PROGRESS') THEN
            SELECT MIN(id) INTO target_id FROM care_schedule
            WHERE admission_id = e.admission_id AND horse_id = e.horse_id AND care_type = 'INITIAL'
              AND status IN ('REQUESTED', 'AWAITING_VET_CONFIRMATION', 'SCHEDULED', 'IN_PROGRESS')
              AND legacy_data = '[]'::jsonb
            HAVING COUNT(*) = 1;
        END IF;
        IF target_id IS NULL THEN
            INSERT INTO care_schedule (horse_id, veterinarian_id, admission_id, care_type, status,
                    scheduled_at, duration_minutes, description, completed_at, created_at, updated_at)
            VALUES (e.horse_id, e.assigned_vet_id, e.admission_id,
                    CASE WHEN e.exam_type = 'FOLLOW_UP' THEN 'ROUTINE' ELSE e.exam_type END,
                    e.status, e.scheduled_at, e.duration_minutes, e.reason,
                    CASE WHEN e.status = 'COMPLETED' THEN
                        COALESCE((SELECT examined_at FROM health_records WHERE id = e.health_record_id), e.updated_at)
                    END, e.created_at, e.updated_at)
            RETURNING id INTO target_id;
        ELSE
            -- Never regress work already progressed in the new flow.
            UPDATE care_schedule SET
                veterinarian_id = CASE WHEN status = 'REQUESTED' AND e.status IN ('SCHEDULED', 'IN_PROGRESS')
                                       THEN e.assigned_vet_id ELSE veterinarian_id END,
                scheduled_at = CASE WHEN status = 'REQUESTED' AND e.status IN ('SCHEDULED', 'IN_PROGRESS')
                                    THEN e.scheduled_at ELSE scheduled_at END,
                status = CASE WHEN status = 'REQUESTED' AND e.status IN ('SCHEDULED', 'IN_PROGRESS')
                              THEN e.status ELSE status END
            WHERE id = target_id;
        END IF;
        UPDATE care_schedule SET legacy_data = legacy_data || jsonb_build_array(
            jsonb_build_object('source', 'vet_exams', 'row', to_jsonb(e))) WHERE id = target_id;
        INSERT INTO legacy_exam_map VALUES (e.id, target_id);
        IF e.health_record_id IS NOT NULL THEN
            IF EXISTS (SELECT 1 FROM health_records WHERE care_schedule_id = target_id AND id <> e.health_record_id) THEN
                RAISE EXCEPTION 'Conflicting health records for legacy vet exam %', e.id;
            END IF;
            UPDATE health_records SET care_schedule_id = target_id WHERE id = e.health_record_id;
        END IF;
    END LOOP;

    FOR p IN SELECT * FROM preventive_care_schedules ORDER BY id LOOP
        target_id := NULL;
        SELECT care_schedule_id INTO target_id FROM health_records WHERE preventive_care_schedule_id = p.id;
        -- V52 cancelled INITIAL_EXAM rows when it created the equivalent exam.
        IF target_id IS NULL AND p.care_type = 'INITIAL_EXAM' AND p.status = 'CANCELLED'
           AND NOT EXISTS (SELECT 1 FROM health_records WHERE preventive_care_schedule_id = p.id) THEN
            SELECT m.new_id INTO target_id
            FROM vet_exams exam_source JOIN legacy_exam_map m ON m.old_id = exam_source.id
            WHERE exam_source.horse_id = p.horse_id AND exam_source.exam_type = 'INITIAL'
              AND exam_source.reason IS NOT DISTINCT FROM COALESCE(p.description, 'Initial admission examination')
              AND exam_source.scheduled_at::date IS NOT DISTINCT FROM p.scheduled_date
            ORDER BY exam_source.created_at, exam_source.id LIMIT 1;
        END IF;
        IF target_id IS NULL THEN
            INSERT INTO care_schedule (horse_id, veterinarian_id, care_type, status,
                    scheduled_at, description, completed_at, created_at, updated_at)
            VALUES (p.horse_id,
                    CASE WHEN p.status = 'COMPLETED' THEN p.veterinarian_id END,
                    CASE WHEN p.care_type = 'INITIAL_EXAM' THEN 'INITIAL' ELSE 'ROUTINE' END,
                    CASE WHEN p.status IN ('PENDING', 'OVERDUE') THEN 'REQUESTED' ELSE p.status END,
                    CASE WHEN p.status = 'COMPLETED' THEN p.scheduled_date::timestamp + INTERVAL '14 hours' END,
                    p.care_type || ': ' || COALESCE(p.description, ''),
                    CASE WHEN p.status = 'COMPLETED' THEN
                        COALESCE((SELECT examined_at FROM health_records WHERE preventive_care_schedule_id = p.id), p.updated_at)
                    END, p.created_at, p.updated_at)
            RETURNING id INTO target_id;
        END IF;
        UPDATE care_schedule SET legacy_data = legacy_data || jsonb_build_array(
            jsonb_build_object('source', 'preventive_care_schedules', 'row', to_jsonb(p))) WHERE id = target_id;
        INSERT INTO legacy_preventive_map VALUES (p.id, target_id);
        IF EXISTS (SELECT 1 FROM health_records WHERE preventive_care_schedule_id = p.id) THEN
            IF EXISTS (SELECT 1 FROM health_records WHERE care_schedule_id = target_id
                       AND preventive_care_schedule_id IS DISTINCT FROM p.id) THEN
                RAISE EXCEPTION 'Conflicting health records for legacy preventive schedule %', p.id;
            END IF;
            UPDATE health_records SET care_schedule_id = target_id WHERE preventive_care_schedule_id = p.id;
        END IF;
    END LOOP;
    IF (SELECT COUNT(*) FROM legacy_exam_map) <> (SELECT COUNT(*) FROM vet_exams)
       OR (SELECT COUNT(*) FROM legacy_preventive_map) <> (SELECT COUNT(*) FROM preventive_care_schedules) THEN
        RAISE EXCEPTION 'Incomplete legacy schedule migration';
    END IF;
END $$;

-- PostgreSQL retargets health_records and vet_offers FKs automatically on rename.
ALTER TABLE health_records DROP COLUMN preventive_care_schedule_id;
DROP TABLE vet_exams;
DROP TABLE preventive_care_schedules;
