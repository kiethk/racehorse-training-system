package com.rtms.backend.training.repository;

import com.rtms.backend.training.entity.Exercise;
import com.rtms.backend.training.enums.ExerciseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    List<Exercise> findByCategory(ExerciseCategory category);
    List<Exercise> findByTrainerId(Long trainerId);
    List<Exercise> findByTrainerIdAndCategory(Long trainerId, ExerciseCategory category);
    boolean existsByNameIgnoreCaseAndTrainerId(String name, Long trainerId);
}
