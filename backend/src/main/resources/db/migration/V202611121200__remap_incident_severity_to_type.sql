-- Remap legacy incident severity values to the new IncidentType vocabulary.
-- Old: LOW, MEDIUM, HIGH, CRITICAL  ->  New: PROBLEM (routine), TRAUMA (urgent).
UPDATE horse_incident_reports SET severity = 'PROBLEM' WHERE severity IN ('LOW', 'MEDIUM');
UPDATE horse_incident_reports SET severity = 'TRAUMA'  WHERE severity IN ('HIGH', 'CRITICAL');