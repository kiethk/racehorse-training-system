package com.rtms.backend.training.repository;
import com.rtms.backend.training.entity.TrainingLot;
import com.rtms.backend.training.service.TrainingLotService;

import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.WorkoutStatus;
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

    /** Đếm các buổi tập trong lot chưa xong (khác các trạng thái truyền vào). */
    long countByLotIdAndStatusNotIn(Long lotId, Collection<WorkoutStatus> statuses);

    /** Danh sách buổi CÒN HIỆU LỰC trong lot — phải khớp với countByLotIdAndStatusNot. */
    List<TrainingWorkout> findByLotIdAndStatusNot(Long lotId, WorkoutStatus status);

    /** BR-09 — Groom này đã phụ trách con nào khác trong cùng lot chưa? */
    boolean existsByLotIdAndAssignedToIdAndStatusNot(Long lotId,
                                                     Long assignedToId,
                                                     WorkoutStatus status);

    /**
     * BR-09 khi ĐỔI Groom — khác bản trên ở mệnh đề HorseIdNot.
     *
     * Vì sao phải loại trừ chính con ngựa đang chuyển: nếu nó được chuyển sang
     * chuồng mà Groom vẫn là người cũ, thì workout của CHÍNH NÓ đã mang
     * assignedToId đó rồi. Dùng bản không loại trừ sẽ báo xung đột giả — con
     * ngựa tự xung đột với chính mình và mọi thao tác đổi chuồng đều bị chặn.
     */
    boolean existsByLotIdAndAssignedToIdAndHorseIdNotAndStatusNot(Long lotId,
                                                                  Long assignedToId,
                                                                  Long horseId,
                                                                  WorkoutStatus status);

    /**
     * Cascade chấn thương: mọi buổi CHƯA diễn ra của một chiến mã.
     */
    @Query("""
           SELECT w FROM TrainingWorkout w
           JOIN TrainingLot l ON w.lotId = l.id
           WHERE w.horseId = :horseId
             AND w.status = com.rtms.backend.training.enums.WorkoutStatus.SCHEDULED
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
             AND w.status <> com.rtms.backend.training.enums.WorkoutStatus.CANCELLED
           ORDER BY l.startTime ASC
           """)
    List<Object[]> findGroomWorkoutsWithLot(@Param("groomId") Long groomId,
                                            @Param("date") LocalDate date);

    /**
     * Đếm số chiến mã còn hiệu lực trong NHIỀU lot bằng MỘT truy vấn.
     *
     * Dùng cho trang chi tiết kế hoạch: một khoá 12 buổi trải trên 12 lot, nếu
     * gọi countByLotId từng cái sẽ là 12 truy vấn. Gom lại còn 1.
     *
     * @return danh sách Object[]{ lotId (Long), soLuong (Long) }
     */
    @Query("""
           SELECT w.lotId, COUNT(w)
           FROM TrainingWorkout w
           WHERE w.lotId IN :lotIds
             AND w.status <> com.rtms.backend.training.enums.WorkoutStatus.CANCELLED
           GROUP BY w.lotId
           """)
    List<Object[]> countActiveByLotIds(@Param("lotIds") Collection<Long> lotIds);

    /**
     * Đếm buổi tập theo từng (kế hoạch, trạng thái) cho NHIỀU kế hoạch bằng
     * MỘT truy vấn — dùng cho màn hình danh sách kế hoạch.
     *
     * Không có nó thì mỗi dòng danh sách tốn 2-3 truy vấn đếm, 20 kế hoạch
     * thành 60 truy vấn.
     *
     * @return danh sách Object[]{ planId (Long), status (WorkoutStatus), soLuong (Long) }
     */
    @Query("""
           SELECT w.planId, w.status, COUNT(w)
           FROM TrainingWorkout w
           WHERE w.planId IN :planIds
           GROUP BY w.planId, w.status
           """)
    List<Object[]> countByPlanIdsGroupedByStatus(@Param("planIds") Collection<Long> planIds);

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

    /**
     * Dùng cho biểu đồ thể lực (fitness-trend): các buổi tập đã hoàn thành của một chiến mã,
     * sắp xếp theo ngày tăng dần để vẽ biểu đồ đường theo chuỗi thời gian.
     *
     * Khoảng ngày lọc NGAY TRONG SQL, không tải hết rồi cắt bằng stream ở Java.
     * Service truyền mốc bao trùm khi người dùng bỏ trống from/to, nên không cần
     * xử lý tham số null trong câu truy vấn.
     */
    @Query("""
           SELECT w, l FROM TrainingWorkout w
           JOIN TrainingLot l ON w.lotId = l.id
           WHERE w.horseId = :horseId
             AND w.status = com.rtms.backend.training.enums.WorkoutStatus.COMPLETED
             AND l.lotDate BETWEEN :fromDate AND :toDate
           ORDER BY l.lotDate ASC, l.startTime ASC
           """)
    List<Object[]> findCompletedWorkoutsWithLotAsc(@Param("horseId") Long horseId,
                                                    @Param("fromDate") LocalDate fromDate,
                                                    @Param("toDate") LocalDate toDate);

    /**
     * Dùng cho bộ 5 luật cảnh báo thể lực & dashboard: các buổi tập đã hoàn thành gần đây,
     * sắp xếp theo ngày giảm dần (mới nhất trước).
     */
    @Query("""
           SELECT w, l FROM TrainingWorkout w
           JOIN TrainingLot l ON w.lotId = l.id
           WHERE w.horseId = :horseId
             AND w.status = com.rtms.backend.training.enums.WorkoutStatus.COMPLETED
           ORDER BY l.lotDate DESC, l.startTime DESC
           """)
    List<Object[]> findCompletedWorkoutsWithLotDesc(@Param("horseId") Long horseId);
}