package com.rtms.backend.repository;

import com.rtms.backend.entity.HorseTrainingPlan;
import com.rtms.backend.enums.TrainingPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
             AND p.status IN (com.rtms.backend.enums.TrainingPlanStatus.UPCOMING,
                              com.rtms.backend.enums.TrainingPlanStatus.ACTIVE)
           GROUP BY p.courseId, p.startDate, p.trainingDays
           ORDER BY p.startDate ASC
           """)
    List<Object[]> findJoinableCohorts(@Param("trainerId") Long trainerId,
                                       @Param("courseId") Long courseId);

    List<HorseTrainingPlan> findByTrainerIdAndStatusIn(Long trainerId,
                                                       List<TrainingPlanStatus> statuses);
}