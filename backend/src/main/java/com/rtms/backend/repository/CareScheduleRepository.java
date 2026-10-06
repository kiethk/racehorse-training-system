package com.rtms.backend.repository;
import com.rtms.backend.entity.CareSchedule;
import com.rtms.backend.enums.CareScheduleStatus;
import com.rtms.backend.enums.CareType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public interface CareScheduleRepository extends JpaRepository<CareSchedule, Long> {

    List<CareSchedule> findByHorseIdOrderByScheduledAtAsc(Long horseId);

    List<CareSchedule> findByHorseIdInAndScheduledAtBetweenAndStatusIn(
            Collection<Long> horseIds, LocalDateTime start, LocalDateTime end,
            Collection<CareScheduleStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cs FROM CareSchedule cs WHERE cs.id = :id")
    Optional<CareSchedule> findByIdForUpdate(@Param("id") Long id);

    Optional<CareSchedule> findFirstByAdmissionIdAndCareTypeOrderByCreatedAtDesc(Long admissionId, CareType careType);

    Optional<CareSchedule> findFirstByAdmissionIdAndStatusOrderByCreatedAtDesc(Long admissionId, CareScheduleStatus status);

    List<CareSchedule> findByHorseIdAndStatusIn(Long horseId, Collection<CareScheduleStatus> statuses);

    boolean existsByHorseIdAndCareTypeAndStatusIn(Long horseId, CareType careType, Collection<CareScheduleStatus> statuses);

    Optional<CareSchedule> findFirstByHorseIdAndCareTypeAndStatusInOrderByCreatedAtDesc(Long horseId, CareType careType, Collection<CareScheduleStatus> statuses);

    boolean existsByHorseIdAndCareTypeAndStatusInAndIdNot(Long horseId, CareType careType, Collection<CareScheduleStatus> statuses, Long id);

    List<CareSchedule> findByStatus(CareScheduleStatus status);

    List<CareSchedule> findByVeterinarianIdAndStatus(Long veterinarianId, CareScheduleStatus status);

    boolean existsByVeterinarianIdAndStatus(Long veterinarianId, CareScheduleStatus status);

    @Query("""
            SELECT COUNT(DISTINCT cs.horseId)
            FROM CareSchedule cs
            WHERE cs.veterinarianId = :vetId
              AND cs.status IN (
                  com.rtms.backend.enums.CareScheduleStatus.SCHEDULED,
                  com.rtms.backend.enums.CareScheduleStatus.IN_PROGRESS,
                  com.rtms.backend.enums.CareScheduleStatus.OVERDUE
              )
            """)
    long countActiveHorsesForVeterinarian(@Param("vetId") Long vetId);

    boolean existsByHorseIdAndStatus(Long horseId, CareScheduleStatus status);

    @Query("SELECT cs FROM CareSchedule cs WHERE cs.horseId = :horseId AND cs.status = :status AND cs.scheduledAt IS NOT NULL")
    List<CareSchedule> findScheduledForHorse(@Param("horseId") Long horseId, @Param("status") CareScheduleStatus status);

    @Query("SELECT cs FROM CareSchedule cs WHERE cs.veterinarianId = :vetId AND cs.status = 'SCHEDULED' AND cs.scheduledAt IS NOT NULL")
    List<CareSchedule> findConflictingSchedulesForVet(@Param("vetId") Long vetId);

    @Query("SELECT cs FROM CareSchedule cs WHERE cs.veterinarianId = :vetId AND cs.status = :status AND cs.scheduledAt IS NOT NULL")
    List<CareSchedule> findScheduledForVet(@Param("vetId") Long vetId, @Param("status") CareScheduleStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cs FROM CareSchedule cs WHERE cs.veterinarianId = :vetId AND cs.status = :status AND cs.scheduledAt IS NOT NULL")
    List<CareSchedule> findScheduledForVetForUpdate(@Param("vetId") Long vetId, @Param("status") CareScheduleStatus status);

    @Query("SELECT cs FROM CareSchedule cs WHERE (:status IS NULL OR cs.status = :status) "
            + "AND (:careType IS NULL OR cs.careType = :careType) "
            + "AND (:horseId IS NULL OR cs.horseId = :horseId) "
            + "AND (:vetId IS NULL OR cs.veterinarianId = :vetId) "
            + "AND (:admissionId IS NULL OR cs.admissionId = :admissionId)")
    Page<CareSchedule> findFiltered(@Param("status") CareScheduleStatus status,
            @Param("careType") CareType careType, @Param("horseId") Long horseId,
            @Param("vetId") Long vetId, @Param("admissionId") Long admissionId, Pageable pageable);

    Page<CareSchedule> findByHorseId(Long horseId, Pageable pageable);

    Page<CareSchedule> findByVeterinarianId(Long veterinarianId, Pageable pageable);

    Page<CareSchedule> findByStatus(CareScheduleStatus status, Pageable pageable);

    Page<CareSchedule> findByCareType(CareType careType, Pageable pageable);

    Page<CareSchedule> findByStatusAndCareType(CareScheduleStatus status, CareType careType, Pageable pageable);
}
