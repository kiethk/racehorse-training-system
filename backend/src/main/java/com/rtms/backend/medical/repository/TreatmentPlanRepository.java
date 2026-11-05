package com.rtms.backend.medical.repository;

import com.rtms.backend.medical.entity.TreatmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TreatmentPlanRepository extends JpaRepository<TreatmentPlan,Long> {
    List<TreatmentPlan> findByHealthRecordId(Long healthRecordId);

    List<TreatmentPlan> findByHealthRecordIdAndStatusIgnoreCase(Long healthRecordId, String status);

    @Query("SELECT tp FROM TreatmentPlan tp, HealthRecord hr WHERE tp.healthRecordId = hr.id AND hr.horseId = :horseId AND UPPER(tp.status) = 'ACTIVE' AND (tp.endDate IS NULL OR tp.endDate >= CURRENT_DATE) ORDER BY tp.startDate DESC, tp.id DESC")
    List<TreatmentPlan> findActivePlansByHorseId(@Param("horseId") Long horseId);
}

