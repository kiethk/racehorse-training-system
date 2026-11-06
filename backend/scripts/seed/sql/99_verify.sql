-- =====================================================================
-- 99_verify.sql — kiểm tra luật nghiệp vụ trên toàn bộ dữ liệu vừa seed. Có vi phạm thì báo từng luật và DỪNG:
-- cả lần seed nằm trong một transaction nên DB được trả về như trước khi chạy.
-- (Business-rule checks over everything just seeded. Any violation is listed and STOPS the run: the whole seed is one
-- transaction, so the database is left as it was.)
--
-- Thêm luật mới cho phần của mình: thêm một dòng (tên luật, câu SELECT trả về các dòng vi phạm).
-- (To add a rule for your part: add one row (rule name, a SELECT returning the violating rows).)
-- =====================================================================
CREATE TEMP TABLE seed_check (name TEXT NOT NULL, query TEXT NOT NULL);
INSERT INTO seed_check VALUES
-- ---------------- Tài khoản, chuồng (Accounts, stable)
('Groom has more than 3 stalls (BR-06)',
 $q$ SELECT groom_id FROM stable_stalls WHERE groom_id IS NOT NULL GROUP BY groom_id HAVING count(*) > 3 $q$),
('Regular groom reports to a trainer other than the area trainer',
 $q$ SELECT s.groom_id FROM stable_stalls s JOIN areas a ON a.id = s.area_id JOIN groom_profiles g ON g.user_id = s.groom_id
     WHERE g.trainer_id IS DISTINCT FROM a.trainer_id $q$),
('Quarantine area has a trainer',
 $q$ SELECT id FROM areas WHERE type = 'QUARANTINE' AND trainer_id IS NOT NULL $q$),
('Staff without a role profile',
 $q$ SELECT u.id FROM users u JOIN roles r ON r.id = u.role_id
     WHERE (r.name = 'HEAD_TRAINER' AND NOT EXISTS (SELECT 1 FROM trainer_profiles p WHERE p.user_id = u.id))
        OR (r.name = 'VETERINARIAN' AND NOT EXISTS (SELECT 1 FROM veterinarian_profiles p WHERE p.user_id = u.id))
        OR (r.name = 'GROOM' AND NOT EXISTS (SELECT 1 FROM groom_profiles p WHERE p.user_id = u.id)) $q$),
('Inactive staff still assigned to stalls or areas',
 $q$ SELECT u.id FROM users u WHERE NOT u.is_active
       AND (EXISTS (SELECT 1 FROM stable_stalls s WHERE s.groom_id = u.id) OR EXISTS (SELECT 1 FROM areas a WHERE a.trainer_id = u.id)) $q$),
('Stall status does not match its horse / reservation',
 $q$ SELECT s.id FROM stable_stalls s
     WHERE s.status <> CASE WHEN EXISTS (SELECT 1 FROM horses h WHERE h.current_stall_id = s.id) THEN 'OCCUPIED'
                            WHEN EXISTS (SELECT 1 FROM admission_applications a WHERE a.quarantine_stall_id = s.id
                                                                                  AND a.status = 'WAITING_FOR_ARRIVAL') THEN 'RESERVED'
                            ELSE 'AVAILABLE' END $q$),
-- ---------------- Nhập học (Admissions)
('CANDIDATE horse not in a quarantine stall with an open admission',
 $q$ SELECT h.id FROM horses h LEFT JOIN stable_stalls s ON s.id = h.current_stall_id LEFT JOIN areas a ON a.id = s.area_id
     WHERE h.current_status = 'CANDIDATE'
       AND (a.type IS DISTINCT FROM 'QUARANTINE' OR NOT EXISTS (SELECT 1 FROM admission_applications x WHERE x.horse_id = h.id
            AND x.status IN ('VET_REVIEW', 'TRAINER_REVIEW', 'MANAGER_REVIEW') AND x.quarantine_stall_id = h.current_stall_id)) $q$),
