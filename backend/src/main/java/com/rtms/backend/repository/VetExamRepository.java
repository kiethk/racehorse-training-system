package com.rtms.backend.repository;

import com.rtms.backend.entity.VetExam;
import com.rtms.backend.enums.VetExamStatus;
import com.rtms.backend.enums.VetExamType;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VetExamRepository extends JpaRepository<VetExam, Long> {
    @Query(value = "SELECT pg_try_advisory_xact_lock(7620050)", nativeQuery = true)
    boolean trySchedulerLock();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM VetExam e WHERE e.id = :id")
    Optional<VetExam> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e FROM VetExam e
            WHERE e.admissionId = :admissionId AND e.examType = :examType
              AND e.status IN :statuses
            ORDER BY e.createdAt DESC
            """)
    List<VetExam> findActiveForAdmissionForUpdate(@Param("admissionId") Long admissionId,
            @Param("examType") VetExamType examType,
            @Param("statuses") Collection<VetExamStatus> statuses);

    Optional<VetExam> findFirstByAdmissionIdAndExamTypeOrderByCreatedAtDesc(
            Long admissionId, VetExamType examType);

    Page<VetExam> findByStatusAndExamType(VetExamStatus status, VetExamType examType, Pageable pageable);
    Page<VetExam> findByStatus(VetExamStatus status, Pageable pageable);
    Page<VetExam> findByExamType(VetExamType examType, Pageable pageable);

    @Query(value = """
            SELECT * FROM vet_exams
            WHERE status = 'REQUESTED'
            ORDER BY priority DESC, created_at ASC, id ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<VetExam> lockNextRequestedBatch(@Param("batchSize") int batchSize);

    @Query("""
            SELECT e FROM VetExam e
            WHERE e.assignedVetId = :vetId
              AND e.status IN :statuses
              AND e.scheduledAt >= :from AND e.scheduledAt < :to
            """)
    List<VetExam> findVetSchedule(@Param("vetId") Long vetId,
            @Param("statuses") Collection<VetExamStatus> statuses,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            SELECT e FROM VetExam e
            WHERE e.horseId = :horseId
              AND e.status IN :statuses
              AND e.scheduledAt >= :from AND e.scheduledAt < :to
            """)
    List<VetExam> findHorseSchedule(@Param("horseId") Long horseId,
            @Param("statuses") Collection<VetExamStatus> statuses,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
