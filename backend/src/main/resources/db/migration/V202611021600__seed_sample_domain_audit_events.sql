-- V202611021600__seed_sample_domain_audit_events.sql
-- Seed sample high-fidelity Tier-2 Domain Audit Events with Before/After state transitions, reasons, and entity links

INSERT INTO audit_logs (
    user_id,
    action,
    entity_name,
    entity_id,
    http_method,
    request_path,
    status_code,
    details,
    created_at
)
SELECT
    (SELECT id FROM users WHERE email = 'admin@rtms.com' LIMIT 1),
    'APPROVE_ADMISSION',
    'AdmissionApplication',
    1,
    'DOMAIN',
    '/manager/admissions/1',
    200,
    '{"module":"ADMISSIONS","summary":"Manager approved admission application #1","reason":"All veterinary clinical notes and quarantine clearance checks met health guidelines.","targetUrl":"/manager/admissions/1","before":{"status":"UNDER_MANAGER_REVIEW","quarantineCleared":true,"managerDecision":null},"after":{"status":"APPROVED","quarantineCleared":true,"managerDecision":"APPROVED","admittedAt":"2026-11-02T10:00:00Z"}}',
    NOW() - INTERVAL '5 minutes'
WHERE NOT EXISTS (
    SELECT 1 FROM audit_logs WHERE action = 'APPROVE_ADMISSION' AND entity_name = 'AdmissionApplication' AND entity_id = 1
);

INSERT INTO audit_logs (
    user_id,
    action,
    entity_name,
    entity_id,
    http_method,
    request_path,
    status_code,
    details,
    created_at
)
SELECT
    COALESCE((SELECT id FROM users WHERE email = 'vet@rtms.com' LIMIT 1), (SELECT id FROM users LIMIT 1)),
    'RESCHEDULE_CARE_SCHEDULE',
    'CareSchedule',
    1,
    'DOMAIN',
    '/vet/schedules',
    200,
    '{"module":"MEDICAL","summary":"Rescheduled preventive vaccination for horse","reason":"Attending veterinarian engaged in emergency colic treatment.","targetUrl":"/vet/schedules","before":{"status":"SCHEDULED","scheduledDate":"2026-11-02 09:00","assignedVet":"Dr. Sarah Jenkins"},"after":{"status":"RESCHEDULED","scheduledDate":"2026-11-03 14:00","assignedVet":"Dr. Sarah Jenkins"}}',
    NOW() - INTERVAL '15 minutes'
WHERE NOT EXISTS (
    SELECT 1 FROM audit_logs WHERE action = 'RESCHEDULE_CARE_SCHEDULE' AND entity_name = 'CareSchedule' AND entity_id = 1
);

INSERT INTO audit_logs (
    user_id,
    action,
    entity_name,
    entity_id,
    http_method,
    request_path,
    status_code,
    details,
    created_at
)
SELECT
    (SELECT id FROM users WHERE email = 'admin@rtms.com' LIMIT 1),
    'UPDATE_HORSE_STATUS',
    'Horse',
    1,
    'DOMAIN',
    '/manager/horses/1',
    200,
    '{"module":"HORSES","summary":"Updated horse lifecycle status to ELIGIBLE","reason":"Passed 14-day training readiness baseline tests with high scores.","targetUrl":"/manager/horses/1","before":{"lifecycleStatus":"CANDIDATE","competitionReadiness":"PENDING","assignedTrainer":null},"after":{"lifecycleStatus":"ELIGIBLE","competitionReadiness":"READY","assignedTrainer":"Head Trainer"}}',
    NOW() - INTERVAL '30 minutes'
WHERE NOT EXISTS (
    SELECT 1 FROM audit_logs WHERE action = 'UPDATE_HORSE_STATUS' AND entity_name = 'Horse' AND entity_id = 1
);

INSERT INTO audit_logs (
    user_id,
    action,
    entity_name,
    entity_id,
    http_method,
    request_path,
    status_code,
    details,
    created_at
)
SELECT
    (SELECT id FROM users WHERE email = 'admin@rtms.com' LIMIT 1),
    'REALLOCATE_STALL',
    'StableStall',
    4,
    'DOMAIN',
    '/manager/stable',
    200,
    '{"module":"STABLE","summary":"Reallocated horse to recovery stall Barn A-04","reason":"Transferred to climate-controlled box near the equine hospital for post-op observation.","targetUrl":"/manager/stable","before":{"stallCode":"B-12","area":"Barn B - General","occupant":"Thunder Spirit"},"after":{"stallCode":"A-04","area":"Barn A - Medical Recovery","occupant":"Thunder Spirit"}}',
    NOW() - INTERVAL '1 hour'
WHERE NOT EXISTS (
    SELECT 1 FROM audit_logs WHERE action = 'REALLOCATE_STALL' AND entity_name = 'StableStall' AND entity_id = 4
);

