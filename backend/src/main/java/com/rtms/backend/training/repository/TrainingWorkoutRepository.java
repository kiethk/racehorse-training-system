package com.rtms.backend.training.repository;

import com.rtms.backend.training.dto.*;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.WorkoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Truy vấn buổi tập (workout).
 * (Workout queries.)
 *
 * Các truy vấn đọc dùng JOIN + DTO projection để service không phải gọi DB nhiều lần
 * và không phải ép kiểu Object[]. Dự án không dùng quan hệ JPA, nên JOIN viết dạng "JOIN X ON ...".
 * (Read queries use JOIN + DTO projection so services do not call the DB repeatedly or cast
 * Object[]. The project has no JPA relations, so joins are written as "JOIN X ON ...".)
 */
@Repository
public interface TrainingWorkoutRepository extends JpaRepository<TrainingWorkout, Long> {

    @Query(value = """
           SELECT w.* FROM training_workouts w
           JOIN training_lots l ON l.id = w.lot_id
           WHERE w.horse_id IN :horseIds AND w.status = 'SCHEDULED'
             AND l.lot_date >= :fromDate
           ORDER BY l.id, w.id FOR UPDATE OF l, w
           """, nativeQuery = true)
    List<TrainingWorkout> findFutureScheduledByHorsesForUpdate(
            @Param("horseIds") Collection<Long> horseIds, @Param("fromDate") LocalDate fromDate);

    @Query(value = """
           SELECT w.* FROM training_workouts w
           WHERE w.lot_id IN :lotIds AND w.status <> 'CANCELLED'
           ORDER BY w.id FOR UPDATE
           """, nativeQuery = true)
    List<TrainingWorkout> findNonCancelledByLotsForUpdate(@Param("lotIds") Collection<Long> lotIds);

    // BR-02 (Trainer không dạy 2 ngựa cùng lúc) không cần query riêng: thuật toán xếp khe trong
    // TrainingLotService không cho tạo lot đè lên lot khác.
    // (BR-02 needs no query of its own: the slot algorithm in TrainingLotService never creates
    // overlapping lots.)

    List<TrainingWorkout> findByLotId(Long lotId);

    /** BR-10 — đếm số ngựa đang chiếm chỗ trong lot. (BR-10 — count horses occupying a lot.) */
    long countByLotIdAndStatusNot(Long lotId, WorkoutStatus status);

    /** Đếm các buổi tập trong lot chưa xong. (Count unfinished workouts in a lot.) */
    long countByLotIdAndStatusNotIn(Long lotId, Collection<WorkoutStatus> statuses);

    /** BR-09 — Groom này đã phụ trách con nào khác trong cùng lot chưa? (BR-09 — is this groom already leading a horse in the lot?) */
    boolean existsByLotIdAndAssignedToIdAndStatusNot(Long lotId,
                                                     Long assignedToId,
                                                     WorkoutStatus status);

    long countByPlanIdAndStatusNotIn(Long planId, Collection<WorkoutStatus> statuses);

    long countByPlanIdAndStatus(Long planId, WorkoutStatus status);

    /**
     * Buổi SCHEDULED của một kế hoạch mà lot CHƯA kết thúc; buổi đã qua giờ thì đã (hoặc đáng lẽ đã) diễn ra nên không
     * nằm trong đây. (SCHEDULED workouts of a plan whose lot has NOT ended; a workout past its time has (or should have)
     * taken place, so it is not included.)
     */
    @Query("""
           SELECT w FROM TrainingWorkout w
           JOIN TrainingLot l ON w.lotId = l.id
           WHERE w.planId = :planId
             AND w.status = com.rtms.backend.training.enums.WorkoutStatus.SCHEDULED
             AND (l.lotDate > :today OR (l.lotDate = :today AND l.endTime > :now))
           """)
    List<TrainingWorkout> findNotYetHeldByPlan(@Param("planId") Long planId,
                                               @Param("today") LocalDate today,
                                               @Param("now") LocalTime now);

