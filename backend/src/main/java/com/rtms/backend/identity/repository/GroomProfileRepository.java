package com.rtms.backend.identity.repository;

import com.rtms.backend.identity.entity.GroomProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GroomProfileRepository extends JpaRepository<GroomProfile, Long> {
    List<GroomProfile> findByTrainerId(Long trainerId);
    long countByTrainerId(Long trainerId);

    /** Stamps the fair-allocation tie-breaker; upserts because a groom may not have a profile row yet. */
    @Modifying
    @Query(value = """
            INSERT INTO groom_profiles (user_id, last_horse_assigned_at) VALUES (:groomId, :assignedAt)
            ON CONFLICT (user_id) DO UPDATE SET last_horse_assigned_at = EXCLUDED.last_horse_assigned_at
            """, nativeQuery = true)
    void touchLastHorseAssignedAt(@Param("groomId") Long groomId, @Param("assignedAt") LocalDateTime assignedAt);
}
