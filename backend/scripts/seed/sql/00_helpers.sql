-- =====================================================================
-- 00_helpers.sql — hàm dùng chung cho mọi phần seed (sống trong pg_temp, tự mất khi đóng kết nối).
-- (Shared functions for every seed part. They live in pg_temp and disappear when the connection closes.)
--
-- Các hàm ở đây làm lại đúng việc các service của backend làm (CareScheduleService, PreventiveStatusService,
-- TrainingDecisionService, NotificationService...), để dữ liệu seed giống hệt dữ liệu do app tạo ra.
-- Khi đổi luật trong service, sửa hàm tương ứng ở đây.
-- (These functions redo what the backend services do, so seeded rows look exactly like rows the app creates.
-- When a service rule changes, change the matching function here.)
-- =====================================================================

-- ---------------------------------------------------------------------
-- Thời gian (Time)
-- ---------------------------------------------------------------------
-- Ngày gốc của dữ liệu trong 10_data.sql. (Reference day the data in 10_data.sql is written for.)
CREATE FUNCTION pg_temp.ref_day() RETURNS DATE AS $$ SELECT DATE '2026-10-25' $$ LANGUAGE sql IMMUTABLE;

-- Giờ hiện tại của câu lạc bộ (Asia/Ho_Chi_Minh), làm tròn phút. Cố định trong cả lần chạy (một transaction).
-- Chỉ để thử seed ở ngày / giờ khác: đặt rtms.seed_now (seed-database.ps1 -AsOf '2026-11-02 08:00').
-- (Club time now, rounded to the minute, constant for the whole run. For testing other days / hours only:
-- set rtms.seed_now, e.g. seed-database.ps1 -AsOf '2026-11-02 08:00'.)
CREATE FUNCTION pg_temp.clock() RETURNS TIMESTAMP AS $$
    SELECT date_trunc('minute', COALESCE(NULLIF(current_setting('rtms.seed_now', TRUE), '')::TIMESTAMP,
                                         now() AT TIME ZONE 'Asia/Ho_Chi_Minh'))::TIMESTAMP
$$ LANGUAGE sql STABLE;

CREATE FUNCTION pg_temp.today() RETURNS DATE AS $$ SELECT pg_temp.clock()::DATE $$ LANGUAGE sql STABLE;

-- Dời một ngày / mốc giờ viết theo ngày gốc sang lịch thật. (Shift a reference date / time to the real calendar.)
CREATE FUNCTION pg_temp.d(p DATE) RETURNS DATE AS $$
    SELECT p + (pg_temp.today() - pg_temp.ref_day())
$$ LANGUAGE sql STABLE;

CREATE FUNCTION pg_temp.ts(p TIMESTAMP) RETURNS TIMESTAMP AS $$
    SELECT p + make_interval(days => pg_temp.today() - pg_temp.ref_day())
$$ LANGUAGE sql STABLE;

-- Ngày tương đối với hôm nay + giờ. (A day relative to today at a time of day.)
CREATE FUNCTION pg_temp.at(p_days INT, p_time TIME) RETURNS TIMESTAMP AS $$
    SELECT (pg_temp.today() + p_days) + p_time
$$ LANGUAGE sql STABLE;

-- Số ngẫu nhiên tất định từ một chuỗi: cùng chuỗi luôn cho cùng số, nên seed chạy lại cho cùng dữ liệu.
-- (Deterministic number from a text: the same text always gives the same number, so reruns give the same data.)
CREATE FUNCTION pg_temp.rnd(p_seed TEXT, p_lo NUMERIC, p_hi NUMERIC) RETURNS NUMERIC AS $$
    SELECT p_lo + (('x' || substr(md5(p_seed), 1, 8))::BIT(32)::BIGINT % 100001) / 100000.0 * (p_hi - p_lo)
$$ LANGUAGE sql IMMUTABLE;

CREATE FUNCTION pg_temp.rndi(p_seed TEXT, p_lo INT, p_hi INT) RETURNS INT AS $$
    SELECT p_lo + (('x' || substr(md5(p_seed), 1, 8))::BIT(32)::BIGINT % (p_hi - p_lo + 1))::INT
$$ LANGUAGE sql IMMUTABLE;

-- ---------------------------------------------------------------------
-- Tra khóa nghiệp vụ -> id (Business key -> id). Không dùng id cứng ở bất kỳ đâu.
-- ---------------------------------------------------------------------
CREATE TEMP TABLE seed_user (k TEXT PRIMARY KEY, id BIGINT NOT NULL);
CREATE TEMP TABLE seed_horse_id (k TEXT PRIMARY KEY, id BIGINT NOT NULL);
CREATE TEMP TABLE seed_admission_id (k TEXT PRIMARY KEY, id BIGINT NOT NULL);

CREATE FUNCTION pg_temp.uid(p_k TEXT) RETURNS BIGINT AS $$
DECLARE v BIGINT;
BEGIN
    SELECT id INTO v FROM seed_user WHERE k = p_k;
    IF v IS NULL THEN RAISE EXCEPTION 'Unknown account key %', p_k; END IF;
    RETURN v;
END $$ LANGUAGE plpgsql STABLE;

CREATE FUNCTION pg_temp.hid(p_k TEXT) RETURNS BIGINT AS $$
DECLARE v BIGINT;
BEGIN
    SELECT id INTO v FROM seed_horse_id WHERE k = p_k;
    IF v IS NULL THEN RAISE EXCEPTION 'Unknown horse key % (not created yet?)', p_k; END IF;
    RETURN v;
