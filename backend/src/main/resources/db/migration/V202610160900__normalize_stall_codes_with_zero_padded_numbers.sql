-- Standardize existing stalls to A01..A12 and Q-A01..Q-A12.
-- Only codes/timestamps change; IDs, numbers, occupants, assignments,
-- reservations and historical foreign-key references are preserved.
-- Keep earlier migrations unchanged, including databases where they ran already.

LOCK TABLE areas, stable_stalls IN SHARE ROW EXCLUSIVE MODE;

DO $$
BEGIN
    -- Fail before renaming if existing Area/number combinations would collide.
    IF EXISTS (
        SELECT a.code || LPAD(ss.stall_number::text,
                             GREATEST(2, LENGTH(ss.stall_number::text)), '0')
        FROM stable_stalls ss
        JOIN areas a ON a.id = ss.area_id
        GROUP BY a.code || LPAD(ss.stall_number::text,
                               GREATEST(2, LENGTH(ss.stall_number::text)), '0')
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot normalize stall codes: duplicate target codes. No stalls were renamed.';
    END IF;

    -- Reserve a temporary namespace for two-phase renaming. This also handles
    -- an existing code held by another stall that is being renamed in this batch.
    IF EXISTS (
        SELECT 1
        FROM stable_stalls ss
        JOIN areas a ON a.id = ss.area_id
        JOIN stable_stalls holder
          ON holder.stall_code = '__rtms_pad_stall_' || ss.id::text || '__'
        WHERE ss.stall_code <> a.code || LPAD(ss.stall_number::text,
                                             GREATEST(2, LENGTH(ss.stall_number::text)), '0')
    ) THEN
        RAISE EXCEPTION 'Cannot normalize stall codes: temporary rename code already exists. No stalls were renamed.';
    END IF;
END $$;

UPDATE stable_stalls ss
SET stall_code = '__rtms_pad_stall_' || ss.id::text || '__'
FROM areas a
WHERE a.id = ss.area_id
  AND ss.stall_code <> a.code || LPAD(ss.stall_number::text,
                                     GREATEST(2, LENGTH(ss.stall_number::text)), '0');

UPDATE stable_stalls ss
SET stall_code = a.code || LPAD(ss.stall_number::text,
                               GREATEST(2, LENGTH(ss.stall_number::text)), '0'),
    updated_at = NOW()
FROM areas a
WHERE a.id = ss.area_id
  AND ss.stall_code <> a.code || LPAD(ss.stall_number::text,
                                     GREATEST(2, LENGTH(ss.stall_number::text)), '0');
