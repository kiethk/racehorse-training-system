package com.rtms.backend.repository;

import com.rtms.backend.entity.PreventiveCareSchedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PreventiveCareScheduleRepository extends JpaRepository<PreventiveCareSchedule, Long> {
    List<PreventiveCareSchedule> findByHorseIdOrderByScheduledDateAsc(Long horseId);
    List<PreventiveCareSchedule> findByStatusOrderByScheduledDateAsc(String status);
    List<PreventiveCareSchedule> findByHorseIdInAndScheduledDate(List<Long> horseIds,
                                                                  LocalDate scheduledDate);
    List<PreventiveCareSchedule> findByHorseIdAndStatusIn(Long horseId, List<String> statuses);
    Optional<PreventiveCareSchedule> findFirstByHorseIdAndCareTypeOrderByIdDesc(Long horseId, String careType);
    Optional<PreventiveCareSchedule> findFirstByHorseIdAndCareTypeAndStatusOrderByIdDesc(Long horseId, String careType, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PreventiveCareSchedule s WHERE s.horseId = :horseId AND s.careType = 'INITIAL_EXAM' AND s.status = 'PENDING'")
    Optional<PreventiveCareSchedule> findPendingInitialExamForUpdate(@Param("horseId") Long horseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PreventiveCareSchedule s WHERE s.id = :id")
    Optional<PreventiveCareSchedule> findByIdForUpdate(@Param("id") Long id);
}
