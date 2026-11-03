package com.rtms.backend.medical.repository;

import com.rtms.backend.medical.entity.ClinicalMedication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClinicalMedicationRepository extends JpaRepository<ClinicalMedication, Long> {
    List<ClinicalMedication> findByActiveTrueOrderByNameAsc();
}
