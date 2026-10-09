package com.rtms.backend.medical.repository;

import com.rtms.backend.medical.entity.TreatmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TreatmentPlanRepository extends JpaRepository<TreatmentPlan,Long> {
    List<TreatmentPlan> findByHealthRecordId(Long healthRecordId);
}
