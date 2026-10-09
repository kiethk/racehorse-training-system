package com.rtms.backend.training.repository;

import com.rtms.backend.training.entity.HorseTrainingPlan;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface HorseTrainingPlanRepository extends JpaRepository<HorseTrainingPlan, Long> {
    List<HorseTrainingPlan> findByHorseId(Long horseId);
    List<HorseTrainingPlan> findByStatus(TrainingPlanStatus status);
    List<HorseTrainingPlan> findByHorseIdAndStatus(Long horseId, TrainingPlanStatus status);
    List<HorseTrainingPlan> findByHorseIdAndStatusInOrderByEndDateDesc(Long horseId, Collection<TrainingPlanStatus> statuses);

    /**
     * API gợi ý nhóm (joinable-cohorts).
     *
     * "Nhóm" KHÔNG phải một entity — nó là một LỚP TƯƠNG ĐƯƠNG: hai plan
     * thuộc cùng nhóm khi trùng bộ ba (trainerId, courseId, startDate) và
     * cùng trainingDays. Nên chỉ cần GROUP BY, không cần bảng mới.
     */
    @Query("""
           SELECT p.courseId, p.startDate, p.trainingDays, COUNT(p.id)
           FROM HorseTrainingPlan p
           WHERE p.trainerId = :trainerId
             AND p.courseId = :courseId
             AND p.status IN (com.rtms.backend.training.enums.TrainingPlanStatus.UPCOMING,
                              com.rtms.backend.training.enums.TrainingPlanStatus.ACTIVE)
           GROUP BY p.courseId, p.startDate, p.trainingDays
           ORDER BY p.startDate ASC
           """)
    List<Object[]> findJoinableCohorts(@Param("trainerId") Long trainerId,
                                       @Param("courseId") Long courseId);

    List<HorseTrainingPlan> findByTrainerIdAndStatusIn(Long trainerId,
                                                       List<TrainingPlanStatus> statuses);

    /**
     * Các kế hoạch ĐÃ TỚI NGÀY BẮT ĐẦU nhưng vẫn còn nhãn UPCOMING.
     *
     * createPlan đặt UPCOMING cho kế hoạch có startDate ở tương lai, nhưng
     * không có gì tự chuyển nó sang ACTIVE khi ngày đó tới. Job nửa đêm dùng
     * truy vấn này để dọn.
     */
    List<HorseTrainingPlan> findByStatusAndStartDateLessThanEqual(TrainingPlanStatus status,
                                                                   LocalDate date);
}