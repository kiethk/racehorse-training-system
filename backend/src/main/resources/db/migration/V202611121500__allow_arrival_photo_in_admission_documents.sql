-- Allow Groom to attach arrival photos when confirming horse arrival.
-- Other documents and candidate profile snapshots remain locked once review starts.
CREATE OR REPLACE FUNCTION protect_admission_snapshot()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    v_admission_id BIGINT;
BEGIN
    IF TG_TABLE_NAME = 'admission_documents' AND TG_OP = 'INSERT' THEN
        IF (to_jsonb(NEW) ->> 'document_type') = 'ARRIVAL_PHOTO' THEN
            RETURN NEW;
        END IF;
    END IF;

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
