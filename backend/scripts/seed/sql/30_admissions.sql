-- =====================================================================
-- 30_admissions.sql — luồng nhập học cho từng đơn trong seed_admission, theo đúng thứ tự thời gian.
-- (The admission flow for every row of seed_admission, in time order.)
--
-- Mỗi bước làm đúng việc service tương ứng làm:
--   nộp đơn (OwnerAdmissionService.submit) -> Groom duyệt + giữ chuồng Q (AdmissionGroomReviewService.review)
--   -> ngựa tới (confirmArrival: tạo Horse CANDIDATE, phả hệ, ca khám INITIAL) -> Thú y khám (completeCareSchedule)
--   -> gán Trainer (TrainerAssignmentService) -> Trainer đánh giá (AdmissionTrainerReviewService)
--   -> Manager duyệt / từ chối (AdmissionManagerReviewService), kèm mốc tiêm phòng.
-- Ảnh chụp hồ sơ và file đính kèm được ghi lúc đơn còn GROOM_REVIEW, đúng như trigger protect_admission_snapshot yêu cầu.
-- (Each step does what the matching service does. The snapshot and the files are written while the admission is
-- still in GROOM_REVIEW, as the protect_admission_snapshot trigger requires.)
-- =====================================================================

-- ---------------------------------------------------------------------
-- Tái khám (Rechecks): xử lý các lịch tái khám FOLLOW_UP đang chờ của một ngựa tới thời điểm p_until.
--   - Lịch tái khám của ngựa đang tạm nghỉ: Thú y khám và cho tập lại (ALLOWED).
--   - Lịch tái khám tự động +14 ngày sau đó: Manager hủy vì ngựa đã ổn.
--   - Lịch nằm sau thời điểm hiện tại: để nguyên (đã xếp giờ nếu trong 14 ngày tới, còn lại REQUESTED).
-- (Pending follow-ups of a horse up to p_until: a recheck of a resting horse clears it; the automatic +14-day recheck
-- after it is cancelled by the manager; anything after now stays pending.)
-- ---------------------------------------------------------------------
CREATE FUNCTION pg_temp.run_rechecks(p_horse BIGINT, p_vet BIGINT, p_until TIMESTAMP) RETURNS VOID AS $$
DECLARE
    c care_schedule;
    v_slot TIMESTAMP;
    v_assign TIMESTAMP;
    v_limit TIMESTAMP := LEAST(p_until, pg_temp.clock());
BEGIN
    LOOP
        SELECT * INTO c FROM care_schedule
         WHERE horse_id = p_horse AND purpose = 'FOLLOW_UP' AND care_type = 'ROUTINE' AND status IN ('REQUESTED', 'SCHEDULED')
         ORDER BY requested_at LIMIT 1;
        EXIT WHEN c.id IS NULL;
        IF c.status = 'REQUESTED' THEN
            v_assign := GREATEST(c.created_at, c.requested_at - INTERVAL '14 days');
            EXIT WHEN v_assign > v_limit;
            PERFORM pg_temp.book(c.id, p_vet);
            SELECT * INTO c FROM care_schedule WHERE id = c.id;
        END IF;
        IF (SELECT training_decision FROM horses WHERE id = p_horse) = 'BLOCKED' THEN
            EXIT WHEN c.scheduled_at + INTERVAL '25 minutes' > v_limit;
            PERFORM pg_temp.complete_care(c.id, c.scheduled_at + INTERVAL '25 minutes',
                p_findings => 'Recheck: the problem noted at the last visit has resolved; gait, temperature and appetite normal.',
                p_diagnosis => 'Recovered; fit to resume training', p_decision => 'ALLOWED',
                p_notes => 'Cleared to return to normal work.',
                p_temp => 37.8, p_hr => 36, p_rr => 12, p_bcs => 5.5);
        ELSE
            v_assign := c.updated_at + INTERVAL '1 day';
            EXIT WHEN v_assign > v_limit OR v_assign >= c.scheduled_at;
            PERFORM pg_temp.cancel_care(c.id, 'Horse recovered at the previous recheck; no further rechecks needed.', v_assign);
        END IF;
    END LOOP;
