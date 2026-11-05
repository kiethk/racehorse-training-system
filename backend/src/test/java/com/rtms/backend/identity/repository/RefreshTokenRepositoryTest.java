package com.rtms.backend.identity.repository;

import com.rtms.backend.identity.entity.RefreshToken;
import com.rtms.backend.identity.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testDeleteExpiredAndOldRevokedTokens() {
        User user = userRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No user in test database"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusDays(7);

        // 1. Active valid token (should NOT be deleted)
        RefreshToken activeToken = new RefreshToken();
        activeToken.setUser(user);
        activeToken.setTokenHash(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""));
        activeToken.setExpiresAt(now.plusDays(7));
        activeToken.setRevokedAt(null);
        refreshTokenRepository.save(activeToken);

        // 2. Expired token (SHOULD be deleted)
        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setUser(user);
        expiredToken.setTokenHash(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""));
        expiredToken.setExpiresAt(now.minusHours(1));
        expiredToken.setRevokedAt(null);
        refreshTokenRepository.save(expiredToken);

        // 3. Recently revoked token (revoked 2 days ago, cutoff is 7 days ago - should NOT be deleted)
        RefreshToken recentlyRevoked = new RefreshToken();
        recentlyRevoked.setUser(user);
        recentlyRevoked.setTokenHash(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""));
        recentlyRevoked.setExpiresAt(now.plusDays(5));
        recentlyRevoked.setRevokedAt(now.minusDays(2));
        refreshTokenRepository.save(recentlyRevoked);

        // 4. Old revoked token (revoked 10 days ago, cutoff is 7 days ago - SHOULD be deleted)
        RefreshToken oldRevoked = new RefreshToken();
        oldRevoked.setUser(user);
        oldRevoked.setTokenHash(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""));
        oldRevoked.setExpiresAt(now.plusDays(5));
        oldRevoked.setRevokedAt(now.minusDays(10));
        refreshTokenRepository.save(oldRevoked);

        int deletedCount = refreshTokenRepository.deleteExpiredAndOldRevokedTokens(now, cutoff);
        assertTrue(deletedCount >= 2, "Expected at least 2 tokens deleted (expired and old revoked)");

        assertTrue(refreshTokenRepository.findById(activeToken.getId()).isPresent(), "Active token must remain");
        assertTrue(refreshTokenRepository.findById(recentlyRevoked.getId()).isPresent(), "Recently revoked token must remain");
        assertTrue(refreshTokenRepository.findById(expiredToken.getId()).isEmpty(), "Expired token must be purged");
        assertTrue(refreshTokenRepository.findById(oldRevoked.getId()).isEmpty(), "Old revoked token must be purged");
    }
}
