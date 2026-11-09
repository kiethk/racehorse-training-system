package com.rtms.backend.training.repository;

import com.rtms.backend.training.dto.GroomWorkoutRow;
import com.rtms.backend.training.dto.HorseFitnessTrendItemResponse;
import com.rtms.backend.training.dto.HorseScheduleItemResponse;
import com.rtms.backend.training.dto.PastWorkoutRow;
import com.rtms.backend.training.dto.PerformanceSessionRow;
import com.rtms.backend.training.dto.RecentWorkoutRow;
import com.rtms.backend.training.dto.SpecialtyWorkoutRow;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.WorkoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Truy vấn buổi tập (workout) của mô hình nhóm: Workout thuộc một Session, Session thuộc một Course Run.
 * (Workout queries of the group model: a Workout belongs to a Session, a Session belongs to a Course Run.)
 *
 * Các truy vấn đọc dùng JOIN + DTO projection để service không phải gọi DB nhiều lần và không phải ép kiểu Object[]. Dự án không
 * dùng quan hệ JPA, nên JOIN viết dạng "JOIN X ON ...".
 * (Read queries use JOIN + DTO projection so services do not call the DB repeatedly or cast Object[]. The project has no JPA
 * relations, so joins are written as "JOIN X ON ...".)
 *
 * "Đã có kết quả" nghĩa là Workout đã HELD và có số đo: ghi kết quả không đổi trạng thái, nên các truy vấn hiệu suất lọc theo số đo.
 * ("Has a result" means the Workout is HELD and has measurements: recording does not change the status, so the performance queries
 * filter on the measurements.)
 */
@Repository
public interface TrainingWorkoutRepository extends JpaRepository<TrainingWorkout, Long> {

    /**
     * Mọi Workout còn SCHEDULED của các ngựa từ một ngày trở đi, khóa dòng để cập nhật.
     * (Every SCHEDULED Workout of the horses from a date on, rows locked for update.)
     */
    @Query(value = """
           SELECT w.* FROM training_workouts w
           JOIN training_sessions s ON s.id = w.session_id
           WHERE w.horse_id IN :horseIds AND w.status = 'SCHEDULED'
             AND s.session_date >= :fromDate
           ORDER BY s.id, w.id FOR UPDATE OF s, w
           """, nativeQuery = true)
    List<TrainingWorkout> findFutureScheduledByHorsesForUpdate(
            @Param("horseIds") Collection<Long> horseIds, @Param("fromDate") LocalDate fromDate);

    List<TrainingWorkout> findBySessionId(Long sessionId);

    List<TrainingWorkout> findBySessionIdIn(Collection<Long> sessionIds);

    long countBySessionIdAndStatusNot(Long sessionId, WorkoutStatus status);

    /**
     * Workout của một chiến mã trong các buổi đến hôm nay đã diễn ra hoặc đã hủy, mới nhất trước. Workout được tạo một lượt khi giao
     * khóa, nên thứ tự phải theo ngày của buổi chứ không theo ngày tạo.
     * (A horse's Workouts in Sessions up to today that took place or were cancelled, newest first. Workouts are all created when a
     * course is assigned, so the order follows the Session date, never the creation date.)
     */
    @Query("""
           SELECT new com.rtms.backend.training.dto.PastWorkoutRow(
                      w, s.sessionDate, s.startTime, s.endTime, sub.name, g.name,
                      CASE WHEN g.disbandedAt IS NULL THEN false ELSE true END, s.status)
           FROM TrainingWorkout w
           JOIN TrainingSession s ON w.sessionId = s.id
           JOIN Exercise sub ON s.exerciseId = sub.id
           JOIN CourseRun p ON s.courseRunId = p.id
           LEFT JOIN TrainingGroup g ON p.groupId = g.id
           WHERE w.horseId = :horseId
             AND s.status <> com.rtms.backend.training.enums.SessionStatus.SCHEDULED
             AND s.sessionDate <= :today
           ORDER BY s.sessionDate DESC, s.startTime DESC
           """)
    List<PastWorkoutRow> findPastWorkouts(@Param("horseId") Long horseId, @Param("today") LocalDate today);

