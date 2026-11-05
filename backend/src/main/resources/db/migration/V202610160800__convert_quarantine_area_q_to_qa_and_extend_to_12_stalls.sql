-- =====================================================================
-- V202610160800: CONVERT LEGACY QUARANTINE AREA 'Q' TO 'Q-A' AND EXTEND TO 12 STALLS
-- =====================================================================
-- 1. Convert legacy quarantine area code 'Q' to standardized 'Q-A'.
-- 2. Update existing stall codes from 'Q1'..'Q5' to 'Q-A-01'..'Q-A-05'.
-- 3. Extend quarantine area to 12 stalls (stalls 6..12 as AVAILABLE).
-- 4. Preserve all IDs, foreign keys (horses.current_stall_id,
--    admission_applications.quarantine_stall_id), groom assignments and statuses.
-- 5. Detect collisions if 'Q-A' already exists.
-- =====================================================================

DO $$
DECLARE
    v_q_id BIGINT;
    v_qa_id BIGINT;
BEGIN
    SELECT id INTO v_q_id FROM areas WHERE code = 'Q';
    SELECT id INTO v_qa_id FROM areas WHERE code = 'Q-A';

    -- Detect collision: if both Q and Q-A already exist
    IF v_q_id IS NOT NULL AND v_qa_id IS NOT NULL THEN
        RAISE EXCEPTION 'Both area Q (id=%) and Q-A (id=%) already exist in areas table', v_q_id, v_qa_id;
    END IF;

    -- If legacy Q exists, rename code to Q-A and name to 'Quarantine Area A'
    IF v_q_id IS NOT NULL THEN
        UPDATE areas
        SET code = 'Q-A',
            name = 'Quarantine Area A',
            updated_at = NOW()
        WHERE id = v_q_id;

        -- Update existing stall codes from legacy Q1..Q5 to Q-A-01..Q-A-05
        UPDATE stable_stalls
        SET stall_code = 'Q-A-' || LPAD(stall_number::text, 2, '0'),
            updated_at = NOW()
        WHERE area_id = v_q_id
          AND (stall_code ~ '^Q[0-9]+$' OR stall_code ~ '^Q-A-[0-9]+$')
          AND stall_code <> ('Q-A-' || LPAD(stall_number::text, 2, '0'));

        v_qa_id := v_q_id;
    END IF;

    -- If Q-A exists (either newly converted or pre-existing), extend to 12 stalls idempotently
    IF v_qa_id IS NOT NULL THEN
        INSERT INTO stable_stalls (area_id, stall_number, stall_code, status, created_at, updated_at)
        SELECT 
            v_qa_id, 
            n.stall_number, 
            'Q-A-' || LPAD(n.stall_number::text, 2, '0'), 
            'AVAILABLE',
            NOW(),
            NOW()
        FROM generate_series(1, 12) AS n(stall_number)
        WHERE NOT EXISTS (
            SELECT 1 FROM stable_stalls ss
            WHERE ss.area_id = v_qa_id AND ss.stall_number = n.stall_number
        );
    END IF;
END $$;
