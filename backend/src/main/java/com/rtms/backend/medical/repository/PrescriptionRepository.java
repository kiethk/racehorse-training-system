package com.rtms.backend.medical.repository;

import com.rtms.backend.medical.entity.Prescription;
import com.rtms.backend.medical.entity.TreatmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription,Long> {
    List<Prescription> findByTreatmentPlanId(Long treatmentPlanId);
}
