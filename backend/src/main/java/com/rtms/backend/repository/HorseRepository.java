/**
 * HorseRepository — REFERENCE IMPLEMENTATION
 *
 * Đây là ví dụ mẫu về cách tạo Spring Data JPA Repository trong dự án.
 * Convention:
 *  - Extends JpaRepository<Entity, IdType> — không cần viết query cơ bản (findAll, findById, save, delete)
 *  - Dùng @Repository để Spring nhận diện
 *  - Thêm custom query bằng method name (findByName, findByStatus...) hoặc @Query nếu phức tạp hơn
 */
package com.rtms.backend.repository;

import com.rtms.backend.entity.Horse;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HorseRepository extends JpaRepository<Horse, Long> {
    List<Horse> findByOwnerId(Long ownerId);
    List<Horse> findByRegistrationNumber(String registrationNumber);
    List<Horse> findByCurrentStallIdIsNotNull();
    List<Horse> findByCurrentStallIdIn(List<Long> stallIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Horse h WHERE h.id = :id")
    Optional<Horse> findByIdForUpdate(@Param("id") Long id);
}
