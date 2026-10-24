package com.rtms.backend.incident.repository;

import com.rtms.backend.incident.entity.HorseIncidentReport;
import com.rtms.backend.incident.enums.IncidentStatus;
import com.rtms.backend.training.dto.IncidentCountRow;
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

    List<HorseIncidentReport> findAllByOrderByReportedAtDesc();

    List<HorseIncidentReport> findByReportedAtAfterOrderByReportedAtDesc(LocalDateTime after);

    long countByHorseIdAndReportedAtAfter(Long horseId, LocalDateTime after);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM HorseIncidentReport r WHERE r.id = :id")
    Optional<HorseIncidentReport> findByIdForUpdate(@Param("id") Long id);

    /**
     * Số báo cáo sự cố của NHIỀU ngựa từ một mốc thời gian, bằng một câu GROUP BY.
     */
    @Query("""
           SELECT new com.rtms.backend.training.dto.IncidentCountRow(r.horseId, COUNT(r))
           FROM HorseIncidentReport r
           WHERE r.horseId IN :horseIds AND r.reportedAt > :after
           GROUP BY r.horseId
           """)
    List<IncidentCountRow> countIncidentsByHorseIds(
            @Param("horseIds") Collection<Long> horseIds,
            @Param("after") LocalDateTime after);
}
