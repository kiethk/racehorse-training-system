package com.rtms.backend.scheduler;

import com.rtms.backend.identity.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class RefreshTokenPurgeScheduler {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenPurgeScheduler.class);
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenPurgeScheduler(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(cron = "${rtms.auth.refresh-token-purge-cron:0 0 2 * * ?}")
    @Transactional
    public void purgeExpiredAndRevokedTokens() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusDays(7);
        int deleted = refreshTokenRepository.deleteExpiredAndOldRevokedTokens(now, cutoff);
        if (deleted > 0) {
            log.info("Purged {} expired or revoked refresh tokens from database", deleted);
        }
    }
}
