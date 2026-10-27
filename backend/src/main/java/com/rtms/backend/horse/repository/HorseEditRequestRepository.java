package com.rtms.backend.horse.repository;

import com.rtms.backend.horse.entity.HorseEditRequest;
import com.rtms.backend.horse.enums.HorseEditRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HorseEditRequestRepository extends JpaRepository<HorseEditRequest, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM HorseEditRequest r WHERE r.id = :id")
    Optional<HorseEditRequest> findByIdForUpdate(@Param("id") Long id);

    boolean existsByHorseIdAndStatus(Long horseId, HorseEditRequestStatus status);

    /** Latest request of a horse, whatever its status: drives the owner's pending/rejected banner. */
    Optional<HorseEditRequest> findFirstByHorseIdOrderByCreatedAtDescIdDesc(Long horseId);

    /**
     * Requests a groom may see: horses currently in one of their stalls, plus requests they reviewed.
     * Oldest first, matching the other review queues.
     */
    @Query("""
            SELECT r FROM HorseEditRequest r
            WHERE (:filterStatus = false OR r.status = :status)
              AND (r.reviewerId = :groomId
                   OR EXISTS (SELECT 1 FROM Horse h, StableStall s
                              WHERE h.id = r.horseId AND s.id = h.currentStallId AND s.groomId = :groomId))
            ORDER BY r.createdAt ASC, r.id ASC
            """)
    List<HorseEditRequest> findVisibleToGroom(@Param("groomId") Long groomId,
                                              @Param("filterStatus") boolean filterStatus,
                                              @Param("status") HorseEditRequestStatus status);
}
