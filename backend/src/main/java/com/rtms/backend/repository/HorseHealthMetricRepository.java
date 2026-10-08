package com.rtms.backend.repository;
import com.rtms.backend.entity.HorseHealthMetric;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HorseHealthMetricRepository extends JpaRepository<HorseHealthMetric, Long> {
    List<HorseHealthMetric> findByHorseIdOrderByRecordedAtDesc(Long horseId);
}
