package com.rtms.backend.medical.repository;

import com.rtms.backend.medical.entity.HealthRecord;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {
    List<HealthRecord> findByHorseIdOrderByExaminedAtDesc(Long horseId);

    Optional<HealthRecord> findByCareScheduleId(Long careScheduleId);

    /** The last exam conclusion that was not itself an urgent case: the state a closing urgent case must restore. */
    Optional<HealthRecord> findTopByHorseIdAndRecordTypeNotAndTrainingDecisionIsNotNullOrderByExaminedAtDesc(
            Long horseId, String recordType);

    /** The most recent completed health record for the horse that has a clinical training decision, regardless of care type. */
    Optional<HealthRecord> findTopByHorseIdAndTrainingDecisionIsNotNullOrderByExaminedAtDesc(Long horseId);

    /** The latest clinical training decision recorded strictly before the given time (state prior to an urgent case). */
    Optional<HealthRecord> findTopByHorseIdAndTrainingDecisionIsNotNullAndExaminedAtBeforeOrderByExaminedAtDesc(
            Long horseId, java.time.LocalDateTime cutoff);
}
