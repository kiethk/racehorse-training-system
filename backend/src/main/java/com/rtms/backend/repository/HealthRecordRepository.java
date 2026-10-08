package com.rtms.backend.repository;
import com.rtms.backend.entity.HealthRecord;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {
    List<HealthRecord> findByHorseIdOrderByExaminedAtDesc(Long horseId);

    Optional<HealthRecord> findByCareScheduleId(Long careScheduleId);

}
