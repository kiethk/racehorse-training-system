package com.rtms.backend.training.repository;

import com.rtms.backend.training.entity.GroupPlan;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import com.rtms.backend.training.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface GroupPlanRepository extends JpaRepository<GroupPlan, Long> {

    List<GroupPlan> findByGroupIdAndStatusIn(Long groupId, Collection<TrainingPlanStatus> statuses);

    boolean existsByGroupIdAndStatusIn(Long groupId, Collection<TrainingPlanStatus> statuses);

    List<GroupPlan> findByGroupIdInAndStatusIn(Collection<Long> groupIds, Collection<TrainingPlanStatus> statuses);

    List<GroupPlan> findByTrainerIdOrderByStartDateDesc(Long trainerId);

    List<GroupPlan> findAllByOrderByStartDateDesc();

    List<GroupPlan> findByStatusAndStartDateLessThanEqual(TrainingPlanStatus status, LocalDate date);

    List<GroupPlan> findByStatusIn(Collection<TrainingPlanStatus> statuses);

    /** Kế hoạch chưa đóng không còn buổi nào ở trạng thái đã cho. (Unfinished plans with no Session in the given status.) */
    @Query("""
           SELECT p FROM GroupPlan p
           WHERE p.status IN :statuses
             AND NOT EXISTS (SELECT 1 FROM TrainingSession s WHERE s.groupPlanId = p.id AND s.status = :scheduled)
           """)
    List<GroupPlan> findWithoutScheduledSessions(@Param("statuses") Collection<TrainingPlanStatus> statuses,
                                                 @Param("scheduled") SessionStatus scheduled);
}
