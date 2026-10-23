-- V202610200840__backfill_friendly_audit_logs.sql
-- Backfill historical audit logs with manager-friendly business action and details

-- 1. Format existing automatic care assignment logs
UPDATE audit_logs
SET action = 'Care Schedule Auto Assigned',
    details = CASE
        WHEN details IS NULL OR details = ''
        THEN 'Care schedule #' || COALESCE(entity_id::text, '') || ' was automatically assigned to veterinarian'
        ELSE details
    END
WHERE action = 'CARE_SCHEDULE_AUTO_ASSIGNED';

-- 2. Backfill profile update logs
UPDATE audit_logs
SET action = 'Update Personal Profile',
    entity_name = 'User',
    details = 'User updated their personal profile and contact details'
WHERE request_path LIKE '/api/auth/me/profile%'
  AND (action IS NULL OR action = '' OR action = 'General Action');

-- 3. Backfill training courses and plans logs
UPDATE audit_logs
SET action = 'Create Training Course',
    entity_name = 'Course',
    details = 'Trainer registered a new curriculum course'
WHERE request_path = '/api/courses' AND (action = 'Create Record' OR details LIKE 'Executed%');

UPDATE audit_logs
SET action = 'Create Training Plan',
    entity_name = 'HorseTrainingPlan',
    details = CASE
        WHEN status_code >= 200 AND status_code < 300 THEN 'Trainer created a new training plan for horse'
        ELSE 'Trainer attempted to create a new training plan (Attempt failed)'
    END
WHERE request_path = '/api/training-plans' AND (action = 'Create Record' OR details LIKE 'Executed%');

-- 4. Any remaining legacy rows without action or details
UPDATE audit_logs
SET action = 'System Operation',
    details = 'Processed system operation'
WHERE (action IS NULL OR action = '') AND (details IS NULL OR details = '');
