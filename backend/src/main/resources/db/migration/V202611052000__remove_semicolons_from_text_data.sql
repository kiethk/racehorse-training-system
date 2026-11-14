-- Normalize semicolon punctuation in seeded text while preserving SQL structure.

ALTER TABLE admission_documents DISABLE TRIGGER trg_admission_document_immutable;

ALTER TABLE candidate_horse_profiles DISABLE TRIGGER trg_candidate_snapshot_immutable;

UPDATE admission_documents
SET note = regexp_replace(regexp_replace(note, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g')
WHERE note LIKE '%;%';

UPDATE groom_task_completions
SET note = regexp_replace(regexp_replace(note, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g')
WHERE note LIKE '%;%';

UPDATE health_records
SET findings = regexp_replace(regexp_replace(findings, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g'), updated_at = now()
WHERE findings LIKE '%;%';

UPDATE health_records
SET diagnosis = regexp_replace(regexp_replace(diagnosis, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g'), updated_at = now()
WHERE diagnosis LIKE '%;%';

UPDATE health_records
SET notes = regexp_replace(regexp_replace(notes, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g'), updated_at = now()
WHERE notes LIKE '%;%';

UPDATE health_records
SET treatment = regexp_replace(regexp_replace(treatment, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g'), updated_at = now()
WHERE treatment LIKE '%;%';

UPDATE health_records
SET assessment_note = regexp_replace(regexp_replace(assessment_note, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g'), updated_at = now()
WHERE assessment_note LIKE '%;%';

UPDATE candidate_horse_profiles
SET pedigree_notes = regexp_replace(regexp_replace(pedigree_notes, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g'), updated_at = now()
WHERE pedigree_notes LIKE '%;%';

UPDATE horse_pedigrees
SET pedigree_notes = regexp_replace(regexp_replace(pedigree_notes, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g')
WHERE pedigree_notes LIKE '%;%';

UPDATE admission_applications
SET vet_feedback = regexp_replace(regexp_replace(vet_feedback, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g'), updated_at = now()
WHERE vet_feedback LIKE '%;%';

UPDATE admission_applications
SET manager_feedback = regexp_replace(regexp_replace(manager_feedback, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g'), updated_at = now()
WHERE manager_feedback LIKE '%;%';

UPDATE admission_applications
SET groom_feedback = regexp_replace(regexp_replace(groom_feedback, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g'), updated_at = now()
WHERE groom_feedback LIKE '%;%';

UPDATE admission_applications
SET trainer_feedback = regexp_replace(regexp_replace(trainer_feedback, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g'), updated_at = now()
WHERE trainer_feedback LIKE '%;%';

UPDATE racing_readiness_assessments
SET remarks = regexp_replace(regexp_replace(remarks, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g')
WHERE remarks LIKE '%;%';

UPDATE horse_health_metrics
SET notes = regexp_replace(regexp_replace(notes, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g')
WHERE notes LIKE '%;%';

UPDATE horse_incident_reports
SET handler_note = regexp_replace(regexp_replace(handler_note, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g')
WHERE handler_note LIKE '%;%';

UPDATE horse_incident_reports
SET description = regexp_replace(regexp_replace(description, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g')
WHERE description LIKE '%;%';

-- Guarded: V202611081100 drops race_registrations table.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = 'public' AND table_name = 'race_registrations') THEN
        UPDATE race_registrations
        SET selection_reason = regexp_replace(regexp_replace(selection_reason, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g'), updated_at = now()
        WHERE selection_reason LIKE '%;%';

        UPDATE race_registrations
        SET manager_feedback = regexp_replace(regexp_replace(manager_feedback, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g'), updated_at = now()
        WHERE manager_feedback LIKE '%;%';
    END IF;
END $$;

UPDATE treatment_plans
SET instructions = regexp_replace(regexp_replace(instructions, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g'), updated_at = now()
WHERE instructions LIKE '%;%';

-- Guarded: V202611042100 drops care_schedule.cancel_reason earlier on a fresh database.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'care_schedule' AND column_name = 'cancel_reason') THEN
        UPDATE care_schedule
        SET cancel_reason = regexp_replace(regexp_replace(cancel_reason, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g'), updated_at = now()
        WHERE cancel_reason LIKE '%;%';
    END IF;
END $$;

UPDATE care_schedule
SET description = regexp_replace(regexp_replace(description, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g'), updated_at = now()
WHERE description LIKE '%;%';

UPDATE horses
SET training_decision_reason = regexp_replace(regexp_replace(training_decision_reason, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g'), updated_at = now()
WHERE training_decision_reason LIKE '%;%';

UPDATE groom_daily_tasks
SET notes = regexp_replace(regexp_replace(notes, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g')
WHERE notes LIKE '%;%';

UPDATE courses
SET target_goal = regexp_replace(regexp_replace(target_goal, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g'), updated_at = now()
WHERE target_goal LIKE '%;%';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'subjects') THEN
        UPDATE subjects
        SET description = regexp_replace(regexp_replace(description, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g'), updated_at = now()
        WHERE description LIKE '%;%';
    ELSIF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'exercises') THEN
        UPDATE exercises
        SET description = regexp_replace(regexp_replace(description, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g'), updated_at = now()
        WHERE description LIKE '%;%';
    END IF;
END $$;

UPDATE health_conditions
SET description = regexp_replace(regexp_replace(description, ';\s*$', '', 'g'), '\s*;\s*', ', ', 'g')
WHERE description LIKE '%;%';

ALTER TABLE candidate_horse_profiles ENABLE TRIGGER trg_candidate_snapshot_immutable;

ALTER TABLE admission_documents ENABLE TRIGGER trg_admission_document_immutable;
