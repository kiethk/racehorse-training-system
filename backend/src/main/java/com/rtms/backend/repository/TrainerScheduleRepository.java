package com.rtms.backend.repository;

import com.rtms.backend.entity.TrainerSchedule;
import com.rtms.backend.enums.TrainerScheduleStatus;
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
public interface TrainerScheduleRepository extends JpaRepository<TrainerSchedule, Long> {

    Optional<TrainerSchedule> findByAdmissionId(Long admissionId);

    Optional<TrainerSchedule> findBySourceCareScheduleId(Long sourceCareScheduleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ts FROM TrainerSchedule ts WHERE ts.id = :id")
    Optional<TrainerSchedule> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ts FROM TrainerSchedule ts WHERE ts.admissionId = :admissionId")
    Optional<TrainerSchedule> findByAdmissionIdForUpdate(@Param("admissionId") Long admissionId);

    List<TrainerSchedule> findByTrainerId(Long trainerId);

    List<TrainerSchedule> findByTrainerIdAndStatus(Long trainerId, TrainerScheduleStatus status);

    List<TrainerSchedule> findByTrainerIdAndStatusIn(Long trainerId, Collection<TrainerScheduleStatus> statuses);

    Page<TrainerSchedule> findByTrainerIdAndStatusIn(Long trainerId, Collection<TrainerScheduleStatus> statuses, Pageable pageable);

    @Query("SELECT ts FROM TrainerSchedule ts WHERE (:trainerId IS NULL OR ts.trainerId = :trainerId) "
            + "AND (:status IS NULL OR ts.status = :status) ORDER BY ts.scheduledAt DESC")
    List<TrainerSchedule> findFiltered(@Param("trainerId") Long trainerId, @Param("status") TrainerScheduleStatus status);

    @Query("SELECT ts.trainerId, ts.horseId FROM TrainerSchedule ts "
            + "WHERE ts.trainerId IN :trainerIds AND ts.status IN :statuses")
    List<Object[]> findActiveTrainerHorsePairs(
            @Param("trainerIds") Collection<Long> trainerIds,
            @Param("statuses") Collection<TrainerScheduleStatus> statuses);

    @Query("SELECT DISTINCT ts.horseId FROM TrainerSchedule ts "
            + "WHERE ts.trainerId = :trainerId AND ts.status IN :statuses")
    List<Long> findActiveHorseIdsByTrainerId(
            @Param("trainerId") Long trainerId,
            @Param("statuses") Collection<TrainerScheduleStatus> statuses);

    @Query("SELECT ts FROM TrainerSchedule ts WHERE ts.trainerId = :trainerId "
            + "AND ts.status IN ('SCHEDULED', 'IN_PROGRESS') "
            + "AND ts.scheduledAt < :slotEnd "
            + "AND ts.scheduledAt >= :slotStartFloor")
    List<TrainerSchedule> findPotentiallyOverlappingSchedules(
            @Param("trainerId") Long trainerId,
            @Param("slotStartFloor") LocalDateTime slotStartFloor,
            @Param("slotEnd") LocalDateTime slotEnd);
}
