-- =====================================================================
-- 70_racing.sql — đề cử thi đấu của Trainer và quyết định của Manager.
-- (Race nominations by trainers and the manager's decisions.)
--
-- Luật đang giữ (RaceRegistrationService): ngựa nằm trong khu của Trainer, đang được tập (ELIGIBLE + ALLOWED) lúc đề cử;
-- ngày đua không ở quá khứ lúc đề cử; hạn đề cử không sau ngày đua; link nguồn là http(s); chỉ đơn PENDING mới được duyệt.
-- (Rules kept: the horse is in the trainer's area and may train when nominated; the race is not in the past at that
-- time; the deadline is not after the race; the source link is http(s); only PENDING nominations are reviewed.)
-- =====================================================================
CREATE FUNCTION pg_temp.nominate(p_horse TEXT, p_created INT, p_race TEXT, p_category TEXT, p_location TEXT,
                                 p_event INT, p_time TIME, p_organizer TEXT, p_url TEXT, p_deadline INT, p_distance INT,
                                 p_track TEXT, p_prize TEXT, p_reason TEXT, p_notes TEXT, p_status TEXT,
                                 p_feedback TEXT DEFAULT NULL) RETURNS VOID AS $$
DECLARE
    v_horse BIGINT := pg_temp.hid(p_horse);
    v_created TIMESTAMP := pg_temp.at(p_created, TIME '09:30');
    v_trainer BIGINT;
    v_reviewed TIMESTAMP;
BEGIN
    SELECT a.trainer_id INTO v_trainer FROM horses h JOIN stable_stalls s ON s.id = h.current_stall_id
      JOIN areas a ON a.id = s.area_id WHERE h.id = v_horse;
    IF pg_temp.blocked_at(v_horse, v_created) THEN
        RAISE EXCEPTION 'Horse % cannot be nominated on % while resting', p_horse, v_created;
    END IF;
    IF p_event < p_created OR p_deadline > p_event THEN
        RAISE EXCEPTION 'Nomination dates of % do not fit (created %, deadline %, race %)', p_horse, p_created, p_deadline, p_event;
    END IF;
    v_reviewed := CASE WHEN p_status <> 'PENDING' THEN v_created + INTERVAL '1 day 5 hours' END;
    INSERT INTO race_registrations (horse_id, trainer_id, race_name, location, event_date, distance_meters, track_type,
                                    trainer_notes, status, reviewed_by_id, manager_feedback, reviewed_at, created_at, updated_at,
                                    race_category, organizer, source_url, nomination_deadline, event_time, prize_details,
                                    selection_reason)
    VALUES (v_horse, v_trainer, p_race, p_location, pg_temp.today() + p_event, p_distance, p_track, p_notes, p_status,
            CASE WHEN v_reviewed IS NOT NULL THEN pg_temp.uid('ADMIN') END, p_feedback, v_reviewed, v_created,
            COALESCE(v_reviewed, v_created), p_category, p_organizer, p_url, pg_temp.today() + p_deadline, p_time, p_prize,
            p_reason);
    PERFORM pg_temp.audit(v_trainer, 'Race Registration', 'RaceRegistration', NULL,
                          'Submitted horse race entry registration', v_created, 'POST', '/api/race-registrations');
END $$ LANGUAGE plpgsql;

DO $$
BEGIN
    PERFORM pg_temp.nominate('H001', -40, 'Greenfield Autumn Sprint', 'Group 3 Sprint', 'Greenfield Racecourse', -20, TIME '15:30',
        'Greenfield Jockey Club', 'https://greenfield-racing.example/autumn-sprint', -27, 1200, 'TURF', 'Winner 120,000,000 VND',
        'Best 1,200 m trial times in the barn; ratings 8-9 in the last block.', 'Book a jockey who knows the turf course.', 'APPROVED',
        'Approved. Entry fee paid from the racing budget.');
    PERFORM pg_temp.nominate('H016', -35, 'Arabian Breeders Cup Juvenile', 'Arabian 3YO', 'Saigon Equestrian Park', -14, TIME '16:00',
        'Arabian Breeders Association', 'https://arabian-breeders.example/cup', -21, 1400, 'DIRT', 'Winner 80,000,000 VND',
        'Young filly improving quickly on dirt; good gate manners.', NULL, 'APPROVED', 'Approved. Ship two days early.');
    PERFORM pg_temp.nominate('H011', -30, 'City Mile Handicap', 'Handicap', 'Greenfield Racecourse', -10, TIME '14:45',
        'Greenfield Jockey Club', 'https://greenfield-racing.example/city-mile', -16, 1600, 'TURF', 'Winner 60,000,000 VND',
        'Steady stayer; the mile suits him.', NULL, 'REJECTED',
        'Rejected: he has a heavy block running that week; aim for the December meeting instead.');
    PERFORM pg_temp.nominate('H010', -6, 'Winter Classic Trial', 'Listed', 'Greenfield Racecourse', 21, TIME '15:00',
        'Greenfield Jockey Club', 'https://greenfield-racing.example/winter-classic', 12, 2000, 'TURF', 'Winner 200,000,000 VND',
        'Recovered fully from the cut; endurance ratings above 8 in this block.', 'Needs one more long gallop before the race.',
        'PENDING');
    PERFORM pg_temp.nominate('H008', -3, 'Ladies Arabian Stakes', 'Fillies and Mares', 'Saigon Equestrian Park', 28, TIME '16:30',
        'Arabian Breeders Association', 'https://arabian-breeders.example/ladies-stakes', 18, 1800, 'DIRT', 'Winner 90,000,000 VND',
        'Consistent mare with a strong finish on dirt.', NULL, 'PENDING');
    PERFORM pg_temp.nominate('H004', -12, 'Greenfield Novice Plate', 'Novice', 'Greenfield Racecourse', 9, TIME '14:00',
        'Greenfield Jockey Club', 'https://greenfield-racing.example/novice-plate', 5, 1000, 'SYNTHETIC', 'Winner 30,000,000 VND',
        'First run to gain race experience.', NULL, 'APPROVED', 'Approved, but watch her recovery heart rate after the fatigue alerts.');
    PERFORM pg_temp.nominate('H019', -5, 'Highland Endurance 40', 'Endurance', 'Da Lat Highland Trail', 30, TIME '06:30',
        'Vietnam Endurance Riders', 'https://endurance-vn.example/highland-40', 20, 40000, 'DIRT', 'Trophy and 25,000,000 VND',
        'Best stamina scores in the C area; calm in company.', NULL, 'PENDING');
END $$;