    /**
     * Mọi buổi CHƯA diễn ra của một chiến mã (lot chưa kết thúc), cho cascade khóa tập và đổi Groom. Lot sáng nay đã
     * xong mà chưa ghi kết quả không nằm trong đây: buổi đó đã diễn ra, phải được ghi chứ không bị hủy hay đổi người.
     * (Every not-yet-held workout of a horse (lot not ended), for the training-block cascade and groom changes. A lot
     * that ended this morning without a result is not included: it took place, so it is recorded, not cancelled or
     * handed to someone else.)
     */
    @Query("""
           SELECT w FROM TrainingWorkout w
           JOIN TrainingLot l ON w.lotId = l.id
           WHERE w.horseId = :horseId
             AND w.status = com.rtms.backend.training.enums.WorkoutStatus.SCHEDULED
             AND (l.lotDate > :today OR (l.lotDate = :today AND l.endTime > :now))
           """)
    List<TrainingWorkout> findNotYetHeldByHorse(@Param("horseId") Long horseId,
                                                @Param("today") LocalDate today,
                                                @Param("now") LocalTime now);

    /**
     * Các lot mà Groom mới sẽ bị trùng (BR-09) nếu nhận con ngựa này, kèm tên con ngựa đang vướng.
     * Một câu JOIN thay cho vòng lặp "exists + findById" trước đây.
     * (Lots where the new groom would clash (BR-09) if given this horse, with the name of the
     * clashing horse. One JOIN replaces the earlier "exists + findById" loop.)
     */
    @Query("""
           SELECT new com.rtms.backend.training.dto.GroomConflictRow(l.id, l.lotDate, l.startTime, h.name)
           FROM TrainingWorkout mine
           JOIN TrainingLot l ON mine.lotId = l.id
           JOIN TrainingWorkout other ON other.lotId = l.id
           JOIN Horse h ON other.horseId = h.id
           WHERE mine.horseId = :horseId
             AND mine.status = com.rtms.backend.training.enums.WorkoutStatus.SCHEDULED
             AND l.lotDate >= :fromDate
             AND other.assignedToId = :groomId
             AND other.horseId <> :horseId
             AND other.status <> com.rtms.backend.training.enums.WorkoutStatus.CANCELLED
           ORDER BY l.lotDate ASC, l.startTime ASC
           """)
    List<GroomConflictRow> findGroomConflicts(@Param("horseId") Long horseId,
                                              @Param("groomId") Long groomId,
                                              @Param("fromDate") LocalDate fromDate);

    /**
     * Nguồn 2 của màn hình Today Tasks (Groom): buổi tập trong ngày kèm giờ lot, tên ngựa, tên bài tập.
     * (Source 2 of the groom Today Tasks screen: the day workouts with lot times, horse and subject names.)
     */
    @Query("""
           SELECT new com.rtms.backend.training.dto.GroomWorkoutRow(
                      w.id, w.horseId, h.name, w.status, l.startTime, l.endTime, s.name)
           FROM TrainingWorkout w
           JOIN TrainingLot l ON w.lotId = l.id
           JOIN Horse h ON w.horseId = h.id
           JOIN Subject s ON l.subjectId = s.id
           WHERE w.assignedToId = :groomId
             AND l.lotDate = :date
             AND w.status <> com.rtms.backend.training.enums.WorkoutStatus.CANCELLED
           ORDER BY l.startTime ASC
           """)
    List<GroomWorkoutRow> findGroomWorkoutRows(@Param("groomId") Long groomId,
                                               @Param("date") LocalDate date);

