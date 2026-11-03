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
                new DashboardHorseRow(3L, "Blaze Spirit", "Thoroughbred", "A-03"),
                new DashboardHorseRow(4L, "Desert Star", "Arabian", "A-04"),
                new DashboardHorseRow(5L, "Shadow Runner", "Arabian", "A-05")
        );
        when(scopeService.horsesOf(mockUser)).thenReturn(horses);

        List<FleetWorkoutSummaryRow> summaries = List.of(
                new FleetWorkoutSummaryRow(1L, 48.0, 92.0, 210),
                new FleetWorkoutSummaryRow(2L, 49.0, 94.0, 212),
                new FleetWorkoutSummaryRow(3L, 50.0, 95.0, 215),
                new FleetWorkoutSummaryRow(4L, 51.0, 96.0, 218),
                new FleetWorkoutSummaryRow(5L, 85.0, 150.0, 240) // upper outlier
        );
        when(workoutRepository.findFleetWorkoutSummaries(anyCollection(), any(LocalDate.class)))
                .thenReturn(summaries);

        FleetEdaResponse res = edaService.getFleetEdaAnalytics(mockUser, 180);

        assertNotNull(res);
        assertEquals(5, res.speedStats().sampleSize());
        assertEquals(5, res.recoveryHeartRateStats().sampleSize());
        assertEquals(5, res.points().size());
        assertNotNull(res.correlation());
        assertTrue(res.correlation().pearsonR() > 0.8);
        assertTrue(res.points().get(4).isOutlier());
        assertEquals("Exceptional speed (85.0 km/h > Q3) | High recovery heart rate anomaly (150 bpm > Q3)",
                res.points().get(4).outlierReason());
    }

    @Test
    @DisplayName("getFleetEdaAnalytics generates English outlier reasons for low values below Q1")
    void testGetFleetEdaAnalytics_LowOutlierReasons() {
        List<DashboardHorseRow> horses = List.of(
                new DashboardHorseRow(1L, "H1", "Thoroughbred", "A-01"),
                new DashboardHorseRow(2L, "H2", "Thoroughbred", "A-02"),
                new DashboardHorseRow(3L, "H3", "Thoroughbred", "A-03"),
                new DashboardHorseRow(4L, "H4", "Thoroughbred", "A-04"),
                new DashboardHorseRow(5L, "H5", "Thoroughbred", "A-05")
        );
        when(scopeService.horsesOf(mockUser)).thenReturn(horses);

        List<FleetWorkoutSummaryRow> summaries = List.of(
                new FleetWorkoutSummaryRow(1L, 20.0, 60.0, 190), // lower outlier
                new FleetWorkoutSummaryRow(2L, 49.0, 94.0, 212),
                new FleetWorkoutSummaryRow(3L, 50.0, 95.0, 215),
                new FleetWorkoutSummaryRow(4L, 51.0, 96.0, 218),
                new FleetWorkoutSummaryRow(5L, 52.0, 98.0, 220)
        );
        when(workoutRepository.findFleetWorkoutSummaries(anyCollection(), any(LocalDate.class)))
                .thenReturn(summaries);

        FleetEdaResponse res = edaService.getFleetEdaAnalytics(mockUser, 180);

        assertNotNull(res);
        assertTrue(res.points().get(0).isOutlier());
        assertEquals("Low speed anomaly (20.0 km/h < Q1) | Exceptional cardio recovery (60 bpm < Q1)",
                res.points().get(0).outlierReason());
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
        assertEquals("Workout", res.points().get(0).sessionName());
        assertEquals(2, res.speedStats().sampleSize());
        verify(accessService).assertCanViewHorse(10L, mockUser);
    }
}
