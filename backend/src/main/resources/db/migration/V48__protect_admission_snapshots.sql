-- Reject silent mutations to candidate snapshots/documents once Groom begins review.
-- Before that point document uploads are allowed, including after Owner submit.
CREATE OR REPLACE FUNCTION admission_snapshot_is_locked(p_admission_id BIGINT)
RETURNS BOOLEAN LANGUAGE SQL STABLE AS $$
    SELECT COALESCE((
        SELECT status <> 'GROOM_REVIEW' OR groom_id IS NOT NULL
               OR groom_decision IS NOT NULL OR groom_reviewed_at IS NOT NULL
        FROM admission_applications WHERE id = p_admission_id
    ), FALSE);
$$;

CREATE OR REPLACE FUNCTION protect_admission_snapshot()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    v_admission_id BIGINT;
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_admission_id := OLD.admission_id;
    ELSE
        v_admission_id := NEW.admission_id;
    END IF;
    IF admission_snapshot_is_locked(v_admission_id)
       OR (TG_OP = 'UPDATE' AND admission_snapshot_is_locked(OLD.admission_id)) THEN
        RAISE EXCEPTION 'Admission snapshot is locked after Groom starts review';
    END IF;
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_candidate_snapshot_immutable
BEFORE INSERT OR UPDATE OR DELETE ON candidate_horse_profiles
FOR EACH ROW EXECUTE FUNCTION protect_admission_snapshot();

CREATE TRIGGER trg_admission_document_immutable
BEFORE INSERT OR UPDATE OR DELETE ON admission_documents
FOR EACH ROW EXECUTE FUNCTION protect_admission_snapshot();