    /** Mọi Workout của chiến mã, mọi trạng thái. (Every Workout of a horse, any status.) */
    List<TrainingWorkout> findByHorseIdAndSessionIdIsNotNull(Long horseId);

    /**
     * Lịch tập của MỘT con ngựa trong khoảng ngày, kể cả buổi đã hủy, theo thứ tự ngày rồi giờ.
     * (One horse schedule over a date range, cancelled Sessions included, in date then time order.)
     */
    @Query("""
           SELECT new com.rtms.backend.training.dto.HorseScheduleItemResponse(
                      s.id, s.sessionDate, s.startTime, s.endTime, sub.name, s.status, w.status)
           FROM TrainingWorkout w
           JOIN TrainingSession s ON w.sessionId = s.id
           JOIN Exercise sub ON s.exerciseId = sub.id
           WHERE w.horseId = :horseId
             AND s.sessionDate BETWEEN :fromDate AND :toDate
           ORDER BY s.sessionDate ASC, s.startTime ASC
           """)
    List<HorseScheduleItemResponse> findHorseSchedule(@Param("horseId") Long horseId,
                                                      @Param("fromDate") LocalDate fromDate,
                                                      @Param("toDate") LocalDate toDate);

    /**
     * Nguồn 2 của màn hình Today Tasks (Groom): buổi tập trong ngày kèm giờ, tên ngựa, tên bài tập.
     * (Source 2 of the groom Today Tasks screen: the day workouts with times, horse and exercise names.)
     */
    @Query("""
           SELECT new com.rtms.backend.training.dto.GroomWorkoutRow(
                      w.id, w.horseId, h.name, w.status, s.startTime, s.endTime, sub.name)
           FROM TrainingWorkout w
           JOIN TrainingSession s ON w.sessionId = s.id
           JOIN Horse h ON w.horseId = h.id
           JOIN Exercise sub ON s.exerciseId = sub.id
           WHERE w.assignedToId = :groomId
             AND s.sessionDate = :date
             AND w.status <> com.rtms.backend.training.enums.WorkoutStatus.CANCELLED
           ORDER BY s.startTime ASC
           """)
    List<GroomWorkoutRow> findGroomWorkoutRows(@Param("groomId") Long groomId,
                                               @Param("date") LocalDate date);

    /**
     * Biểu đồ thể lực: các buổi đã có kết quả của một chiến mã theo ngày tăng dần. Khoảng ngày lọc ngay trong SQL, service truyền
     * mốc bao trùm khi người dùng bỏ trống from/to.
     * (Fitness chart: the Workouts of a horse that have results, by ascending date. The date range is filtered in SQL, and the
     * service passes wide bounds when the user leaves from/to empty.)
     */
    @Query("""
           SELECT new com.rtms.backend.training.dto.HorseFitnessTrendItemResponse(
                      w.id, s.sessionDate, sub.name, w.actualDistanceMeters, w.actualDurationMinutes,
                      w.averageSpeedKmh, w.topSpeedKmh, w.averageHeartRate, w.maxHeartRate,
                      w.recoveryHeartRate, w.performanceRating)
           FROM TrainingWorkout w
           JOIN TrainingSession s ON w.sessionId = s.id
           JOIN Exercise sub ON s.exerciseId = sub.id
           WHERE w.horseId = :horseId
             AND w.status = com.rtms.backend.training.enums.WorkoutStatus.RAN
             AND s.sessionDate BETWEEN :fromDate AND :toDate
           ORDER BY s.sessionDate ASC, s.startTime ASC
           """)
    List<HorseFitnessTrendItemResponse> findFitnessTrend(@Param("horseId") Long horseId,
                                                         @Param("fromDate") LocalDate fromDate,
                                                         @Param("toDate") LocalDate toDate);