END $$ LANGUAGE plpgsql STABLE;

CREATE FUNCTION pg_temp.aid(p_k TEXT) RETURNS BIGINT AS $$
DECLARE v BIGINT;
BEGIN
    SELECT id INTO v FROM seed_admission_id WHERE k = p_k;
    IF v IS NULL THEN RAISE EXCEPTION 'Unknown admission key %', p_k; END IF;
    RETURN v;
END $$ LANGUAGE plpgsql STABLE;

CREATE FUNCTION pg_temp.stall(p_code TEXT) RETURNS BIGINT AS $$
DECLARE v BIGINT;
BEGIN
    SELECT id INTO v FROM stable_stalls WHERE stall_code = p_code;
    IF v IS NULL THEN RAISE EXCEPTION 'Unknown stall %', p_code; END IF;
    RETURN v;
END $$ LANGUAGE plpgsql STABLE;

CREATE FUNCTION pg_temp.protocol(p_code TEXT) RETURNS BIGINT AS $$
DECLARE v BIGINT;
BEGIN
    SELECT id INTO v FROM preventive_protocols WHERE code = p_code;
    IF v IS NULL THEN RAISE EXCEPTION 'Unknown preventive protocol % (migration data missing?)', p_code; END IF;
    RETURN v;
END $$ LANGUAGE plpgsql STABLE;

-- Khóa file của một hồ sơ: "local:<uuid>.<đuôi>", uuid = md5 của đường dẫn trong bộ hồ sơ. seed-admission-files.ps1
-- đặt tên file đúng theo công thức này. (Storage key of a document; seed-admission-files.ps1 names the file the same way.)
CREATE FUNCTION pg_temp.file_key(p_path TEXT) RETURNS TEXT AS $$
    SELECT 'local:' || md5('rtms-seed/' || p_path)::UUID::TEXT || lower(substring(p_path FROM '\.[A-Za-z0-9]+$'))
$$ LANGUAGE sql IMMUTABLE;

-- Ngày giờ theo kiểu LocalDateTime.toString() của Java (bỏ giây khi bằng 0), dùng cho request_fingerprint.
-- (Date-time as Java LocalDateTime.toString(), used by request_fingerprint.)
CREATE FUNCTION pg_temp.java_ts(p TIMESTAMP) RETURNS TEXT AS $$
    SELECT CASE WHEN extract(SECOND FROM p) = 0 THEN to_char(p, 'YYYY-MM-DD"T"HH24:MI')
                ELSE to_char(p, 'YYYY-MM-DD"T"HH24:MI:SS') END
$$ LANGUAGE sql IMMUTABLE;

-- Giống CareScheduleService.fingerprint. (Same as CareScheduleService.fingerprint.)
CREATE FUNCTION pg_temp.fingerprint(p_horse BIGINT, p_admission BIGINT, p_at TIMESTAMP, p_desc TEXT) RETURNS TEXT AS $$
    SELECT encode(sha256(convert_to(p_horse || '|' || COALESCE(p_admission::TEXT, 'null') || '|ROUTINE|'
                                    || pg_temp.java_ts(p_at) || '|' || p_desc, 'UTF8')), 'hex')
$$ LANGUAGE sql IMMUTABLE;

-- ---------------------------------------------------------------------
-- Thông báo và nhật ký (Notifications and audit log)
-- ---------------------------------------------------------------------
-- Giống NotificationService.sendAssignmentNotification: khóa chống trùng "<người nhận>:<loại tham chiếu>_<id>:<loại>".
-- Thông báo cũ hơn 2 ngày coi như đã đọc. (Same dedup key as the app. Notifications older than 2 days are read.)
CREATE FUNCTION pg_temp.notify(p_to BIGINT, p_ref_type TEXT, p_ref_id BIGINT, p_type TEXT, p_title TEXT,
                               p_message TEXT, p_at TIMESTAMP) RETURNS VOID AS $$
    INSERT INTO notifications (recipient_id, title, message, notification_type, reference_type, reference_id,
                               deduplication_key, is_read, created_at)
    VALUES (p_to, p_title, p_message, p_type, p_ref_type, p_ref_id,
            format('%s:%s_%s:%s', p_to, p_ref_type, p_ref_id, p_type), p_at < pg_temp.clock() - INTERVAL '2 days', p_at)
    ON CONFLICT (deduplication_key) DO NOTHING
$$ LANGUAGE sql;

-- Một dòng nhật ký như AuditLogService ghi cho request ghi dữ liệu thành công.
-- (One audit row as AuditLogService writes for a successful write request.)
CREATE FUNCTION pg_temp.audit(p_user BIGINT, p_action TEXT, p_entity TEXT, p_entity_id BIGINT, p_details TEXT,
                              p_at TIMESTAMP, p_method TEXT, p_path TEXT) RETURNS VOID AS $$
    INSERT INTO audit_logs (user_id, action, entity_name, entity_id, created_at, http_method, request_path, status_code, details)
    VALUES (p_user, p_action, p_entity, p_entity_id, p_at, p_method, p_path, CASE WHEN p_method IS NULL THEN NULL ELSE 200 END, p_details)
$$ LANGUAGE sql;

