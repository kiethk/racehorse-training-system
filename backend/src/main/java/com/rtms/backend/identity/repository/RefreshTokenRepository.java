package com.rtms.backend.identity.repository;

import com.rtms.backend.identity.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now OR (r.revokedAt IS NOT NULL AND r.revokedAt < :revokedCutoff)")
    int deleteExpiredAndOldRevokedTokens(@Param("now") LocalDateTime now, @Param("revokedCutoff") LocalDateTime revokedCutoff);
}
