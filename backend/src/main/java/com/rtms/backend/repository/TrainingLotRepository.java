package com.rtms.backend.repository;

import com.rtms.backend.entity.TrainingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TrainingLotRepository extends JpaRepository<TrainingLot, Long> {

    /**
     * Mọi lot CÒN HIỆU LỰC của Trainer trong một ngày, sắp theo giờ tăng dần.
     * Dùng cho thuật toán quét khe — lot đã CANCELLED không còn chiếm chỗ.
     * Sắp xếp là BẮT BUỘC: con trỏ quét chỉ tiến về phía trước, danh sách lộn
     * xộn sẽ khiến nó bỏ sót hoặc tính sai khe.
     */
    @Query("""
           SELECT l FROM TrainingLot l
           WHERE l.trainerId = :trainerId
             AND l.lotDate = :date
             AND l.status <> com.rtms.backend.enums.LotStatus.CANCELLED
           ORDER BY l.startTime ASC
           """)
    List<TrainingLot> findActiveLotsOfDay(@Param("trainerId") Long trainerId,
                                          @Param("date") LocalDate date);

    /**
     * BƯỚC 1 của thuật toán: các lot cùng Trainer + cùng ngày + CÙNG BÀI TẬP.
     * Sắp theo giờ tăng dần để lot sớm được lấp đầy trước (kết quả tất định,
     * và Groom quen nhịp vì ngựa vào hệ thống sớm luôn đi lot sớm).
     */
    @Query("""
           SELECT l FROM TrainingLot l
           WHERE l.trainerId = :trainerId
             AND l.lotDate = :date
             AND l.subjectId = :subjectId
             AND l.status <> com.rtms.backend.enums.LotStatus.CANCELLED
           ORDER BY l.startTime ASC
           """)
    List<TrainingLot> findActiveLotsOfDayBySubject(@Param("trainerId") Long trainerId,
                                                   @Param("date") LocalDate date,
                                                   @Param("subjectId") Long subjectId);

    // =================================================================
    // API XEM LỊCH (khác hẳn query dùng cho thuật toán xếp khe)
    // =================================================================

    /**
     * Lịch lot của Trainer — BẢN LỌC, ẩn lot đã huỷ.
     *
     * Vì sao phải có: thuật toán xếp khe (findActiveLotsOfDay) vốn đã bỏ qua
     * lot CANCELLED, nghĩa là khe giờ của nó thực tế đang TRỐNG. Nếu API xem
     * lịch vẫn trả lot đó về, Trainer thấy một khối nằm chình ình trong
     * timeline và tưởng khe đã bận — đúng ngược mục đích của API. Tệ hơn: khi
     * một lot mới đã được đặt đè lên khoảng đó, response trả về hai lot chồng
     * giờ, nhìn như BR-02 bị vi phạm dù không hề.
     */
    @Query("""
           SELECT l FROM TrainingLot l
           WHERE l.trainerId = :trainerId
             AND l.lotDate BETWEEN :from AND :to
             AND l.status <> com.rtms.backend.enums.LotStatus.CANCELLED
           ORDER BY l.lotDate ASC, l.startTime ASC
           """)
    List<TrainingLot> findActiveLotsInRange(@Param("trainerId") Long trainerId,
                                            @Param("from") LocalDate from,
                                            @Param("to") LocalDate to);

    /**
     * Lịch lot của một GROOM: các lot mà groom này có ngựa phải dắt.
     *
     * KHÔNG dùng lại được query của Trainer, vì quan hệ sở hữu khác nhau:
     *   - Trainer SỞ HỮU lot        -> lọc thẳng l.trainerId
     *   - Groom KHÔNG sở hữu lot nào, chỉ CÓ MẶT trong đó
     *         -> bắt buộc JOIN qua training_workouts.assigned_to_id
     * Tra theo trainerId cho Groom sẽ luôn trả rỗng.
     *
     * Vì sao DISTINCT: BR-09 đã cấm một Groom có hai ngựa CÒN HIỆU LỰC trong
     * cùng một lot, nhưng một buổi đã huỷ cộng một buổi mới vẫn có thể cùng
     * trỏ về một lot. DISTINCT chặn dòng lặp ở trường hợp đó.
     *
     * Lọc hai tầng là cố ý:
     *   l.status  -> cả lot bị huỷ (trời mưa)
     *   w.status  -> chỉ buổi của con ngựa này bị huỷ (nó chấn thương),
     *                lot vẫn chạy với những con khác — khi đó Groom không
     *                còn việc gì ở lot đó nữa nên không được hiện ra.
     */
    @Query("""
           SELECT DISTINCT l FROM TrainingLot l
           JOIN TrainingWorkout w ON w.lotId = l.id
           WHERE w.assignedToId = :groomId
             AND l.lotDate BETWEEN :from AND :to
             AND l.status <> com.rtms.backend.enums.LotStatus.CANCELLED
             AND w.status <> com.rtms.backend.enums.WorkoutStatus.CANCELLED
           ORDER BY l.lotDate ASC, l.startTime ASC
           """)
    List<TrainingLot> findActiveGroomLotsInRange(@Param("groomId") Long groomId,
                                                 @Param("from") LocalDate from,
                                                 @Param("to") LocalDate to);

    /** API xem lịch lot theo tuần. */
    List<TrainingLot> findByTrainerIdAndLotDateBetweenOrderByLotDateAscStartTimeAsc(
            Long trainerId, LocalDate from, LocalDate to);

    List<TrainingLot> findByLotDateOrderByStartTimeAsc(LocalDate date);
}
