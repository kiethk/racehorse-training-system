-- =====================================================================
-- Migration: V202610311800__refactor_vet_clinical_domain_v2.sql
-- Description: Refactor Vet Clinical Domain Schema (v2.1 Consensus)
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. PRESCRIPTIONS: Thêm thuộc tính cách ly Doping
-- ---------------------------------------------------------------------
ALTER TABLE prescriptions 
    ADD COLUMN IF NOT EXISTS withdrawal_days INT NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_prescriptions_withdrawal') THEN
        ALTER TABLE prescriptions ADD CONSTRAINT chk_prescriptions_withdrawal CHECK (withdrawal_days >= 0);
    END IF;
END $$;

-- ---------------------------------------------------------------------
-- 2. HEALTH_RECORDS: Đồng bộ thuật ngữ & Dọn dẹp cột chết
-- ---------------------------------------------------------------------
-- Đổi tên restriction_details thành assessment_note
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'health_records' AND column_name = 'restriction_details'
    ) THEN
        ALTER TABLE health_records RENAME COLUMN restriction_details TO assessment_note;
    END IF;
END $$;

-- Đảm bảo care_schedule_id là duy nhất (1-1 với ca khám)
CREATE UNIQUE INDEX IF NOT EXISTS uq_health_records_care_schedule 
    ON health_records(care_schedule_id) 
    WHERE care_schedule_id IS NOT NULL;

-- Xóa cột chết product_or_service từ thời V38
ALTER TABLE health_records 
    DROP COLUMN IF EXISTS product_or_service;

-- Hợp nhất dữ liệu symptoms vào findings và xóa cột symptoms
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'health_records' AND column_name = 'symptoms'
    ) THEN
        UPDATE health_records 
        SET findings = CASE 
            WHEN findings IS NULL OR findings = '' THEN symptoms 
            WHEN symptoms IS NOT NULL AND symptoms <> '' AND findings NOT ILIKE '%' || symptoms || '%' THEN findings || ' | Symptoms: ' || symptoms
            ELSE findings 
        END
        WHERE symptoms IS NOT NULL AND symptoms <> '';

        ALTER TABLE health_records DROP COLUMN symptoms;
    END IF;
END $$;

-- ---------------------------------------------------------------------
-- 3. INJURY_RECORDS -> HEALTH_CONDITIONS: Mở rộng cho Bệnh lý nội khoa
-- ---------------------------------------------------------------------
-- Đổi tên bảng
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'injury_records' AND table_type = 'BASE TABLE') THEN
        ALTER TABLE injury_records RENAME TO health_conditions;
    END IF;
END $$;

-- Thêm phân loại category (INJURY / ILLNESS)
ALTER TABLE health_conditions 
    ADD COLUMN IF NOT EXISTS category VARCHAR(20) NOT NULL DEFAULT 'INJURY';

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_health_conditions_category') THEN
        ALTER TABLE health_conditions ADD CONSTRAINT chk_health_conditions_category CHECK (category IN ('INJURY', 'ILLNESS'));
    END IF;
END $$;

-- Đổi tên cột loại chấn thương thành loại bệnh lý
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'health_conditions' AND column_name = 'injury_type'
    ) THEN
        ALTER TABLE health_conditions RENAME COLUMN injury_type TO condition_type;
    END IF;
END $$;

-- Cho phép NULL đối với bệnh lý nội khoa không có vị trí 3D
ALTER TABLE health_conditions 
    ALTER COLUMN body_region_id DROP NOT NULL;

-- Dọn dẹp cột thừa
ALTER TABLE health_conditions 
    DROP COLUMN IF EXISTS mesh_name,
    DROP COLUMN IF EXISTS diagnosed_at;

-- Đổi tên Index cho đồng bộ
ALTER INDEX IF EXISTS idx_injury_records_horse_status 
    RENAME TO idx_health_conditions_horse_status;

-- Tạo View tương thích ngược cho các query cũ
CREATE OR REPLACE VIEW injury_records AS 
    SELECT id, horse_id, health_record_id, condition_type AS injury_type, body_region_id, 
           severity, description, expected_recovery_date, recovered_at, status, 
           position_x, position_y, position_z, zone_id, local_x, local_y, local_z, 
           normal_x, normal_y, normal_z, created_at, updated_at
    FROM health_conditions;

-- ---------------------------------------------------------------------
-- 4. CARE_SCHEDULES: Tạo View chuẩn hóa tên số nhiều & Dọn dẹp bảng rác
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW care_schedules AS 
    SELECT * FROM care_schedule;

-- Dọn dẹp bảng rác không còn sử dụng sau khi chuyển sang Direct Assignment
DROP TABLE IF EXISTS deprecated_vet_offers CASCADE;
