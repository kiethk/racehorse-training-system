package com.rtms.backend.training.repository;

import com.rtms.backend.training.dto.PlanSessionCountRow;
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

    List<TrainingSession> findByGroupPlanIdOrderBySessionDateAscStartTimeAsc(Long groupPlanId);

    List<TrainingSession> findByGroupPlanIdInOrderBySessionDateAscStartTimeAsc(Collection<Long> groupPlanIds);

    /** Mọi buổi chưa hủy của Trainer trong một ngày, theo giờ tăng dần. (A trainer's non-cancelled Sessions of a day, ascending.) */
    List<TrainingSession> findByTrainerIdAndSessionDateAndStatusNotOrderByStartTimeAsc(
            Long trainerId, LocalDate sessionDate, SessionStatus status);

    long countByGroupPlanIdAndStatus(Long groupPlanId, SessionStatus status);

    /** Buổi chưa hủy của Trainer trong một khoảng ngày, một truy vấn cho cả khóa. (A trainer's non-cancelled Sessions over a date range, one query for the whole course.) */
    List<TrainingSession> findByTrainerIdAndSessionDateBetweenAndStatusNot(
            Long trainerId, LocalDate from, LocalDate to, SessionStatus status);

    /** Số buổi của nhiều kế hoạch theo trạng thái bằng MỘT truy vấn. (Session counts of many plans per status in ONE query.) */
    @Query("""
           SELECT new com.rtms.backend.training.dto.PlanSessionCountRow(s.groupPlanId, s.status, COUNT(s))
           FROM TrainingSession s
           WHERE s.groupPlanId IN :planIds
           GROUP BY s.groupPlanId, s.status
           """)
    List<PlanSessionCountRow> countByPlanAndStatus(@Param("planIds") Collection<Long> planIds);

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
     * (Cancels every SCHEDULED Session of a plan that has NOT started with ONE UPDATE; returns how many were cancelled.)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           UPDATE TrainingSession s
           SET s.status = com.rtms.backend.training.enums.SessionStatus.CANCELLED
           WHERE s.groupPlanId = :planId
             AND s.status = com.rtms.backend.training.enums.SessionStatus.SCHEDULED
             AND (s.sessionDate > :today OR (s.sessionDate = :today AND s.startTime > :time))
           """)
    int cancelNotStartedOfPlan(@Param("planId") Long planId, @Param("today") LocalDate today, @Param("time") LocalTime time);

    /** Buổi SCHEDULED CHƯA bắt đầu của các kế hoạch đã cho, theo thứ tự. (SCHEDULED Sessions of the given plans that have NOT started, in order.) */
    @Query("""
           SELECT s FROM TrainingSession s
           WHERE s.groupPlanId IN :planIds
             AND s.status = com.rtms.backend.training.enums.SessionStatus.SCHEDULED
             AND (s.sessionDate > :today OR (s.sessionDate = :today AND s.startTime > :time))
           ORDER BY s.sessionDate, s.startTime
           """)
    List<TrainingSession> findNotStartedOfPlans(@Param("planIds") Collection<Long> planIds,
                                                @Param("today") LocalDate today, @Param("time") LocalTime time);

    List<TrainingSession> findByTrainerIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
            Long trainerId, LocalDate from, LocalDate to);

    List<TrainingSession> findBySessionDateBetweenOrderBySessionDateAscStartTimeAsc(LocalDate from, LocalDate to);

    /** Buổi trong khoảng ngày có ít nhất một ngựa do Groom này dắt. (Sessions in a range with at least one horse this Groom leads.) */
    @Query("""
           SELECT DISTINCT s FROM TrainingSession s
           JOIN TrainingWorkout w ON w.sessionId = s.id
           WHERE w.assignedToId = :groomId AND s.sessionDate BETWEEN :from AND :to
           ORDER BY s.sessionDate, s.startTime
           """)
    List<TrainingSession> findOfGroom(@Param("groomId") Long groomId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Buổi trong khoảng ngày có ít nhất một ngựa của Owner này. (Sessions in a range with at least one horse of this Owner.) */
    @Query("""
           SELECT DISTINCT s FROM TrainingSession s
           JOIN TrainingWorkout w ON w.sessionId = s.id
           JOIN Horse h ON w.horseId = h.id
           WHERE h.ownerId = :ownerId AND s.sessionDate BETWEEN :from AND :to
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
                       s.id, s.trainerId, s.sessionDate, s.startTime, s.endTime, s.subjectId, sub.name,
                       sub.durationMinutes, s.status,
                       (SELECT COUNT(w) FROM TrainingWorkout w
                        WHERE w.sessionId = s.id
                          AND w.status <> com.rtms.backend.training.enums.WorkoutStatus.CANCELLED))
            FROM TrainingSession s
            JOIN Subject sub ON s.subjectId = sub.id
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
