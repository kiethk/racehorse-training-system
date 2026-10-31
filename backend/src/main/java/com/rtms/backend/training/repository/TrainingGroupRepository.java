package com.rtms.backend.training.repository;

import com.rtms.backend.training.entity.TrainingGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingGroupRepository extends JpaRepository<TrainingGroup, Long> {

    List<TrainingGroup> findByTrainerIdOrderByNameAsc(Long trainerId);

    boolean existsByTrainerIdAndNameIgnoreCase(Long trainerId, String name);
}