-- ---------------------------------------------------------------------
-- Quyết định tập luyện (Training decision) — giống TrainingDecisionService.
-- Mỗi lần chặn mở một "khoảng nghỉ" trong seed_block; phần training (50_training.sql) không xếp buổi tập nào rơi
-- vào khoảng nghỉ, và hủy plan đang chạy lúc bị chặn, đúng như cancelFutureTrainingForHorse.
-- (Each block opens a rest window in seed_block; the training part never places a session inside a window and
-- cancels the plan running when the block starts, exactly like cancelFutureTrainingForHorse.)
-- ---------------------------------------------------------------------
CREATE TEMP TABLE seed_block (horse_id BIGINT NOT NULL, from_ts TIMESTAMP NOT NULL, to_ts TIMESTAMP, reason TEXT);

CREATE FUNCTION pg_temp.block(p_horse BIGINT, p_reason TEXT, p_at TIMESTAMP) RETURNS VOID AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM seed_block WHERE horse_id = p_horse AND to_ts IS NULL) THEN
        INSERT INTO seed_block (horse_id, from_ts, reason) VALUES (p_horse, p_at, p_reason);
    END IF;
    UPDATE horses SET training_decision = 'BLOCKED', training_decision_reason = p_reason, updated_at = p_at
     WHERE id = p_horse;
END $$ LANGUAGE plpgsql;

-- Phần seed không chạy đúng thứ tự thời gian giữa các file (ca Thú y ở 40, tiêm phòng ở 45), nên một lần khám cũ hơn
-- không được mở khóa một lần chặn mới hơn đã ghi trước đó. (Files do not run in strict time order between each other,
-- so an older visit must not lift a newer block written earlier.)
CREATE FUNCTION pg_temp.allow(p_horse BIGINT, p_at TIMESTAMP) RETURNS VOID AS $$
BEGIN
    UPDATE seed_block SET to_ts = p_at WHERE horse_id = p_horse AND to_ts IS NULL AND from_ts <= p_at;
    IF NOT EXISTS (SELECT 1 FROM seed_block WHERE horse_id = p_horse AND from_ts > p_at) THEN
        UPDATE horses SET training_decision = 'ALLOWED', training_decision_reason = NULL, updated_at = p_at
         WHERE id = p_horse;
    END IF;
END $$ LANGUAGE plpgsql;

-- Ngựa có đang trong khoảng nghỉ tại thời điểm này không. (Is the horse inside a rest window at this time?)
CREATE FUNCTION pg_temp.blocked_at(p_horse BIGINT, p_at TIMESTAMP) RETURNS BOOLEAN AS $$
    SELECT EXISTS (SELECT 1 FROM seed_block b
                   WHERE b.horse_id = p_horse AND b.from_ts <= p_at AND (b.to_ts IS NULL OR b.to_ts > p_at))
$$ LANGUAGE sql STABLE;

-- ---------------------------------------------------------------------
-- Lịch Thú y (Veterinary schedule) — giống CareScheduleService.
-- ---------------------------------------------------------------------
CREATE FUNCTION pg_temp.vet_busy(p_vet BIGINT, p_start TIMESTAMP, p_minutes INT, p_except BIGINT) RETURNS BOOLEAN AS $$
    SELECT EXISTS (SELECT 1 FROM care_schedule c
                   WHERE c.veterinarian_id = p_vet AND c.id IS DISTINCT FROM p_except
                     AND c.status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED') AND c.scheduled_at IS NOT NULL
                     AND c.scheduled_at < p_start + make_interval(mins => p_minutes)
                     AND c.scheduled_at + make_interval(mins => c.duration_minutes) > p_start)
$$ LANGUAGE sql STABLE;

CREATE FUNCTION pg_temp.horse_busy(p_horse BIGINT, p_start TIMESTAMP, p_minutes INT, p_except BIGINT) RETURNS BOOLEAN AS $$
    SELECT EXISTS (SELECT 1 FROM care_schedule c
                   WHERE c.horse_id = p_horse AND c.id IS DISTINCT FROM p_except
                     AND c.status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED') AND c.scheduled_at IS NOT NULL
                     AND c.scheduled_at < p_start + make_interval(mins => p_minutes)
                     AND c.scheduled_at + make_interval(mins => c.duration_minutes) > p_start)
$$ LANGUAGE sql STABLE;

-- Khe trống đầu tiên từ p_earliest: lịch thường chỉ trong khung Thú y 13:30-15:30 (4 khe 30 phút); khám nhập trại
-- ưu tiên khung đó rồi tới 08:00-17:30. Ưu tiên Thú y p_vet, sau đó tới các Thú y đang làm việc khác.
-- (First free slot from p_earliest: routine care only in the 13:30-15:30 vet window; intake exams prefer it, then
-- 08:00-17:30. Prefers vet p_vet, then the other active vets.)
CREATE FUNCTION pg_temp.find_slot(p_horse BIGINT, p_vet BIGINT, p_earliest TIMESTAMP, p_care_type TEXT,
                                  p_except BIGINT DEFAULT NULL, OUT vet_id BIGINT, OUT slot TIMESTAMP) AS $$
DECLARE
    v_day DATE;
    v_time TIME;
    v_times TIME[];
    v_vet BIGINT;
    v_vets BIGINT[];
