package com.rtms.backend.training.repository;

import com.rtms.backend.training.dto.CourseRunSessionCountRow;
import com.rtms.backend.training.dto.SessionHorseNameRow;
import com.rtms.backend.training.dto.SessionRow;
import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {

    /**
     * Khóa lịch của một Trainer trong suốt transaction hiện tại (advisory lock), dùng chung khóa với mọi thao tác xếp giờ.
     * (Locks the schedule of one trainer for the current transaction (advisory lock), shared by every operation that places
     * a Session.)
     */
    @Query(value = "SELECT 1 FROM (SELECT pg_advisory_xact_lock(:key)) locked", nativeQuery = true)
    int lockSchedule(@Param("key") long key);

    List<TrainingSession> findByCourseRunIdOrderBySessionDateAscStartTimeAsc(Long courseRunId);

    List<TrainingSession> findByCourseRunIdInOrderBySessionDateAscStartTimeAsc(Collection<Long> courseRunIds);

    /** Mọi buổi chưa hủy của Trainer trong một ngày, theo giờ tăng dần. (A trainer's non-cancelled Sessions of a day, ascending.) */
    List<TrainingSession> findByTrainerIdAndSessionDateAndStatusNotOrderByStartTimeAsc(
            Long trainerId, LocalDate sessionDate, SessionStatus status);

    long countByCourseRunIdAndStatus(Long courseRunId, SessionStatus status);

    /** Buổi chưa hủy của Trainer trong một khoảng ngày, một truy vấn cho cả khóa. (A trainer's non-cancelled Sessions over a date range, one query for the whole course.) */
    List<TrainingSession> findByTrainerIdAndSessionDateBetweenAndStatusNot(
            Long trainerId, LocalDate from, LocalDate to, SessionStatus status);

    /** Số buổi của nhiều kế hoạch theo trạng thái bằng MỘT truy vấn. (Session counts of many course runs per status in ONE query.) */
    @Query("""
           SELECT new com.rtms.backend.training.dto.CourseRunSessionCountRow(s.courseRunId, s.status, COUNT(s))
           FROM TrainingSession s
           WHERE s.courseRunId IN :courseRunIds
           GROUP BY s.courseRunId, s.status
           """)
    List<CourseRunSessionCountRow> countByCourseRunAndStatus(@Param("courseRunIds") Collection<Long> courseRunIds);

    /**
     * Buổi SCHEDULED đã hết giờ thành HELD bằng MỘT câu UPDATE; trả về số buổi đã đổi.
     * (SCHEDULED Sessions whose end time passed become HELD with ONE UPDATE; returns how many changed.)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           UPDATE TrainingSession s
           SET s.status = com.rtms.backend.training.enums.SessionStatus.HELD
           WHERE s.status = com.rtms.backend.training.enums.SessionStatus.SCHEDULED
             AND (s.sessionDate < :today OR (s.sessionDate = :today AND s.endTime <= :time))
           """)
    int markEndedAsHeld(@Param("today") LocalDate today, @Param("time") LocalTime time);

    /**
     * Hủy mọi buổi SCHEDULED CHƯA bắt đầu của một kế hoạch bằng MỘT câu UPDATE; trả về số buổi đã hủy.
     * (Cancels every SCHEDULED Session of a course run that has NOT started with ONE UPDATE; returns how many were cancelled.)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           UPDATE TrainingSession s
           SET s.status = com.rtms.backend.training.enums.SessionStatus.CANCELLED
           WHERE s.courseRunId = :courseRunId
             AND s.status = com.rtms.backend.training.enums.SessionStatus.SCHEDULED
             AND (s.sessionDate > :today OR (s.sessionDate = :today AND s.startTime > :time))
           """)
    int cancelNotStartedOfCourseRun(@Param("courseRunId") Long courseRunId, @Param("today") LocalDate today, @Param("time") LocalTime time);

    /** Buổi SCHEDULED CHƯA bắt đầu của các kế hoạch đã cho, theo thứ tự. (SCHEDULED Sessions of the given course runs that have NOT started, in order.) */
    @Query("""
           SELECT s FROM TrainingSession s
           WHERE s.courseRunId IN :courseRunIds
             AND s.status = com.rtms.backend.training.enums.SessionStatus.SCHEDULED
             AND (s.sessionDate > :today OR (s.sessionDate = :today AND s.startTime > :time))
           ORDER BY s.sessionDate, s.startTime
           """)
    List<TrainingSession> findNotStartedOfCourseRuns(@Param("courseRunIds") Collection<Long> courseRunIds,
                                                @Param("today") LocalDate today, @Param("time") LocalTime time);

    List<TrainingSession> findByTrainerIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
            Long trainerId, LocalDate from, LocalDate to);

    List<TrainingSession> findBySessionDateBetweenOrderBySessionDateAscStartTimeAsc(LocalDate from, LocalDate to);

    /**
     * Buổi trong khoảng ngày có ít nhất một ngựa do Groom này dắt và chưa bị hủy; ngựa đã rời nhóm (Workout bị hủy) không kéo buổi
     * về lịch Groom.
     * (Sessions in a range with at least one horse this Groom leads whose Workout is not cancelled; a horse that left the group
     * (its Workout cancelled) does not pull the Session onto the Groom's calendar.)
     */
    @Query("""
           SELECT DISTINCT s FROM TrainingSession s
           JOIN TrainingWorkout w ON w.sessionId = s.id
           WHERE w.assignedToId = :groomId AND w.status <> com.rtms.backend.training.enums.WorkoutStatus.CANCELLED
             AND s.sessionDate BETWEEN :from AND :to
           ORDER BY s.sessionDate, s.startTime
           """)
    List<TrainingSession> findOfGroom(@Param("groomId") Long groomId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Buổi trong khoảng ngày có ít nhất một ngựa của Owner này chưa bị hủy Workout. (Sessions in a range with at least one horse of this Owner whose Workout is not cancelled.) */
    @Query("""
           SELECT DISTINCT s FROM TrainingSession s
           JOIN TrainingWorkout w ON w.sessionId = s.id
           JOIN Horse h ON w.horseId = h.id
           WHERE h.ownerId = :ownerId AND w.status <> com.rtms.backend.training.enums.WorkoutStatus.CANCELLED
             AND s.sessionDate BETWEEN :from AND :to
           ORDER BY s.sessionDate, s.startTime
           """)
    List<TrainingSession> findOfOwner(@Param("ownerId") Long ownerId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    // =================================================================
    // API XEM LỊCH: trả thẳng DTO, số ngựa đếm bằng sub-query
    // (SCHEDULE API: returns DTO rows directly, horse count computed by a sub-query)
    // =================================================================

    /** Phần SELECT dùng chung cho mọi truy vấn xem buổi. (SELECT part shared by every Session view query.) */
    String SESSION_ROW_SELECT = """
            SELECT new com.rtms.backend.training.dto.SessionRow(
                       s.id, s.trainerId, s.sessionDate, s.startTime, s.endTime, s.exerciseId, sub.name,
                       sub.durationMinutes, s.status,
                       (SELECT COUNT(w) FROM TrainingWorkout w
                        WHERE w.sessionId = s.id
                          AND w.status <> com.rtms.backend.training.enums.WorkoutStatus.CANCELLED))
            FROM TrainingSession s
            JOIN Exercise sub ON s.exerciseId = sub.id
            """;

    /** Lịch của Trainer. includeCancelled = false thì ẩn buổi đã hủy. (A Trainer's schedule; cancelled Sessions hidden unless asked.) */
    @Query(SESSION_ROW_SELECT + """
            WHERE s.trainerId = :trainerId
              AND s.sessionDate BETWEEN :from AND :to
              AND (:includeCancelled = true
                   OR s.status <> com.rtms.backend.training.enums.SessionStatus.CANCELLED)
            ORDER BY s.sessionDate ASC, s.startTime ASC
            """)
    List<SessionRow> findTrainerSessionRows(@Param("trainerId") Long trainerId,
                                            @Param("from") LocalDate from,
                                            @Param("to") LocalDate to,
                                            @Param("includeCancelled") boolean includeCancelled);

    /** Các buổi có ít nhất một con ngựa CỦA OWNER ĐÓ còn hiệu lực. (Sessions with at least one active horse BELONGING TO THAT OWNER.) */
    @Query(SESSION_ROW_SELECT + """
            WHERE s.sessionDate BETWEEN :from AND :to
              AND s.status <> com.rtms.backend.training.enums.SessionStatus.CANCELLED
              AND EXISTS (SELECT 1 FROM TrainingWorkout ow
                          JOIN Horse oh ON ow.horseId = oh.id
                          WHERE ow.sessionId = s.id
                            AND oh.ownerId = :ownerId
                            AND ow.status <> com.rtms.backend.training.enums.WorkoutStatus.CANCELLED)
            ORDER BY s.sessionDate ASC, s.startTime ASC
            """)
    List<SessionRow> findOwnerSessionRows(@Param("ownerId") Long ownerId,
                                          @Param("from") LocalDate from,
                                          @Param("to") LocalDate to);

    /** Tên các ngựa còn hiệu lực trong NHIỀU buổi bằng MỘT truy vấn. (Names of active horses in MANY Sessions with ONE query.) */
    @Query("""
           SELECT new com.rtms.backend.training.dto.SessionHorseNameRow(w.sessionId, h.id, h.name)
           FROM TrainingWorkout w
           JOIN Horse h ON w.horseId = h.id
           WHERE w.sessionId IN :sessionIds
             AND w.status <> com.rtms.backend.training.enums.WorkoutStatus.CANCELLED
           ORDER BY w.sessionId ASC, w.id ASC
           """)
    List<SessionHorseNameRow> findActiveHorseNames(@Param("sessionIds") Collection<Long> sessionIds);

    /** Như trên nhưng chỉ ngựa của một owner, để owner không thấy tên ngựa của chủ khác. (Same, but only one owner's horses.) */
    @Query("""
           SELECT new com.rtms.backend.training.dto.SessionHorseNameRow(w.sessionId, h.id, h.name)
           FROM TrainingWorkout w
           JOIN Horse h ON w.horseId = h.id
           WHERE w.sessionId IN :sessionIds
             AND h.ownerId = :ownerId
             AND w.status <> com.rtms.backend.training.enums.WorkoutStatus.CANCELLED
           ORDER BY w.sessionId ASC, w.id ASC
           """)
    List<SessionHorseNameRow> findActiveHorseNamesOfOwner(@Param("sessionIds") Collection<Long> sessionIds,
                                                          @Param("ownerId") Long ownerId);
}