('ELIGIBLE horse not in a regular stall or without an approved admission',
 $q$ SELECT h.id FROM horses h LEFT JOIN stable_stalls s ON s.id = h.current_stall_id LEFT JOIN areas a ON a.id = s.area_id
     WHERE h.current_status = 'ELIGIBLE' AND (a.type IS DISTINCT FROM 'REGULAR'
       OR NOT EXISTS (SELECT 1 FROM admission_applications x WHERE x.horse_id = h.id AND x.status = 'APPROVED')) $q$),
('REJECTED horse still in a stall',
 $q$ SELECT id FROM horses WHERE current_status = 'REJECTED' AND current_stall_id IS NOT NULL $q$),
('Admission without its snapshot or required documents',
 $q$ SELECT a.id FROM admission_applications a
     WHERE NOT EXISTS (SELECT 1 FROM candidate_horse_profiles c WHERE c.admission_id = a.id)
        OR (SELECT count(DISTINCT d.document_type) FROM admission_documents d WHERE d.admission_id = a.id
             AND d.document_type IN ('HORSE_PHOTO', 'REGISTRATION_DOCUMENT', 'PEDIGREE_CERTIFICATE', 'VACCINATION_RECORD')) < 4 $q$),
('Admission past Groom review without a Groom decision',
 $q$ SELECT id FROM admission_applications
     WHERE status NOT IN ('GROOM_REVIEW') AND groom_decision IS NULL
       AND NOT (status = 'REJECTED' AND manager_decision = 'REJECTED') $q$),
('Groom-rejected admission went further',
 $q$ SELECT id FROM admission_applications WHERE groom_decision = 'REJECTED' AND (status <> 'REJECTED' OR horse_id IS NOT NULL) $q$),
('Waiting for arrival with a deadline already passed',
 $q$ SELECT id FROM admission_applications WHERE status = 'WAITING_FOR_ARRIVAL' AND arrival_deadline_at <= pg_temp.clock() $q$),
('Waiting for a stall while capacity is available',
 $q$ SELECT id FROM admission_applications WHERE status = 'WAITING_FOR_STALL'
       AND (SELECT count(*) FROM stable_stalls s JOIN areas a ON a.id = s.area_id WHERE a.type = 'QUARANTINE' AND s.status = 'AVAILABLE') >= 1 $q$),
('Admission at or past vet review without its own intake exam',
 $q$ SELECT a.id FROM admission_applications a WHERE a.horse_id IS NOT NULL AND a.arrived_at IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM care_schedule c WHERE c.admission_id = a.id AND c.care_type = 'INITIAL') $q$),
('Admission past vet review without a completed intake exam and vet decision',
 $q$ SELECT a.id FROM admission_applications a
     WHERE (a.status IN ('TRAINER_REVIEW', 'MANAGER_REVIEW', 'APPROVED') OR a.manager_decision = 'REJECTED')
       AND (a.vet_training_decision IS NULL OR NOT EXISTS (SELECT 1 FROM care_schedule c
            WHERE c.admission_id = a.id AND c.care_type = 'INITIAL' AND c.status = 'COMPLETED')) $q$),
('Trainer step without an assigned trainer',
 $q$ SELECT id FROM admission_applications WHERE status IN ('TRAINER_REVIEW', 'MANAGER_REVIEW', 'APPROVED') AND trainer_id IS NULL $q$),
('Manager step without the trainer assessment of this admission',
 $q$ SELECT a.id FROM admission_applications a
     WHERE (a.status IN ('MANAGER_REVIEW', 'APPROVED') OR a.manager_decision = 'REJECTED')
       AND NOT EXISTS (SELECT 1 FROM racing_readiness_assessments r WHERE r.admission_id = a.id AND r.trainer_id = a.trainer_id) $q$),
('Steps out of time order',
 $q$ SELECT id FROM admission_applications
     WHERE groom_reviewed_at < submitted_at OR arrived_at < groom_reviewed_at OR vet_reviewed_at < arrived_at
        OR trainer_reviewed_at < vet_reviewed_at OR manager_reviewed_at < trainer_reviewed_at $q$),
-- ---------------- Thú y (Veterinary)
('Vet has two overlapping appointments',
 $q$ SELECT a.id FROM care_schedule a JOIN care_schedule b ON a.veterinarian_id = b.veterinarian_id AND a.id < b.id
     WHERE a.status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED') AND b.status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED')
       AND a.scheduled_at < b.scheduled_at + make_interval(mins => b.duration_minutes)
       AND b.scheduled_at < a.scheduled_at + make_interval(mins => a.duration_minutes) $q$),