BEGIN
    v_times := ARRAY[TIME '13:30', TIME '14:00', TIME '14:30', TIME '15:00'];
    IF p_care_type = 'INITIAL' THEN
        v_times := v_times || ARRAY(SELECT t::TIME FROM generate_series(TIMESTAMP '2000-01-01 08:00',
                                                                         TIMESTAMP '2000-01-01 17:30', INTERVAL '30 minutes') t
                                    WHERE t::TIME NOT IN (TIME '13:30', TIME '14:00', TIME '14:30', TIME '15:00'));
    END IF;
    SELECT ARRAY_AGG(u.id ORDER BY (u.id = p_vet) DESC, u.id) INTO v_vets
      FROM users u JOIN roles r ON r.id = u.role_id
     WHERE r.name = 'VETERINARIAN' AND u.is_active;
    FOR i IN 0..60 LOOP
        v_day := p_earliest::DATE + i;
        FOREACH v_time IN ARRAY v_times LOOP
            CONTINUE WHEN v_day + v_time < p_earliest;
            CONTINUE WHEN pg_temp.horse_busy(p_horse, v_day + v_time, 30, p_except);
            FOREACH v_vet IN ARRAY v_vets LOOP
                IF NOT pg_temp.vet_busy(v_vet, v_day + v_time, 30, p_except) THEN
                    vet_id := v_vet; slot := v_day + v_time; RETURN;
                END IF;
            END LOOP;
        END LOOP;
    END LOOP;
    RAISE EXCEPTION 'No free vet slot for horse % from %', p_horse, p_earliest;
END $$ LANGUAGE plpgsql;

CREATE FUNCTION pg_temp.new_schedule(p_horse BIGINT, p_care_type TEXT, p_purpose TEXT, p_desc TEXT,
                                     p_requested_at TIMESTAMP, p_created_at TIMESTAMP,
                                     p_admission BIGINT DEFAULT NULL, p_incident BIGINT DEFAULT NULL,
                                     p_source BIGINT DEFAULT NULL, p_requested_by BIGINT DEFAULT NULL,
                                     p_key TEXT DEFAULT NULL, p_fingerprint TEXT DEFAULT NULL) RETURNS BIGINT AS $$
    INSERT INTO care_schedule (horse_id, admission_id, source_incident_id, care_type, status, duration_minutes, description,
                               created_at, updated_at, requested_at, source_schedule_id, requested_by_id, idempotency_key,
                               request_fingerprint, purpose)
    VALUES (p_horse, p_admission, p_incident, p_care_type, 'REQUESTED', 30, p_desc, p_created_at, p_created_at,
            p_requested_at, p_source, p_requested_by, p_key, p_fingerprint, p_purpose)
    RETURNING id
$$ LANGUAGE sql;

-- Giao lịch cho Thú y lúc p_at (giống assignRequestedSchedule): đặt giờ, SCHEDULED, thông báo, nhật ký.
-- (Assign the schedule to a vet at p_at, like assignRequestedSchedule: slot, SCHEDULED, notification, audit.)
CREATE FUNCTION pg_temp.assign(p_schedule BIGINT, p_vet BIGINT, p_slot TIMESTAMP, p_at TIMESTAMP) RETURNS VOID AS $$
DECLARE
    c care_schedule;
    v_horse TEXT;
    v_type TEXT; v_title TEXT; v_message TEXT;
BEGIN
    UPDATE care_schedule SET veterinarian_id = p_vet, scheduled_at = p_slot, status = 'SCHEDULED', updated_at = p_at
     WHERE id = p_schedule RETURNING * INTO c;
    SELECT name INTO v_horse FROM horses WHERE id = c.horse_id;
    IF c.care_type = 'INITIAL' AND c.admission_id IS NOT NULL THEN
        UPDATE admission_applications SET veterinarian_id = p_vet WHERE id = c.admission_id;
    END IF;
    IF c.care_type = 'URGENT' THEN
        v_type := 'CARE_SCHEDULE_URGENT_ASSIGNED'; v_title := 'Urgent case assigned';
        v_message := 'You have been assigned to an urgent care case for horse ' || v_horse
                     || '. Training is blocked until the examination is complete.';
    ELSIF c.care_type = 'ROUTINE' THEN
        v_type := 'ROUTINE_ASSIGNED'; v_title := 'Care schedule assigned';
        v_message := 'You have been assigned to a routine care visit for horse ' || v_horse || ' on care schedule #' || c.id || '.';
    ELSE
        v_type := 'ADMISSION_VET_ASSIGNED'; v_title := 'New horse assignment';
        v_message := 'You have been assigned to candidate horse ' || v_horse || ' for care schedule #' || c.id || '.';
    END IF;
    PERFORM pg_temp.notify(p_vet, 'CARE_SCHEDULE', c.id, v_type, v_title, v_message, p_at);
    PERFORM pg_temp.audit(p_vet, 'Care Schedule Auto Assigned', 'CareSchedule', c.id,
                          'Care schedule #' || c.id || ' (' || c.care_type || ') was automatically assigned to veterinarian '
                          || (SELECT full_name FROM users WHERE id = p_vet), p_at, NULL, NULL);
END $$ LANGUAGE plpgsql;

-- Giao vào khe trống đầu tiên. Lịch thường chỉ được xếp khi còn trong cửa sổ 14 ngày (ASSIGNMENT_HORIZON_DAYS), nên
-- lúc giao = max(lúc tạo, ngày hẹn - 14 ngày). (Assign to the first free slot. Routine care is only assigned inside the
-- 14-day horizon, so the assignment time is max(created, requested - 14 days).)
CREATE FUNCTION pg_temp.book(p_schedule BIGINT, p_vet BIGINT, p_not_before TIMESTAMP DEFAULT NULL) RETURNS TIMESTAMP AS $$
DECLARE
    c care_schedule;
    v_assign_at TIMESTAMP;
    s RECORD;
