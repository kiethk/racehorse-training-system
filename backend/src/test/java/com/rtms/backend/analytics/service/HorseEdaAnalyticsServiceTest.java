package com.rtms.backend.analytics.service;

import com.rtms.backend.analytics.dto.FleetEdaResponse;
import com.rtms.backend.analytics.dto.FleetWorkoutSummaryRow;
import com.rtms.backend.analytics.dto.IndividualHorseEdaResponse;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.DashboardHorseRow;
import com.rtms.backend.training.dto.HorseFitnessTrendItemResponse;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import com.rtms.backend.training.service.HorseScopeService;
import com.rtms.backend.training.service.HorseViewAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HorseEdaAnalyticsServiceTest {

    @Mock
    private TrainingWorkoutRepository workoutRepository;

    @Mock
    private HorseScopeService scopeService;

    @Mock
    private HorseViewAccessService accessService;

    @Mock
    private HorseRepository horseRepository;

    private HorseEdaAnalyticsService edaService;
    private AuthenticatedUser mockUser;

    @BeforeEach
    void setUp() {
        edaService = new HorseEdaAnalyticsService(workoutRepository, scopeService, accessService, horseRepository);
        mockUser = new AuthenticatedUser(1L, "trainer@rtms.com", "HEAD_TRAINER");
    }

    @Test
    @DisplayName("getFleetEdaAnalytics returns empty stats when no horses accessible")
    void testGetFleetEdaAnalytics_EmptyHorses() {
        when(scopeService.horsesOf(mockUser)).thenReturn(Collections.emptyList());

        FleetEdaResponse res = edaService.getFleetEdaAnalytics(mockUser, 180);

        assertNotNull(res);
        assertEquals(0, res.speedStats().sampleSize());
        assertEquals(0, res.recoveryHeartRateStats().sampleSize());
        assertEquals(0, res.points().size());
    }

    @Test
    @DisplayName("getFleetEdaAnalytics aggregates workout summaries and detects outliers")
    void testGetFleetEdaAnalytics_SuccessWithOutlier() {
        List<DashboardHorseRow> horses = List.of(
                new DashboardHorseRow(1L, "Thunder Flash", "Thoroughbred", "A-01"),
                new DashboardHorseRow(2L, "Storm Wind", "Thoroughbred", "A-02"),
                new DashboardHorseRow(3L, "Shadow Runner", "Arabian", "A-03")
        );
        when(scopeService.horsesOf(mockUser)).thenReturn(horses);

        List<FleetWorkoutSummaryRow> summaries = List.of(
                new FleetWorkoutSummaryRow(1L, 48.0, 95.0, 210),
                new FleetWorkoutSummaryRow(2L, 50.0, 100.0, 215),
                new FleetWorkoutSummaryRow(3L, 68.0, 145.0, 230) // outlier
        );
        when(workoutRepository.findFleetWorkoutSummaries(anyCollection(), any(LocalDate.class)))
                .thenReturn(summaries);

        FleetEdaResponse res = edaService.getFleetEdaAnalytics(mockUser, 180);

        assertNotNull(res);
        assertEquals(3, res.speedStats().sampleSize());
        assertEquals(3, res.recoveryHeartRateStats().sampleSize());
        assertEquals(3, res.points().size());
        assertNotNull(res.correlation());
        assertTrue(res.correlation().pearsonR() > 0.8);
    }

    @Test
    @DisplayName("getIndividualEdaAnalytics computes single horse EDA across workouts")
    void testGetIndividualEdaAnalytics_Success() {
        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("Thunder Flash");
        horse.setBreed("Thoroughbred");
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        HorseFitnessTrendItemResponse w1 = new HorseFitnessTrendItemResponse();
        w1.setWorkoutId(101L);
        w1.setDate(LocalDate.now().minusDays(10));
        w1.setAverageSpeedKmh(BigDecimal.valueOf(45.0));
        w1.setRecoveryHeartRate(92);
        w1.setMaxHeartRate(205);

        HorseFitnessTrendItemResponse w2 = new HorseFitnessTrendItemResponse();
        w2.setWorkoutId(102L);
        w2.setDate(LocalDate.now().minusDays(5));
        w2.setAverageSpeedKmh(BigDecimal.valueOf(49.0));
        w2.setRecoveryHeartRate(98);
        w2.setMaxHeartRate(212);

        when(workoutRepository.findFitnessTrend(eq(10L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(w1, w2));

        IndividualHorseEdaResponse res = edaService.getIndividualEdaAnalytics(10L, mockUser, 365);

        assertNotNull(res);
        assertEquals(10L, res.horseId());
        assertEquals("Thunder Flash", res.horseName());
        assertEquals(2, res.points().size());
        assertEquals(2, res.speedStats().sampleSize());
        verify(accessService).assertCanViewHorse(10L, mockUser);
    }
}