    /**
     * Các buổi tập của một kế hoạch theo thời gian thật; số ngựa trong lot được đếm bằng sub-query.
     * (A plan workouts in real time order; the lot horse count comes from a sub-query.)
     */
    @Query("""
           SELECT new com.rtms.backend.training.dto.PlanWorkoutItemResponse(
                      w.id, l.id, l.lotDate, l.startTime, l.endTime, l.subjectId, s.name, s.surfaceType,
                      w.horseId, w.assignedToId, w.status,
                      w.actualDistanceMeters, w.actualDurationMinutes, w.averageSpeedKmh,
                      w.averageHeartRate, w.maxHeartRate, w.recoveryHeartRate, w.topSpeedKmh,
                      w.performanceRating, w.trainerFeedback,
                      (SELECT COUNT(w2) FROM TrainingWorkout w2
                       WHERE w2.lotId = l.id
                         AND w2.status <> com.rtms.backend.training.enums.WorkoutStatus.CANCELLED))
           FROM TrainingWorkout w
           JOIN TrainingLot l ON w.lotId = l.id
           JOIN Subject s ON l.subjectId = s.id
           WHERE w.planId = :planId
           ORDER BY l.lotDate ASC, l.startTime ASC
           """)
    List<PlanWorkoutItemResponse> findPlanWorkoutItems(@Param("planId") Long planId);

    /**
     * Biểu đồ thể lực: các buổi đã hoàn thành của một chiến mã theo ngày tăng dần.
     * Khoảng ngày lọc ngay trong SQL, service truyền mốc bao trùm khi người dùng bỏ trống from/to.
     * (Fitness chart: completed workouts of a horse by ascending date. The date range is filtered
     * in SQL, and the service passes wide bounds when the user leaves from/to empty.)
     */
    @Query("""
           SELECT new com.rtms.backend.training.dto.HorseFitnessTrendItemResponse(
                      w.id, l.lotDate, s.name, w.actualDistanceMeters, w.actualDurationMinutes,
                      w.averageSpeedKmh, w.topSpeedKmh, w.averageHeartRate, w.maxHeartRate,
                      w.recoveryHeartRate, w.performanceRating)
           FROM TrainingWorkout w
           JOIN TrainingLot l ON w.lotId = l.id
           JOIN Subject s ON l.subjectId = s.id
           WHERE w.horseId = :horseId
             AND w.status = com.rtms.backend.training.enums.WorkoutStatus.COMPLETED
             AND l.lotDate BETWEEN :fromDate AND :toDate
           ORDER BY l.lotDate ASC, l.startTime ASC
           """)
    List<HorseFitnessTrendItemResponse> findFitnessTrend(@Param("horseId") Long horseId,
                                                         @Param("fromDate") LocalDate fromDate,
                                                         @Param("toDate") LocalDate toDate);

    /**
     * Các buổi đã hoàn thành của một tập chiến mã trong khoảng ngày, mỗi buổi một dòng, một câu truy vấn cho cả tập thay vì
     * gọi biểu đồ của từng con. Việc gom theo tuần làm ở PerformanceTrendCalculator.
     * (The completed sessions of a set of horses in a date range, one row per session, one query for the whole set
     * instead of calling each horse chart. Grouping by week is done in PerformanceTrendCalculator.)
     */
    @Query("""
           SELECT new com.rtms.backend.training.dto.PerformanceSessionRow(
                      w.horseId, l.lotDate, w.performanceRating, w.averageSpeedKmh, w.maxHeartRate)
           FROM TrainingWorkout w
           JOIN TrainingLot l ON w.lotId = l.id
           WHERE w.horseId IN :horseIds
             AND w.status = com.rtms.backend.training.enums.WorkoutStatus.COMPLETED
             AND l.lotDate BETWEEN :fromDate AND :toDate
           ORDER BY l.lotDate ASC
           """)
    List<PerformanceSessionRow> findPerformanceSessions(@Param("horseIds") Collection<Long> horseIds,
                                                        @Param("fromDate") LocalDate fromDate,
                                                        @Param("toDate") LocalDate toDate);

