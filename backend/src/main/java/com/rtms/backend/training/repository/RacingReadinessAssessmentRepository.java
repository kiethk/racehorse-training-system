package com.rtms.backend.training.repository;

import com.rtms.backend.training.entity.RacingReadinessAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RacingReadinessAssessmentRepository
        extends JpaRepository<RacingReadinessAssessment, Long> {

    Optional<RacingReadinessAssessment> findByAdmissionId(Long admissionId);

    Optional<RacingReadinessAssessment> findTopByHorseIdOrderByCreatedAtDesc(Long horseId);
}
