package com.rtms.backend.ai.rag.repository;

import com.rtms.backend.ai.rag.entity.MedicalSymptomMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalSymptomMappingRepository extends JpaRepository<MedicalSymptomMapping, Long> {

    List<MedicalSymptomMapping> findByIsActiveTrue();

    List<MedicalSymptomMapping> findByCategoryAndIsActiveTrue(String category);
}
