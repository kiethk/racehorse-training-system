-- V202611050900__seed_sample_treatment_plan_instructions_json.sql
-- Seed structured JSON prescriptions into treatment_plans.instructions for active case (BV KAROL)
-- Supports CP-1 Groom Medication Board & Instruction Modal verification

UPDATE treatment_plans
SET instructions = '[{"medication_name":"Doxycycline","dosage":"10 mg/kg","frequency":"Twice daily","route":"ORAL","duration":7,"notes":"Give with morning and evening feed. Ensure full feed consumption."},{"medication_name":"Vitamin B Complex","dosage":"10 mL","frequency":"Once daily","route":"INJECTION_IM","duration":5,"notes":"Deep intramuscular injection into alternate neck sides. Vet administration only."}]'
WHERE health_record_id IN (
    SELECT hr.id FROM health_records hr
    JOIN horses h ON hr.horse_id = h.id
    WHERE h.name = 'BV KAROL'
) AND status = 'ACTIVE';