END $$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------
-- Tiêm phòng (Preventive visits): xếp và hoàn tất các mốc PREVENTIVE của một ngựa tới thời điểm p_until.
-- Ở mỗi lần khám, Thú y tiêm mọi phác đồ bắt buộc tới hạn trong 7 ngày (kể cả phác đồ chưa từng tiêm), rồi app tự đặt
-- mốc kế tiếp. Ngựa đang tạm nghỉ thì hoãn tiêm tới khi hết nghỉ.
-- p_defer: hoãn tiêm lần đầu 7 ngày; p_hub: lần thứ hai tiêm từ màn hình "To do" thay vì chờ lịch;
-- p_leave_overdue: để lại một lịch đã quá giờ chưa khám (Thú y lỡ hẹn).
-- (Books and completes a horse's PREVENTIVE milestones up to p_until. At each visit the vet gives every mandatory
-- protocol due within 7 days; the app then sets the next milestone. A resting horse is vaccinated after the rest.)
-- ---------------------------------------------------------------------
CREATE FUNCTION pg_temp.run_preventive(p_horse BIGINT, p_vet BIGINT, p_until TIMESTAMP,
                                       p_defer BOOLEAN DEFAULT FALSE, p_hub BOOLEAN DEFAULT FALSE,
                                       p_leave_overdue BOOLEAN DEFAULT FALSE) RETURNS VOID AS $$
DECLARE
    m care_schedule;
    v_limit TIMESTAMP := LEAST(p_until, pg_temp.clock());
    v_assign TIMESTAMP;
    v_done TIMESTAMP;
    v_block TIMESTAMP;
    v_codes TEXT[];
    v_names TEXT;
    v_visits INT := 0;
    v_hr BIGINT;
    v_rest seed_block;