('Horse has two overlapping appointments',
 $q$ SELECT a.id FROM care_schedule a JOIN care_schedule b ON a.horse_id = b.horse_id AND a.id < b.id
     WHERE a.status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED') AND b.status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED')
       AND a.scheduled_at < b.scheduled_at + make_interval(mins => b.duration_minutes)
       AND b.scheduled_at < a.scheduled_at + make_interval(mins => a.duration_minutes) $q$),
('Routine visit outside the 13:30-15:30 vet window',
 $q$ SELECT id FROM care_schedule WHERE care_type = 'ROUTINE' AND scheduled_at IS NOT NULL
       AND scheduled_at::TIME NOT IN (TIME '13:30', TIME '14:00', TIME '14:30', TIME '15:00') $q$),
('Horse with more than one examination in progress',
 $q$ SELECT horse_id FROM care_schedule WHERE status = 'IN_PROGRESS' GROUP BY horse_id HAVING count(*) > 1 $q$),
('Booked schedule without vet or time / unbooked schedule with a vet slot',
 $q$ SELECT id FROM care_schedule
     WHERE (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED') AND (veterinarian_id IS NULL OR scheduled_at IS NULL))
        OR (status = 'REQUESTED' AND scheduled_at IS NOT NULL)
        OR (status = 'REQUESTED' AND care_type = 'ROUTINE' AND veterinarian_id IS NOT NULL) $q$),
('Pending routine schedule inside the 14-day horizon left unbooked',
 $q$ SELECT id FROM care_schedule WHERE status = 'REQUESTED' AND care_type = 'ROUTINE'
       AND GREATEST(created_at, requested_at - INTERVAL '14 days') <= pg_temp.clock() $q$),
('Completed schedule without exactly one health record / open schedule with one',
 $q$ SELECT c.id FROM care_schedule c
     WHERE (c.status = 'COMPLETED') <> EXISTS (SELECT 1 FROM health_records h WHERE h.care_schedule_id = c.id)
        OR (c.status = 'COMPLETED' AND c.completed_at IS NULL) $q$),
('Something completed in the future',
 $q$ SELECT id FROM care_schedule WHERE completed_at > pg_temp.clock() OR (status = 'COMPLETED' AND scheduled_at > pg_temp.clock())
     UNION ALL SELECT id FROM health_records WHERE examined_at > pg_temp.clock()
     UNION ALL SELECT id FROM groom_daily_tasks WHERE completed_at > pg_temp.clock() $q$),
('BLOCKED exam without a follow-up',
 $q$ SELECT h.id FROM health_records h WHERE h.training_decision = 'BLOCKED'
       AND NOT EXISTS (SELECT 1 FROM care_schedule c WHERE c.source_schedule_id = h.care_schedule_id) $q$),
('Completed routine follow-up without its automatic recheck',
 $q$ SELECT c.id FROM care_schedule c WHERE c.status = 'COMPLETED' AND c.care_type = 'ROUTINE' AND c.purpose = 'FOLLOW_UP'
       AND NOT EXISTS (SELECT 1 FROM care_schedule x WHERE x.source_schedule_id = c.id) $q$),
('BLOCKED horse without a pending examination to lift the block',
 $q$ SELECT h.id FROM horses h WHERE h.training_decision = 'BLOCKED'
       AND NOT EXISTS (SELECT 1 FROM care_schedule c WHERE c.horse_id = h.id AND c.purpose = 'FOLLOW_UP'
                        AND c.status IN ('REQUESTED', 'SCHEDULED', 'IN_PROGRESS')) $q$),
('Open urgent case on a horse that is not BLOCKED',
 $q$ SELECT c.id FROM care_schedule c JOIN horses h ON h.id = c.horse_id
     WHERE c.care_type = 'URGENT' AND c.status IN ('REQUESTED', 'SCHEDULED', 'IN_PROGRESS') AND h.training_decision <> 'BLOCKED' $q$),
('Incident status does not match its urgent case',
 $q$ SELECT i.id FROM horse_incident_reports i JOIN care_schedule c ON c.id = i.care_schedule_id
     WHERE (i.status IN ('REPORTED', 'IN_REVIEW') AND c.status NOT IN ('REQUESTED', 'SCHEDULED', 'IN_PROGRESS'))
        OR (i.status = 'RESOLVED' AND (c.status <> 'COMPLETED' OR i.handled_by_id IS DISTINCT FROM c.veterinarian_id))
        OR i.care_schedule_id IS NULL $q$),
('PAUSED schedule without a paused_from timestamp',
 $q$ SELECT id FROM care_schedule WHERE status = 'PAUSED' AND paused_from IS NULL $q$),
('Cancelled schedule without a reason',
 $q$ SELECT id FROM care_schedule WHERE status = 'CANCELLED' AND (cancel_reason IS NULL OR btrim(cancel_reason) = '') $q$),
('Health metric hydration status outside the HydrationStatus enum',
 $q$ SELECT id FROM horse_health_metrics
     WHERE hydration_status NOT IN ('NORMAL', 'MILD_DEHYDRATION', 'MODERATE_DEHYDRATION', 'SEVERE_DEHYDRATION', 'ABNORMAL_CRT') $q$),
('Admitted or candidate horse without a pending vaccination milestone',
 $q$ SELECT h.id FROM horses h WHERE h.current_status = 'ELIGIBLE'
       AND NOT EXISTS (SELECT 1 FROM care_schedule c WHERE c.horse_id = h.id AND c.purpose = 'PREVENTIVE'
                        AND c.status IN ('REQUESTED', 'SCHEDULED', 'IN_PROGRESS')) $q$),
('Vaccination milestone without the list of due protocols',
 $q$ SELECT id FROM care_schedule WHERE purpose = 'PREVENTIVE' AND (description IS NULL OR description NOT LIKE 'Preventive care: _%') $q$),
('Vital reading outside what a living horse can show',
 $q$ SELECT id FROM horse_health_metrics
     WHERE temperature NOT BETWEEN 35.5 AND 41.5 OR heart_rate NOT BETWEEN 24 AND 90 OR respiratory_rate NOT BETWEEN 8 AND 60
        OR body_condition_score NOT BETWEEN 1 AND 9 OR weight NOT BETWEEN 250 AND 800 OR recorded_at > pg_temp.clock() $q$),
('INITIAL or ROUTINE-FOLLOW_UP completed exam missing full 6-field vitals',
 $q$ SELECT c.id FROM care_schedule c
     JOIN health_records h ON h.care_schedule_id = c.id
     WHERE c.status = 'COMPLETED'
       AND (c.care_type = 'INITIAL' OR (c.care_type = 'ROUTINE' AND c.purpose = 'FOLLOW_UP'))
       AND NOT EXISTS (
         SELECT 1 FROM horse_health_metrics m
         WHERE m.health_record_id = h.id
           AND m.heart_rate IS NOT NULL AND m.temperature IS NOT NULL AND m.weight IS NOT NULL
           AND m.respiratory_rate IS NOT NULL AND m.hydration_status IS NOT NULL AND m.body_condition_score IS NOT NULL
       ) $q$),
('Prescription or treatment plan ending before it starts',
 $q$ SELECT id FROM prescriptions WHERE end_date < start_date UNION ALL SELECT id FROM treatment_plans WHERE end_date < start_date $q$),
-- ---------------- Huấn luyện (Training)
('Group member outside the trainer areas, resting, or not admitted',
 $q$ SELECT m.id FROM training_group_members m JOIN training_groups g ON g.id = m.group_id JOIN horses h ON h.id = m.horse_id
     JOIN stable_stalls s ON s.id = h.current_stall_id JOIN areas a ON a.id = s.area_id
     WHERE (a.trainer_id <> g.trainer_id OR h.training_decision = 'BLOCKED' OR h.current_status <> 'ELIGIBLE') AND m.left_at IS NULL $q$),
('Group member groom is not the stall groom',
 $q$ SELECT m.id FROM training_group_members m JOIN horses h ON h.id = m.horse_id JOIN stable_stalls s ON s.id = h.current_stall_id
     WHERE m.groom_id IS DISTINCT FROM s.groom_id AND m.left_at IS NULL $q$),
('Group with more than 6 horses or a groom twice (BR-10, BR-09)',
 $q$ SELECT group_id FROM training_group_members WHERE left_at IS NULL GROUP BY group_id HAVING count(*) > 6
     UNION ALL SELECT group_id FROM training_group_members WHERE groom_id IS NOT NULL AND left_at IS NULL GROUP BY group_id, groom_id HAVING count(*) > 1 $q$),
('Plan does not have exactly total_sessions sessions',
 $q$ SELECT p.id FROM group_plans p JOIN courses c ON c.id = p.course_id
     WHERE (SELECT count(*) FROM training_sessions s WHERE s.group_plan_id = p.id) <> c.total_sessions $q$),
('Plan status does not match its sessions (closes when no session is left to run)',
 $q$ SELECT p.id FROM group_plans p
     WHERE (p.status = 'COMPLETED' AND (EXISTS (SELECT 1 FROM training_sessions s WHERE s.group_plan_id = p.id AND s.status = 'SCHEDULED')
                                         OR NOT EXISTS (SELECT 1 FROM training_sessions s WHERE s.group_plan_id = p.id AND s.status = 'HELD')))
        OR (p.status IN ('ACTIVE', 'UPCOMING') AND NOT EXISTS (SELECT 1 FROM training_sessions s WHERE s.group_plan_id = p.id AND s.status = 'SCHEDULED'))
        OR (p.status = 'UPCOMING' AND p.start_date <= pg_temp.today())
        OR (p.status = 'ACTIVE' AND p.start_date > pg_temp.today()) $q$),
('Session on a weekday the plan does not train',
 $q$ SELECT s.id FROM training_sessions s JOIN group_plans p ON p.id = s.group_plan_id
     WHERE trim(to_char(s.session_date, 'DAY')) <> ALL (string_to_array(p.training_days, ',')) OR s.session_date NOT BETWEEN p.start_date AND p.end_date $q$),
('Session status does not match the clock',
 $q$ SELECT s.id FROM training_sessions s
     WHERE (s.status = 'SCHEDULED' AND s.session_date + s.end_time <= pg_temp.clock())
        OR (s.status = 'HELD' AND s.session_date + s.end_time > pg_temp.clock()) $q$),
('Workout status does not match its session',
 $q$ SELECT w.id FROM training_workouts w JOIN training_sessions s ON s.id = w.session_id
     WHERE (s.status = 'CANCELLED' AND w.status <> 'CANCELLED')
        OR (s.status = 'SCHEDULED' AND w.status IN ('HELD', 'RAN'))
        OR (s.status = 'HELD' AND w.status = 'SCHEDULED') $q$),
('Scheduled session with no effective workout',
 $q$ SELECT s.id FROM training_sessions s
     WHERE s.status = 'SCHEDULED' AND NOT EXISTS (SELECT 1 FROM training_workouts w WHERE w.session_id = s.id AND w.status <> 'CANCELLED') $q$),
('Workout groom is not the group member groom',
 $q$ SELECT w.id FROM training_workouts w JOIN training_sessions s ON s.id = w.session_id JOIN group_plans p ON p.id = s.group_plan_id
     JOIN training_group_members m ON m.group_id = p.group_id AND m.horse_id = w.horse_id
     WHERE w.assigned_to_id IS DISTINCT FROM m.groom_id $q$),
('Horse trained or booked while resting',
 $q$ SELECT w.id FROM training_workouts w JOIN training_sessions s ON s.id = w.session_id
     WHERE w.status <> 'CANCELLED' AND pg_temp.blocked_at(w.horse_id, s.session_date + s.start_time) $q$),
('BLOCKED horse in a group or with a Workout still to come',
 $q$ SELECT h.id FROM horses h WHERE h.training_decision = 'BLOCKED'
       AND (EXISTS (SELECT 1 FROM training_group_members m WHERE m.horse_id = h.id AND m.left_at IS NULL)
            OR EXISTS (SELECT 1 FROM training_workouts w JOIN training_sessions s ON s.id = w.session_id
                        WHERE w.horse_id = h.id AND w.status = 'SCHEDULED' AND s.session_date + s.start_time > pg_temp.clock())) $q$),
('Recorded rating differs from the app formula',
 $q$ SELECT id FROM training_workouts WHERE status = 'RAN'
       AND performance_rating <> pg_temp.rating(actual_distance_meters, actual_duration_minutes, top_speed_kmh, average_speed_kmh,
                                                average_heart_rate, max_heart_rate, recovery_heart_rate) $q$),
('Result recorded before the session ended or in the future',
 $q$ SELECT w.id FROM training_workouts w JOIN training_sessions s ON s.id = w.session_id
     WHERE w.status = 'RAN'
       AND (w.updated_at < s.session_date + s.end_time OR w.updated_at > pg_temp.clock()) $q$),
('Unfinished plans of one group overlap',
 $q$ SELECT a.id FROM group_plans a JOIN group_plans b ON b.group_id = a.group_id AND b.id < a.id
     WHERE a.status IN ('UPCOMING', 'ACTIVE') AND b.status IN ('UPCOMING', 'ACTIVE')
       AND a.start_date <= b.end_date AND b.start_date <= a.end_date $q$),
('Disbanded group with members or an unfinished plan',
 $q$ SELECT g.id FROM training_groups g WHERE g.disbanded_at IS NOT NULL
       AND (EXISTS (SELECT 1 FROM training_group_members m WHERE m.group_id = g.id AND m.left_at IS NULL)
            OR EXISTS (SELECT 1 FROM group_plans p WHERE p.group_id = g.id AND p.status IN ('UPCOMING', 'ACTIVE'))) $q$),
('Disbanded group dated before it was created or in the future',
 $q$ SELECT id FROM training_groups WHERE disbanded_at IS NOT NULL AND (disbanded_at < created_at OR disbanded_at > pg_temp.clock()) $q$),
('Plan or session of a disbanded group dated after the group was disbanded and not cancelled',
 $q$ SELECT p.id FROM group_plans p JOIN training_groups g ON g.id = p.group_id
     WHERE g.disbanded_at IS NOT NULL AND (p.created_at > g.disbanded_at
        OR EXISTS (SELECT 1 FROM training_sessions s WHERE s.group_plan_id = p.id AND s.status <> 'CANCELLED' AND s.session_date + s.start_time > g.disbanded_at)) $q$),
('Two live groups of one trainer with the same name',
 $q$ SELECT min(id) FROM training_groups WHERE disbanded_at IS NULL GROUP BY trainer_id, name HAVING count(*) > 1 $q$),
('Course uses a subject of another trainer',
 $q$ SELECT cs.id FROM course_subjects cs JOIN courses c ON c.id = cs.course_id JOIN subjects s ON s.id = cs.subject_id
     WHERE s.trainer_id <> c.trainer_id $q$),
-- ---------------- Groom
('Routine task not given to the groom of the stall the horse was in',
 $q$ SELECT t.id FROM groom_daily_tasks t
     WHERE NOT EXISTS (SELECT 1 FROM seed_stay st JOIN stable_stalls s ON s.id = st.stall_id
                        WHERE st.horse_id = t.horse_id AND s.groom_id = t.groom_id
                          AND st.from_ts <= t.scheduled_time AND (st.to_ts IS NULL OR st.to_ts > t.scheduled_time)) $q$),
('Task completed without a completion row of the same groom, or completed before its time',
 $q$ SELECT t.id FROM groom_daily_tasks t LEFT JOIN groom_task_completions c ON c.id = t.completion_id
     WHERE t.is_completed AND (c.id IS NULL OR c.groom_id <> t.groom_id OR t.completed_at < t.scheduled_time) $q$),
('Trainer assessment score that is not a whole number from 0 to 10',
 $q$ SELECT id FROM racing_readiness_assessments
     WHERE conformation_score <> trunc(conformation_score) OR temperament_score <> trunc(temperament_score)
        OR gait_quality_score <> trunc(gait_quality_score)
        OR conformation_score NOT BETWEEN 0 AND 10 OR temperament_score NOT BETWEEN 0 AND 10
        OR gait_quality_score NOT BETWEEN 0 AND 10 $q$),
('Course with more subjects than sessions',
 $q$ SELECT c.id FROM courses c WHERE (SELECT count(*) FROM course_subjects cs WHERE cs.course_id = c.id) > c.total_sessions $q$),
('Two courses of one trainer with the same subjects in the same order',
 $q$ SELECT a.id FROM courses a JOIN courses b ON b.trainer_id = a.trainer_id AND b.id > a.id
     WHERE (SELECT array_agg(subject_id ORDER BY order_index) FROM course_subjects WHERE course_id = a.id)
         = (SELECT array_agg(subject_id ORDER BY order_index) FROM course_subjects WHERE course_id = b.id) $q$),
('Admission document whose file key is not a Cloudinary asset',
 $q$ SELECT id FROM admission_documents
     WHERE file_url !~ '^https://res\.cloudinary\.com/[^/]+/image/authenticated/v[0-9]+/rtms/admissions/[0-9]+/[0-9a-f-]{36}\.(pdf|jpg|png|webp)$' $q$),
('Completion photo key that is not a stored file',
 $q$ SELECT p.completion_id FROM groom_task_completion_photos p
     WHERE NOT EXISTS (SELECT 1 FROM admission_documents d WHERE d.file_url = p.image_key) $q$),
-- ---------------- Thi đấu (Racing)
('Nomination for a horse outside the trainer areas, or out of date order',
 $q$ SELECT r.id FROM race_registrations r JOIN horses h ON h.id = r.horse_id JOIN stable_stalls s ON s.id = h.current_stall_id
     JOIN areas a ON a.id = s.area_id
     WHERE a.trainer_id <> r.trainer_id OR r.event_date < r.created_at::DATE OR r.nomination_deadline > r.event_date
        OR (r.status = 'PENDING') <> (r.reviewed_at IS NULL) $q$),
-- ---------------- Dược lâm sàng (Track 0: catalog + đơn thuốc + severity D4)
('Treatment plan instructions stored as JSON',
 $q$ SELECT id FROM treatment_plans WHERE BTRIM(instructions) LIKE '[%' $q$),
('Health condition severity outside the MILD/MODERATE/SEVERE/CRITICAL vocabulary',
 $q$ SELECT id FROM health_conditions
     WHERE UPPER(severity) NOT IN ('MILD', 'MODERATE', 'SEVERE', 'CRITICAL') $q$),
('Prescription with invalid status or administered_by_role',
 $q$ SELECT id FROM prescriptions
     WHERE status NOT IN ('ACTIVE', 'COMPLETED', 'DISCONTINUED')
        OR administered_by_role NOT IN ('GROOM', 'VETERINARIAN') $q$),
('Injection route in a treatment-plan prescription (vaccination excluded)',
 $q$ SELECT id FROM prescriptions
     WHERE preventive_protocol_id IS NULL
       AND (
           UPPER(BTRIM(COALESCE(route, ''))) IN ('IV', 'IM', 'SC', 'SQ', 'INJECTION_IV', 'INJECTION_IM')
           OR UPPER(BTRIM(COALESCE(route, ''))) LIKE '%INTRAVENOUS%'
           OR UPPER(BTRIM(COALESCE(route, ''))) LIKE '%INTRAMUSCULAR%'
           OR UPPER(BTRIM(COALESCE(route, ''))) LIKE '%SUBCUTANEOUS%'
       ) $q$);

DO $$
DECLARE
    c RECORD;
    n BIGINT;
    v_failed TEXT := '';
BEGIN
    FOR c IN SELECT * FROM seed_check LOOP
        EXECUTE 'SELECT count(*) FROM (' || c.query || ') x' INTO n;
        IF n > 0 THEN v_failed := v_failed || E'\n  - ' || c.name || ': ' || n; END IF;
    END LOOP;
    IF v_failed <> '' THEN
        RAISE EXCEPTION 'Seed data breaks business rules (nothing was saved):%', v_failed;
    END IF;
    RAISE NOTICE 'All % business-rule checks passed.', (SELECT count(*) FROM seed_check);
END $$;
