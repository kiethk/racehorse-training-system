-- Normalize hydration_status free-text values onto the HydrationStatus enum
-- (HorseHealthMetric.hydrationStatus is now @Enumerated(STRING)).

UPDATE horse_health_metrics SET hydration_status = 'NORMAL'
WHERE lower(hydration_status) IN ('normal', 'optimal')
   OR hydration_status LIKE 'Normal (%';

UPDATE horse_health_metrics SET hydration_status = 'MILD_DEHYDRATION'
WHERE lower(hydration_status) IN ('mild dehydration', 'mild_dehydration');

-- Any other stray value would break enum reads; fold it into NORMAL.
UPDATE horse_health_metrics SET hydration_status = 'NORMAL'
WHERE hydration_status IS NOT NULL
  AND hydration_status NOT IN ('NORMAL', 'MILD_DEHYDRATION', 'MODERATE_DEHYDRATION', 'SEVERE_DEHYDRATION', 'ABNORMAL_CRT');
