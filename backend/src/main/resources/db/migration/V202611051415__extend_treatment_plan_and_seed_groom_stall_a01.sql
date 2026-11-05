-- V202611051415__extend_treatment_plan_and_seed_groom_stall_a01.sql
-- Extend BV KAROL active plan end_date and seed active treatment plan for groom1 stall A01 (NAUSICAA BY NIKA)

-- 1. Extend BV KAROL active plan end_date into the future so it remains active today
UPDATE treatment_plans
SET end_date = CURRENT_DATE + INTERVAL '7 days'
WHERE health_record_id IN (
    SELECT hr.id FROM health_records hr
    JOIN horses h ON hr.horse_id = h.id
    WHERE h.name = 'BV KAROL'
) AND status = 'ACTIVE';

-- 2. NAUSICAA BY NIKA (A01 - Groom 1 stall): Ensure an active treatment plan with JSON instructions exists
INSERT INTO treatment_plans (health_record_id, treatment_name, instructions, start_date, end_date, status, created_at, updated_at)
SELECT hr.id, 'Post-Workout Joint Therapy',
'[{"medication_name":"Phenylbutazone (Bute)","dosage":"2.2 mg/kg","frequency":"Twice daily with feed","route":"FEED_ADDITIVE","duration":5,"notes":"Mix thoroughly with morning and evening grain rations."},{"medication_name":"Procaine Penicillin G","dosage":"20,000 IU/kg","frequency":"Once daily","route":"INJECTION_IM","duration":7,"notes":"Strictly administered by Vet. Alternate neck muscle sites."},{"medication_name":"Topical DMSO Gel","dosage":"Apply thin layer","frequency":"Twice daily","route":"TOPICAL","duration":5,"notes":"Apply with protective gloves over swollen tendon region."}]',
CURRENT_DATE - INTERVAL '1 day',
CURRENT_DATE + INTERVAL '5 days',
'ACTIVE',
NOW(),
NOW()
FROM health_records hr
JOIN horses h ON hr.horse_id = h.id
WHERE h.name = 'NAUSICAA BY NIKA'
ORDER BY hr.examined_at DESC
LIMIT 1
ON CONFLICT DO NOTHING;

