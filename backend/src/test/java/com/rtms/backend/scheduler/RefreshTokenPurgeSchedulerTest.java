package com.rtms.backend.scheduler;

import com.rtms.backend.identity.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefreshTokenPurgeSchedulerTest {

    private RefreshTokenRepository refreshTokenRepository;
    private RefreshTokenPurgeScheduler scheduler;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        scheduler = new RefreshTokenPurgeScheduler(refreshTokenRepository);
    }

    @Test
    void testPurgeExpiredAndRevokedTokens_DeletesTokensSuccessfully() {
        when(refreshTokenRepository.deleteExpiredAndOldRevokedTokens(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5);

        scheduler.purgeExpiredAndRevokedTokens();

        ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(refreshTokenRepository).deleteExpiredAndOldRevokedTokens(nowCaptor.capture(), cutoffCaptor.capture());

        LocalDateTime now = nowCaptor.getValue();
        LocalDateTime cutoff = cutoffCaptor.getValue();

        assertTrue(cutoff.isBefore(now));
        // Cutoff should be approximately 7 days before now
        assertTrue(cutoff.isBefore(now.minusDays(6)));
        assertTrue(cutoff.isAfter(now.minusDays(8)));
    }

    @Test
    void testPurgeExpiredAndRevokedTokens_ZeroDeleted() {
        when(refreshTokenRepository.deleteExpiredAndOldRevokedTokens(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0);

        scheduler.purgeExpiredAndRevokedTokens();

        verify(refreshTokenRepository, times(1))
                .deleteExpiredAndOldRevokedTokens(any(LocalDateTime.class), any(LocalDateTime.class));
    }
}