    /**
     * Các buổi đã có kết quả của một tập chiến mã trong khoảng ngày, mỗi buổi một dòng, một câu truy vấn cho cả tập. Việc gom theo
     * tuần làm ở PerformanceTrendCalculator.
     * (The Workouts with results of a set of horses in a date range, one row each, one query for the whole set. Grouping by week is
     * done in PerformanceTrendCalculator.)
     */
    @Query("""
           SELECT new com.rtms.backend.training.dto.PerformanceSessionRow(
                      w.horseId, s.sessionDate, w.performanceRating, w.averageSpeedKmh, w.maxHeartRate)
           FROM TrainingWorkout w
           JOIN TrainingSession s ON w.sessionId = s.id
           WHERE w.horseId IN :horseIds
             AND w.status = com.rtms.backend.training.enums.WorkoutStatus.RAN
             AND s.sessionDate BETWEEN :fromDate AND :toDate
           ORDER BY s.sessionDate ASC
           """)
    List<PerformanceSessionRow> findPerformanceSessions(@Param("horseIds") Collection<Long> horseIds,
                                                        @Param("fromDate") LocalDate fromDate,
                                                        @Param("toDate") LocalDate toDate);

    /**
     * Buổi đã có kết quả từ một mốc ngày của NHIỀU ngựa, mới nhất trước: nguồn cho bộ luật cảnh báo và dashboard.
     * (Workouts with results since a date for MANY horses, newest first: the source for the alert rules and the dashboard.)
     */
    @Query("""
           SELECT new com.rtms.backend.training.dto.RecentWorkoutRow(
                      w.horseId, s.sessionDate, s.endTime, w.maxHeartRate, w.averageHeartRate, w.recoveryHeartRate, w.performanceRating)
           FROM TrainingWorkout w
           JOIN TrainingSession s ON w.sessionId = s.id
           WHERE w.horseId IN :horseIds
             AND w.status = com.rtms.backend.training.enums.WorkoutStatus.RAN
             AND s.sessionDate >= :since
           ORDER BY w.horseId ASC, s.sessionDate DESC, s.startTime DESC
           """)
    List<RecentWorkoutRow> findRecentCompletedWorkouts(@Param("horseIds") Collection<Long> horseIds,
                                                       @Param("since") LocalDate since);

    /**
     * Các buổi tập dùng để suy ra sở trường của NHIỀU ngựa: đã có kết quả, có điểm phong độ, có cự ly (thực tế, hoặc cự ly mục tiêu
     * của bài tập), không thuộc nhóm hồi phục, từ một mốc ngày.
     * (The Workouts used to infer the specialty of MANY horses: with results, with a rating, with a distance (actual, or the
     * Exercise target distance), not in the recovery category, since a date.)
     */
    @Query("""
           SELECT new com.rtms.backend.training.dto.SpecialtyWorkoutRow(
                      w.horseId, COALESCE(w.actualDistanceMeters, sub.targetDistanceMeters),
                      sub.surfaceType, w.performanceRating, w.topSpeedKmh)
           FROM TrainingWorkout w
           JOIN TrainingSession s ON w.sessionId = s.id
           JOIN Exercise sub ON s.exerciseId = sub.id
           WHERE w.horseId IN :horseIds
             AND w.status = com.rtms.backend.training.enums.WorkoutStatus.RAN
             AND w.performanceRating IS NOT NULL
             AND sub.category <> com.rtms.backend.training.enums.ExerciseCategory.RECOVERY
             AND s.sessionDate >= :since
           """)
    List<SpecialtyWorkoutRow> findSpecialtyWorkouts(@Param("horseIds") Collection<Long> horseIds,
                                                    @Param("since") LocalDate since);

    /** Workout này có thuộc một buổi của Trainer này không? (Does this Workout belong to a Session of this Trainer?) */
    @Query("""
           SELECT w FROM TrainingWorkout w
           JOIN TrainingSession s ON w.sessionId = s.id
           WHERE w.id = :workoutId AND s.trainerId = :trainerId
           """)
    Optional<TrainingWorkout> findByIdAndTrainerId(@Param("workoutId") Long workoutId, @Param("trainerId") Long trainerId);

