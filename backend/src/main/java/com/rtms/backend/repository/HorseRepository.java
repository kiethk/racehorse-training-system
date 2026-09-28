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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HorseRepository extends JpaRepository<Horse, Long> {
    List<Horse> findByOwnerId(Long ownerId);
    List<Horse> findByRegistrationNumber(String registrationNumber);
    List<Horse> findByCurrentStallIdIsNotNull();
    List<Horse> findByCurrentStallIdIn(List<Long> stallIds);

    /**
     * Chiến mã đang ở một chuồng cụ thể.
     *
     * Trả Optional vì horses.current_stall_id có ràng buộc UNIQUE — mỗi chuồng
     * tối đa một con (BR-07). Dùng để kiểm chuồng đích đã có ngựa chưa, và để
     * tìm con ngựa cần đồng bộ Groom khi chuồng đổi người phụ trách.
     */
    Optional<Horse> findByCurrentStallId(Long stallId);
}