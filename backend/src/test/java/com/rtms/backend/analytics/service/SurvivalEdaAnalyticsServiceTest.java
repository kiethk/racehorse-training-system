package com.rtms.backend.analytics.service;

import com.rtms.backend.analytics.dto.SurvivalEdaResponse;
import com.rtms.backend.analytics.dto.SurvivalEdaResponse.SurvivalStratumDto;
import com.rtms.backend.medical.entity.HorseBodyRegion;
import com.rtms.backend.medical.entity.InjuryRecord;
import com.rtms.backend.medical.repository.HorseBodyRegionRepository;
import com.rtms.backend.medical.repository.InjuryRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SurvivalEdaAnalyticsServiceTest {

    @Mock
    private InjuryRecordRepository injuryRecordRepository;

    @Mock
    private HorseBodyRegionRepository bodyRegionRepository;

    private SurvivalEdaAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new SurvivalEdaAnalyticsService(
                injuryRecordRepository,
                bodyRegionRepository
        );
    }

    @Test
    @DisplayName("getSurvivalAnalytics stratified by SEVERITY in clinical order (Mild -> Moderate -> Severe)")
    void testSurvivalBySeverity() {
        LocalDateTime baseTime = LocalDateTime.now().minusDays(30);

        InjuryRecord inj1 = new InjuryRecord();
        inj1.setId(1L);
        inj1.setSeverity("MILD");
        inj1.setCreatedAt(baseTime);
        inj1.setRecoveredAt(baseTime.plusDays(7));
        inj1.setStatus("RESOLVED");

        InjuryRecord inj2 = new InjuryRecord();
        inj2.setId(2L);
        inj2.setSeverity("MODERATE");
        inj2.setCreatedAt(baseTime);
        inj2.setRecoveredAt(null); // censored
        inj2.setStatus("ACTIVE");

        InjuryRecord inj3 = new InjuryRecord();
        inj3.setId(3L);
        inj3.setSeverity("SEVERE");
        inj3.setCreatedAt(baseTime);
        inj3.setRecoveredAt(baseTime.plusDays(25));
        inj3.setStatus("RECOVERED");

        when(injuryRecordRepository.findAll()).thenReturn(List.of(inj1, inj2, inj3));

        SurvivalEdaResponse res = service.getSurvivalAnalytics("SEVERITY");

        assertNotNull(res);
        assertEquals("SEVERITY", res.stratifyBy());
        assertEquals(3, res.strata().size());

        // Verify clinical order
        assertEquals("Mild Severity", res.strata().get(0).stratumName());
        assertEquals(1, res.strata().get(0).totalCases());
        assertEquals(1, res.strata().get(0).recoveredCount());
        assertEquals(0, res.strata().get(0).censoredCount());

        assertEquals("Moderate Severity", res.strata().get(1).stratumName());
        assertEquals(1, res.strata().get(1).totalCases());
        assertEquals(0, res.strata().get(1).recoveredCount());
        assertEquals(1, res.strata().get(1).censoredCount());

        assertEquals("Severe", res.strata().get(2).stratumName());
        assertEquals(1, res.strata().get(2).totalCases());
        assertEquals(1, res.strata().get(2).recoveredCount());
        assertEquals(0, res.strata().get(2).censoredCount());
    }

    @Test
    @DisplayName("getSurvivalAnalytics stratified by BODY_ZONE")
    void testSurvivalByBodyZone() {
        LocalDateTime baseTime = LocalDateTime.now().minusDays(25);

        InjuryRecord inj1 = new InjuryRecord();
        inj1.setId(1L);
        inj1.setBodyRegionId(10L);
        inj1.setCreatedAt(baseTime);
        inj1.setRecoveredAt(baseTime.plusDays(14));
        inj1.setStatus("RESOLVED");

        InjuryRecord inj2 = new InjuryRecord();
        inj2.setId(2L);
        inj2.setBodyRegionId(20L);
        inj2.setCreatedAt(baseTime);
        inj2.setRecoveredAt(null);
        inj2.setStatus("ACTIVE");

        when(injuryRecordRepository.findAll()).thenReturn(List.of(inj1, inj2));

        HorseBodyRegion r1 = new HorseBodyRegion();
        r1.setId(10L);
        r1.setDisplayName("Left Fore Fetlock");

        HorseBodyRegion r2 = new HorseBodyRegion();
        r2.setId(20L);
        r2.setDisplayName("Right Hind Hock");

        when(bodyRegionRepository.findAll()).thenReturn(List.of(r1, r2));

        SurvivalEdaResponse res = service.getSurvivalAnalytics("BODY_ZONE");

        assertNotNull(res);
        assertEquals("BODY_ZONE", res.stratifyBy());
        assertEquals(2, res.strata().size());

        assertTrue(res.strata().stream().anyMatch(s -> s.stratumName().equals("Left Fore Fetlock")));
        assertTrue(res.strata().stream().anyMatch(s -> s.stratumName().equals("Right Hind Hock")));
    }

    @Test
    @DisplayName("Empty injury records return empty strata cleanly")
    void testEmptyInjuries() {
        when(injuryRecordRepository.findAll()).thenReturn(List.of());

        SurvivalEdaResponse res = service.getSurvivalAnalytics("SEVERITY");

        assertNotNull(res);
        assertTrue(res.strata().isEmpty());
    }

    @Test
    @DisplayName("Null or unknown severity values fall back to Unspecified Severity stratum")
    void testNullAndUnknownSeverityFallback() {
        LocalDateTime baseTime = LocalDateTime.now().minusDays(20);

        InjuryRecord inj1 = new InjuryRecord();
        inj1.setId(10L);
        inj1.setSeverity(null);
        inj1.setCreatedAt(baseTime);
        inj1.setStatus("ACTIVE");

        InjuryRecord inj2 = new InjuryRecord();
        inj2.setId(11L);
        inj2.setSeverity("UNKNOWN_GRADE");
        inj2.setCreatedAt(baseTime);
        inj2.setStatus("ACTIVE");

        when(injuryRecordRepository.findAll()).thenReturn(List.of(inj1, inj2));

        SurvivalEdaResponse res = service.getSurvivalAnalytics("SEVERITY");

        assertNotNull(res);
        assertEquals(1, res.strata().size());
        assertEquals("Unspecified Severity", res.strata().get(0).stratumName());
        assertEquals(2, res.strata().get(0).totalCases());
    }

    @Test
    @DisplayName("Resolved injury with null recoveredAt uses updatedAt for realistic recovery days")
    void testResolvedInjuryWithNullRecoveredAtUsesUpdatedAt() {
        LocalDateTime diagnosed = LocalDateTime.now().minusDays(40);
        LocalDateTime resolvedAtUpdate = diagnosed.plusDays(12);

        InjuryRecord inj = new InjuryRecord();
        inj.setId(15L);
        inj.setSeverity("MILD");
        inj.setDiagnosedAt(diagnosed);
        inj.setRecoveredAt(null); // null recoveredAt
        inj.setStatus("RESOLVED");

        // Use reflection to set updatedAt
        try {
            java.lang.reflect.Field updatedAtField = InjuryRecord.class.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(inj, resolvedAtUpdate);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }

        when(injuryRecordRepository.findAll()).thenReturn(List.of(inj));

        SurvivalEdaResponse res = service.getSurvivalAnalytics("SEVERITY");

        assertNotNull(res);
        assertEquals(1, res.strata().size());
        SurvivalStratumDto stratum = res.strata().get(0);
        assertEquals(1, stratum.recoveredCount());
        // Verify the step days corresponds to ~12 days rather than 40 days
        assertEquals(12, stratum.steps().get(1).days());
    }
}
