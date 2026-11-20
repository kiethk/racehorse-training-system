-- =====================================================================
-- 40_vet_cases.sql — các ca Thú y sau nhập trại: sự cố Groom báo, ca khẩn cấp, tạm nghỉ, tái khám, chấn thương,
-- phác đồ điều trị, bệnh án ghi tay và lịch tái khám Thú y tự hẹn (chỉ số sinh hiệu định kỳ: 65_vitals.sql).
-- (Veterinary cases after admission: groom incident reports, urgent cases, rest periods, rechecks, injuries,
-- treatment plans, a hand-written health record and a vet-booked recheck; routine vitals are in 65_vitals.sql.)
--
-- Mọi thời điểm tính theo hôm nay (pg_temp.at(số ngày, giờ)) hoặc theo giờ hiện tại (pg_temp.clock()).
-- Luật đang giữ: mỗi ngựa tối đa một ca khẩn cấp đang mở; chặn tập (BLOCKED) luôn kèm lịch tái khám; ca khẩn cấp
-- xong thì đóng mọi báo cáo sự cố còn mở; lịch Thú y không trùng giờ (vet và ngựa).
-- (Times are relative to today or to now. Rules kept: one open urgent case per horse; BLOCKED always comes with a
-- recheck; closing an urgent case resolves the open incident reports; no overlapping vet slots.)
-- =====================================================================

