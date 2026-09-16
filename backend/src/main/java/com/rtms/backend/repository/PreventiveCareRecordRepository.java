package com.rtms.backend.repository;

import com.rtms.backend.entity.PreventiveCareRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PreventiveCareRecordRepository extends JpaRepository<PreventiveCareRecord, Long> {
}