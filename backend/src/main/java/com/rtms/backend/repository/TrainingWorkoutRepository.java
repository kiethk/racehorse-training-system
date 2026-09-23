package com.rtms.backend.repository;

import com.rtms.backend.entity.TrainingWorkout;
import com.rtms.backend.enums.WorkoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface TrainingWorkoutRepository extends JpaRepository<TrainingWorkout, Long> {

    // -----------------------------------------------------------------
    // ĐÃ XOÁ (cột workout_date / start_time / end_time không còn tồn tại):
    //   - findByPlanIdOrderByWorkoutDateAsc
    //   - findConflictingWorkoutsForTrainer
    //
    // BR-02 (Trainer không dạy 2 ngựa cùng lúc) KHÔNG còn cần query riêng:
    // thuật toán xếp khe trong TrainingLotService vốn đã không cho phép tạo
    // lot đè lên lot khác, nên ràng buộc được thực thi ở cấp cấu trúc.
    // -----------------------------------------------------------------

    List<TrainingWorkout> findByPlanIdOrderByIdAsc(Long planId);

    List<TrainingWorkout> findByHorseId(Long horseId);

    List<TrainingWorkout> findByLotId(Long lotId);

    /** BR-10 — đếm số ngựa đang chiếm chỗ trong lot. */
    long countByLotIdAndStatusNot(Long lotId, WorkoutStatus status);

    /** Danh sách buổi CÒN HIỆU LỰC trong lot — phải khớp với countByLotIdAndStatusNot. */
    List<TrainingWorkout> findByLotIdAndStatusNot(Long lotId, WorkoutStatus status);

    /** BR-09 — Groom này đã phụ trách con nào khác trong cùng lot chưa? */
    boolean existsByLotIdAndAssignedToIdAndStatusNot(Long lotId,
                                                     Long assignedToId,
                                                     WorkoutStatus status);

    /**
     * Cascade chấn thương: mọi buổi CHƯA diễn ra của một chiến mã.
     */
    @Query("""
           SELECT w FROM TrainingWorkout w
           JOIN TrainingLot l ON w.lotId = l.id
           WHERE w.horseId = :horseId
             AND w.status = com.rtms.backend.enums.WorkoutStatus.SCHEDULED
             AND l.lotDate >= :fromDate
           """)
    List<TrainingWorkout> findFutureScheduledByHorse(@Param("horseId") Long horseId,
                                                     @Param("fromDate") LocalDate fromDate);

    /**
     * Nguồn 2 của màn hình Today Tasks (Groom) — xem PLAN_02.
     * Trả Object[]{TrainingWorkout, TrainingLot} vì dự án không dùng quan hệ JPA.
     */
    @Query("""
           SELECT w, l FROM TrainingWorkout w
           JOIN TrainingLot l ON w.lotId = l.id
           WHERE w.assignedToId = :groomId
             AND l.lotDate = :date
             AND w.status <> com.rtms.backend.enums.WorkoutStatus.CANCELLED
           ORDER BY l.startTime ASC
           """)
    List<Object[]> findGroomWorkoutsWithLot(@Param("groomId") Long groomId,
                                            @Param("date") LocalDate date);

    /** Dùng khi trả chi tiết plan: workout kèm lot, sắp theo thời gian thật. */
    @Query("""
           SELECT w, l FROM TrainingWorkout w
           JOIN TrainingLot l ON w.lotId = l.id
           WHERE w.planId = :planId
           ORDER BY l.lotDate ASC, l.startTime ASC
           """)
    List<Object[]> findByPlanIdWithLot(@Param("planId") Long planId);

    long countByPlanIdAndStatusNot(Long planId, WorkoutStatus status);

    long countByPlanIdAndStatusNotIn(Long planId, Collection<WorkoutStatus> statuses);
}