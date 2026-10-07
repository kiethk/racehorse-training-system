-- V58__clean_legacy_admission_constraints.sql
-- Drop legacy V52 check constraints on health_records that forced follow_up_date
-- and tied clinical health records to legacy vet admission decisions.

ALTER TABLE health_records DROP CONSTRAINT IF EXISTS chk_health_record_vet_decision;
ALTER TABLE health_records DROP CONSTRAINT IF EXISTS chk_health_record_rejection_reason;
ALTER TABLE health_records DROP CONSTRAINT IF EXISTS chk_health_record_follow_up;
