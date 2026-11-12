-- Keep the injection route definition in one place for all data updates below.
CREATE OR REPLACE FUNCTION pg_temp.is_injection_route(route_value TEXT)
RETURNS BOOLEAN
LANGUAGE SQL
IMMUTABLE
AS $$
    SELECT UPPER(BTRIM(COALESCE(route_value, ''))) IN (
               'IV', 'IM', 'SC', 'SQ',
               'INJECTION_IV', 'INJECTION_IM', 'INJECTION_SC'
           )
        OR UPPER(BTRIM(COALESCE(route_value, ''))) LIKE '%INTRAVENOUS%'
        OR UPPER(BTRIM(COALESCE(route_value, ''))) LIKE '%INTRAMUSCULAR%'
        OR UPPER(BTRIM(COALESCE(route_value, ''))) LIKE '%SUBCUTANEOUS%'
        OR UPPER(BTRIM(COALESCE(route_value, ''))) LIKE '%INJECT%'
$$;

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

-- Replace known non-vaccination prescriptions before normalizing other routes.
UPDATE prescriptions
   SET medication_name = 'Vitamin B Complex',
       route = 'FEED_ADDITIVE',
       instructions = 'Mix into the morning feed.',
       updated_at = now()
 WHERE preventive_protocol_id IS NULL
   AND medication_name ILIKE '%Vitamin B%'
   AND pg_temp.is_injection_route(route);

UPDATE prescriptions
   SET medication_name = 'Trimethoprim-Sulfa',
       dosage = '30 mg/kg',
       frequency = 'BID',
       route = 'ORAL',
       instructions = 'Give with morning and evening feed.',
       updated_at = now()
 WHERE preventive_protocol_id IS NULL
   AND medication_name ILIKE '%Penicillin%'
   AND pg_temp.is_injection_route(route);

UPDATE prescriptions
   SET medication_name = 'Flunixin Meglumine paste',
       dosage = '1.1 mg/kg',
       route = 'ORAL',
       instructions = 'Oral paste given before feed.',
       updated_at = now()
 WHERE preventive_protocol_id IS NULL
   AND medication_name ILIKE '%Flunixin%'
   AND pg_temp.is_injection_route(route);

DELETE FROM prescriptions
 WHERE preventive_protocol_id IS NULL
   AND medication_name ILIKE '%Butylscopolamine%'
   AND pg_temp.is_injection_route(route);

UPDATE prescriptions
   SET route = 'ORAL',
       updated_at = now()
 WHERE preventive_protocol_id IS NULL
   AND pg_temp.is_injection_route(route);

UPDATE prescriptions
   SET administered_by_role = 'GROOM',
       updated_at = now()
 WHERE preventive_protocol_id IS NULL
   AND administered_by_role IS DISTINCT FROM 'GROOM';

