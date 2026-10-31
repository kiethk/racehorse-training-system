package com.rtms.backend.training.repository;

import com.rtms.backend.training.entity.GroupPlan;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
