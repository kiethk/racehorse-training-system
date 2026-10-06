package com.rtms.backend.repository;
import com.rtms.backend.entity.StableStall;
import com.rtms.backend.enums.StallStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StableStallRepository extends JpaRepository<StableStall, Long> {

    List<StableStall> findByAreaId(Long areaId);

    List<StableStall> findByStatus(StallStatus status);

    List<StableStall> findByAreaIdAndStatus(Long areaId, StallStatus status);

    Optional<StableStall> findByStallCode(String stallCode);

    List<StableStall> findByGroomId(Long groomId);

    long countByGroomId(Long groomId);

    List<StableStall> findByAreaIdAndGroomId(Long areaId, Long groomId);

    @Query(value = """
            SELECT ss.*
            FROM stable_stalls ss
            JOIN areas a ON a.id = ss.area_id
            WHERE a.type IN ('QUARANTINE', 'REGULAR')
            ORDER BY ss.id ASC
            FOR UPDATE
            """, nativeQuery = true)
    List<StableStall> lockAdmissionCapacityStallsForUpdate();

    @Query(value = """
            SELECT COUNT(*)
            FROM stable_stalls ss
            JOIN areas a ON a.id = ss.area_id
            WHERE a.type = 'QUARANTINE'
              AND ss.status = 'AVAILABLE'
            """, nativeQuery = true)
    long countAvailableQuarantineStalls();

    @Query(value = """
            SELECT COUNT(*)
            FROM stable_stalls ss
            JOIN areas a ON a.id = ss.area_id
            WHERE a.type = 'QUARANTINE'
              AND ss.status = 'OCCUPIED'
            """, nativeQuery = true)
    long countOccupiedQuarantineStalls();

    @Query(value = """
            SELECT COUNT(*)
            FROM stable_stalls ss
            JOIN areas a ON a.id = ss.area_id
            WHERE a.type = 'REGULAR'
              AND ss.status = 'AVAILABLE'
            """, nativeQuery = true)
    long countAvailableRegularStalls();

    @Query(value = """
            SELECT ss.*
            FROM stable_stalls ss
            JOIN areas a ON a.id = ss.area_id
            WHERE a.type = 'QUARANTINE'
              AND ss.status = 'AVAILABLE'
            ORDER BY a.code ASC, ss.stall_number ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<StableStall> findFirstAvailableQuarantineStallForUpdate();

    @Query(value = """
            SELECT ss.*
            FROM stable_stalls ss
            JOIN areas a ON a.id = ss.area_id
            WHERE a.type = 'REGULAR'
              AND ss.status = 'AVAILABLE'
            ORDER BY a.code ASC, ss.stall_number ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<StableStall> findFirstAvailableRegularStallForUpdate();

    @Query(value = """
            SELECT ss.*
            FROM stable_stalls ss
            JOIN areas a ON a.id = ss.area_id
            WHERE ss.id = :stallId
              AND a.type = 'REGULAR'
              AND ss.status = 'AVAILABLE'
            FOR UPDATE
            """, nativeQuery = true)
    Optional<StableStall> findAvailableRegularStallByIdForUpdate(
            @Param("stallId") Long stallId);

    @Query(value = """
            SELECT ss.* FROM stable_stalls ss
            JOIN areas a ON a.id = ss.area_id
            WHERE ss.id = :stallId AND a.type = 'QUARANTINE'
            FOR UPDATE OF ss
            """, nativeQuery = true)
    Optional<StableStall> findQuarantineStallByIdForUpdate(@Param("stallId") Long stallId);

    @Query(value = """
            SELECT ss.*
            FROM stable_stalls ss
            JOIN areas a ON a.id = ss.area_id
            WHERE a.type = 'REGULAR'
              AND ss.status = 'AVAILABLE'
            ORDER BY a.code ASC, ss.stall_number ASC
            """, nativeQuery = true)
    List<StableStall> findAllAvailableRegularStallsOrdered();

    /**
     * Find all stalls in a given area with stall_number within [fromNumber, toNumber], ordered ascending.
     * Used for groom block assignment to inspect a specific 3-stall block.
     */
    List<StableStall> findByAreaIdAndStallNumberBetweenOrderByStallNumberAsc(
            Long areaId, Integer stallNumberFrom, Integer stallNumberTo);

    /**
     * Reload and row-lock the exact stalls of a candidate 3-stall block inside the current transaction.
     * Must be called AFTER the candidate block has been chosen via the non-locking scan, and BEFORE
     * any groom_id is written, to prevent concurrent double-assignment.
     * <p>
     * Uses {@code FOR UPDATE} so concurrent transactions that attempt to lock the same rows will block
     * (rather than silently reading stale data), ensuring exactly one Groom wins each block.
     */
    @Query(value = """
            SELECT ss.*
            FROM stable_stalls ss
            WHERE ss.area_id = :areaId
              AND ss.stall_number BETWEEN :fromNumber AND :toNumber
            ORDER BY ss.stall_number ASC
            FOR UPDATE
            """, nativeQuery = true)
    List<StableStall> findBlockForUpdate(
            @Param("areaId") Long areaId,
            @Param("fromNumber") Integer fromNumber,
            @Param("toNumber") Integer toNumber);

    /** Find all stalls belonging to any of the given area IDs, ordered area code + stall_number ascending. */
    @Query(value = """
            SELECT ss.*
            FROM stable_stalls ss
            JOIN areas a ON a.id = ss.area_id
            WHERE ss.area_id IN :areaIds
            ORDER BY a.code ASC, ss.stall_number ASC
            """, nativeQuery = true)
    List<StableStall> findByAreaIdInOrdered(@Param("areaIds") List<Long> areaIds);
}