BEGIN
    LOOP
        SELECT * INTO m FROM care_schedule
         WHERE horse_id = p_horse AND purpose = 'PREVENTIVE' AND status IN ('REQUESTED', 'SCHEDULED');
        EXIT WHEN m.id IS NULL;
        IF m.status = 'REQUESTED' THEN
            v_assign := GREATEST(m.created_at, m.requested_at - INTERVAL '14 days');
            EXIT WHEN v_assign > v_limit;
            -- Lần tiêm thứ hai ghi từ "To do" trước khi lịch được xếp. (Second round recorded from "To do".)
            IF p_hub AND v_visits = 1 AND v_assign - INTERVAL '2 days' > m.created_at THEN
                v_done := date_trunc('day', v_assign - INTERVAL '2 days') + TIME '10:30';
                SELECT array_agg(c.code), string_agg(c.name, ', ') INTO v_codes, v_names
                  FROM (SELECT pp.code, pp.name FROM pg_temp.due_list(p_horse, v_done::DATE, v_done::DATE) d
                          JOIN preventive_protocols pp ON pp.id = d.protocol_id WHERE d.due <= v_done::DATE + 7) c;
                IF v_codes IS NOT NULL THEN
                    INSERT INTO health_records (horse_id, veterinarian_id, examined_at, notes, created_at, updated_at,
                                                record_type, treatment)
                    VALUES (p_horse, p_vet, v_done, 'Given during the morning round from the To do list.', v_done, v_done,
                            'VACCINATION', v_names)
                    RETURNING id INTO v_hr;
                    INSERT INTO treatment_plans (health_record_id, treatment_name, instructions, start_date, end_date, status,
                                                 created_at, updated_at)
                    VALUES (v_hr, 'Vaccination', 'Vaccination / deworming per preventive protocol', v_done::DATE, v_done::DATE,
                            'COMPLETED', v_done, v_done);
                    INSERT INTO prescriptions (treatment_plan_id, medication_name, dosage, frequency, route, start_date,
                                               end_date, instructions, created_at, updated_at, preventive_protocol_id)
                    SELECT (SELECT id FROM treatment_plans WHERE health_record_id = v_hr), pp.name, pp.default_dosage, 'ONCE',
                           pp.default_route, v_done::DATE, v_done::DATE, 'Given during the morning round from the To do list.',
                           v_done, v_done, pp.id
                      FROM preventive_protocols pp WHERE pp.code = ANY (v_codes);
                    PERFORM pg_temp.upsert_milestone(p_horse, 'preventive:' || v_hr, p_vet, v_done::DATE, v_done);
                    v_visits := v_visits + 1;
                    CONTINUE;
                END IF;
            END IF;
            -- Ngựa đang tạm nghỉ: hoãn tới khi hết nghỉ. (Resting horse: wait until the rest ends.)
            SELECT max(COALESCE(b.to_ts, 'infinity')) INTO v_block FROM seed_block b
             WHERE b.horse_id = p_horse AND b.from_ts <= GREATEST(m.requested_at, v_assign) + INTERVAL '1 day'
               AND (b.to_ts IS NULL OR b.to_ts > GREATEST(m.requested_at, v_assign));
            IF v_block = 'infinity' THEN
                EXIT WHEN v_limit < pg_temp.clock();
                PERFORM pg_temp.book(m.id, p_vet, pg_temp.clock());
                EXIT;
            END IF;
            PERFORM pg_temp.book(m.id, p_vet, v_block);
            SELECT * INTO m FROM care_schedule WHERE id = m.id;
        END IF;
        v_done := m.scheduled_at + make_interval(mins => pg_temp.rndi('pv' || m.id, 18, 28));
        EXIT WHEN v_done > v_limit;
        EXIT WHEN p_leave_overdue AND m.scheduled_at > pg_temp.clock() - INTERVAL '4 days';
        IF pg_temp.blocked_at(p_horse, m.scheduled_at) THEN
            -- Ngựa bị chặn sau khi lịch đã xếp: Manager dời ca tiêm tới sau đợt nghỉ (CareRescheduleService: lịch về
            -- REQUESTED ở ngày mới). (Blocked after booking: the manager moves the visit after the rest period.)
            SELECT * INTO v_rest FROM seed_block WHERE horse_id = p_horse AND from_ts <= m.scheduled_at
             ORDER BY from_ts DESC LIMIT 1;
            UPDATE care_schedule SET status = 'REQUESTED', veterinarian_id = NULL, scheduled_at = NULL,
                                     requested_at = (COALESCE(v_rest.to_ts, pg_temp.clock())::DATE + 1) + TIME '14:00',
                                     reschedule_reason = 'UNDER_TREATMENT', reschedule_note = 'Vaccinate after the rest period.',
                                     updated_at = v_rest.from_ts
             WHERE id = m.id;
            PERFORM pg_temp.audit(pg_temp.uid('ADMIN'), 'Care Schedule Rescheduled', 'CareSchedule', m.id,
                                  'Care schedule #' || m.id || ' rescheduled to '
                                  || (COALESCE(v_rest.to_ts, pg_temp.clock())::DATE + 1) || ' (Reason: UNDER_TREATMENT)',
                                  v_rest.from_ts, NULL, NULL);
            EXIT WHEN v_rest.to_ts IS NULL;
            CONTINUE;
        END IF;
        IF p_defer AND v_visits = 0 THEN
            PERFORM pg_temp.complete_care(m.id, v_done,
                p_findings => 'Pre-vaccination check: mild serous nasal discharge and a soft cough; lungs clear, temperature 38.4 °C.',
                p_diagnosis => 'Mild upper airway irritation; vaccination postponed', p_decision => 'ALLOWED',
                p_treatment => 'Steam the hay for a week; recheck before vaccinating.', p_notes => 'Vaccination deferred by 7 days.',
                p_symptoms => 'Soft cough, clear nasal discharge',
                p_temp => 38.4, p_hr => 40, p_rr => 16, p_bcs => 5.5, p_defer_days => 7);
        ELSE
            SELECT array_agg(pp.code) INTO v_codes
              FROM pg_temp.due_list(p_horse, v_done::DATE, v_done::DATE) d JOIN preventive_protocols pp ON pp.id = d.protocol_id
             WHERE d.due <= v_done::DATE + 7;
            IF pg_temp.rndi('opt' || m.id, 1, 6) = 1 THEN v_codes := v_codes || 'RABIES'::TEXT; END IF;
            SELECT string_agg(pp.name, ', ') INTO v_names FROM preventive_protocols pp WHERE pp.code = ANY (v_codes);
            PERFORM pg_temp.complete_care(m.id, v_done,
                p_findings => 'Pre-vaccination check: temperature, heart and lungs normal; no swelling at previous injection sites.',
                p_diagnosis => 'Healthy; vaccinated per preventive protocol', p_decision => 'ALLOWED',
                p_treatment => 'Administered: ' || v_names, p_vaccines => v_codes,
                p_temp => round(pg_temp.rnd('t' || m.id, 37.4, 38.1), 1), p_hr => pg_temp.rndi('h' || m.id, 30, 40),
                p_rr => pg_temp.rndi('r' || m.id, 10, 15), p_weight => round(pg_temp.rnd('w' || m.id, 410, 480), 1),
                p_bcs => 5.5);
        END IF;
        v_visits := v_visits + 1;
    END LOOP;
