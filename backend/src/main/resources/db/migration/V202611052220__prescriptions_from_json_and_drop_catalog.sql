-- Parse only JSON arrays, returning NULL for malformed JSON and other values.
CREATE OR REPLACE FUNCTION pg_temp.try_jsonb_array(value TEXT)
RETURNS JSONB
LANGUAGE plpgsql
IMMUTABLE
AS $$
DECLARE
    parsed_value JSONB;
BEGIN
    IF value IS NULL OR LEFT(BTRIM(value), 1) <> '[' THEN
        RETURN NULL;
    END IF;

    parsed_value := value::JSONB;
    IF JSONB_TYPEOF(parsed_value) = 'array' THEN
        RETURN parsed_value;
    END IF;

    RETURN NULL;
EXCEPTION
    WHEN invalid_text_representation THEN
        RETURN NULL;
END;
$$;

WITH parsed_plans AS (
    SELECT plan.id, plan.start_date, plan.end_date, plan.status,
           pg_temp.try_jsonb_array(plan.instructions) AS items
      FROM treatment_plans plan
     WHERE LEFT(BTRIM(plan.instructions), 1) = '['
), plans_without_prescriptions AS (
    SELECT plan.*
      FROM parsed_plans plan
     WHERE plan.items IS NOT NULL
       AND NOT EXISTS (
           SELECT 1 FROM prescriptions prescription
            WHERE prescription.treatment_plan_id = plan.id
       )
)
INSERT INTO prescriptions
    (treatment_plan_id, medication_name, dosage, frequency, route, start_date, end_date,
     instructions, preventive_protocol_id, withdrawal_days, status, administered_by_role,
     created_at, updated_at)
SELECT plan.id,
       COALESCE(item.value ->> 'medication_name', item.value ->> 'medicationName', 'Unknown medication'),
       COALESCE(item.value ->> 'dosage', ''),
       item.value ->> 'frequency',
       COALESCE(NULLIF(UPPER(item.value ->> 'route'), ''), 'ORAL'),
       plan.start_date,
       plan.end_date,
       item.value ->> 'notes',
       NULL,
       0,
       CASE WHEN plan.status = 'ACTIVE' THEN 'ACTIVE' ELSE 'COMPLETED' END,
       'GROOM',
       now(),
       now()
  FROM plans_without_prescriptions plan
 CROSS JOIN LATERAL jsonb_array_elements(plan.items) AS item(value);

UPDATE treatment_plans plan
   SET instructions = 'Follow the dosage and notes for each medication.',
       updated_at = now()
 WHERE pg_temp.try_jsonb_array(plan.instructions) IS NOT NULL;

DROP TABLE IF EXISTS clinical_medications;

DROP FUNCTION pg_temp.try_jsonb_array(TEXT);
