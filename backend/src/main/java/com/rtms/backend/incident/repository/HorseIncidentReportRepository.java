package com.rtms.backend.incident.repository;

import com.rtms.backend.incident.entity.HorseIncidentReport;
import com.rtms.backend.incident.enums.IncidentSeverity;
import com.rtms.backend.incident.enums.IncidentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface HorseIncidentReportRepository extends JpaRepository<HorseIncidentReport, Long> {

    List<HorseIncidentReport> findByHorseId(Long horseId);

    List<HorseIncidentReport> findByStatus(IncidentStatus status);

    List<HorseIncidentReport> findByReportedById(Long reportedById);

    default List<HorseIncidentReport> findByGroomId(Long groomId) {
        return findByReportedById(groomId);
    }

    List<HorseIncidentReport> findByHorseIdAndStatus(Long horseId, IncidentStatus status);

    List<HorseIncidentReport> findByHorseIdAndStatusIn(Long horseId, Collection<IncidentStatus> statuses);

    List<HorseIncidentReport> findByCareScheduleIdOrderByReportedAtAsc(Long careScheduleId);

    /** Báo cáo Trainer được xem: của ngựa đang ở khu mình phụ trách, và báo cáo do chính mình gửi. */
    @Query("""
            SELECT r FROM HorseIncidentReport r
            WHERE r.reportedById = :trainerId
               OR r.horseId IN (SELECT h.id FROM Horse h
                                JOIN StableStall s ON h.currentStallId = s.id
                                JOIN Area a ON s.areaId = a.id
                                WHERE a.trainerId = :trainerId)
            """)
    List<HorseIncidentReport> findVisibleToTrainer(@Param("trainerId") Long trainerId);

    List<HorseIncidentReport> findAllByOrderByReportedAtDesc();

    List<HorseIncidentReport> findByReportedAtAfterOrderByReportedAtDesc(LocalDateTime after);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM HorseIncidentReport r WHERE r.id = :id")
    Optional<HorseIncidentReport> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT r FROM HorseIncidentReport r
            WHERE r.status IN :statuses AND r.severity IN :severities
            ORDER BY r.reportedAt DESC
            """)
    List<HorseIncidentReport> findUrgentIncidents(
            @Param("statuses") Collection<IncidentStatus> statuses,
            @Param("severities") Collection<IncidentSeverity> severities);

    long countByStatusInAndSeverityIn(
            Collection<IncidentStatus> statuses,
            Collection<IncidentSeverity> severities);

    long countByStatusIn(Collection<IncidentStatus> statuses);
}