END $$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------
-- Luồng nhập học (The admission flow)
-- ---------------------------------------------------------------------
DO $$
DECLARE
    a RECORD;
    hs seed_horse;
    v_adm BIGINT;
    v_horse BIGINT;
    v_owner BIGINT;
    v_groom BIGINT;
    v_vet BIGINT;
    v_trainer BIGINT;
    v_manager BIGINT := pg_temp.uid('ADMIN');
    v_q BIGINT;
    v_sched BIGINT;
    v_slot TIMESTAMP;
    v_done TIMESTAMP;
    v_t TIMESTAMP;
    v_codes TEXT[];
    v_dates DATE[];
    v_sire RECORD;
    v_dam RECORD;
    v_remarks TEXT;
BEGIN
    FOR a IN SELECT * FROM seed_admission ORDER BY submitted, k LOOP
        SELECT * INTO hs FROM seed_horse WHERE k = a.horse_k;
        v_owner := pg_temp.uid(a.owner_k);

        -- 1. Chủ ngựa nộp đơn: đơn + ảnh chụp hồ sơ ngựa + file đính kèm. (Owner submits: admission, snapshot, files.)
        v_t := pg_temp.ts(a.submitted);
        INSERT INTO admission_applications (owner_id, status, submitted_at, groom_review_started_at, created_at, updated_at)
        VALUES (v_owner, 'GROOM_REVIEW', v_t, v_t, v_t, v_t) RETURNING id INTO v_adm;
        INSERT INTO seed_admission_id VALUES (a.k, v_adm);
        INSERT INTO candidate_horse_profiles (admission_id, name, breed, date_of_birth, registration_number, registry_name,
                                              sire_name, sire_registration_number, dam_name, dam_registration_number,
                                              pedigree_notes, created_at, updated_at, sex)
        VALUES (v_adm, hs.name, hs.breed, hs.dob, hs.ueln, hs.registry, hs.sire_name, hs.sire_ueln, hs.dam_name, hs.dam_ueln,
                hs.pedigree_notes, v_t, v_t, hs.sex);
        INSERT INTO admission_documents (admission_id, document_type, file_url, record_date, note, uploaded_at, original_file_name)
        SELECT v_adm, doc.doc_type, pg_temp.file_key(doc.path), doc.record_date, doc.note, v_t, doc.file_name
          FROM seed_doc doc WHERE doc.horse_k = a.horse_k ORDER BY doc.ord;
        PERFORM pg_temp.audit(v_owner, 'Submit Admission Application', 'AdmissionApplication', NULL,
                              'Horse owner submitted a new admission application dossier', v_t, 'POST', '/api/owner/admissions');

        -- 2. Groom duyệt hồ sơ; duyệt thì giữ chuồng cách ly 14 ngày. (Groom review; approval holds a Q stall 14 days.)
        IF a.groom_at IS NOT NULL THEN
            v_groom := pg_temp.uid(a.groom_k);
            v_t := pg_temp.ts(a.groom_at);
            UPDATE admission_applications
               SET groom_id = v_groom, groom_decision = a.groom_decision, groom_reviewed_at = v_t, updated_at = v_t,
                   groom_feedback = CASE WHEN a.groom_decision = 'REJECTED'
                       THEN 'The vaccination record is out of date: the last influenza / tetanus dose was given in November 2024. Please resubmit with a current vaccination record.'
                       ELSE (ARRAY['Identity, UELN and pedigree match the registry; the required documents are complete and legible.',
                                   'Dossier complete. Registration and vaccination record checked against the photo and UELN.',
                                   'All four required documents present; health certificate and deworming record also supplied.'])
                            [pg_temp.rndi('gf' || a.k, 1, 3)] END
             WHERE id = v_adm;
            IF a.groom_decision = 'REJECTED' THEN
                UPDATE admission_applications SET status = 'REJECTED' WHERE id = v_adm;
            ELSE
                v_q := pg_temp.stall(a.q_stall);
                UPDATE stable_stalls SET status = 'RESERVED', updated_at = v_t WHERE id = v_q;
                UPDATE admission_applications SET status = 'WAITING_FOR_ARRIVAL', quarantine_stall_id = v_q,
                       arrival_deadline_at = v_t + INTERVAL '14 days'
                 WHERE id = v_adm;
            END IF;
            PERFORM pg_temp.audit(v_groom, 'Groom Admission Review', 'AdmissionApplication', v_adm,
                                  'Groom reviewed intake dossier and verified arrival schedule for application #' || v_adm,
                                  v_t, 'POST', '/api/admissions/' || v_adm || '/groom-review');
        END IF;

        -- Hết hạn chờ ngựa tới: trả chuồng Q, đơn ARRIVAL_EXPIRED. (Arrival deadline passed: the Q stall is released.)
        IF a.status = 'ARRIVAL_EXPIRED' THEN
            v_t := pg_temp.ts(a.groom_at) + INTERVAL '14 days';
            UPDATE stable_stalls SET status = 'AVAILABLE', updated_at = v_t WHERE id = v_q;
            UPDATE admission_applications SET status = 'ARRIVAL_EXPIRED', quarantine_stall_id = NULL, updated_at = v_t
             WHERE id = v_adm;
        END IF;

        -- 3. Ngựa tới trại: tạo (hoặc dùng lại ngựa từng bị từ chối) Horse CANDIDATE trong chuồng Q, phả hệ, ca khám INITIAL.
        -- (Arrival: create, or reuse a previously rejected, CANDIDATE horse in the Q stall, its pedigree, the intake exam.)
        IF a.arrived IS NOT NULL THEN
            v_t := pg_temp.ts(a.arrived);
            UPDATE stable_stalls SET status = 'OCCUPIED', updated_at = v_t WHERE id = v_q;
            SELECT id INTO v_horse FROM horses WHERE registration_number = hs.ueln;
            IF v_horse IS NULL THEN
                INSERT INTO horses (name, breed, date_of_birth, current_status, owner_id, created_at, updated_at,
                                    current_stall_id, registry_name, registration_number, training_decision, sex)
                VALUES (hs.name, hs.breed, hs.dob, 'CANDIDATE', v_owner, v_t, v_t, v_q, hs.registry, hs.ueln, 'ALLOWED', hs.sex)
                RETURNING id INTO v_horse;
                INSERT INTO seed_horse_id VALUES (a.horse_k, v_horse);
            ELSE
                UPDATE horses SET current_status = 'CANDIDATE', current_stall_id = v_q, owner_id = v_owner,
                                  training_decision = 'ALLOWED', training_decision_reason = NULL, updated_at = v_t
                 WHERE id = v_horse;
            END IF;
            SELECT id, name, registration_number INTO v_sire FROM horses WHERE registration_number = hs.sire_ueln;
            SELECT id, name, registration_number INTO v_dam FROM horses WHERE registration_number = hs.dam_ueln;
            DELETE FROM horse_pedigrees WHERE horse_id = v_horse;
            INSERT INTO horse_pedigrees (horse_id, sire_id, dam_id, pedigree_notes, sire_name, sire_registration_number,
                                         dam_name, dam_registration_number)
            VALUES (v_horse, v_sire.id, v_dam.id, hs.pedigree_notes, COALESCE(v_sire.name, hs.sire_name), hs.sire_ueln,
                    COALESCE(v_dam.name, hs.dam_name), hs.dam_ueln);
            UPDATE admission_applications SET horse_id = v_horse, arrived_at = v_t, status = 'VET_REVIEW', updated_at = v_t
             WHERE id = v_adm;
            PERFORM pg_temp.audit(v_groom, 'Confirm Horse Arrival', 'AdmissionApplication', v_adm,
                                  'Confirmed physical arrival of horse into quarantine for application #' || v_adm,
                                  v_t, 'POST', '/api/admissions/' || v_adm || '/arrival-confirmation');

            v_vet := pg_temp.uid(a.vet_k);
            v_sched := pg_temp.new_schedule(v_horse, 'INITIAL', 'FOLLOW_UP',
                                            'Initial admission physical examination in quarantine area', NULL, v_t, v_adm);
            PERFORM pg_temp.notify(v_vet, 'ADMISSION', v_adm, 'ADMISSION_VET_ASSIGNED', 'New horse assignment',
                                   'You have been assigned to candidate horse ' || hs.name
                                   || ' for initial admission examination in quarantine.', v_t);
            -- Hai ca đang chờ khám: một ca chiều nay, một ca chiều mai. (Two pending intake exams: this and tomorrow afternoon.)
            v_slot := pg_temp.book(v_sched, v_vet, CASE a.horse_k WHEN 'H043' THEN pg_temp.at(0, TIME '13:30')
                                                                 WHEN 'H044' THEN pg_temp.at(1, TIME '14:00') END);
        END IF;

        -- 4. Thú y khám nhập trại (trừ hai đơn còn đang chờ khám). (Intake exam, except the two still waiting.)
        IF a.arrived IS NOT NULL AND a.status <> 'VET_REVIEW' THEN
            v_done := v_slot + make_interval(mins => pg_temp.rndi('ex' || a.k, 20, 35));
            v_codes := NULL; v_dates := NULL;
            IF NOT a.no_history THEN
                SELECT array_agg(c ORDER BY dt), array_agg(dt ORDER BY dt) INTO v_codes, v_dates
                  FROM (SELECT unnest(ARRAY['EI', 'TETANUS']) c, x dt FROM unnest(hs.vaccinations) x
                        UNION ALL SELECT 'DEWORM', x FROM unnest(hs.dewormings) x) y;
            END IF;
            PERFORM pg_temp.complete_care(v_sched, v_done,
                p_findings => CASE WHEN a.vet_decision = 'BLOCKED'
                     THEN 'Physical examination completed in person. ' || a.restriction
                     ELSE 'Physical examination completed in person. General condition, respiratory, cardiovascular, musculoskeletal and digestive systems within normal limits.' END,
                p_diagnosis => CASE WHEN a.vet_decision = 'BLOCKED' THEN 'Minor finding after transport; fit for admission after a short rest'
                     ELSE 'Clinically healthy' END,
                p_decision => a.vet_decision, p_restriction => a.restriction,
                p_treatment => CASE WHEN a.vet_decision = 'BLOCKED' THEN 'Rest and monitoring until the recheck.'
                     ELSE 'No medical treatment required.' END,
                p_notes => CASE WHEN a.vet_decision = 'BLOCKED' THEN 'Recheck booked before the horse starts training.'
                     ELSE 'Calm, alert and responsive after transport; supplied certificates checked.' END,
                p_symptoms => CASE WHEN a.vet_decision = 'BLOCKED' THEN 'Noted on arrival by the quarantine groom' ELSE 'No symptoms observed' END,
                p_next_at => CASE WHEN a.vet_decision = 'BLOCKED' THEN date_trunc('day', v_done) + INTERVAL '7 days 14 hours' END,
                p_next_desc => 'Recheck before training', p_history_codes => v_codes, p_history_dates => v_dates,
                p_temp => hs.exam_temp, p_hr => hs.exam_hr, p_rr => hs.exam_rr,
                p_weight => round(pg_temp.rnd('iw' || a.k, 395, 470), 1), p_bcs => 5.5);
            PERFORM pg_temp.audit(v_vet, 'Veterinarian Intake Exam', 'AdmissionApplication', v_adm,
                                  'Veterinarian performed initial clinical examination for application #' || v_adm,
                                  v_done, 'POST', '/api/admissions/' || v_adm || '/vet-review');

            -- Gán Head Trainer ngay sau ca khám. (A head trainer is assigned right after the exam.)
            v_trainer := pg_temp.uid(a.trainer_k);
            UPDATE admission_applications SET trainer_id = v_trainer WHERE id = v_adm;
            PERFORM pg_temp.notify(v_trainer, 'ADMISSION', v_adm, 'ADMISSION_TRAINER_ASSIGNED', 'New horse assignment',
                                   'You have been assigned to candidate horse ' || hs.name || ' for racing readiness assessment.',
                                   v_done + INTERVAL '1 minute');
        END IF;

        -- 5. Trainer đánh giá; đơn sang MANAGER_REVIEW. (Trainer assessment; the admission moves to MANAGER_REVIEW.)
        IF a.trainer_at IS NOT NULL THEN
            v_t := pg_temp.ts(a.trainer_at);
            v_remarks := CASE a.readiness
                WHEN 'READY' THEN (ARRAY[
                    'Balanced, correct conformation and a calm, workable temperament. Moves straight with good reach; can start a racing preparation straight after admission.',
                    'Strong, well-muscled and light on the forehand. Settles quickly on the track; ready for race-specific work.'])
                    [pg_temp.rndi('rm' || a.k, 1, 2)]
                WHEN 'UNSUITABLE' THEN
                    'Tense and difficult to handle on the track, with an irregular, short stride behind. Not a racing prospect at this stage.'
                ELSE (ARRAY[
                    'Sound and willing but still building strength through the topline and hindquarters. Needs a conditioning block before any race entry.',
                    'Good temperament; stride still short and the horse tires in the last furlong. Conditioning first, then speed work.',
                    'Correct legs and a kind eye, but green at the gate and on turns. Needs gate schooling and a base fitness block.'])
                    [pg_temp.rndi('rm' || a.k, 1, 3)] END;
            INSERT INTO racing_readiness_assessments (horse_id, readiness_status, assessment_date, remarks, created_at, trainer_id,
                                                      admission_id, conformation_score, temperament_score, gait_quality_score,
                                                      estimated_months_to_race)
            VALUES (v_horse, a.readiness, v_t::DATE, v_remarks, v_t, v_trainer, v_adm, a.conformation, a.temperament, a.gait,
                    a.months_to_race);
            UPDATE admission_applications SET trainer_feedback = v_remarks, trainer_reviewed_at = v_t, status = 'MANAGER_REVIEW',
                                              updated_at = v_t
             WHERE id = v_adm;
            PERFORM pg_temp.audit(v_trainer, 'Trainer Fitness Assessment', 'AdmissionApplication', v_adm,
                                  'Head Trainer assessed conformation, gait, and racing readiness for application #' || v_adm,
                                  v_t, 'POST', '/api/admissions/' || v_adm || '/trainer-assessment');
        END IF;

        -- Trong thời gian cách ly: tái khám và tiêm phòng tới lúc Manager quyết định (hoặc tới hôm nay).
        -- (During quarantine: rechecks and vaccinations up to the manager decision, or up to now.)
        IF a.arrived IS NOT NULL AND a.status <> 'VET_REVIEW' THEN
            PERFORM pg_temp.run_rechecks(v_horse, v_vet, COALESCE(pg_temp.ts(a.manager_at), pg_temp.clock()));
            PERFORM pg_temp.run_preventive(v_horse, v_vet, COALESCE(pg_temp.ts(a.manager_at), pg_temp.clock()));
        END IF;

        -- 6. Manager quyết định. (Manager decision.)
        IF a.manager_at IS NOT NULL THEN
            v_t := pg_temp.ts(a.manager_at);
            IF a.manager_decision = 'APPROVED' THEN
                UPDATE stable_stalls SET status = 'OCCUPIED', updated_at = v_t WHERE id = pg_temp.stall(a.regular_stall);
                UPDATE stable_stalls SET status = 'AVAILABLE', updated_at = v_t WHERE id = v_q;
                UPDATE horses SET current_status = 'ELIGIBLE', current_stall_id = pg_temp.stall(a.regular_stall), updated_at = v_t
                 WHERE id = v_horse;
                UPDATE admission_applications
                   SET manager_id = v_manager, manager_decision = 'APPROVED', manager_reviewed_at = v_t, status = 'APPROVED',
                       updated_at = v_t,
                       manager_feedback = (ARRAY['Dossier, intake examination and trainer assessment reviewed; admitted to the regular stable.',
                                                 'Approved. Owner informed; the horse moves to its regular stall today.',
                                                 'Approved on the trainer''s recommendation; review progress after the first training block.'])
                                          [pg_temp.rndi('mf' || a.k, 1, 3)]
                 WHERE id = v_adm;
                PERFORM pg_temp.upsert_milestone(v_horse, 'preventive:approval:' || v_adm, v_manager, v_t::DATE + 1, v_t);
                PERFORM pg_temp.audit(v_manager, 'Manager Admission Approval', 'AdmissionApplication', v_adm,
                                      'Club Manager approved application #' || v_adm || ' and allocated regular stable stall',
                                      v_t, 'POST', '/api/admissions/' || v_adm || '/manager-review');
            ELSE
                IF EXISTS (SELECT 1 FROM care_schedule WHERE horse_id = v_horse AND status IN ('REQUESTED', 'IN_PROGRESS')) THEN
                    RAISE EXCEPTION 'Admission % cannot be rejected while veterinary care is REQUESTED or IN_PROGRESS', a.k;
                END IF;
                UPDATE care_schedule SET status = 'CANCELLED', cancel_reason = 'Admission rejected by manager', updated_at = v_t
                 WHERE horse_id = v_horse AND status = 'SCHEDULED';
                UPDATE stable_stalls SET status = 'AVAILABLE', updated_at = v_t WHERE id = v_q;
                UPDATE horses SET current_status = 'REJECTED', current_stall_id = NULL, updated_at = v_t WHERE id = v_horse;
                UPDATE admission_applications
                   SET manager_id = v_manager, manager_decision = 'REJECTED', manager_reviewed_at = v_t, status = 'REJECTED',
                       updated_at = v_t,
                       manager_feedback = 'Rejected: the trainer assessed the horse as unsuitable for racing (temperament and an irregular hind stride). The owner may apply again after a period of schooling.'
                 WHERE id = v_adm;
                PERFORM pg_temp.audit(v_manager, 'Manager Admission Approval', 'AdmissionApplication', v_adm,
                                      'Club Manager approved application #' || v_adm || ' and allocated regular stable stall',
                                      v_t, 'POST', '/api/admissions/' || v_adm || '/manager-review');
            END IF;
        END IF;
    END LOOP;
END $$;