-- Rewrite structured treatment plan instructions while leaving plain text untouched.
WITH parsed_plans AS (
    SELECT id, pg_temp.try_jsonb_array(instructions) AS items
      FROM treatment_plans
     WHERE LEFT(BTRIM(instructions), 1) = '['
), rewritten_plans AS (
    SELECT id,
           COALESCE(
               jsonb_agg(rewritten_item ORDER BY item_order)
                   FILTER (WHERE rewritten_item IS NOT NULL),
               '[]'::JSONB
           ) AS items
      FROM parsed_plans
      CROSS JOIN LATERAL jsonb_array_elements(items)
           WITH ORDINALITY AS source(item, item_order)
      CROSS JOIN LATERAL (
          SELECT CASE
              WHEN pg_temp.is_injection_route(item ->> 'route')
                   AND item ->> 'medication_name' ILIKE '%Butylscopolamine%'
                  THEN NULL::JSONB
              WHEN pg_temp.is_injection_route(item ->> 'route')
                   AND item ->> 'medication_name' ILIKE '%Vitamin B%'
                  THEN jsonb_set(
                           jsonb_set(
                               jsonb_set(item, '{medication_name}', '"Vitamin B Complex"'::JSONB, TRUE),
                               '{route}', '"FEED_ADDITIVE"'::JSONB, TRUE
                           ),
                           '{notes}', '"Mix into the morning feed."'::JSONB, TRUE
                       )
              WHEN pg_temp.is_injection_route(item ->> 'route')
                   AND item ->> 'medication_name' ILIKE '%Penicillin%'
                  THEN jsonb_set(
                           jsonb_set(
                               jsonb_set(
                                   jsonb_set(
                                       jsonb_set(item, '{medication_name}', '"Trimethoprim-Sulfa"'::JSONB, TRUE),
                                       '{dosage}', '"30 mg/kg"'::JSONB, TRUE
                                   ),
                                   '{frequency}', '"Twice daily with feed"'::JSONB, TRUE
                               ),
                               '{route}', '"ORAL"'::JSONB, TRUE
                           ),
                           '{notes}', '"Give with morning and evening feed."'::JSONB, TRUE
                       )
              WHEN pg_temp.is_injection_route(item ->> 'route')
                   AND item ->> 'medication_name' ILIKE '%Flunixin%'
                  THEN jsonb_set(
                           jsonb_set(
                               jsonb_set(item, '{medication_name}', '"Flunixin Meglumine paste"'::JSONB, TRUE),
                               '{route}', '"ORAL"'::JSONB, TRUE
                           ),
                           '{notes}', '"Oral paste given before feed."'::JSONB, TRUE
                       )
              WHEN pg_temp.is_injection_route(item ->> 'route')
                  THEN jsonb_set(item, '{route}', '"ORAL"'::JSONB, TRUE)
              ELSE item
          END AS rewritten_item
      ) AS rewritten
     WHERE items IS NOT NULL
     GROUP BY id
)
UPDATE treatment_plans plan
   SET instructions = rewritten_plans.items::TEXT
  FROM rewritten_plans
 WHERE plan.id = rewritten_plans.id
   AND pg_temp.try_jsonb_array(plan.instructions) IS DISTINCT FROM rewritten_plans.items;

-- Move supported catalog drugs to non-injection routes.
-- Guarded: V202611031200 drops clinical_medications earlier on a fresh database.
DO $$
BEGIN
    IF to_regclass('public.clinical_medications') IS NOT NULL THEN
        UPDATE clinical_medications
           SET default_route = 'ORAL',
               default_dosage = '1.1 mg/kg oral paste',
               updated_at = now()
         WHERE name = 'Flunixin Meglumine'
           AND (default_route IS DISTINCT FROM 'ORAL'
                OR default_dosage IS DISTINCT FROM '1.1 mg/kg oral paste');

        UPDATE clinical_medications
           SET default_route = 'ORAL',
               default_dosage = '1 mg/kg oral',
               updated_at = now()
         WHERE name = 'Furosemide'
           AND (default_route IS DISTINCT FROM 'ORAL'
                OR default_dosage IS DISTINCT FROM '1 mg/kg oral');

        UPDATE clinical_medications
           SET default_route = 'ORAL',
               default_dosage = '0.05 mg/kg oral powder',
               updated_at = now()
         WHERE name = 'Dexamethasone'
           AND (default_route IS DISTINCT FROM 'ORAL'
                OR default_dosage IS DISTINCT FROM '0.05 mg/kg oral powder');

        UPDATE clinical_medications
           SET default_route = 'ORAL',
               default_dosage = '0.05 mg/kg oral granules',
               updated_at = now()
         WHERE name = 'Acepromazine'
           AND (default_route IS DISTINCT FROM 'ORAL'
                OR default_dosage IS DISTINCT FROM '0.05 mg/kg oral granules');

        UPDATE clinical_medications
           SET active = FALSE,
               updated_at = now()
         WHERE pg_temp.is_injection_route(default_route)
           AND active IS DISTINCT FROM FALSE;

        UPDATE clinical_medications
           SET active = TRUE,
               updated_at = now()
         WHERE name = 'Trimethoprim-Sulfa'
           AND active IS DISTINCT FROM TRUE;

        INSERT INTO clinical_medications
            (name, therapeutic_class, default_route, standard_withdrawal_days,
             default_dosage, default_frequency)
        VALUES
            ('Vitamin B Complex', 'Supplement', 'FEED_ADDITIVE', 0,
             '10 mL in feed', 'SID')
        ON CONFLICT (name) DO NOTHING;
    END IF;
END $$;

DROP FUNCTION pg_temp.try_jsonb_array(TEXT);
DROP FUNCTION pg_temp.is_injection_route(TEXT);