    /**
     * Buổi đã hoàn thành từ một mốc ngày của NHIỀU ngựa, mới nhất trước: nguồn cho bộ luật cảnh báo
     * và dashboard. Lọc ngày ngay trong SQL thay vì tải toàn bộ lịch sử rồi lọc bằng stream.
     * (Completed workouts since a date for MANY horses, newest first: the source for the alert rules
     * and the dashboard. The date filter runs in SQL instead of loading all history and filtering.)
     */
    @Query("""
           SELECT new com.rtms.backend.training.dto.RecentWorkoutRow(
                      w.horseId, l.lotDate, l.endTime, w.maxHeartRate, w.recoveryHeartRate, w.performanceRating)
           FROM TrainingWorkout w
           JOIN TrainingLot l ON w.lotId = l.id
           WHERE w.horseId IN :horseIds
             AND w.status = com.rtms.backend.training.enums.WorkoutStatus.COMPLETED
             AND l.lotDate >= :since
           ORDER BY w.horseId ASC, l.lotDate DESC, l.startTime DESC
           """)
    List<RecentWorkoutRow> findRecentCompletedWorkouts(@Param("horseIds") Collection<Long> horseIds,
                                                       @Param("since") LocalDate since);

    /**
     * Các buổi tập dùng để suy ra sở trường của NHIỀU ngựa: đã hoàn thành, có điểm phong độ, có cự ly (thực tế, hoặc
     * cự ly mục tiêu của bài tập), không thuộc nhóm hồi phục, từ một mốc ngày. Mặt sân lấy từ bài tập của lot.
     * (The workouts used to infer the specialty of MANY horses: completed, with a rating, with a distance (actual,
     * or the subject target distance), not in the recovery category, since a date. The surface comes from the
     * subject of the lot.)
     */
    @Query("""
           SELECT new com.rtms.backend.training.dto.SpecialtyWorkoutRow(
                      w.horseId, COALESCE(w.actualDistanceMeters, s.targetDistanceMeters),
                      s.surfaceType, w.performanceRating, w.topSpeedKmh)
           FROM TrainingWorkout w
           JOIN TrainingLot l ON w.lotId = l.id
           JOIN Subject s ON l.subjectId = s.id
           WHERE w.horseId IN :horseIds
             AND w.status = com.rtms.backend.training.enums.WorkoutStatus.COMPLETED
             AND w.performanceRating IS NOT NULL
             AND COALESCE(w.actualDistanceMeters, s.targetDistanceMeters) IS NOT NULL
             AND s.category <> com.rtms.backend.training.enums.SubjectCategory.RECOVERY
             AND l.lotDate >= :since
           """)
    List<SpecialtyWorkoutRow> findSpecialtyWorkouts(@Param("horseIds") Collection<Long> horseIds,
                                                    @Param("since") LocalDate since);

    /**
     * Owner có ít nhất một con ngựa còn hiệu lực trong lot này không? Dùng để kiểm quyền xem một lot.
     * (Does this owner have at least one active horse in this lot? Used to authorize viewing a lot.)
     */
    @Query("""
           SELECT COUNT(w) > 0
           FROM TrainingWorkout w
           JOIN Horse h ON w.horseId = h.id
           WHERE w.lotId = :lotId
             AND h.ownerId = :ownerId
             AND w.status <> com.rtms.backend.training.enums.WorkoutStatus.CANCELLED
           """)
    boolean existsActiveWorkoutOfOwnerInLot(@Param("lotId") Long lotId, @Param("ownerId") Long ownerId);

    @Query("""
       SELECT w FROM TrainingWorkout w
       JOIN TrainingLot l ON w.lotId = l.id
       WHERE w.id = :workoutId AND l.trainerId = :trainerId
       """)
    Optional<TrainingWorkout> findByIdAndTrainerId(@Param("workoutId") Long workoutId, @Param("trainerId") Long trainerId);
    List<TrainingWorkout> findByHorseIdOrderByCreatedAtDesc(Long horseId);
}
