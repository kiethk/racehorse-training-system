package com.rtms.backend.repository;

import com.rtms.backend.entity.TrainingWorkout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingWorkoutRepository extends JpaRepository<TrainingWorkout, Long> {
    List<TrainingWorkout> findByPlanIdOrderByWorkoutDateAsc(Long planId);
    List<TrainingWorkout> findByHorseId(Long horseId);
}