package com.rtms.backend.medical.repository;

import com.rtms.backend.medical.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription,Long> {
    List<Prescription> findByTreatmentPlanId(Long treatmentPlanId);

    /** Ngày tiêm gần nhất của mỗi (ngựa, phác đồ). Ngựa chưa từng tiêm phác đồ nào thì không có dòng. */
    interface LastPreventiveDose {
        Long getHorseId();
        Long getProtocolId();
        LocalDate getLastDoseOn();
    }

    @Query("""
            SELECT hr.horseId AS horseId, p.preventiveProtocolId AS protocolId, MAX(p.startDate) AS lastDoseOn
            FROM Prescription p, TreatmentPlan tp, HealthRecord hr
            WHERE p.treatmentPlanId = tp.id AND tp.healthRecordId = hr.id
              AND hr.horseId IN :horseIds
              AND p.preventiveProtocolId IS NOT NULL AND p.startDate IS NOT NULL
              AND tp.status <> 'CANCELLED'
            GROUP BY hr.horseId, p.preventiveProtocolId
            """)
    List<LastPreventiveDose> findLastPreventiveDoses(@Param("horseIds") Collection<Long> horseIds);
}
