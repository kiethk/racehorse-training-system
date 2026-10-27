/**
 * HorseRepository — REFERENCE IMPLEMENTATION
 *
 * Convention:
 *  - Extends JpaRepository<Entity, IdType> — không cần viết query cơ bản (findAll, findById, save, delete)
 *  - Dùng @Repository để Spring nhận diện
 *  - Thêm custom query bằng method name (findByName, findByStatus...) hoặc @Query nếu phức tạp hơn
 */
package com.rtms.backend.horse.repository;

import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.training.enums.TrainingDecision;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface HorseRepository extends JpaRepository<Horse, Long> {
    List<Horse> findByOwnerId(Long ownerId);

    @Query("SELECT h.ownerId, COUNT(h.id) FROM Horse h WHERE h.ownerId IS NOT NULL GROUP BY h.ownerId")
    List<Object[]> countHorsesGroupedByOwner();

    List<Horse> findByCurrentStatusInOrderByNameAsc(Collection<HorseStatus> statuses);
    List<Horse> findByRegistrationNumber(String registrationNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Horse h WHERE h.registrationNumber = :ueln")
    List<Horse> findByRegistrationNumberForUpdate(@Param("ueln") String ueln);

    List<Horse> findByCurrentStallIdIsNotNull();
    List<Horse> findByCurrentStallIdIn(List<Long> stallIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Horse h WHERE h.id = :id")
    Optional<Horse> findByIdForUpdate(@Param("id") Long id);

    /**
     * Chiến mã đang ở một chuồng cụ thể.
     *
     * Trả Optional vì horses.current_stall_id có ràng buộc UNIQUE — mỗi chuồng
     * tối đa một con (BR-07). Dùng để kiểm chuồng đích đã có ngựa chưa, và để
     * tìm con ngựa cần đồng bộ Groom khi chuồng đổi người phụ trách.
     */
    Optional<Horse> findByCurrentStallId(Long stallId);

    @Query("""
            SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END
            FROM Horse h
            JOIN StableStall ss ON h.currentStallId = ss.id
            JOIN Area a ON ss.areaId = a.id
            WHERE h.id = :horseId
              AND a.trainerId = :trainerId
            """)
    boolean isManagedByTrainer(@Param("horseId") Long horseId,
                               @Param("trainerId") Long trainerId);

    @Query("""
            SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END
            FROM Horse h
            JOIN StableStall ss ON h.currentStallId = ss.id
            WHERE h.id = :horseId
              AND ss.groomId = :groomId
            """)
    boolean isManagedByGroom(@Param("horseId") Long horseId,
                             @Param("groomId") Long groomId);

    @Query("""
            SELECT a.trainerId, COUNT(h.id)
            FROM Horse h
            JOIN StableStall ss ON h.currentStallId = ss.id
            JOIN Area a ON ss.areaId = a.id
            WHERE a.trainerId IN :trainerIds
            GROUP BY a.trainerId
            """)
    List<Object[]> countHorsesByTrainerIds(@Param("trainerIds") Collection<Long> trainerIds);

    @Query("""
            SELECT a.trainerId, h.id
            FROM Horse h
            JOIN StableStall ss ON h.currentStallId = ss.id
            JOIN Area a ON ss.areaId = a.id
            WHERE a.trainerId IN :trainerIds
            """)
    List<Object[]> findManagedHorsePairs(@Param("trainerIds") Collection<Long> trainerIds);

    @Query("""
            SELECT DISTINCT h.id
            FROM Horse h
            JOIN StableStall ss ON h.currentStallId = ss.id
            JOIN Area a ON ss.areaId = a.id
            WHERE a.trainerId = :trainerId
            """)
    List<Long> findManagedHorseIdsByTrainerId(@Param("trainerId") Long trainerId);

    @Query("""
            SELECT DISTINCT h
            FROM Horse h
            JOIN StableStall ss ON h.currentStallId = ss.id
            JOIN Area a ON ss.areaId = a.id
            WHERE a.trainerId = :trainerId
            ORDER BY h.name ASC
            """)
    List<Horse> findManagedHorsesByTrainerId(@Param("trainerId") Long trainerId);

    @Query("""
            SELECT DISTINCT h
            FROM Horse h
            JOIN StableStall ss ON h.currentStallId = ss.id
            WHERE ss.groomId = :groomId
            ORDER BY h.name ASC
            """)
    List<Horse> findManagedHorsesByGroomUserId(@Param("groomId") Long groomId);

    @Query("""
            SELECT DISTINCT h
            FROM Horse h
            JOIN StableStall ss ON h.currentStallId = ss.id
            JOIN Area a ON ss.areaId = a.id
            WHERE a.trainerId = :trainerId
              AND h.currentStatus IN :statuses
            ORDER BY h.name ASC
            """)
    List<Horse> findManagedHorsesByTrainerIdAndStatusIn(@Param("trainerId") Long trainerId,
                                                       @Param("statuses") Collection<HorseStatus> statuses);

    @Query("SELECT h FROM Horse h WHERE UPPER(h.registrationNumber) = UPPER(:identifier) OR LOWER(h.name) = LOWER(:identifier)")
    List<Horse> findByIdentifierOrName(@Param("identifier") String identifier);

    @Query("""
            SELECT a.trainerId
            FROM Horse h
            JOIN StableStall ss ON h.currentStallId = ss.id
            JOIN Area a ON ss.areaId = a.id
            WHERE h.id = :horseId
            """)
    Optional<Long> findTrainerIdByHorseId(@Param("horseId") Long horseId);

    long countByCurrentStatus(HorseStatus status);

    long countByCurrentStatusIn(Collection<HorseStatus> statuses);

    long countByTrainingDecision(TrainingDecision decision);
}
