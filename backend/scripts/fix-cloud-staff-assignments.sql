-- =====================================================================
-- FIX STAFF ASSIGNMENTS ON NEON CLOUD (Idempotent & Dynamic)
-- =====================================================================
-- Mục tiêu:
-- 1. Phân bổ đều Vet, Trainer, Groom theo Area và Stall Block chuẩn.
-- 2. Giữ nguyên 100% hồ sơ, bảng horses, admission_documents & Cloudinary assets.
-- 3. Không dùng ID cứng; phân giải qua email/mã code.
-- =====================================================================

DO $$
DECLARE
    -- Trainer IDs
    v_tr01 BIGINT;
    v_tr02 BIGINT;
    v_tr03 BIGINT;

    -- Vet IDs
    v_vt01 BIGINT;
    v_vt02 BIGINT;
    v_vt03 BIGINT;

    -- Groom GR01 & GR21 for pending intakes
    v_gr01 BIGINT;
    v_gr21 BIGINT;
    v_gr22 BIGINT;
BEGIN
    -- 1. Resolve Trainer IDs
    SELECT id INTO v_tr01 FROM users WHERE email = 'trainer1@rtms.com';
    SELECT id INTO v_tr02 FROM users WHERE email = 'trainer2@rtms.com';
    SELECT id INTO v_tr03 FROM users WHERE email = 'trainer3@rtms.com';
    IF v_tr03 IS NULL THEN
        SELECT id INTO v_tr03 FROM users WHERE email = 'trainer@rtms.com';
    END IF;

    -- 2. Resolve Vet IDs
    SELECT id INTO v_vt01 FROM users WHERE email = 'vet1@rtms.com';
    SELECT id INTO v_vt02 FROM users WHERE email = 'vet2@rtms.com';
    SELECT id INTO v_vt03 FROM users WHERE email = 'vet3@rtms.com';
    IF v_vt03 IS NULL THEN
        SELECT id INTO v_vt03 FROM users WHERE email = 'vet@rtms.com';
    END IF;

    -- 3. Resolve Groom IDs
    SELECT id INTO v_gr01 FROM users WHERE email = 'groom1@rtms.com';
    IF v_gr01 IS NULL THEN
        SELECT id INTO v_gr01 FROM users WHERE email = 'groom@rtms.com';
    END IF;
    SELECT id INTO v_gr21 FROM users WHERE email = 'jack.wilson@demo.rtms.example';
    SELECT id INTO v_gr22 FROM users WHERE email = 'zoe.turner@demo.rtms.example';

    RAISE NOTICE 'Resolved Trainers: %, %, %', v_tr01, v_tr02, v_tr03;
    RAISE NOTICE 'Resolved Vets: %, %, %', v_vt01, v_vt02, v_vt03;

    -- 4. Sync Area Trainer Assignments
    IF v_tr01 IS NOT NULL THEN
        UPDATE areas SET trainer_id = v_tr01 WHERE code IN ('A', 'B');
    END IF;
    IF v_tr02 IS NOT NULL THEN
        UPDATE areas SET trainer_id = v_tr02 WHERE code IN ('C', 'D');
    END IF;
    IF v_tr03 IS NOT NULL THEN
        UPDATE areas SET trainer_id = v_tr03 WHERE code = 'E';
    END IF;

    -- 5. Rebalance APPROVED admissions to match their assigned Stable Stall & Area
    UPDATE admission_applications a
    SET 
        trainer_id = CASE
            WHEN ar.code IN ('A', 'B') THEN v_tr01
            WHEN ar.code IN ('C', 'D') THEN v_tr02
            ELSE v_tr03
        END,
        veterinarian_id = CASE
            WHEN ar.code IN ('A', 'B') THEN v_vt01
            WHEN ar.code IN ('C', 'D') THEN v_vt02
            ELSE v_vt03
        END,
        groom_id = COALESCE(s.groom_id, a.groom_id)
    FROM horses h
    JOIN stable_stalls s ON h.current_stall_id = s.id
    JOIN areas ar ON s.area_id = ar.id
    WHERE a.horse_id = h.id AND a.status = 'APPROVED';

    -- 6. Rebalance pending admissions in Quarantine (Q-A)
    -- H043, H044: VET_REVIEW -> VT01
    UPDATE admission_applications a
    SET veterinarian_id = v_vt01, groom_id = COALESCE(v_gr21, a.groom_id)
    FROM horses h
    WHERE a.horse_id = h.id AND a.status = 'VET_REVIEW';

    -- H045, H046: TRAINER_REVIEW -> TR01
    UPDATE admission_applications a
    SET trainer_id = v_tr01, groom_id = COALESCE(v_gr21, a.groom_id)
    FROM horses h
    WHERE a.horse_id = h.id AND a.status = 'TRAINER_REVIEW';

    -- H047, H048: MANAGER_REVIEW -> TR01 / TR02
    UPDATE admission_applications a
    SET 
        trainer_id = CASE WHEN h.name ILIKE '%PANERAI%' THEN v_tr01 ELSE v_tr02 END,
        veterinarian_id = CASE WHEN h.name ILIKE '%PANERAI%' THEN v_vt01 ELSE v_vt02 END,
        groom_id = COALESCE(v_gr22, a.groom_id)
    FROM horses h
    WHERE a.horse_id = h.id AND a.status = 'MANAGER_REVIEW';

    -- Pending applicant without horse (WAITING_FOR_ARRIVAL, etc.)
    IF v_gr01 IS NOT NULL THEN
        UPDATE admission_applications
        SET groom_id = v_gr01
        WHERE status IN ('WAITING_FOR_ARRIVAL', 'GROOM_REVIEW') AND groom_id IS NULL;
    END IF;

    -- 7. Sync care_schedule & health_records to match assigned Veterinarian
    UPDATE care_schedule cs
    SET veterinarian_id = a.veterinarian_id
    FROM admission_applications a
    WHERE cs.admission_id = a.id AND a.veterinarian_id IS NOT NULL;

    UPDATE health_records hr
    SET veterinarian_id = cs.veterinarian_id
    FROM care_schedule cs
    WHERE hr.care_schedule_id = cs.id AND cs.veterinarian_id IS NOT NULL;

    RAISE NOTICE 'Staff rebalance completed successfully!';
END $$;
