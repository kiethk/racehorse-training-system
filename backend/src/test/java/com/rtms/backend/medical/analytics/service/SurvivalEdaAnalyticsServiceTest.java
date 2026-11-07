package com.rtms.backend.medical.analytics.service;

import com.rtms.backend.medical.analytics.dto.SurvivalEdaResponse;
import com.rtms.backend.medical.analytics.dto.SurvivalEdaResponse.SurvivalStratumDto;
import com.rtms.backend.medical.entity.HorseBodyRegion;
import com.rtms.backend.medical.entity.InjuryRecord;
import com.rtms.backend.medical.entity.TreatmentPlan;
import com.rtms.backend.medical.repository.HorseBodyRegionRepository;
import com.rtms.backend.medical.repository.InjuryRecordRepository;
import com.rtms.backend.medical.repository.TreatmentPlanRepository;
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
    private TreatmentPlanRepository treatmentPlanRepository;

    @Mock
    private HorseBodyRegionRepository bodyRegionRepository;

    private SurvivalEdaAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new SurvivalEdaAnalyticsService(
                injuryRecordRepository,
                treatmentPlanRepository,
                bodyRegionRepository
        );
    }

    @Test
    @DisplayName("getSurvivalAnalytics stratified by TREATMENT_PLAN")
    void testSurvivalByTreatmentPlan() {
        LocalDateTime baseTime = LocalDateTime.now().minusDays(30);

        InjuryRecord inj1 = new InjuryRecord();
        inj1.setId(1L);
        inj1.setHealthRecordId(101L);
        inj1.setCreatedAt(baseTime);
        inj1.setRecoveredAt(baseTime.plusDays(10));
        inj1.setStatus("RESOLVED");

        InjuryRecord inj2 = new InjuryRecord();
        inj2.setId(2L);
        inj2.setHealthRecordId(101L);
        inj2.setCreatedAt(baseTime);
        inj2.setRecoveredAt(null); // censored
        inj2.setStatus("ACTIVE");

        InjuryRecord inj3 = new InjuryRecord();
        inj3.setId(3L);
        inj3.setHealthRecordId(102L);
        inj3.setCreatedAt(baseTime);
        inj3.setRecoveredAt(baseTime.plusDays(20));
        inj3.setStatus("RECOVERED");

        when(injuryRecordRepository.findAll()).thenReturn(List.of(inj1, inj2, inj3));

        TreatmentPlan tp1 = new TreatmentPlan();
        tp1.setId(501L);
        tp1.setHealthRecordId(101L);
        tp1.setTreatmentName("Hydrotherapy Protocol");

        TreatmentPlan tp2 = new TreatmentPlan();
        tp2.setId(502L);
        tp2.setHealthRecordId(102L);
        tp2.setTreatmentName("Laser & Cryotherapy");

        when(treatmentPlanRepository.findAll()).thenReturn(List.of(tp1, tp2));

        SurvivalEdaResponse res = service.getSurvivalAnalytics("TREATMENT_PLAN");

        assertNotNull(res);
        assertEquals("TREATMENT_PLAN", res.stratifyBy());
        assertEquals(2, res.strata().size());

        SurvivalStratumDto stratum1 = res.strata().stream()
                .filter(s -> s.stratumName().equals("Hydrotherapy Protocol"))
                .findFirst().orElseThrow();
        assertEquals(2, stratum1.totalCases());
        assertEquals(1, stratum1.recoveredCount());
        assertEquals(1, stratum1.censoredCount());
        assertFalse(stratum1.steps().isEmpty());

        SurvivalStratumDto stratum2 = res.strata().stream()
                .filter(s -> s.stratumName().equals("Laser & Cryotherapy"))
                .findFirst().orElseThrow();
        assertEquals(1, stratum2.totalCases());
        assertEquals(1, stratum2.recoveredCount());
        assertEquals(0, stratum2.censoredCount());
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

        SurvivalEdaResponse res = service.getSurvivalAnalytics("TREATMENT_PLAN");

        assertNotNull(res);
        assertTrue(res.strata().isEmpty());
    }

    @Test
    @DisplayName("Cancelled treatment plans are excluded in favor of active treatment plans")
    void testCancelledTreatmentPlansExcluded() {
        LocalDateTime baseTime = LocalDateTime.now().minusDays(20);

        InjuryRecord inj = new InjuryRecord();
        inj.setId(10L);
        inj.setHealthRecordId(200L);
        inj.setCreatedAt(baseTime);
        inj.setStatus("ACTIVE");

        when(injuryRecordRepository.findAll()).thenReturn(List.of(inj));

        TreatmentPlan tpCancelled = new TreatmentPlan();
        tpCancelled.setId(901L);
        tpCancelled.setHealthRecordId(200L);
        tpCancelled.setTreatmentName("Cancelled Old Plan");
        tpCancelled.setStatus("CANCELLED");

        TreatmentPlan tpActive = new TreatmentPlan();
        tpActive.setId(902L);
        tpActive.setHealthRecordId(200L);
        tpActive.setTreatmentName("Acupuncture & Shockwave");
        tpActive.setStatus("ACTIVE");

        when(treatmentPlanRepository.findAll()).thenReturn(List.of(tpCancelled, tpActive));

        SurvivalEdaResponse res = service.getSurvivalAnalytics("TREATMENT_PLAN");

        assertNotNull(res);
        assertEquals(1, res.strata().size());
        assertEquals("Acupuncture & Shockwave", res.strata().get(0).stratumName());
    }

    @Test
    @DisplayName("Resolved injury with null recoveredAt uses updatedAt for realistic recovery days")
    void testResolvedInjuryWithNullRecoveredAtUsesUpdatedAt() {
        LocalDateTime diagnosed = LocalDateTime.now().minusDays(40);
        LocalDateTime resolvedAtUpdate = diagnosed.plusDays(12);

        InjuryRecord inj = new InjuryRecord();
        inj.setId(15L);
        inj.setHealthRecordId(300L);
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
        when(treatmentPlanRepository.findAll()).thenReturn(List.of());

        SurvivalEdaResponse res = service.getSurvivalAnalytics("TREATMENT_PLAN");

        assertNotNull(res);
        assertEquals(1, res.strata().size());
        SurvivalStratumDto stratum = res.strata().get(0);
        assertEquals(1, stratum.recoveredCount());
        // Verify the step days corresponds to ~12 days rather than 40 days
        assertEquals(12, stratum.steps().get(1).days());
    }
}
