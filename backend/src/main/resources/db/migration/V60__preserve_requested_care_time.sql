-- Requested dates must remain usable by offer allocation after consolidation.
ALTER TABLE care_schedule ADD COLUMN requested_at TIMESTAMP;
UPDATE care_schedule cs SET requested_at = (
    SELECT COALESCE((entry->'row'->>'requested_for_date')::date,
                    (entry->'row'->>'scheduled_date')::date)::timestamp + INTERVAL '14 hours'
    FROM jsonb_array_elements(cs.legacy_data) WITH ORDINALITY AS source(entry, ordinal)
    WHERE COALESCE(entry->'row'->>'requested_for_date', entry->'row'->>'scheduled_date') IS NOT NULL
    ORDER BY ordinal LIMIT 1
);
