-- =====================================================================
-- 46_showcase_vt02_vt03.sql — Bổ sung dữ liệu đa dạng cho Dr. Emily Wilson (VT02)
-- và Dr. Robert Hayes (VT03) hôm nay (today) và ngày mai (tomorrow).
-- Đảm bảo cả 3 bác sĩ đều có ca khám phong phú, đúng quota (ROUTINE <= 3, PREVENTIVE <= 3, INITIAL <= 2).
-- =====================================================================

DO $$
DECLARE
    v_cs RECORD;
BEGIN
    -- 1. Cho Dr. Robert Hayes (VT03, id=7)
    -- Dời các ca Routine cũ (quá khứ) của VT03 về hôm nay và ngày mai để có việc làm
    UPDATE care_schedule
       SET scheduled_date = pg_temp.today(),
           scheduled_at = NULL,
           status = 'SCHEDULED',
           rollover_count = 1,
           reschedule_reason = 'TIME_EXCEEDED',
           reschedule_note = 'Routine examination window closed at 15:30, rescheduled to next day'
     WHERE id = 113; -- Ngựa H039 SANTIAGO EGA

    UPDATE care_schedule
       SET scheduled_date = pg_temp.today(),
           scheduled_at = NULL,
           status = 'SCHEDULED',
           rollover_count = 0,
           reschedule_reason = NULL,
           reschedule_note = NULL
     WHERE id = 76; -- Ngựa H037 ENTULA

    -- VT03 thêm 1 ca Preventive hôm nay (kéo từ ngày 15/11 về hôm nay)
    UPDATE care_schedule
       SET scheduled_date = pg_temp.today(),
           scheduled_at = NULL,
           status = 'SCHEDULED',
           rollover_count = 0,
           reschedule_reason = NULL,
           reschedule_note = NULL
     WHERE id = 206; -- Ngựa H039 SANTIAGO EGA (PREVENTIVE)

    -- Ca 25 của VT03 xếp sang ngày mai (today + 1)
    UPDATE care_schedule
       SET scheduled_date = pg_temp.today() + 1,
           scheduled_at = NULL,
           status = 'SCHEDULED',
           rollover_count = 0,
           reschedule_reason = NULL,
           reschedule_note = NULL
     WHERE id = 25; -- Ngựa H012 BADEA MAFAL

    -- 2. Cho Dr. Emily Wilson (VT02, id=6)
    -- Kéo 1 ca Routine lâm sàng (id 106) từ 20/11 về hôm nay
    UPDATE care_schedule
       SET scheduled_date = pg_temp.today(),
           scheduled_at = NULL,
           status = 'SCHEDULED',
           rollover_count = 1,
           reschedule_reason = 'TIME_EXCEEDED',
           reschedule_note = 'Routine examination window closed at 15:30, rescheduled to next day'
     WHERE id = 106; -- Ngựa H014 MYSTICA MAHRIACHI

    -- Kéo 1 ca Routine lâm sàng (id 109) từ 22/11 về ngày mai
    UPDATE care_schedule
       SET scheduled_date = pg_temp.today() + 1,
           scheduled_at = NULL,
           status = 'SCHEDULED',
           rollover_count = 0,
           reschedule_reason = NULL,
           reschedule_note = NULL
     WHERE id = 109; -- Ngựa H020 CORALLO DA CLODIA

    -- 3. Cho Dr. James Carter (VT01, id=5)
    -- Thêm 1 ca Routine ngày mai để ngày mai cũng dày lịch
    -- Kéo ca Routine 57 về ngày mai (today + 1)
    UPDATE care_schedule
       SET scheduled_date = pg_temp.today() + 1,
           scheduled_at = NULL,
           status = 'SCHEDULED',
           rollover_count = 0,
           reschedule_reason = NULL,
           reschedule_note = NULL
     WHERE id = 57;

END $$;
