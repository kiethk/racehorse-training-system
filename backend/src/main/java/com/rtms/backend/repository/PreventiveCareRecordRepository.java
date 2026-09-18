package com.rtms.backend.repository;

import com.rtms.backend.entity.PreventiveCareRecord;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PreventiveCareRecordRepository extends JpaRepository<PreventiveCareRecord, Long> {
    List<PreventiveCareRecord> findByHorseIdOrderByPerformedAtDesc(Long horseId);

}