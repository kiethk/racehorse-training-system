-- V202611010900 — Track 0 (ROADMAP v2.2): danh mục thuốc chuẩn + cột đơn thuốc + severity D4.
-- Phạm vi Vet. MAR (medication_administration_logs) để dành Track 3e — user chốt 01/11.

-- ============================================================
-- 1. clinical_medications — A3: withdrawal_days chuẩn FEI, Vet không được giảm dưới mốc này.
-- ============================================================
CREATE TABLE IF NOT EXISTS clinical_medications (
    id                          BIGSERIAL PRIMARY KEY,
    name                        VARCHAR(150) NOT NULL UNIQUE,
    therapeutic_class           VARCHAR(80)  NOT NULL,
    default_route               VARCHAR(30)  NOT NULL,
    standard_withdrawal_days    INT          NOT NULL CHECK (standard_withdrawal_days >= 0),
    default_dosage              VARCHAR(100),
    default_frequency           VARCHAR(20),
    active                      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMP
);

INSERT INTO clinical_medications
    (name, therapeutic_class, default_route, standard_withdrawal_days, default_dosage, default_frequency)
VALUES
    ('Flunixin Meglumine',        'NSAID',                'INJECTION_IV',  4,  '1.1 mg/kg IV',      'BID'),
    ('Phenylbutazone',            'NSAID',                'ORAL',          7,  '4.4 mg/kg oral',    'BID'),
    ('Furosemide',                'Diuretic',             'INJECTION_IV',  30, '1 mg/kg IV',        'ONCE'),
    ('Dexamethasone',             'Corticosteroid',       'INJECTION_IV',  7,  '0.05 mg/kg IV',     'ONCE'),
    ('Prednisolone',              'Corticosteroid',       'ORAL',          7,  '0.5 mg/kg oral',    'SID'),
    ('Omeprazole',                'Gastric protectant',   'ORAL',          1,  '4 mg/kg oral',      'SID'),
    ('Aspirin',                   'NSAID',                'ORAL',          4,  '25 mg/kg oral',     'SID'),
    ('Procaine HCl Penicillin G', 'Antibiotic',           'INJECTION_IM',  2,  '22000 IU/kg IM',    'SID'),
    ('Isoxsuprine HCl',           'Vasodilator',          'ORAL',          4,  '0.4 mg/kg oral',    'BID'),
    ('Midazolam',                 'Sedative',             'INJECTION_IV',  1,  '0.1 mg/kg IV',      'ONCE'),
    ('Atropine Sulfate',          'Anticholinergic',      'INJECTION_IV',  1,  '0.02 mg/kg IV',     'ONCE'),
    ('Acepromazine',              'Sedative',             'INJECTION_IM',  4,  '0.05 mg/kg IM',     'ONCE'),
    ('Magnesium Sulfate',         'Sedative adjunct',     'INJECTION_IV',  2,  '1 g/100 kg IV',     'ONCE'),
    ('Trimethoprim-Sulfa',        'Antibiotic',           'ORAL',          5,  '30 mg/kg oral',     'BID'),
    ('Diclofenac Gel',            'Topical NSAID',        'TOPICAL',       4,  '10 g topical',      'BID')
ON CONFLICT (name) DO NOTHING;

-- ============================================================
-- 2. prescriptions: status (A1 contract) + administered_by_role (B5).
--    route vẫn VARCHAR free-text (dữ liệu lịch sử 'IV'/'Oral'...) — không đổi để tránh breaking.
-- ============================================================
ALTER TABLE prescriptions ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_prescriptions_status') THEN
        ALTER TABLE prescriptions ADD CONSTRAINT chk_prescriptions_status
            CHECK (status IN ('ACTIVE', 'COMPLETED', 'DISCONTINUED'));
    END IF;
END $$;

ALTER TABLE prescriptions ADD COLUMN IF NOT EXISTS administered_by_role VARCHAR(30);
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_prescriptions_administered_role') THEN
        ALTER TABLE prescriptions ADD CONSTRAINT chk_prescriptions_administered_role
            CHECK (administered_by_role IN ('GROOM', 'VETERINARIAN'));
    END IF;
END $$;

-- Backfill role theo B5: tiêm (IV/IM) -> VETERINARIAN, còn lại -> GROOM.
UPDATE prescriptions
   SET administered_by_role = CASE
           WHEN UPPER(COALESCE(route, '')) IN ('IV', 'IM', 'INJECTION_IV', 'INJECTION_IM')
           THEN 'VETERINARIAN' ELSE 'GROOM' END
 WHERE administered_by_role IS NULL;

ALTER TABLE prescriptions ALTER COLUMN administered_by_role SET NOT NULL;

-- Backfill status: thuốc của phác đồ đã kết thúc -> COMPLETED, đang chạy -> ACTIVE.
UPDATE prescriptions p
   SET status = CASE
           WHEN tp.status IN ('COMPLETED', 'CANCELLED')
                OR (p.end_date IS NOT NULL AND p.end_date < CURRENT_DATE)
           THEN 'COMPLETED' ELSE 'ACTIVE' END
  FROM treatment_plans tp
 WHERE tp.id = p.treatment_plan_id
   AND p.status = 'ACTIVE';

-- ============================================================
-- 3. D4: severity chuẩn hoá MILD/MODERATE/SEVERE/CRITICAL cho health_conditions.
--    (horse_incident_reports giữ bộ từ vựng riêng — ngoài scope WF-02 §3.3.)
-- ============================================================
UPDATE health_conditions SET severity = UPPER(severity) WHERE severity <> UPPER(severity);
UPDATE health_conditions SET severity = 'MILD'     WHERE severity = 'LOW';
UPDATE health_conditions SET severity = 'MODERATE' WHERE severity = 'MEDIUM';
UPDATE health_conditions SET severity = 'SEVERE'   WHERE severity = 'HIGH';
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_health_conditions_severity') THEN
        ALTER TABLE health_conditions ADD CONSTRAINT chk_health_conditions_severity
            CHECK (severity IN ('MILD', 'MODERATE', 'SEVERE', 'CRITICAL'));
    END IF;
END $$;
