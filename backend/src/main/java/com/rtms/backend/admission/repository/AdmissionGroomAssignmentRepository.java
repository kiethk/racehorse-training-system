package com.rtms.backend.admission.repository;

import com.rtms.backend.admission.entity.AdmissionGroomAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AdmissionGroomAssignmentRepository extends JpaRepository<AdmissionGroomAssignment, Long> {
    @Query("SELECT a FROM AdmissionGroomAssignment a WHERE a.admissionId = :admissionId AND a.endedAt IS NULL")
    Optional<AdmissionGroomAssignment> findOpenByAdmissionId(@Param("admissionId") Long admissionId);

    @Query("SELECT DISTINCT a.groomId FROM AdmissionGroomAssignment a WHERE a.admissionId = :admissionId")
    List<Long> findPreviousGroomIds(@Param("admissionId") Long admissionId);

    boolean existsByAdmissionIdAndGroomId(Long admissionId, Long groomId);
    List<AdmissionGroomAssignment> findByAdmissionIdOrderByAssignedAtAsc(Long admissionId);
}
