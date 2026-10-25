-- =====================================================================
-- SEED VET DEMO DATA (Idempotent & Dynamic - Leader Compliant)
-- =====================================================================
-- Quy tac cua Leader:
-- 1. Chi ghi phan nghiep vu Vet:
--    health_records, care_schedule (ROUTINE/URGENT), horse_health_metrics,
--    treatment_plans, prescriptions, injury_records (+ quyet dinh training
--    decision cua ngua dang bi BLOCKED)
-- 2. Tim nhan vien bang EMAIL (vet1/vet2/vet3@rtms.com)
-- 3. Tim ngua bang UELN (horses.registration_number)
-- 4. Tim body region bang CODE (horse_body_regions.code)
-- 5. Tuyet doi KHONG dung ID so cung (local ID != cloud ID)
-- 6. Idempotent: chi xoa du lieu cua chinh script nay (marker '[VET-DEMO]')
--    truoc khi nap lai -> chay lai khong trung, khong dot pha du lieu nen
-- 7. Khong cham vao 324 file nen, admission, preventive_protocols,
--    veterinarian_profiles, health_records/care_schedule co san cua base.
-- =====================================================================

DO $$
DECLARE
    -- Vet IDs (resolved via email)
    v_vet1 BIGINT;
    v_vet2 BIGINT;
    v_vet3 BIGINT;

    -- Horses resolved via UELN, GIU NGUYEN THU TU nay khi tra ve
    v_uelns TEXT[] := ARRAY[
        '380005278762021',  -- 1  NAUSICAA BY NIKA   (A)
        '380005253762019',  -- 2  EAGLE ATENA        (A)
        '380005301322023',  -- 3  SI RENESME         (A)
        '380005286802022',  -- 4  ENUMA              (A)
        '380005286322022',  -- 5  FC NAJAA           (B)
        '380005245932018',  -- 6  CHILIVANESU        (B)
        '380005245662018',  -- 7  BADEA MAFAL        (B)
        '380005254462019',  -- 8  TO BE OR NOT TO BE (B)
        '380005280922020',  -- 9  EL VALENTINO       (C)
        '380005249422019',  -- 10 GOLD               (C)
        '380005255772019',  -- 11 DAENERYS           (D)
        '380005252372019'   -- 12 GRESIA             (D)
    ];
    v_horses BIGINT[];
    v_sched  BIGINT[];
    v_hr     BIGINT[];

    -- Body regions (resolved via code)
    v_br_foreleg_left  BIGINT;
    v_br_foreleg_right BIGINT;
    v_br_hindleg_right BIGINT;
    v_br_back          BIGINT;
    v_br_neck          BIGINT;

    v_base  DATE       := (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date;
    v_now   TIMESTAMP  := (now() AT TIME ZONE 'Asia/Ho_Chi_Minh');

    -- care_schedule ids (RETURNING)
    v_care_rout    BIGINT[];
    v_care_urg     BIGINT[];

    -- health_record ids (RETURNING)
    v_hr_rout  BIGINT[];
    v_hr_ill   BIGINT[];
    v_hr_urg   BIGINT[];

    v_i INT;
    v_vet BIGINT;
    v_id BIGINT;
BEGIN
    -- 1. RESOLVE STAFF BY EMAIL (no hardcoded IDs)
    SELECT id INTO v_vet1 FROM users WHERE email = 'vet1@rtms.com';
    SELECT id INTO v_vet2 FROM users WHERE email = 'vet2@rtms.com';
    SELECT id INTO v_vet3 FROM users WHERE email = 'vet3@rtms.com';
    IF v_vet1 IS NULL OR v_vet2 IS NULL OR v_vet3 IS NULL THEN
        RAISE EXCEPTION 'Missing vet accounts (vet1/vet2/vet3@rtms.com)! Run baseline seed first.';
    END IF;

    -- 2. RESOLVE HORSES BY UELN (keeps array order = comment order above)
    SELECT array_agg(h.id ORDER BY array_position(v_uelns, h.registration_number))
      INTO v_horses
      FROM horses h
     WHERE h.registration_number = ANY (v_uelns);
    IF v_horses IS NULL OR array_length(v_horses, 1) <> 12 THEN
        RAISE EXCEPTION 'Only % of 12 demo UELNs found! Run baseline seed first.',
            CASE WHEN v_horses IS NULL THEN 0 ELSE array_length(v_horses, 1) END;
    END IF;

    -- 3. RESOLVE BODY REGIONS BY CODE
    SELECT id INTO v_br_foreleg_left  FROM horse_body_regions WHERE code = 'FORELEG_LEFT';
    SELECT id INTO v_br_foreleg_right FROM horse_body_regions WHERE code = 'FORELEG_RIGHT';
    SELECT id INTO v_br_hindleg_right FROM horse_body_regions WHERE code = 'HINDLEG_RIGHT';
    SELECT id INTO v_br_back          FROM horse_body_regions WHERE code = 'BACK';
    SELECT id INTO v_br_neck          FROM horse_body_regions WHERE code = 'NECK';
    IF v_br_foreleg_left IS NULL OR v_br_foreleg_right IS NULL
       OR v_br_hindleg_right IS NULL OR v_br_back IS NULL OR v_br_neck IS NULL THEN
        RAISE EXCEPTION 'horse_body_regions codes missing! Run baseline seed first.';
    END IF;

    -- 4. IDEMPOTENT CLEANUP: xoa du lieu cua script nay (marker [VET-DEMO]) va moi dong phat sinh tu no
    -- (care_schedule con qua source_schedule_id, health_records / incident reports do app tao tu lich demo).
    WITH RECURSIVE tree AS (
        SELECT id FROM care_schedule WHERE description LIKE '[VET-DEMO]%'
        UNION
        SELECT c.id FROM care_schedule c JOIN tree t ON c.source_schedule_id = t.id
    )
    SELECT array_agg(id) INTO v_sched FROM tree;
    v_sched := COALESCE(v_sched, ARRAY[]::BIGINT[]);

    SELECT COALESCE(array_agg(id), ARRAY[]::BIGINT[]) INTO v_hr
      FROM health_records
     WHERE notes LIKE '[VET-DEMO]%' OR care_schedule_id = ANY (v_sched);

    DELETE FROM prescriptions WHERE instructions LIKE '[VET-DEMO]%'
        OR treatment_plan_id IN (SELECT id FROM treatment_plans
                                  WHERE instructions LIKE '[VET-DEMO]%' OR health_record_id = ANY (v_hr));
    DELETE FROM treatment_plans   WHERE instructions LIKE '[VET-DEMO]%' OR health_record_id = ANY (v_hr);
    DELETE FROM injury_records    WHERE description  LIKE '[VET-DEMO]%' OR health_record_id = ANY (v_hr);
    DELETE FROM horse_health_metrics WHERE notes     LIKE '[VET-DEMO]%';
    DELETE FROM horse_incident_reports WHERE care_schedule_id = ANY (v_sched);
    DELETE FROM health_records    WHERE id = ANY (v_hr);
    DELETE FROM care_schedule     WHERE id = ANY (v_sched);

    -- ================================================================
    -- 5. care_schedule: 10 ROUTINE/FOLLOW_UP + 6 URGENT/FOLLOW_UP
    -- ================================================================
    -- 5a. 5 ROUTINE da hoan thanh (khoa 1-2, 5-6, 9) --------------------------------
    FOR v_i IN 1..5 LOOP
        v_vet := CASE (v_i % 3) WHEN 0 THEN v_vet1 WHEN 1 THEN v_vet2 ELSE v_vet3 END;
        INSERT INTO care_schedule
            (horse_id, veterinarian_id, care_type, status, purpose, scheduled_at,
             duration_minutes, description, completed_at, created_at, updated_at)
        VALUES
            (v_horses[CASE v_i WHEN 1 THEN 1 WHEN 2 THEN 2 WHEN 3 THEN 5 WHEN 4 THEN 6 ELSE 9 END],
             v_vet, 'ROUTINE', 'COMPLETED', 'FOLLOW_UP',
             (v_base - (7 - v_i)) + TIME '09:00', 40,
             '[VET-DEMO] Khám định kỳ tổng quát theo kế hoạch',
             (v_base - (7 - v_i)) + TIME '09:40', v_now, v_now)
        RETURNING id INTO v_id;
        v_care_rout[v_i] := v_id;
    END LOOP;

    -- 5b. 1 ROUTINE tuong lai (ngua 10)
    INSERT INTO care_schedule
        (horse_id, veterinarian_id, care_type, status, purpose, scheduled_at,
         duration_minutes, description, created_at, updated_at)
    VALUES
        (v_horses[10], v_vet2, 'ROUTINE', 'SCHEDULED', 'FOLLOW_UP',
         (v_base + 3) + TIME '14:00', 45,
         '[VET-DEMO] Tái khám theo dõi thể lực định kỳ', v_now, v_now);

    -- 5c. 4 ROUTINE tai khoan cho ngua BLOCKED (ngua 3, 4, 7, 11)
    FOR v_i IN 1..4 LOOP
        v_vet := CASE (v_i % 3) WHEN 0 THEN v_vet1 WHEN 1 THEN v_vet2 ELSE v_vet3 END;
        INSERT INTO care_schedule
            (horse_id, veterinarian_id, care_type, status, purpose, scheduled_at,
             duration_minutes, description, created_at, updated_at)
        VALUES
            (v_horses[CASE v_i WHEN 1 THEN 3 WHEN 2 THEN 4 WHEN 3 THEN 7 ELSE 11 END],
             v_vet, 'ROUTINE', 'SCHEDULED', 'FOLLOW_UP',
             (v_base + (v_i + 1)) + TIME '10:30', 30,
             '[VET-DEMO] Tái khám sau thời gian nghỉ tập (BLOCKED)', v_now, v_now);
    END LOOP;

    -- 5d. 6 URGENT: 2 COMPLETED, 2 IN_PROGRESS, 2 SCHEDULED (ngua 8, 12 / 2, 6 / 5, 10)
    INSERT INTO care_schedule
        (horse_id, veterinarian_id, care_type, status, purpose, scheduled_at,
         duration_minutes, description, completed_at, created_at, updated_at)
    VALUES
        (v_horses[8],  v_vet1, 'URGENT', 'COMPLETED', 'FOLLOW_UP',
         (v_base - 2) + TIME '16:00', 30, '[VET-DEMO] Cấp cứu tại chỗ — đã xử trí',
         (v_base - 2) + TIME '16:30', v_now, v_now),
        (v_horses[12], v_vet3, 'URGENT', 'COMPLETED', 'FOLLOW_UP',
         (v_base - 1) + TIME '15:30', 30, '[VET-DEMO] Cấp cứu tại chỗ — đã xử trí',
         (v_base - 1) + TIME '16:05', v_now, v_now);

    SELECT array_agg(id ORDER BY id) INTO v_care_urg
      FROM care_schedule
     WHERE description LIKE '[VET-DEMO]%' AND care_type = 'URGENT' AND status = 'COMPLETED';

    INSERT INTO care_schedule
        (horse_id, veterinarian_id, care_type, status, purpose, scheduled_at,
         duration_minutes, description, created_at, updated_at)
    VALUES
        (v_horses[2],  v_vet2, 'URGENT', 'IN_PROGRESS', 'FOLLOW_UP',
         v_base + TIME '08:30', 30, '[VET-DEMO] Đang theo dõi cấp cứu', v_now, v_now),
        (v_horses[6],  v_vet1, 'URGENT', 'IN_PROGRESS', 'FOLLOW_UP',
         v_base + TIME '10:00', 30, '[VET-DEMO] Đang theo dõi cấp cứu', v_now, v_now),
        (v_horses[5],  v_vet3, 'URGENT', 'SCHEDULED', 'FOLLOW_UP',
         (v_base + 1) + TIME '08:00', 30, '[VET-DEMO] Lịch cấp cứu dự kiến', v_now, v_now),
        (v_horses[10], v_vet2, 'URGENT', 'SCHEDULED', 'FOLLOW_UP',
         (v_base + 1) + TIME '11:00', 30, '[VET-DEMO] Lịch cấp cứu dự kiến', v_now, v_now);

    -- ================================================================
    -- 6. health_records: 5 ROUTINE + 5 ILLNESS + 4 URGENT = 14
    -- ================================================================
    -- 6a. 5 ROUTINE lien ket care_schedule da hoan thanh
    FOR v_i IN 1..5 LOOP
        v_vet := CASE (v_i % 3) WHEN 0 THEN v_vet1 WHEN 1 THEN v_vet2 ELSE v_vet3 END;
        INSERT INTO health_records
            (horse_id, veterinarian_id, record_type, examined_at,
             symptoms, findings, diagnosis, treatment, notes,
             training_decision, care_schedule_id, created_at, updated_at)
        VALUES
            (v_horses[CASE v_i WHEN 1 THEN 1 WHEN 2 THEN 2 WHEN 3 THEN 5 WHEN 4 THEN 6 ELSE 9 END],
             v_vet, 'ROUTINE',
             (v_base - (7 - v_i)) + TIME '09:40',
             'Không có triệu chứng bất thường',
             'Tim mạch, hô hấp, tiêu hoá, cơ xương khớp: bình thường. Cân nặng ổn định.',
             'Khỏe mạnh, đủ điều kiện tập luyện theo kế hoạch',
             'Không chỉ định điều trị. Duy trì dinh dưỡng và lịch tập hiện tại.',
             '[VET-DEMO] Khám định kỳ tổng quát',
             'ALLOWED', v_care_rout[v_i], v_now, v_now)
        RETURNING id INTO v_id;
        v_hr_rout[v_i] := v_id;
    END LOOP;

    -- 6b. 5 ILLNESS doc lap: 4 BLOCKED (ngua 3, 4, 7, 11) + 1 ALLOWED (ngua 2)
    INSERT INTO health_records
        (horse_id, veterinarian_id, record_type, examined_at,
         symptoms, findings, diagnosis, treatment, notes,
         training_decision, restriction_details, created_at, updated_at)
    VALUES
        (v_horses[3], v_vet1, 'ILLNESS', (v_base - 4) + TIME '10:00',
         'Sốt nhẹ 38.9°C, chán ăn, phân lỏng',
         'Niêm mạc nhợt, tiếng ruột tăng, không có dấu hiệu đau bụng dữ dội',
         'Viêm dạ dày tá tràng',
         'Điều trị kháng acid + bảo hộ niêm mạc, tái khám sau 7 ngày',
         '[VET-DEMO] Bệnh án nghỉ tập',
         'BLOCKED', 'Nghỉ tập hoàn toàn 10 ngày, chỉ thả rãnh nhẹ 30 phút/ngày. Tái khám trước khi tập lại.',
         v_now, v_now),
        (v_horses[4], v_vet2, 'ILLNESS', (v_base - 5) + TIME '14:30',
         'Ho, sổ mũi, sốt 38.6°C sau chuyến vận chuyển',
         'Niêm mạc mũi đỏ, phổi rõ, không có tiếng rên bất thường',
         'Nhiễm khuẩn đường hô hấp trên',
         'Kháng sinh 7 ngày, hạ sốt, giữ chuồng ấm',
         '[VET-DEMO] Bệnh án nghỉ tập',
         'BLOCKED', 'Nghỉ tập 7 ngày, theo dõi thân nhiệt 2 lần/ngày. Tái khám ngày thứ 7.',
         v_now, v_now),
        (v_horses[7], v_vet3, 'ILLNESS', (v_base - 3) + TIME '08:45',
         'Đau quặn bụng tái diễn, bồn chồn, kém ăn',
         'Tiếng ruột giảm, mồ hôi dính, mạch 52 nhịp/phút',
         'Colic spasmodic mức nhẹ',
         'Hạ co thắt, bôi trơn đường ruột, theo dõi 24 giờ',
         '[VET-DEMO] Bệnh án nghỉ tập',
         'BLOCKED', 'Nghỉ tập 5 ngày, kiêng cỏ khô và tinh bột 48 giờ đầu. Tái khám khi hết đau.',
         v_now, v_now),
        (v_horses[11], v_vet1, 'ILLNESS', (v_base - 6) + TIME '11:15',
         'Nhận thấy đi khập khiễng nhẹ sau tập, sưng nhẹ kẽ móng trái',
         'Kẽ móng trái nóng, đau khi ấn, không có mủ',
         'Viêm kẽ móng trái',
         'Rửa sát trùng, đắp bùn y tế, nghỉ chuồng cứng 10 ngày',
         '[VET-DEMO] Bệnh án nghỉ tập',
         'BLOCKED', 'Nghỉ tập 14 ngày, đi kẽ móng và kiểm tra mỗi 3 ngày. Tái khám trước khi tập nặng.',
         v_now, v_now),
        (v_horses[2], v_vet2, 'ILLNESS', (v_base - 10) + TIME '09:30',
         'Sốt nhẹ qua ngày, đã ăn uống bình phục',
         'Toàn trạng tốt, không còn sốt sau 24 giờ',
         'Sốt không rõ nguyên nhân, đã tự hết',
         'Theo dõi, không dùng kháng sinh',
         '[VET-DEMO] Bệnh án theo dõi',
         'ALLOWED', NULL, v_now, v_now);

    SELECT array_agg(id ORDER BY id) INTO v_hr_ill
      FROM health_records
     WHERE notes LIKE '[VET-DEMO]%' AND record_type = 'ILLNESS';

    -- 6c. 2 URGENT lien ket care completed (ngua 8, 12) + 2 URGENT doc lap (ngua 1, 5)
    INSERT INTO health_records
        (horse_id, veterinarian_id, record_type, examined_at,
         symptoms, findings, diagnosis, treatment, notes,
         training_decision, care_schedule_id, created_at, updated_at)
    VALUES
        (v_horses[8], v_vet1, 'URGENT', (v_base - 2) + TIME '16:30',
         'Chạy tập thì vấp ngã, xây xát đầu gối phải',
         'Vết thương da nông ở đầu gối phải, không ảnh hưởng khớp',
         'Vết thương ngoài da đầu gối phải',
         'Rửa sát trùng, băng vô trùng, kháng sinh dự phòng 3 ngày',
         '[VET-DEMO] Hồ sơ cấp cứu',
         'ALLOWED', v_care_urg[1], v_now, v_now),
        (v_horses[12], v_vet3, 'URGENT', (v_base - 1) + TIME '16:05',
         'Tiêu chảy cấp sau thay đổi thức ăn',
         'Mất nước độ I, niêm mạc khô nhẹ, không có dấu chướng bụng',
         'Tiêu chảy cấp do đổi thức ăn',
         'Bù nước điện giải đường uống, kiêng tinh bột 24 giờ',
         '[VET-DEMO] Hồ sơ cấp cứu',
         'ALLOWED', v_care_urg[2], v_now, v_now);

    SELECT array_agg(id ORDER BY id) INTO v_hr_urg
      FROM health_records
     WHERE notes LIKE '[VET-DEMO]%' AND record_type = 'URGENT' AND care_schedule_id IS NOT NULL;

    INSERT INTO health_records
        (horse_id, veterinarian_id, record_type, examined_at,
         symptoms, findings, diagnosis, treatment, notes,
         training_decision, created_at, updated_at)
    VALUES
        (v_horses[1], v_vet2, 'URGENT', (v_base - 8) + TIME '17:10',
         'Khó thở, ho khan ngay sau buổi tập nặng',
         'Tăng nhịp thở, phổi thông thoáng, không có sốt',
         'Co thắt phổi do gắng sức (faint related)',
         'Nghỉ ngơi mát, xịt giãn phế quản, theo dõi 48 giờ',
         '[VET-DEMO] Hồ sơ cấp cứu tại chỗ',
         'ALLOWED', v_now, v_now),
        (v_horses[5], v_vet1, 'URGENT', (v_base - 9) + TIME '15:40',
         'Phù cổ dưới mức nhẹ cuối buổi tập',
         'Phù giãn thấu dưới, mạch bẹn bình thường',
         'Phù bạch huyết do đứng lâu',
         'Điều chỉnh chuồng đệm, tập luyện kéo dài tăng dần',
         '[VET-DEMO] Hồ sơ cấp cứu tại chỗ',
         'ALLOWED', v_now, v_now);

    -- v_hr_urg chi co 2 dong; gan 2 dong doc lap vao bien rieng
    -- (khong can ket noi gi them)

    -- ================================================================
    -- 7. horse_health_metrics: 9 ngua x 5 lan do = 45 (ngua 1,2,3,4,5,6,7,9,11)
    -- ================================================================
    FOR v_i IN 1..45 LOOP
        INSERT INTO horse_health_metrics
            (horse_id, recorded_at, heart_rate, temperature, weight,
             respiratory_rate, hydration_status, body_condition_score,
             notes, created_at)
        SELECT
            v_horses[CASE ((v_i - 1) / 5) + 1
                         WHEN 1 THEN 1 WHEN 2 THEN 2 WHEN 3 THEN 3 WHEN 4 THEN 4
                         WHEN 5 THEN 5 WHEN 6 THEN 6 WHEN 7 THEN 7
                         WHEN 8 THEN 9 ELSE 11 END],
            (v_base - (8 - ((v_i - 1) % 5) * 2)) + TIME '07:30',
            36 + ((v_i * 3) % 11),
            round((37.4 + ((v_i * 7) % 10) * 0.1)::numeric, 1),
            430 + (((v_i - 1) / 5) + 1) * 6 + ((v_i - 1) % 5) * 2,
            20 + ((v_i * 5) % 9),
            CASE WHEN v_i % 11 = 0 THEN 'Mild dehydration' ELSE 'Normal' END,
            5.5 + ((v_i + ((v_i - 1) / 5)) % 4) * 0.5,
            '[VET-DEMO] Chỉ số sinh hiệu định kỳ',
            v_now;
    END LOOP;

    -- ================================================================
    -- 8. treatment_plans: 8 (5 ACTIVE + 3 COMPLETED)
    -- ================================================================
    -- v_hr_ill = 5 dong: [1]=ngua3 [2]=ngua4 [3]=ngua7 [4]=ngua11 [5]=ngua2
    INSERT INTO treatment_plans
        (health_record_id, treatment_name, instructions, start_date, end_date,
         status, created_at, updated_at)
    VALUES
        (v_hr_ill[1], 'Điều trị viêm dạ dày tá tràng',
         '[VET-DEMO] Uống thuốc đúng giờ, chia bữa nhỏ 4 lần/ngày, theo dõi phân.',
         v_base - 4, v_base + 6, 'ACTIVE', v_now, v_now),
        (v_hr_ill[2], 'Điều trị nhiễm khuẩn hô hấp trên',
         '[VET-DEMO] Kháng sinh đủ 7 ngày, đo thân nhiệt sáng tối, cách ly chuồng ấm.',
         v_base - 5, v_base + 2, 'ACTIVE', v_now, v_now),
        (v_hr_ill[3], 'Điều trị colic spasmodic',
         '[VET-DEMO] Theo dõi nhịn ăn 12 giờ đầu, tái khám ngay nếu tái phát đau.',
         v_base - 3, v_base + 2, 'ACTIVE', v_now, v_now),
        (v_hr_ill[4], 'Điều trị viêm kẽ móng',
         '[VET-DEMO] Sat trùng sáng tối, đi kẽ móng, kiêng tập nặng 14 ngày.',
         v_base - 6, v_base + 8, 'ACTIVE', v_now, v_now),
        (v_hr_urg[1], 'Chăm sóc vết thương đầu gối',
         '[VET-DEMO] Thay băng mỗi 24 giờ, theo dõi dấu hiệu viêm nhiễm 5 ngày.',
         v_base - 2, v_base + 3, 'ACTIVE', v_now, v_now),
        (v_hr_ill[5], 'Theo dõi sốt không nguyên nhân',
         '[VET-DEMO] Đã kết thúc theo dõi, ngựa ăn uống và tập luyện bình thường.',
         v_base - 10, v_base - 3, 'COMPLETED', v_now, v_now),
        (v_hr_rout[1], 'Bổ sung dinh dưỡng định kỳ',
         '[VET-DEMO] Đã hoàn thành đợt bổ sung vitamin và khoáng chất.',
         v_base - 7, v_base - 1, 'COMPLETED', v_now, v_now),
        (v_hr_urg[2], 'Điều trị phù bạch huyết',
         '[VET-DEMO] Đã hết phù, điều chỉnh chuồng đệm xong, tái khám khi cần.',
         v_base - 9, v_base - 2, 'COMPLETED', v_now, v_now);

    -- ================================================================
    -- 9. prescriptions: 10 (4 don cho 4 plan ACTIVE + 2 don cho cac plan con)
    -- ================================================================
    INSERT INTO prescriptions
        (treatment_plan_id, medication_name, dosage, frequency, route,
         start_date, end_date, instructions, created_at, updated_at)
    SELECT tp.id, x.medication, x.dosage, x.frequency, x.route,
           tp.start_date, tp.end_date, x.instructions, v_now, v_now
    FROM (SELECT id, treatment_name, start_date, end_date
            FROM treatment_plans WHERE instructions LIKE '[VET-DEMO]%') tp
    JOIN (VALUES
        ('Điều trị viêm dạ dày tá tràng',        'Omeprazole paste',    '20 mg/kg',  '1 lần/ngày',  'PO',       '[VET-DEMO] Uống sáng sớm trước ăn 30 phút.'),
        ('Điều trị viêm dạ dày tá tràng',        'Sucralfate',          '1 g',       '2 lần/ngày',  'PO',       '[VET-DEMO] Uống sau ăn 1 giờ, cách thuốc khác 2 giờ.'),
        ('Điều trị nhiễm khuẩn hô hấp trên',     'Doxycycline',         '10 mg/kg',  '2 lần/ngày',  'PO',       '[VET-DEMO] Uống sau ăn, theo dõi tiêu hoá.'),
        ('Điều trị nhiễm khuẩn hô hấp trên',     'Vitamin B complex',   '10 mL',     '1 lần/ngày',  'IM',       '[VET-DEMO] Tiêm bắp, đổi bên mỗi ngày.'),
        ('Điều trị colic spasmodic',              'Phenylbutazone',      '4.4 mg/kg', '2 lần/ngày',  'PO',       '[VET-DEMO] Uống sau ăn, tối đa 5 ngày liên tục.'),
        ('Điều trị colic spasmodic',              'Omeprazole paste',    '20 mg/kg',  '1 lần/ngày',  'PO',       '[VET-DEMO] Bảo hộ dạ dày trong thời gian dùng Bute.'),
        ('Điều trị viêm kẽ móng',                 'Gentamicin',          '6.6 mg/kg', '1 lần/ngày',  'IV',       '[VET-DEMO] Tiêm tĩnh mạch chậm, theo dõi thận.'),
        ('Điều trị viêm kẽ móng',                 'Povidone-iodine 10%', 'bôi đủ lượng', '2 lần/ngày', 'TOPICAL','[VET-DEMO] Vệ sinh kẽ móng trước khi bôi.'),
        ('Chăm sóc vết thương đầu gối',           'Meloxicam',           '0.6 mg/kg', '1 lần/ngày',  'PO',       '[VET-DEMO] Giảm đau chống viêm, kèm theo dõi dạ dày.'),
        ('Theo dõi sốt không nguyên nhân',        'Doxycycline',         '10 mg/kg',  '2 lần/ngày',  'PO',       '[VET-DEMO] Đã dùng đủ liệu trình 7 ngày.')
    ) AS x(treatment_name, medication, dosage, frequency, route, instructions)
      ON x.treatment_name = tp.treatment_name;

    -- ================================================================
    -- 10. injury_records: 5 (body region resolve theo CODE)
    -- ================================================================
    INSERT INTO injury_records
        (horse_id, health_record_id, injury_type, body_region_id, severity,
         description, diagnosed_at, expected_recovery_date, recovered_at,
         status, position_x, position_y, position_z, created_at, updated_at)
    VALUES
        (v_horses[4], v_hr_ill[2], 'TENDON', v_br_foreleg_left, 'MODERATE',
         '[VET-DEMO] Viêm gân cơ duỗi ngón chân trái trước',
         (v_base - 6) + TIME '10:00', v_base + 14, NULL,
         'ACTIVE', 0.42, 0.18, 0.55, v_now, v_now),
        (v_horses[7], v_hr_ill[3], 'MUSCLE', v_br_hindleg_right, 'SEVERE',
         '[VET-DEMO] Rách cơ đùi phải độ II, đau khi sờ, sưng nhẹ',
         (v_base - 9) + TIME '15:00', v_base + 21, NULL,
         'ACTIVE', 0.68, 0.35, 0.72, v_now, v_now),
        (v_horses[3], v_hr_ill[1], 'CONTUSION', v_br_back, 'MILD',
         '[VET-DEMO] Bầm tím vùng thắt lưng do cọ yên, đã bình phục',
         (v_base - 20) + TIME '09:00', v_base - 6, (v_base - 6) + TIME '17:00',
         'RECOVERED', 0.50, 0.62, 0.35, v_now, v_now),
        (v_horses[11], v_hr_ill[4], 'JOINT', v_br_neck, 'MILD',
         '[VET-DEMO] Căng cơ nhẹ vùng cổ sau tập, đang theo dõi',
         (v_base - 3) + TIME '11:30', v_base + 10, NULL,
         'ACTIVE', 0.55, 0.80, 0.30, v_now, v_now),
        (v_horses[1], v_hr_rout[1], 'LACERATION', v_br_foreleg_right, 'MODERATE',
         '[VET-DEMO] Vết thương da cẳng chân phải, đã liền sẹo',
         (v_base - 25) + TIME '16:20', v_base - 12, (v_base - 12) + TIME '08:00',
         'RECOVERED', 0.44, 0.20, 0.48, v_now, v_now);

    -- ================================================================
    -- 11. Quyet dinh training cua ngua dang BLOCKED (4 ngua: 3, 4, 7, 11)
    -- ================================================================
    UPDATE horses
       SET training_decision = 'BLOCKED',
           training_decision_reason = '[VET-DEMO] ' || diagnosis
      FROM health_records hr
     WHERE horses.id = hr.horse_id
       AND hr.notes LIKE '[VET-DEMO]%'
       AND hr.training_decision = 'BLOCKED'
       AND horses.registration_number = ANY (v_uelns)
       AND hr.examined_at = (
            SELECT max(hr2.examined_at) FROM health_records hr2
            WHERE hr2.horse_id = hr.horse_id AND hr2.training_decision = 'BLOCKED'
       );

    -- 11b. Chan tap keo theo (giong TrainingDecisionService.block): huy buoi tap chua dien ra, huy lot da rong, huy plan
    -- dang chay. Khong lam buoc nay thi seed de lai ngua BLOCKED van co plan ACTIVE/UPCOMING.
    -- (Cascade of a block, like TrainingDecisionService.block: cancel not-yet-held workouts, cancel emptied lots, cancel
    -- running plans. Without it the seed leaves BLOCKED horses with an ACTIVE/UPCOMING plan.)
    UPDATE training_workouts w
       SET status = 'CANCELLED', updated_at = v_now
      FROM training_lots l, horses h
     WHERE l.id = w.lot_id AND h.id = w.horse_id
       AND h.training_decision = 'BLOCKED'
       AND w.status = 'SCHEDULED' AND l.lot_date >= CURRENT_DATE;

    UPDATE training_lots l
       SET status = 'CANCELLED', updated_at = v_now
     WHERE l.status = 'SCHEDULED'
       AND NOT EXISTS (SELECT 1 FROM training_workouts w WHERE w.lot_id = l.id AND w.status <> 'CANCELLED')
       AND EXISTS (SELECT 1 FROM training_workouts w WHERE w.lot_id = l.id);

    UPDATE horse_training_plans p
       SET status = 'CANCELLED', updated_at = v_now
      FROM horses h
     WHERE h.id = p.horse_id AND h.training_decision = 'BLOCKED'
       AND p.status IN ('ACTIVE', 'UPCOMING');

    RAISE NOTICE '[VET-DEMO] seed complete: 16 care_schedule, 14 health_records, 45 metrics, 8 plans, 10 prescriptions, 5 injuries.';
END;
$$;

-- =====================================================================
-- VERIFICATION
-- =====================================================================
SELECT 'care_schedule'     AS bang, count(*) FILTER (WHERE description LIKE '[VET-DEMO]%')   AS rows_vet_demo FROM care_schedule
UNION ALL SELECT 'health_records',       count(*) FILTER (WHERE notes LIKE '[VET-DEMO]%')        FROM health_records
UNION ALL SELECT 'horse_health_metrics', count(*) FILTER (WHERE notes LIKE '[VET-DEMO]%')        FROM horse_health_metrics
UNION ALL SELECT 'treatment_plans',      count(*) FILTER (WHERE instructions LIKE '[VET-DEMO]%') FROM treatment_plans
UNION ALL SELECT 'prescriptions',        count(*) FILTER (WHERE instructions LIKE '[VET-DEMO]%') FROM prescriptions
UNION ALL SELECT 'injury_records',       count(*) FILTER (WHERE description LIKE '[VET-DEMO]%')  FROM injury_records
UNION ALL SELECT 'horses_BLOCKED',       count(*) FILTER (WHERE training_decision = 'BLOCKED')   FROM horses;

SELECT care_type, status, count(*)
  FROM care_schedule WHERE description LIKE '[VET-DEMO]%'
 GROUP BY 1, 2 ORDER BY 1, 2;
