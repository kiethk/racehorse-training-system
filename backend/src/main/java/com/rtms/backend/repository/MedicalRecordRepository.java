package com.rtms.backend.repository;

import com.rtms.backend.entity.MedicalRecord;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findByHorseIdOrderByExaminedAtDesc(Long horseId);

}
