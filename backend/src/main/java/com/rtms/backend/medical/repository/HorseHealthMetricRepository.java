package com.rtms.backend.medical.repository;

import com.rtms.backend.medical.entity.HorseHealthMetric;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HorseHealthMetricRepository extends JpaRepository<HorseHealthMetric, Long> {
    List<HorseHealthMetric> findByHorseIdOrderByRecordedAtDesc(Long horseId);
}
