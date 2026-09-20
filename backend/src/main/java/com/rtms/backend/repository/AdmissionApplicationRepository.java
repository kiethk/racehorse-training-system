package com.rtms.backend.repository;

import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.enums.AdmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdmissionApplicationRepository
        extends JpaRepository<AdmissionApplication, Long> {

    List<AdmissionApplication> findByOwnerId(Long ownerId);

    List<AdmissionApplication> findByStatus(AdmissionStatus status);
}