-- Groom của chuồng ngựa đang ở: chỉ người này báo được sự cố của ngựa (HorseIncidentReportService.assertCanReport).
-- (The groom of the horse's current stall: the only groom who can report an incident for it.)
CREATE FUNCTION pg_temp.stall_groom(p_horse TEXT) RETURNS BIGINT AS $$
    SELECT ss.groom_id FROM horses h JOIN stable_stalls ss ON ss.id = h.current_stall_id WHERE h.id = pg_temp.hid(p_horse)
$$ LANGUAGE sql STABLE;

-- Một ca khẩn cấp từ báo cáo của Groom. p_state: SCHEDULED (đã giao, chưa khám), IN_PROGRESS (đang khám) hoặc
-- COMPLETED (khám xong sau p_minutes phút). Trả về id ca khám. (One urgent case from a groom report.)
CREATE FUNCTION pg_temp.urgent_case(p_horse TEXT, p_at TIMESTAMP, p_title TEXT, p_desc TEXT,
                                    p_severity TEXT, p_vet TEXT, p_state TEXT, p_minutes INT DEFAULT 40,
                                    p_decision TEXT DEFAULT 'ALLOWED', p_symptoms TEXT DEFAULT NULL,
                                    p_findings TEXT DEFAULT NULL, p_diagnosis TEXT DEFAULT NULL, p_treatment TEXT DEFAULT NULL,
                                    p_restriction TEXT DEFAULT NULL, p_next_at TIMESTAMP DEFAULT NULL,
                                    p_temp NUMERIC DEFAULT 37.9, p_hr NUMERIC DEFAULT 42, p_bcs NUMERIC DEFAULT 5.5,
                                    p_note TEXT DEFAULT NULL, p_image TEXT DEFAULT NULL,
                                    p_marker TEXT DEFAULT NULL, p_marker_xyz NUMERIC[] DEFAULT NULL) RETURNS BIGINT AS $$
DECLARE
    v_incident BIGINT;
    v_schedule BIGINT;
BEGIN
    v_incident := pg_temp.report_incident(pg_temp.hid(p_horse), pg_temp.stall_groom(p_horse), p_at, p_title, p_desc, p_severity,
                                          pg_temp.uid(p_vet), p_image);
    SELECT care_schedule_id INTO v_schedule FROM horse_incident_reports WHERE id = v_incident;
    IF p_state IN ('IN_PROGRESS', 'COMPLETED') THEN
        PERFORM pg_temp.start_care(v_schedule);
        UPDATE care_schedule SET updated_at = p_at + INTERVAL '10 minutes' WHERE id = v_schedule;
    END IF;
    IF p_state = 'COMPLETED' THEN
        PERFORM pg_temp.complete_care(v_schedule, p_at + make_interval(mins => p_minutes),
            p_findings => p_findings, p_diagnosis => p_diagnosis, p_decision => p_decision, p_restriction => p_restriction,
            p_treatment => p_treatment, p_symptoms => p_symptoms, p_next_at => p_next_at, p_notes => p_note,
            p_next_desc => 'Recheck after the urgent case', p_temp => p_temp, p_hr => p_hr, p_rr => 16, p_bcs => p_bcs,
            p_weight => (SELECT round(pg_temp.rnd('uw' || horse_id, 410, 480), 1) FROM care_schedule WHERE id = v_schedule),
            p_hydration => 'NORMAL', p_metric_notes => 'Recorded during the urgent examination.',
            p_marker => p_marker, p_marker_xyz => p_marker_xyz);
    END IF;
    RETURN v_schedule;
END $$ LANGUAGE plpgsql;

-- Khám tái khám đang chờ của một ca: xếp giờ rồi khám xong (hoặc để nguyên nếu p_state = 'SCHEDULED').
-- (Book the pending recheck of a visit, then complete it, or leave it booked when p_state = 'SCHEDULED'.)
CREATE FUNCTION pg_temp.recheck(p_source BIGINT, p_vet TEXT, p_state TEXT, p_decision TEXT DEFAULT 'ALLOWED',
                                p_findings TEXT DEFAULT NULL, p_diagnosis TEXT DEFAULT NULL, p_treatment TEXT DEFAULT NULL,
                                p_restriction TEXT DEFAULT NULL, p_next_at TIMESTAMP DEFAULT NULL) RETURNS BIGINT AS $$
DECLARE
    v_child BIGINT := pg_temp.child_of(p_source);
    v_slot TIMESTAMP;
BEGIN
    v_slot := pg_temp.book(v_child, pg_temp.uid(p_vet));
    IF p_state = 'COMPLETED' THEN
        PERFORM pg_temp.complete_care(v_child, v_slot + INTERVAL '25 minutes',
            p_findings => p_findings, p_diagnosis => p_diagnosis, p_decision => p_decision, p_restriction => p_restriction,
            p_treatment => p_treatment, p_next_at => p_next_at, p_temp => 37.7, p_hr => 36, p_rr => 12, p_bcs => 5.5,
            p_weight => (SELECT round(pg_temp.rnd('uw' || horse_id, 410, 480), 1) FROM care_schedule WHERE id = v_child),
            p_hydration => 'NORMAL', p_resolve => (p_decision = 'ALLOWED'));
    END IF;
    RETURN v_child;
END $$ LANGUAGE plpgsql;

CREATE FUNCTION pg_temp.record_of(p_schedule BIGINT) RETURNS BIGINT AS $$
    SELECT id FROM health_records WHERE care_schedule_id = p_schedule
$$ LANGUAGE sql STABLE;

CREATE FUNCTION pg_temp.region(p_code TEXT) RETURNS BIGINT AS $$
    SELECT id FROM horse_body_regions WHERE code = p_code
$$ LANGUAGE sql STABLE;

-- Phác đồ điều trị + đơn thuốc (TreatmentPlanService). p_drugs: mảng [tên, liều, tần suất, đường dùng, ghi chú].
-- (Treatment plan with prescriptions. p_drugs: rows of [name, dosage, frequency, route, instructions].)
CREATE FUNCTION pg_temp.treatment(p_record BIGINT, p_name TEXT, p_instructions TEXT, p_start DATE, p_end DATE,
                                  p_status TEXT, p_drugs TEXT[][]) RETURNS BIGINT AS $$
DECLARE
    v_plan BIGINT;
    v_at TIMESTAMP := (SELECT examined_at FROM health_records WHERE id = p_record) + INTERVAL '5 minutes';
BEGIN
    INSERT INTO treatment_plans (health_record_id, treatment_name, instructions, start_date, end_date, status, created_at, updated_at)
    VALUES (p_record, p_name, p_instructions, p_start, p_end, p_status, v_at, v_at) RETURNING id INTO v_plan;
    FOR i IN 1..COALESCE(array_length(p_drugs, 1), 0) LOOP
        INSERT INTO prescriptions (treatment_plan_id, medication_name, dosage, frequency, route, start_date, end_date,
                                   instructions, created_at, updated_at, status, administered_by_role)
        VALUES (v_plan, p_drugs[i][1], p_drugs[i][2], p_drugs[i][3], p_drugs[i][4], p_start, p_end, p_drugs[i][5], v_at, v_at,
                CASE WHEN p_status = 'ACTIVE' AND (p_end IS NULL OR p_end >= pg_temp.today()) THEN 'ACTIVE' ELSE 'COMPLETED' END,
                CASE WHEN UPPER(p_drugs[i][4]) IN ('IV', 'IM', 'INJECTION_IV', 'INJECTION_IM') THEN 'VETERINARIAN' ELSE 'GROOM' END);
    END LOOP;
    RETURN v_plan;
END $$ LANGUAGE plpgsql;

DO $$
DECLARE
    s BIGINT; r BIGINT; c BIGINT; v_injury BIGINT; v_new BIGINT; v_h RECORD;
BEGIN
    -- 1. EAGLE ATENA (Groom of her stall): khập khiễng sau buổi phi sáng nay, Thú y vet1 đang khám (IN_PROGRESS).
    --    Groom báo thêm lần hai, báo cáo đó gắn vào cùng ca khẩn cấp. Ngựa bị chặn tập, plan đang chạy bị hủy.
    --    (Lame after this morning's gallop; vet1 is examining now. A second report joins the same urgent case.)
    s := pg_temp.urgent_case('H002', pg_temp.clock() - INTERVAL '95 minutes', 'Lame on the right fore after gallop',
                             'Short and uneven on the right fore when walked back from the track, warm over the fetlock.',
                             'TRAUMA', 'VT01', 'IN_PROGRESS', p_image => 'admission-packs/h002-eagle-atena/horse-photo.jpg');
    PERFORM pg_temp.report_incident(pg_temp.hid('H002'), pg_temp.stall_groom('H002'), pg_temp.clock() - INTERVAL '60 minutes',
                                    'Swelling over the right fore fetlock',
                                    'The swelling is larger now and she is reluctant to put full weight on the leg.', 'TRAUMA', NULL,
                                    'admission-packs/h002-eagle-atena/horse-photo.jpg');

    -- 1b. WILD IVANA (Groom of his stall): đau bụng (colic) cấp, vừa báo cáo 25 phút trước, đã gán VT01 (SCHEDULED - Pending Intake).
    --     (Acute colic symptoms reported 25 minutes ago, assigned to VT01 waiting for vet intake.)
    s := pg_temp.report_incident(pg_temp.hid('H008'), pg_temp.stall_groom('H008'), pg_temp.clock() - INTERVAL '25 minutes',
                                 'Acute colic signs - sweating and rolling',
                                 'Sweating profusely, pawing ground and attempting to roll in the stall. Urgent veterinary exam requested.',
                                 'TRAUMA', NULL, 'admission-packs/h008-wild-ivana/horse-photo.jpg', p_auto_assign => FALSE);
    -- Giao ngay cho Thú y đang ít ngựa nhất hôm nay (rankVeterinarians). (Assigned to the vet with the fewest horses today.)
    SELECT u.id INTO r FROM users u JOIN roles ro ON ro.id = u.role_id
     WHERE ro.name = 'VETERINARIAN' AND u.is_active AND NOT pg_temp.vet_busy(u.id, pg_temp.clock() - INTERVAL '25 minutes', 30, NULL)
     ORDER BY (SELECT count(DISTINCT cs.horse_id) FROM care_schedule cs WHERE cs.veterinarian_id = u.id
                  AND cs.scheduled_date = pg_temp.today() AND cs.status IN ('SCHEDULED', 'IN_PROGRESS')), u.id LIMIT 1;
    PERFORM pg_temp.assign((SELECT care_schedule_id FROM horse_incident_reports WHERE id = s), r,
                           pg_temp.clock() - INTERVAL '25 minutes', pg_temp.clock() - INTERVAL '25 minutes');
    UPDATE horse_incident_reports SET status = 'IN_REVIEW', handled_by_id = r, handled_at = pg_temp.clock() - INTERVAL '25 minutes'
     WHERE id = s;

    -- 2. DOUGLAS (Groom of his stall): sưng ống chân sau phải, vừa báo, đã giao Thú y (SCHEDULED, chưa bắt đầu khám).
    --    (Swollen right hind cannon, just reported and assigned, not started yet.)
    s := pg_temp.urgent_case('H005', pg_temp.clock() - INTERVAL '25 minutes', 'Swollen right hind cannon',
                             'Firm swelling on the inside of the right hind cannon noticed during grooming, no lameness at walk.',
                             'TRAUMA', 'VT02', 'SCHEDULED', p_image => 'admission-packs/h005-douglas/horse-photo.jpg');

    -- 3. FC NAJAA (B01): vết rách nông chân trước trái, khám xong cho tập ngay (ALLOWED); chấn thương đã lành.
    --    (Superficial cut on the left fore, cleared to train at once; the injury has healed.)
    s := pg_temp.urgent_case('H010', pg_temp.at(-20, TIME '08:20'), 'Cut on the left fore',
                             'Kicked the stall door, 4 cm cut above the left fore coronet, bleeding stopped.', 'TRAUMA', 'VT01',
                             'COMPLETED', 40, 'ALLOWED', 'Small bleeding cut, sound at walk and trot',
                             'Superficial 4 cm laceration above the coronet band, no joint or tendon involvement.',
                             'Superficial laceration, left fore', 'Wound cleaned and dressed, topical antiseptic for 5 days.',
                             p_note => 'Bandage held and the wound edges are clean, she is weight-bearing comfortably. Review the dressing with the groom tomorrow.',
                             p_next_at => pg_temp.at(-13, TIME '14:00'), p_marker => 'FORELEG_LEFT', p_marker_xyz => ARRAY[0.42, 0.18, 0.55],
                             p_image => 'admission-packs/h010-fc-najaa/horse-photo.jpg');
    r := pg_temp.record_of(s);
    PERFORM pg_temp.treatment(r, 'Wound care, left fore', 'Clean and redress daily, keep the leg dry.',
                              pg_temp.today() - 20, pg_temp.today() - 15, 'COMPLETED',
                              ARRAY[['Povidone-iodine 10%', 'Apply to the wound', 'Twice daily', 'Topical', 'Clean the wound first.'],
                                    ['Silver sulfadiazine cream', 'Thin layer', 'Once daily', 'Topical', 'Under a light dressing.']]);

    -- 4. MYSTICA MAHRIACHI (B05): căng gân chân trước phải -> nghỉ; tái khám lần 1 vẫn nghỉ; lần 2 cho tập lại.
    --    Chấn thương được Thú y hiệu chỉnh (bản cũ SUPERSEDED, có nhật ký). Còn lịch tái khám tự động sắp tới.
    --    (Tendon strain: rest, still resting at the first recheck, cleared at the second. The injury was corrected
    --    by the vet (old row SUPERSEDED, with an audit row). An automatic recheck is still upcoming.)
    s := pg_temp.urgent_case('H014', pg_temp.at(-35, TIME '07:50'), 'Heat in the right fore tendon',
                             'Heat and a soft swelling along the back of the right fore cannon after the canter.', 'TRAUMA',
                             'VT02', 'COMPLETED', 45, 'BLOCKED', 'Heat and swelling along the flexor tendon, lame at trot',
                             'Ultrasound: mild core lesion in the superficial digital flexor tendon, right fore.',
                             'Superficial digital flexor tendon strain, right fore', 'Cold hosing twice daily, support bandage, box rest.',
                             'Tendon strain: box rest and hand walking only.', pg_temp.at(-21, TIME '14:00'), 38.1, 44,
                             p_note => 'Guarded prognosis. The rest window is not negotiable - ultrasound recheck before any fast work.',
                             p_marker => 'FORELEG_RIGHT', p_marker_xyz => ARRAY[0.44, 0.20, 0.48],
                             p_image => 'admission-packs/h014-mystica-mahriachi/horse-photo.jpg');
    r := pg_temp.record_of(s);
    PERFORM pg_temp.treatment(r, 'Tendon rehabilitation', 'Cold hosing 20 minutes twice daily, controlled hand walking from week 2.',
                              pg_temp.today() - 35, pg_temp.today() + 14, 'ACTIVE',
                              ARRAY[['Phenylbutazone', '2.2 mg/kg', 'Twice daily', 'Oral', 'With feed, maximum 5 days.'],
                                    ['Omeprazole paste', '1 mg/kg', 'Once daily', 'Oral', 'Stomach protection while on bute.']]);
    SELECT id INTO v_injury FROM health_conditions WHERE health_record_id = r ORDER BY id LIMIT 1;
    c := pg_temp.recheck(s, 'VT02', 'COMPLETED', 'BLOCKED',
                         'Recheck: swelling reduced, sound at walk, still short at trot.', 'Tendon strain, healing',
                         'Continue the rehabilitation plan.', 'Tendon strain: hand walking only.', pg_temp.at(-7, TIME '14:00'));
    c := pg_temp.recheck(c, 'VT02', 'COMPLETED', 'ALLOWED',
                         'Recheck: no heat or swelling, sound at walk and trot, ultrasound shows good fibre alignment.',
                         'Tendon strain, resolved', 'Return to light work for two weeks before fast work.');
    INSERT INTO health_conditions (horse_id, health_record_id, condition_type, body_region_id, severity, description,
                                expected_recovery_date, status, position_x, position_y, position_z, created_at, updated_at)
    VALUES (pg_temp.hid('H014'), r, 'Tendon strain', pg_temp.region('FORELEG_RIGHT'), 'MILD',
            'Mild core lesion, healing well, monitoring during the return to work.',
            pg_temp.today() + 20, 'MONITORING', 0.4400, 0.2000, 0.4800, pg_temp.at(-7, TIME '15:00'), pg_temp.at(-7, TIME '15:00'))
    RETURNING id INTO v_new;
    UPDATE health_conditions SET status = 'SUPERSEDED', updated_at = pg_temp.at(-7, TIME '15:00') WHERE id = v_injury;
    PERFORM pg_temp.audit(pg_temp.uid('VT02'), 'INJURY_RECORD_CORRECTION: Severity lowered after the ultrasound recheck',
                          'InjuryRecord', v_new, NULL, pg_temp.at(-7, TIME '15:00'), NULL, NULL);
    -- Lịch tái khám tự động sau lần cho tập lại: đã xếp giờ trong tuần tới. (The automatic recheck is booked next week.)
    PERFORM pg_temp.book(pg_temp.child_of(c), pg_temp.uid('VT02'));

    -- 5. GOLD (C02, Trainer trainer2): đau bụng (colic) nhẹ -> nghỉ 1 tuần -> cho tập lại; lịch tái khám tự động sắp tới.
    --    (Mild colic: one week of rest, then cleared; the automatic recheck is upcoming.)
    s := pg_temp.urgent_case('H020', pg_temp.at(-12, TIME '16:40'), 'Rolling and pawing in the stall',
                             'Pawing, looking at her flank and trying to roll after the evening feed.', 'TRAUMA', 'VT02',
                             'COMPLETED', 50, 'BLOCKED', 'Pawing, flank watching, reduced gut sounds',
                             'Heart rate 52 bpm, reduced gut sounds on the right, no reflux, rectal exam normal.',
                             'Mild spasmodic colic', 'Butylscopolamine IV and flunixin, feed withheld 12 hours.',
                             'Colic: no training until the recheck, small soaked feeds.', pg_temp.at(-5, TIME '14:00'), 38.2, 52,
                             p_note => 'Stable after treatment and monitored through the night for recurrence.',
                             p_marker => 'ABDOMEN', p_marker_xyz => ARRAY[0.50, 0.35, 0.40],
                             p_image => 'admission-packs/h020-gold/horse-photo.jpg');
    r := pg_temp.record_of(s);
    PERFORM pg_temp.treatment(r, 'Colic management', 'Withhold feed 12 hours, then small soaked feeds, hand walk 10 minutes every 2 hours.',
                              pg_temp.today() - 12, pg_temp.today() - 9, 'COMPLETED',
                              ARRAY[['Flunixin Meglumine paste', '1.1 mg/kg', 'Once daily', 'ORAL', 'Oral paste given before feed. For 2 days.']]);
    c := pg_temp.recheck(s, 'VT02', 'COMPLETED', 'ALLOWED', 'Recheck: eating well, normal droppings, gut sounds normal.',
                         'Colic resolved', 'Back to normal feeding, light work for three days.');
    PERFORM pg_temp.book(pg_temp.child_of(c), pg_temp.uid('VT02'));

    -- 6. BV KAROL (D04): ho, chảy mũi -> đang nghỉ, tái khám trong vài ngày tới, phác đồ kháng sinh đang dùng.
    --    (Cough and nasal discharge: resting now, recheck in a few days, antibiotics running.)
    s := pg_temp.urgent_case('H031', pg_temp.at(-3, TIME '08:10'), 'Coughing and nasal discharge',
                             'Coughing during the warm-up and thick nasal discharge from both nostrils.', 'TRAUMA', 'VT03',
                             'COMPLETED', 40, 'BLOCKED', 'Cough, bilateral mucopurulent nasal discharge, temperature 38.9 °C',
                             'Enlarged submandibular lymph nodes, harsh lung sounds cranially.',
                             'Upper respiratory tract infection', 'Antibiotics for 7 days, stable in a well-ventilated box.',
                             'Respiratory infection: rest until the recheck, take the temperature twice daily.',
                             pg_temp.at(4, TIME '14:00'), 38.9, 46,
                             p_note => 'She is bright and eating well, an early antibiotic response is expected. Recheck before returning to work.',
                             p_marker => 'HEAD', p_marker_xyz => ARRAY[0.30, 0.45, 0.55],
                             p_image => 'admission-packs/h031-bv-karol/horse-photo.jpg');
    r := pg_temp.record_of(s);
    PERFORM pg_temp.treatment(r, 'Respiratory infection treatment', 'Antibiotic course with feed supplement. Follow the dosage and notes for each medication.',
                              pg_temp.today() - 3, pg_temp.today() + 4, 'ACTIVE',
                              ARRAY[['Doxycycline', '10 mg/kg', 'Twice daily', 'Oral', 'After feed.'],
                                    ['Vitamin B Complex', '10 mL', 'Once daily', 'FEED_ADDITIVE', 'Mix into the morning feed.']]);
    PERFORM pg_temp.recheck(s, 'VT03', 'SCHEDULED');

    -- 7. SANTIAGO EGA (E02, Trainer trainer3): áp xe móng -> nghỉ; lịch tái khám đã quá giờ mà chưa khám (quá hạn).
    --    (Hoof abscess: resting; the recheck time has passed without the visit, so it shows as overdue.)
    s := pg_temp.urgent_case('H038', pg_temp.at(-10, TIME '07:30'), 'Sudden lameness, right fore',
                             'Will not put weight on the right fore this morning, foot is warm.', 'TRAUMA', 'VT03',
                             'COMPLETED', 45, 'BLOCKED', 'Severe lameness, strong digital pulse',
                             'Hoof testers positive at the toe, abscess opened and drained.', 'Subsolar hoof abscess, right fore',
                             'Poultice and hoof boot, tetanus status checked.', 'Hoof abscess: box rest, daily poultice.',
                             pg_temp.at(-2, TIME '14:00'), 38.0, 48,
                             p_note => 'Drainage is good and the coronary band is cool, lameness to be re-checked at the next visit.',
                             p_marker => 'FORELEG_RIGHT', p_marker_xyz => ARRAY[0.45, 0.05, 0.52],
                             p_image => 'admission-packs/h038-santiago-ega/horse-photo.jpg');
    r := pg_temp.record_of(s);
    PERFORM pg_temp.treatment(r, 'Hoof abscess care', 'Poultice daily for 5 days, then keep the foot dry and padded.',
                              pg_temp.today() - 10, NULL, 'ACTIVE',
                              ARRAY[['Magnesium sulphate poultice', 'One poultice', 'Once daily', 'Topical', 'Change daily.']]);
    PERFORM pg_temp.recheck(s, 'VT03', 'SCHEDULED');

    -- 8. AG NOBEL EMIR (A07): xây xát nhẹ, Groom báo hai lần. Như trên app, báo cáo thứ hai gắn vào ca đang mở và
    --    theo Vet của ca; khám xong thì cả hai báo cáo cùng RESOLVED.
    --    (A scrape reported twice: like the app, the second report joins the open case and both resolve with it.)
    s := pg_temp.urgent_case('H007', pg_temp.at(-15, TIME '10:20'), 'Scrape on the left hip',
                             'Hair rubbed off and a small scrape on the left hip, probably from the stall wall.', 'TRAUMA', 'VT01',
                             'SCHEDULED', p_image => 'admission-packs/h007-ag-nobel-emir/horse-photo.jpg');
    PERFORM pg_temp.report_incident(pg_temp.hid('H007'), pg_temp.stall_groom('H007'), pg_temp.at(-15, TIME '10:35'),
                                    'Scrape on the left hip', 'Same scrape on the left hip, it looks a little deeper now.',
                                    'TRAUMA', NULL);
    PERFORM pg_temp.complete_care(s, pg_temp.at(-15, TIME '11:00'),
        p_findings => 'Superficial abrasion 3 x 2 cm on the left hip, no swelling, sound.', p_diagnosis => 'Skin abrasion',
        p_decision => 'ALLOWED', p_treatment => 'Cleaned, antiseptic spray.', p_symptoms => 'Small scrape, no lameness',
        p_notes => 'Simple stall rest not needed, the skin will settle with the spray.',
        p_temp => 37.6, p_hr => 34, p_rr => 12, p_bcs => 5.5,
        p_weight => (SELECT round(pg_temp.rnd('uw' || horse_id, 410, 480), 1) FROM care_schedule WHERE id = s),
        p_hydration => 'NORMAL', p_metric_notes => 'Recorded during the urgent examination.',
        p_next_at => pg_temp.at(-8, TIME '14:00'), p_marker => 'HINDLEG_LEFT', p_marker_xyz => ARRAY[0.65, 0.40, 0.45]);
    r := pg_temp.record_of(s);

    -- 9. D ADOOB (C08): sự cố cũ hai tháng trước, mắt chảy nước, khám xong cho tập. (Two-month-old case: watery eye.)
    s := pg_temp.urgent_case('H026', pg_temp.at(-60, TIME '09:10'), 'Watery left eye',
                             'Left eye half closed and watering after turnout.', 'TRAUMA', 'VT02', 'COMPLETED', 35, 'ALLOWED',
                             'Squinting, tear staining', 'Fluorescein stain negative, mild conjunctivitis.', 'Conjunctivitis, left eye',
                             'Eye ointment for 5 days, fly mask at turnout.',
                             p_note => 'No corneal involvement, expect full recovery on the ointment.',
                             p_next_at => pg_temp.at(-53, TIME '14:00'), p_marker => 'EYE', p_marker_xyz => ARRAY[0.22, 0.45, 0.60],
                             p_image => 'admission-packs/h026-d-adoob/horse-photo.jpg');
    PERFORM pg_temp.treatment(pg_temp.record_of(s), 'Eye treatment', 'Apply the ointment after cleaning the eye.',
                              pg_temp.today() - 60, pg_temp.today() - 55, 'COMPLETED',
                              ARRAY[['Chloramphenicol eye ointment', '1 cm strip', 'Three times daily', 'Topical', 'Wash hands after use.']]);

    -- 10. SI RENESME (Thú y ghi tay): bệnh án Thú y ghi tay khi đi buồng (nấm da nhẹ), không đổi quyết định tập luyện.
    --     (A health record written by the vet on the stable round: mild skin fungus, no change to training.)
    INSERT INTO health_records (horse_id, veterinarian_id, examined_at, findings, diagnosis, notes, created_at,
                                updated_at, record_type, treatment)
    VALUES (pg_temp.hid('H003'), pg_temp.uid('VT01'), pg_temp.at(-6, TIME '15:40'),
            'Several round, scaly patches on the left side of the neck, no itching. (Symptoms: Patchy hair loss on the neck)', 'Dermatophytosis (ringworm), mild',
            'Seen on the afternoon stable round. Keep her tack and brushes separate.', pg_temp.at(-6, TIME '15:40'),
            pg_temp.at(-6, TIME '15:40'), 'ILLNESS', 'Antifungal wash every 3 days for 2 weeks.')
    RETURNING id INTO r;
    PERFORM pg_temp.treatment(r, 'Antifungal wash', 'Wash the patches every 3 days, disinfect grooming kit weekly.',
                              pg_temp.today() - 6, pg_temp.today() + 8, 'ACTIVE',
                              ARRAY[['Enilconazole 0.2% wash', 'Diluted 1:50', 'Every 3 days', 'Topical', 'Leave on, do not rinse.']]);
    -- Một phác đồ bị hủy vì đổi thuốc. (A plan cancelled because the drug was changed.)
    PERFORM pg_temp.treatment(r, 'Topical antifungal cream', 'Replaced by the wash: patches too widespread for cream.',
                              pg_temp.today() - 6, pg_temp.today() - 5, 'CANCELLED',
                              ARRAY[['Miconazole 2% cream', 'Thin layer', 'Twice daily', 'Topical', 'Stopped after one day.']]);

    -- Mọi lịch tái khám còn lại chạy tiếp theo chuỗi +14 ngày tới hiện tại; ngựa H038 vẫn phải nghỉ.
    -- (Every remaining follow-up runs on its +14-day chain up to now; H038 stays resting.)
    FOR v_h IN SELECT x.id, x.k, COALESCE((SELECT a.veterinarian_id FROM admission_applications a
                                              WHERE a.horse_id = x.id AND a.veterinarian_id IS NOT NULL ORDER BY a.id DESC LIMIT 1),
                                             pg_temp.uid('VT01')) AS vet
                 FROM seed_horse_id x JOIN horses hs ON hs.id = x.id WHERE hs.current_status IN ('ELIGIBLE', 'CANDIDATE') ORDER BY x.k LOOP
        PERFORM pg_temp.run_rechecks(v_h.id, v_h.vet, pg_temp.clock(), v_h.k = 'H038');
    END LOOP;
END $$;
