-- A Workout with recorded results is RAN: the proof its horse took part, and what Attendance counts.
-- HELD now means the Session took place but the Workout is not yet confirmed (no result, not marked "Did not run").
-- Existing HELD Workouts with a result become RAN; a Workout has results exactly when it is RAN, and then all of them.

ALTER TABLE training_workouts
    DROP CONSTRAINT chk_workouts_status,
    DROP CONSTRAINT IF EXISTS chk_workouts_held_result_all_or_none;

UPDATE training_workouts SET status = 'RAN' WHERE status = 'HELD' AND actual_distance_meters IS NOT NULL;

ALTER TABLE training_workouts
    ADD CONSTRAINT chk_workouts_status CHECK (status IN ('SCHEDULED', 'HELD', 'RAN', 'CANCELLED')),
    ADD CONSTRAINT chk_workouts_ran_has_result CHECK (CASE WHEN status = 'RAN'
        THEN actual_distance_meters IS NOT NULL AND actual_duration_minutes IS NOT NULL
            AND top_speed_kmh IS NOT NULL AND average_speed_kmh IS NOT NULL
            AND average_heart_rate IS NOT NULL AND max_heart_rate IS NOT NULL
            AND recovery_heart_rate IS NOT NULL AND performance_rating IS NOT NULL
        ELSE actual_distance_meters IS NULL AND actual_duration_minutes IS NULL
            AND top_speed_kmh IS NULL AND average_speed_kmh IS NULL
            AND average_heart_rate IS NULL AND max_heart_rate IS NULL
            AND recovery_heart_rate IS NULL AND performance_rating IS NULL END);
