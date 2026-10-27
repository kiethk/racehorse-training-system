package com.rtms.backend.stable.repository;

import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.enums.StallStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StableStallRepository extends JpaRepository<StableStall, Long> {

    @Query(value = "SELECT * FROM stable_stalls WHERE deleted_at IS NULL ORDER BY id FOR UPDATE", nativeQuery = true)
    List<StableStall> findAllForAssignmentUpdate();

    @Query(value = """
            SELECT ss.* FROM stable_stalls ss JOIN areas a ON a.id=ss.area_id
            WHERE ss.id=:id AND ss.deleted_at IS NULL AND a.deleted_at IS NULL
            FOR UPDATE OF ss
            """, nativeQuery = true)
    Optional<StableStall> findActiveByIdForUpdate(@Param("id") Long id);

    List<StableStall> findByAreaId(Long areaId);

    List<StableStall> findByStatus(StallStatus status);

    List<StableStall> findByAreaIdAndStatus(Long areaId, StallStatus status);

    Optional<StableStall> findByStallCode(String stallCode);

    List<StableStall> findByGroomId(Long groomId);

    long countByGroomId(Long groomId);

    /** Whether the stall is one the groom looks after; used to limit what a groom may see. */
    boolean existsByIdAndGroomId(Long id, Long groomId);

    List<StableStall> findByAreaIdAndGroomId(Long areaId, Long groomId);

    @Query(value = """
            SELECT DISTINCT ss.groom_id FROM stable_stalls ss
            JOIN areas a ON a.id = ss.area_id
            JOIN users u ON u.id = ss.groom_id
            WHERE a.deleted_at IS NULL AND ss.deleted_at IS NULL AND a.type = 'QUARANTINE'
              AND u.is_active = TRUE
            ORDER BY ss.groom_id
            """, nativeQuery = true)
    List<Long> findQuarantineGroomIds();

    /**
     * One row per active groom of a quarantine stall: {groomId, load, free}. Load counts their quarantine
     * stalls holding a horse (OCCUPIED or RESERVED); free counts their AVAILABLE quarantine stalls.
     * Other area types are left out, so a groom's regular stalls never weigh on the quarantine pool.
     */
    @Query(value = """
            SELECT ss.groom_id,
                   COUNT(*) FILTER (WHERE ss.status IN ('OCCUPIED', 'RESERVED')) AS horse_load,
                   COUNT(*) FILTER (WHERE ss.status = 'AVAILABLE') AS free_stalls
            FROM stable_stalls ss
            JOIN areas a ON a.id = ss.area_id
            JOIN users u ON u.id = ss.groom_id
            WHERE a.deleted_at IS NULL AND ss.deleted_at IS NULL AND a.type = 'QUARANTINE'
              AND u.is_active = TRUE
            GROUP BY ss.groom_id
            ORDER BY ss.groom_id
            """, nativeQuery = true)
    List<Object[]> findQuarantineGroomLoads();

    /** Read-only twin of {@link #findFirstAvailableQuarantineStallOfGroomForUpdate}, for showing the groom what they would get. */
    @Query(value = """
            SELECT ss.*
            FROM stable_stalls ss
            JOIN areas a ON a.id = ss.area_id
            WHERE a.deleted_at IS NULL AND ss.deleted_at IS NULL AND a.type = 'QUARANTINE'
              AND ss.groom_id = :groomId AND ss.status = 'AVAILABLE'
            ORDER BY a.code ASC, ss.stall_number ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<StableStall> findFirstAvailableQuarantineStallOfGroom(@Param("groomId") Long groomId);

    /** The groom's first free quarantine stall (area code, then stall number), locked for a reservation. */
    @Query(value = """
            SELECT ss.*
            FROM stable_stalls ss
            JOIN areas a ON a.id = ss.area_id
            WHERE a.deleted_at IS NULL AND ss.deleted_at IS NULL AND a.type = 'QUARANTINE'
              AND ss.groom_id = :groomId AND ss.status = 'AVAILABLE'
            ORDER BY a.code ASC, ss.stall_number ASC
            LIMIT 1
            FOR UPDATE OF ss
            """, nativeQuery = true)
    Optional<StableStall> findFirstAvailableQuarantineStallOfGroomForUpdate(@Param("groomId") Long groomId);

    @Query(value = """
            SELECT ss.*
            FROM stable_stalls ss
            JOIN areas a ON a.id = ss.area_id
            WHERE a.deleted_at IS NULL AND ss.deleted_at IS NULL AND a.type IN ('QUARANTINE', 'REGULAR')
            ORDER BY ss.id ASC
            FOR UPDATE OF ss
            """, nativeQuery = true)
    List<StableStall> lockAdmissionCapacityStallsForUpdate();

    @Query(value = """
            SELECT COUNT(*)
            FROM stable_stalls ss
            JOIN areas a ON a.id = ss.area_id
            WHERE a.deleted_at IS NULL AND ss.deleted_at IS NULL AND a.type = 'QUARANTINE'
              AND ss.status = 'AVAILABLE' AND ss.groom_id IS NOT NULL
            """, nativeQuery = true)
    long countAvailableQuarantineStalls();

    @Query(value = """
            SELECT COUNT(*)
            FROM stable_stalls ss
            JOIN areas a ON a.id = ss.area_id
            WHERE a.deleted_at IS NULL AND ss.deleted_at IS NULL AND a.type = 'QUARANTINE'
              AND ss.status IN ('OCCUPIED', 'RESERVED')
            """, nativeQuery = true)
    long countOccupiedQuarantineStalls();

    @Query(value = """
            SELECT COUNT(*)
            FROM stable_stalls ss
            JOIN areas a ON a.id = ss.area_id
            WHERE a.deleted_at IS NULL AND ss.deleted_at IS NULL AND a.type = 'REGULAR'
              AND ss.status = 'AVAILABLE' AND ss.groom_id IS NOT NULL
            """, nativeQuery = true)
    long countAvailableRegularStalls();

    /**
     * The AVAILABLE stall (with an assigned groom) of the given area type whose groom is the fairest
     * next recipient: fewest horses (OCCUPIED + RESERVED stalls), then longest since the last new horse
     * (never = first), then lowest groom id; within that groom, area code and stall number.
     * <p>
     * Callers must already hold {@link #lockAdmissionCapacityStallsForUpdate()}: the groom loads are read
     * from the same rows, so without that lock two allocations could both pick the same "least loaded" groom.
     */
    @Query(value = """
            WITH groom_load AS (
                SELECT ss.groom_id, COUNT(*) FILTER (WHERE ss.status IN ('OCCUPIED', 'RESERVED')) AS horses
                FROM stable_stalls ss
                JOIN areas a ON a.id = ss.area_id
                WHERE a.deleted_at IS NULL AND ss.deleted_at IS NULL AND ss.groom_id IS NOT NULL
                GROUP BY ss.groom_id
            )
            SELECT ss.*
            FROM stable_stalls ss
            JOIN areas a ON a.id = ss.area_id
            JOIN groom_load gl ON gl.groom_id = ss.groom_id
            LEFT JOIN groom_profiles gp ON gp.user_id = ss.groom_id
            WHERE a.deleted_at IS NULL AND ss.deleted_at IS NULL AND a.type = :areaType
              AND ss.status = 'AVAILABLE' AND ss.groom_id IS NOT NULL
            ORDER BY gl.horses ASC, gp.last_horse_assigned_at ASC NULLS FIRST, ss.groom_id ASC,
                     a.code ASC, ss.stall_number ASC
            LIMIT 1
            FOR UPDATE OF ss
            """, nativeQuery = true)
    Optional<StableStall> findFairAvailableStallForUpdate(@Param("areaType") String areaType);

    @Query(value = """
            SELECT ss.*
            FROM stable_stalls ss
            JOIN areas a ON a.id = ss.area_id
            WHERE a.deleted_at IS NULL AND ss.deleted_at IS NULL AND ss.id = :stallId
              AND a.type = 'REGULAR'
              AND ss.status = 'AVAILABLE' AND ss.groom_id IS NOT NULL
            FOR UPDATE OF ss
            """, nativeQuery = true)
    Optional<StableStall> findAvailableRegularStallByIdForUpdate(
            @Param("stallId") Long stallId);

    @Query(value = """
            SELECT ss.* FROM stable_stalls ss
            JOIN areas a ON a.id = ss.area_id
            WHERE a.deleted_at IS NULL AND ss.deleted_at IS NULL AND ss.id = :stallId AND a.type = 'QUARANTINE'
            FOR UPDATE OF ss
            """, nativeQuery = true)
    Optional<StableStall> findQuarantineStallByIdForUpdate(@Param("stallId") Long stallId);

    @Query(value = """
            SELECT ss.*
            FROM stable_stalls ss
            JOIN areas a ON a.id = ss.area_id
            WHERE a.deleted_at IS NULL AND ss.deleted_at IS NULL AND a.type = 'REGULAR'
              AND ss.status = 'AVAILABLE' AND ss.groom_id IS NOT NULL
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
            WHERE ss.deleted_at IS NULL AND ss.area_id = :areaId
              AND ss.stall_number BETWEEN :fromNumber AND :toNumber
            ORDER BY ss.stall_number ASC
            FOR UPDATE OF ss
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
            WHERE a.deleted_at IS NULL AND ss.deleted_at IS NULL AND ss.area_id IN :areaIds
            ORDER BY a.code ASC, ss.stall_number ASC
            """, nativeQuery = true)
    List<StableStall> findByAreaIdInOrdered(@Param("areaIds") List<Long> areaIds);
}