BEGIN
    SELECT * INTO c FROM care_schedule WHERE id = p_schedule;
    v_assign_at := CASE WHEN c.care_type IN ('URGENT', 'INITIAL') OR c.requested_at IS NULL THEN c.created_at
                        ELSE GREATEST(c.created_at, c.requested_at - INTERVAL '14 days') END;
    s := pg_temp.find_slot(c.horse_id, p_vet,
                           GREATEST(COALESCE(c.requested_at, c.created_at), v_assign_at, COALESCE(p_not_before, v_assign_at)),
                           c.care_type, c.id);
    PERFORM pg_temp.assign(c.id, s.vet_id, s.slot, v_assign_at);
    RETURN s.slot;
END $$ LANGUAGE plpgsql;

CREATE FUNCTION pg_temp.start_care(p_schedule BIGINT) RETURNS VOID AS $$
    UPDATE care_schedule SET status = 'IN_PROGRESS', updated_at = scheduled_at WHERE id = p_schedule
$$ LANGUAGE sql;

-- Quản lý hủy một lịch đã xếp (chỉ SCHEDULED, kèm lý do). (The manager cancels a booked schedule, with a reason.)
CREATE FUNCTION pg_temp.cancel_care(p_schedule BIGINT, p_reason TEXT, p_at TIMESTAMP) RETURNS VOID AS $$
BEGIN
    IF (SELECT status FROM care_schedule WHERE id = p_schedule) <> 'SCHEDULED' THEN
        RAISE EXCEPTION 'Only SCHEDULED care schedules can be cancelled (schedule %)', p_schedule;
    END IF;
    UPDATE care_schedule SET status = 'CANCELLED', cancel_reason = p_reason, updated_at = p_at WHERE id = p_schedule;
END $$ LANGUAGE plpgsql;

-- Lịch tái khám sinh ra khi hoàn tất một ca (createNextSchedule): ROUTINE, một-một với ca nguồn qua source_schedule_id.
-- (Follow-up created by completing a visit: ROUTINE, one-to-one with its source through source_schedule_id.)
CREATE FUNCTION pg_temp.follow_up(p_source BIGINT, p_at TIMESTAMP, p_desc TEXT, p_created_at TIMESTAMP) RETURNS BIGINT AS $$
DECLARE
    c care_schedule;
    v_admission BIGINT;
BEGIN
    SELECT * INTO c FROM care_schedule WHERE id = p_source;
    v_admission := c.admission_id;
    IF v_admission IS NOT NULL AND (SELECT status FROM admission_applications WHERE id = v_admission) = 'APPROVED' THEN
        v_admission := NULL;
    END IF;
    RETURN pg_temp.new_schedule(c.horse_id, 'ROUTINE', 'FOLLOW_UP', p_desc, p_at, p_created_at, v_admission, NULL,
                                c.id, c.veterinarian_id, 'completion:' || c.id,
                                pg_temp.fingerprint(c.horse_id, v_admission, p_at, p_desc));
END $$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------
-- Tiêm phòng (Preventive care) — giống PreventiveStatusService / PreventiveVaccinationService.
-- ---------------------------------------------------------------------
-- Mũi gần nhất của từng phác đồ tính tới ngày p_on. (Last dose of each protocol up to p_on.)
CREATE FUNCTION pg_temp.last_dose(p_horse BIGINT, p_protocol BIGINT, p_on DATE) RETURNS DATE AS $$
    SELECT max(p.start_date) FROM prescriptions p
      JOIN treatment_plans t ON t.id = p.treatment_plan_id
      JOIN health_records hr ON hr.id = t.health_record_id
     WHERE hr.horse_id = p_horse AND p.preventive_protocol_id = p_protocol AND p.start_date <= p_on
$$ LANGUAGE sql STABLE;

