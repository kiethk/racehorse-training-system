-- V202611021635__update_sample_domain_audit_events.sql
-- Update sample domain audit event for horse status to include trainer reference ID

UPDATE audit_logs
SET details = '{"module":"HORSES","summary":"Updated horse lifecycle status to ELIGIBLE","reason":"Passed 14-day training readiness baseline tests with high scores.","targetUrl":"/manager/horses/1","before":{"lifecycleStatus":"CANDIDATE","competitionReadiness":"PENDING","assignedTrainer":null},"after":{"lifecycleStatus":"ELIGIBLE","competitionReadiness":"READY","assignedTrainer":"Head Trainer (#2)"}}'
WHERE action = 'UPDATE_HORSE_STATUS' AND entity_name = 'Horse' AND entity_id = 1;

