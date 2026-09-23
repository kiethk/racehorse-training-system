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

    /** API xem lịch lot theo tuần. */
    List<TrainingLot> findByTrainerIdAndLotDateBetweenOrderByLotDateAscStartTimeAsc(
            Long trainerId, LocalDate from, LocalDate to);

    List<TrainingLot> findByLotDateOrderByStartTimeAsc(LocalDate date);
}