-- Ngày đến hạn của các phác đồ bắt buộc (MISSING = p_missing_due, hoặc bỏ qua khi NULL), theo thứ tự hiển thị của app.
-- (Due day of each mandatory protocol, in the app's display order.)
CREATE FUNCTION pg_temp.due_list(p_horse BIGINT, p_missing_due DATE, p_on DATE)
RETURNS TABLE (protocol_id BIGINT, name TEXT, last_dose DATE, due DATE) AS $$
    SELECT x.id, x.name, x.last_dose, COALESCE(x.last_dose + x.interval_days, p_missing_due)
      FROM (SELECT pp.id, pp.name, pp.interval_days, pp.category, pg_temp.last_dose(p_horse, pp.id, p_on) AS last_dose
              FROM preventive_protocols pp WHERE pp.active AND pp.mandatory) x
     WHERE COALESCE(x.last_dose + x.interval_days, p_missing_due) IS NOT NULL
     ORDER BY x.category, x.id
$$ LANGUAGE sql STABLE;

-- "Preventive care: Deworming (due 18/04/2027), Equine Influenza (no record)".
CREATE FUNCTION pg_temp.describe_due(p_horse BIGINT, p_missing_due DATE, p_on DATE, p_visit DATE) RETURNS TEXT AS $$
    SELECT 'Preventive care: ' || string_agg(name || CASE WHEN last_dose IS NULL THEN ' (no record)'
                                                          ELSE ' (due ' || to_char(due, 'DD/MM/YYYY') || ')' END, ', '
                                             ORDER BY ord)
      FROM (SELECT d.*, row_number() OVER () AS ord FROM pg_temp.due_list(p_horse, p_missing_due, p_on) d) x
     WHERE due <= p_visit + 7
$$ LANGUAGE sql STABLE;

-- Đặt lại mốc PREVENTIVE đang chờ (upsertMilestone). p_on = "hôm nay" tại thời điểm gọi trong dòng thời gian.
-- (Reset the pending PREVENTIVE milestone. p_on = "today" at that point of the timeline.)
CREATE FUNCTION pg_temp.upsert_milestone(p_horse BIGINT, p_key TEXT, p_actor BIGINT, p_missing_due DATE,
                                         p_at TIMESTAMP) RETURNS BIGINT AS $$
DECLARE
    v_on DATE := p_at::DATE;
    v_target DATE;
    v_req TIMESTAMP;
    v_slot_day DATE;
    m care_schedule;
    v_admission BIGINT;
    v_id BIGINT;
BEGIN
    IF (SELECT current_status FROM horses WHERE id = p_horse) = 'REJECTED' THEN RETURN NULL; END IF;
    SELECT * INTO m FROM care_schedule
     WHERE horse_id = p_horse AND purpose = 'PREVENTIVE' AND status IN ('REQUESTED', 'SCHEDULED', 'IN_PROGRESS');
    SELECT min(due) INTO v_target FROM pg_temp.due_list(p_horse, p_missing_due, v_on);
    IF v_target IS NULL THEN RETURN m.id; END IF;
    v_req := (CASE WHEN v_target > v_on THEN v_target ELSE v_on + 1 END) + TIME '14:00';
    IF m.id IS NOT NULL THEN
        IF m.status = 'IN_PROGRESS' THEN RETURN m.id; END IF;
        IF m.status = 'REQUESTED' THEN
            UPDATE care_schedule SET requested_at = v_req, description = pg_temp.describe_due(p_horse, p_missing_due, v_on, v_req::DATE),
                                     updated_at = p_at
             WHERE id = m.id;
            RETURN m.id;
        END IF;
        v_slot_day := COALESCE(m.scheduled_at, m.requested_at)::DATE;
        IF v_target <= v_slot_day + 7 THEN
            UPDATE care_schedule SET description = pg_temp.describe_due(p_horse, p_missing_due, v_on, v_slot_day), updated_at = p_at
             WHERE id = m.id;
            RETURN m.id;
        END IF;
        UPDATE care_schedule SET status = 'CANCELLED', updated_at = p_at,
                                 cancel_reason = 'Vaccinated early in a combined visit on ' || to_char(v_on, 'DD/MM')
         WHERE id = m.id;
    END IF;
    SELECT a.id INTO v_admission FROM admission_applications a
     WHERE a.horse_id = p_horse AND a.status NOT IN ('APPROVED', 'REJECTED') ORDER BY a.submitted_at DESC LIMIT 1;
    v_id := pg_temp.new_schedule(p_horse, 'ROUTINE', 'PREVENTIVE', pg_temp.describe_due(p_horse, p_missing_due, v_on, v_req::DATE),
                                 v_req, p_at, v_admission, NULL, NULL, p_actor, p_key, NULL);
    RETURN v_id;
END $$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------
-- Hoàn tất một ca khám (completeCareSchedule). Bản ghi bệnh án, mũi tiêm, chỉ số, quyết định tập luyện, cập nhật đơn
-- nhập học (ca INITIAL), đóng sự cố (ca URGENT), lịch tái khám và mốc tiêm kế tiếp — đúng thứ tự của service.
-- p_vaccines: mã phác đồ tiêm trong ca; p_history_codes/p_history_dates: mũi cũ nhập lúc khám nhập trại.
-- (Completes a visit: health record, doses, vitals, training decision, the admission (INITIAL), incidents (URGENT),
-- the follow-up and the next vaccination milestone, in the same order as the service.)
-- ---------------------------------------------------------------------
CREATE FUNCTION pg_temp.complete_care(
    p_schedule BIGINT, p_at TIMESTAMP, p_findings TEXT, p_diagnosis TEXT, p_decision TEXT,
    p_restriction TEXT DEFAULT NULL, p_treatment TEXT DEFAULT NULL, p_notes TEXT DEFAULT NULL, p_symptoms TEXT DEFAULT NULL,
    p_next_at TIMESTAMP DEFAULT NULL, p_next_desc TEXT DEFAULT 'Routine follow-up recheck examination',
    p_vaccines TEXT[] DEFAULT NULL, p_history_codes TEXT[] DEFAULT NULL, p_history_dates DATE[] DEFAULT NULL,
    p_temp NUMERIC DEFAULT NULL, p_hr NUMERIC DEFAULT NULL, p_rr NUMERIC DEFAULT NULL, p_weight NUMERIC DEFAULT NULL,
    p_bcs NUMERIC DEFAULT NULL, p_hydration TEXT DEFAULT 'Normal (CRT < 2s, pink)', p_metric_notes TEXT DEFAULT NULL,
    p_defer_days INT DEFAULT NULL)
RETURNS BIGINT AS $$
DECLARE
    c care_schedule;
    h horses;
    v_prev health_records;
    v_hr BIGINT;
    v_plan BIGINT;
    v_code TEXT;
    v_inc RECORD;
    v_trainer BIGINT;
    v_recipients BIGINT[] := '{}';
    v_r BIGINT;
    v_note TEXT;
    v_has_vaccines BOOLEAN := COALESCE(array_length(p_vaccines, 1), 0) + COALESCE(array_length(p_history_codes, 1), 0) > 0;
BEGIN
    SELECT * INTO c FROM care_schedule WHERE id = p_schedule;
    IF c.status = 'SCHEDULED' THEN PERFORM pg_temp.start_care(c.id); END IF;
    IF p_decision = 'BLOCKED' AND p_next_at IS NULL THEN
        RAISE EXCEPTION 'A follow-up examination is required when training is BLOCKED (schedule %)', c.id;
    END IF;
    IF c.purpose = 'PREVENTIVE' AND COALESCE(array_length(p_vaccines, 1), 0) = 0 AND p_defer_days IS NULL THEN
        RAISE EXCEPTION 'A preventive visit records vaccinations or a deferral (schedule %)', c.id;
    END IF;
    SELECT * INTO h FROM horses WHERE id = c.horse_id;

    -- Quyết định tập luyện. (Training decision.)
    IF c.care_type = 'URGENT' AND p_decision = 'ALLOWED' THEN
        SELECT * INTO v_prev FROM health_records
         WHERE horse_id = h.id AND training_decision IS NOT NULL AND examined_at < c.created_at
         ORDER BY examined_at DESC LIMIT 1;
        IF v_prev.training_decision = 'BLOCKED' THEN PERFORM pg_temp.block(h.id, v_prev.restriction_details, p_at);
        ELSE PERFORM pg_temp.allow(h.id, p_at); END IF;
    ELSIF p_decision = 'ALLOWED' THEN
        PERFORM pg_temp.allow(h.id, p_at);
    ELSE
        PERFORM pg_temp.block(h.id, p_restriction, p_at);
    END IF;

    INSERT INTO health_records (horse_id, veterinarian_id, examined_at, symptoms, findings, diagnosis, notes, created_at,
                                updated_at, record_type, treatment, care_schedule_id, training_decision, restriction_details)
    VALUES (h.id, c.veterinarian_id, p_at, p_symptoms, p_findings, p_diagnosis, p_notes, p_at, p_at, c.care_type,
            p_treatment, c.id, p_decision, p_restriction)
    RETURNING id INTO v_hr;

    -- Mũi tiêm: một TreatmentPlan "Vaccination" + một Prescription cho mỗi phác đồ. (Doses: one plan + one row each.)
    IF v_has_vaccines THEN
        INSERT INTO treatment_plans (health_record_id, treatment_name, instructions, start_date, end_date, status, created_at, updated_at)
        VALUES (v_hr, 'Vaccination', 'Vaccination / deworming per preventive protocol',
                LEAST(p_at::DATE, (SELECT min(x) FROM unnest(p_history_dates) x)), p_at::DATE, 'COMPLETED', p_at, p_at)
        RETURNING id INTO v_plan;
        FOR i IN 1..COALESCE(array_length(p_history_codes, 1), 0) LOOP
            INSERT INTO prescriptions (treatment_plan_id, medication_name, dosage, frequency, route, start_date, end_date,
                                       instructions, created_at, updated_at, preventive_protocol_id)
            SELECT v_plan, pp.name, 'Not recorded', 'ONCE', pp.default_route, p_history_dates[i], p_history_dates[i],
                   '[HISTORICAL] Entered from the vaccination record supplied with the admission', p_at, p_at, pp.id
              FROM preventive_protocols pp WHERE pp.code = p_history_codes[i];
        END LOOP;
        FOREACH v_code IN ARRAY COALESCE(p_vaccines, '{}') LOOP
            INSERT INTO prescriptions (treatment_plan_id, medication_name, dosage, frequency, route, start_date, end_date,
                                       instructions, created_at, updated_at, preventive_protocol_id)
            SELECT v_plan, pp.name, pp.default_dosage, 'ONCE', pp.default_route, p_at::DATE, p_at::DATE, NULL, p_at, p_at, pp.id
              FROM preventive_protocols pp WHERE pp.code = v_code;
        END LOOP;
    END IF;

    IF p_temp IS NOT NULL THEN
        INSERT INTO horse_health_metrics (horse_id, recorded_at, heart_rate, temperature, weight, respiratory_rate,
                                          hydration_status, body_condition_score, notes, created_at, health_record_id)
        VALUES (h.id, p_at, p_hr, p_temp, p_weight, p_rr, p_hydration, p_bcs, p_metric_notes, p_at, v_hr);
    END IF;

    -- Ca INITIAL quyết định bước Thú y của đơn: đơn luôn sang TRAINER_REVIEW kèm kết luận được tập / tạm nghỉ.
    -- (An intake exam closes the vet step: the admission always moves on to TRAINER_REVIEW with the conclusion.)
    IF c.care_type = 'INITIAL' THEN
        UPDATE admission_applications
           SET veterinarian_id = c.veterinarian_id, vet_reviewed_at = p_at, vet_feedback = COALESCE(p_notes, p_findings),
               vet_training_decision = p_decision, status = 'TRAINER_REVIEW', updated_at = p_at
         WHERE id = c.admission_id;
    END IF;

    UPDATE care_schedule SET status = 'COMPLETED', completed_at = p_at, updated_at = p_at WHERE id = c.id;

    -- Mốc tiêm kế tiếp sau khi ca này đã COMPLETED. (Next vaccination milestone, after this visit is COMPLETED.)
    IF p_defer_days IS NOT NULL THEN
        PERFORM pg_temp.upsert_milestone(h.id, 'preventive:' || v_hr, c.veterinarian_id, p_at::DATE, p_at);
        UPDATE care_schedule SET requested_at = (p_at::DATE + p_defer_days) + TIME '14:00', reschedule_reason = 'UNDER_TREATMENT'
         WHERE horse_id = h.id AND purpose = 'PREVENTIVE' AND status = 'REQUESTED';
    ELSIF v_has_vaccines THEN
        PERFORM pg_temp.upsert_milestone(h.id, 'preventive:' || v_hr, c.veterinarian_id,
                                         CASE WHEN c.care_type = 'INITIAL' THEN NULL ELSE p_at::DATE END, p_at);
    END IF;

    -- Ca khẩn cấp: đóng mọi báo cáo sự cố còn mở của ngựa, báo Groom đã báo và Trainer của khu.
    -- (Urgent case: resolve the horse's open incident reports, notify the reporting grooms and the area trainer.)
    IF c.care_type = 'URGENT' THEN
        v_note := 'Diagnosis: ' || p_diagnosis || COALESCE(E'\nTreatment: ' || p_treatment, '')
                  || CASE WHEN (SELECT training_decision FROM horses WHERE id = h.id) = 'BLOCKED'
                          THEN E'\nTraining: blocked - ' || (SELECT training_decision_reason FROM horses WHERE id = h.id)
                          ELSE E'\nTraining: allowed' END;
        FOR v_inc IN SELECT * FROM horse_incident_reports
                      WHERE horse_id = h.id AND status IN ('REPORTED', 'IN_REVIEW') LOOP
            UPDATE horse_incident_reports SET status = 'RESOLVED', handled_by_id = c.veterinarian_id, handled_at = p_at,
                                              handler_note = v_note
             WHERE id = v_inc.id;
            v_recipients := v_recipients || v_inc.reported_by_id;
        END LOOP;
        SELECT a.trainer_id INTO v_trainer FROM horses x JOIN stable_stalls s ON s.id = x.current_stall_id
          JOIN areas a ON a.id = s.area_id WHERE x.id = h.id;
        IF v_trainer IS NOT NULL THEN v_recipients := v_recipients || v_trainer; END IF;
        FOREACH v_r IN ARRAY v_recipients LOOP
            PERFORM pg_temp.notify(v_r, 'CARE_SCHEDULE', c.id, 'URGENT_CASE_CLOSED', 'Ca khám khẩn cấp hoàn tất: ' || h.name,
                                   'Bác sĩ Thú y đã hoàn tất ca khám khẩn cấp cho ngựa ' || h.name || '. Quyết định tập luyện: '
                                   || (SELECT training_decision FROM horses WHERE id = h.id), p_at);
        END LOOP;
    END IF;

    -- Lịch tái khám: theo ngày Thú y chọn, hoặc tự động +14 ngày sau một ca tái khám thường.
    -- (Follow-up: on the vet's date, or automatically 14 days after a routine follow-up visit.)
    IF p_next_at IS NOT NULL THEN
        PERFORM pg_temp.follow_up(c.id, p_next_at, p_next_desc, p_at);
    ELSIF c.care_type = 'ROUTINE' AND c.purpose = 'FOLLOW_UP' THEN
        PERFORM pg_temp.follow_up(c.id, p_at + INTERVAL '14 days', 'Routine follow-up recheck examination', p_at);
    END IF;
    RETURN v_hr;
END $$ LANGUAGE plpgsql;

-- Lịch tái khám đang chờ do ca p_source sinh ra. (The pending follow-up created by visit p_source.)
CREATE FUNCTION pg_temp.child_of(p_source BIGINT) RETURNS BIGINT AS $$
    SELECT id FROM care_schedule WHERE source_schedule_id = p_source
$$ LANGUAGE sql STABLE;

-- Sự cố do Groom báo (createReport): mở ca khẩn cấp nếu ngựa chưa có, chặn tập, giao Thú y ngay.
-- (An incident reported by a groom: opens an urgent case if none is open, blocks training, assigns a vet at once.)
CREATE FUNCTION pg_temp.report_incident(p_horse BIGINT, p_reporter BIGINT, p_at TIMESTAMP, p_title TEXT, p_desc TEXT,
                                        p_severity TEXT, p_vet BIGINT) RETURNS BIGINT AS $$
DECLARE
    v_incident BIGINT;
    v_schedule BIGINT;
    v_vet BIGINT;
BEGIN
    INSERT INTO horse_incident_reports (reported_by_id, horse_id, title, description, severity, reported_at, status)
    VALUES (p_reporter, p_horse, p_title, p_desc, p_severity, p_at, 'REPORTED') RETURNING id INTO v_incident;
    SELECT id INTO v_schedule FROM care_schedule
     WHERE horse_id = p_horse AND care_type = 'URGENT' AND status IN ('REQUESTED', 'SCHEDULED', 'IN_PROGRESS');
    IF v_schedule IS NULL THEN
        v_schedule := pg_temp.new_schedule(p_horse, 'URGENT', 'FOLLOW_UP', p_desc, NULL, p_at, NULL, v_incident);
        PERFORM pg_temp.block(p_horse, 'Urgent veterinary care pending: ' || p_desc, p_at);
        -- Thú y rảnh lúc này, ưu tiên p_vet. (A vet free right now, preferring p_vet.)
        SELECT u.id INTO v_vet FROM users u JOIN roles r ON r.id = u.role_id
         WHERE r.name = 'VETERINARIAN' AND u.is_active AND NOT pg_temp.vet_busy(u.id, p_at, 30, NULL)
         ORDER BY (u.id = p_vet) DESC, u.id LIMIT 1;
        PERFORM pg_temp.assign(v_schedule, v_vet, p_at, p_at);
        UPDATE horse_incident_reports SET status = 'IN_REVIEW', handled_by_id = v_vet, handled_at = p_at WHERE id = v_incident;
    ELSE
        PERFORM pg_temp.block(p_horse, 'Urgent veterinary care pending', p_at);
    END IF;
    UPDATE horse_incident_reports SET care_schedule_id = v_schedule WHERE id = v_incident;
    RETURN v_incident;
END $$ LANGUAGE plpgsql;