    /** Workout còn SCHEDULED của các buổi đã HELD thành HELD. (SCHEDULED Workouts of HELD Sessions become HELD.) */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           UPDATE TrainingWorkout w
           SET w.status = com.rtms.backend.training.enums.WorkoutStatus.HELD
           WHERE w.status = com.rtms.backend.training.enums.WorkoutStatus.SCHEDULED
             AND w.sessionId IN (SELECT s.id FROM TrainingSession s
                                 WHERE s.status = com.rtms.backend.training.enums.SessionStatus.HELD)
           """)
    int markWorkoutsOfHeldSessionsAsHeld();

    /** Hủy các Workout còn SCHEDULED của một buổi đã bị hủy. (Cancels the SCHEDULED Workouts of a cancelled Session.) */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           UPDATE TrainingWorkout w
           SET w.status = com.rtms.backend.training.enums.WorkoutStatus.CANCELLED
           WHERE w.status = com.rtms.backend.training.enums.WorkoutStatus.SCHEDULED
             AND w.sessionId IN (SELECT s.id FROM TrainingSession s
                                 WHERE s.status = com.rtms.backend.training.enums.SessionStatus.CANCELLED)
           """)
    int cancelWorkoutsOfCancelledSessions();

    /** Workout còn SCHEDULED của một chiến mã trong các buổi đã cho. (A horse's SCHEDULED Workouts in the given Sessions.) */
    @Query("""
           SELECT w FROM TrainingWorkout w
           WHERE w.horseId = :horseId AND w.sessionId IN :sessionIds
             AND w.status = com.rtms.backend.training.enums.WorkoutStatus.SCHEDULED
           """)
    List<TrainingWorkout> findScheduledOfHorseInSessions(@Param("horseId") Long horseId,
                                                         @Param("sessionIds") Collection<Long> sessionIds);

    /**
     * Workout còn SCHEDULED của các ngựa trong những buổi CHƯA bắt đầu: phần việc còn đổi được (Groom, hủy). Buổi đã bắt đầu hay đã
     * diễn ra là LỊCH SỬ nên không nằm trong đây.
     * (The SCHEDULED Workouts of the horses in Sessions that have NOT started: the part of the work that can still change (Groom,
     * cancelling). A Session that started or took place is HISTORY, so it is not here.)
     */
    @Query("""
           SELECT w FROM TrainingWorkout w
           JOIN TrainingSession s ON w.sessionId = s.id
           WHERE w.horseId IN :horseIds
             AND w.status = com.rtms.backend.training.enums.WorkoutStatus.SCHEDULED
             AND s.status = com.rtms.backend.training.enums.SessionStatus.SCHEDULED
             AND (s.sessionDate > :today OR (s.sessionDate = :today AND s.startTime > :time))
           """)
    List<TrainingWorkout> findNotStartedOfHorses(@Param("horseIds") Collection<Long> horseIds,
                                                 @Param("today") LocalDate today, @Param("time") LocalTime time);

    @Query("""
           SELECT new com.rtms.backend.analytics.dto.FleetWorkoutSummaryRow(
               w.horseId,
               AVG(w.averageSpeedKmh),
               AVG(w.recoveryHeartRate),
               MAX(w.maxHeartRate)
           )
           FROM TrainingWorkout w
           JOIN TrainingSession s ON w.sessionId = s.id
           WHERE w.horseId IN :horseIds
             AND w.status = com.rtms.backend.training.enums.WorkoutStatus.RAN
             AND w.averageSpeedKmh IS NOT NULL
             AND w.recoveryHeartRate IS NOT NULL
             AND s.sessionDate >= :since
           GROUP BY w.horseId
           """)
    List<com.rtms.backend.analytics.dto.FleetWorkoutSummaryRow> findFleetWorkoutSummaries(
            @Param("horseIds") java.util.Collection<Long> horseIds,
            @Param("since") java.time.LocalDate since);
}
