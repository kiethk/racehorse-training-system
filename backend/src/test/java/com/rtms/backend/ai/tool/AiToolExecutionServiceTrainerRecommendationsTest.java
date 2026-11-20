package com.rtms.backend.ai.tool;

import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.security.AuthenticatedUser;

import com.rtms.backend.training.dto.*;
//
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.DistanceBand;
//
import com.rtms.backend.training.enums.SurfaceType;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.enums.WorkoutStatus;
//
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class AiToolExecutionServiceTrainerRecommendationsTest extends AiToolExecutionServiceTestSupport {

    // =================================================================
    // HEAD_TRAINER Tests
    // =================================================================

    @Test
    @DisplayName("HEAD_TRAINER calls analyzeWorkoutLoadAndOvertrainingRisk to analyze the risk of overload over the past 14 days")
    void testTrainer_AnalyzeWorkoutLoadAndOvertrainingRisk() {
        AuthenticatedUser trainer = new AuthenticatedUser(200L, "trainer@rtms.com", "HEAD_TRAINER");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("THUNDER");
        horse.setRegistrationNumber("VN-THUNDER");
        horse.setTrainingDecision(TrainingDecision.ALLOWED);
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        TrainingWorkout w1 = new TrainingWorkout();
        w1.setId(1L);
        w1.setActualDistanceMeters(new BigDecimal("1200"));
        w1.setAverageHeartRate(98);
        w1.setMaxHeartRate(112);
        w1.setRecoveryHeartRate(88);
        w1.setTopSpeedKmh(new BigDecimal("54.0"));
        w1.setCreatedAt(LocalDateTime.now().minusDays(2));

        when(trainingWorkoutRepository.findPastWorkouts(eq(10L), any())).thenReturn(List.of(ranYesterday(w1)));

        Object result = toolExecutionService.executeTool("analyzeWorkoutLoadAndOvertrainingRisk", Map.of("horseId", 10L), trainer);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("THUNDER", map.get("horseName"));
        assertEquals("VN-THUNDER", map.get("registrationNumber"));
        assertEquals("1.2 km", map.get("totalDistance14Days"));
        assertEquals(1, map.get("workoutCount"));
        assertEquals("98 bpm", map.get("avgHeartRate"));
        assertEquals("Low", map.get("riskAssessment"));
        assertEquals("/trainer/dashboard", map.get("actionUrl"));
    }

    @Test
    @DisplayName("HEAD_TRAINER calls recommendOptimalRaceDistanceAndTrack to recommend an optimal distance and track surface")
    void testTrainer_RecommendOptimalRaceDistanceAndTrack() {
        AuthenticatedUser trainer = new AuthenticatedUser(200L, "trainer@rtms.com", "HEAD_TRAINER");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("THUNDER");
        horse.setRegistrationNumber("VN-THUNDER");
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        HorseSpecialtyResponse specialty = new HorseSpecialtyResponse(
                3, new BigDecimal("7.0"),
                List.of(DistanceBand.SPRINT),
                SurfaceType.TURF,
                List.of()
        );
        when(horseSpecialtyService.getSpecialties(List.of(10L))).thenReturn(Map.of(10L, specialty));

        TrainingWorkout w1 = new TrainingWorkout();
        w1.setTopSpeedKmh(new BigDecimal("60.5"));
        when(trainingWorkoutRepository.findPastWorkouts(eq(10L), any())).thenReturn(List.of(ranYesterday(w1)));

        Object result = toolExecutionService.executeTool("recommendOptimalRaceDistanceAndTrack", Map.of("horseId", 10L), trainer);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("THUNDER", map.get("horseName"));
        assertTrue(map.get("recommendedDistance").toString().contains("Sprint"));
        assertTrue(map.get("preferredSurface").toString().contains("Turf"));
        assertEquals("60.5 km/h", map.get("optimalSpeed"));
    }

    @Test
    @DisplayName("HEAD_TRAINER calls recommendNextWorkoutAdjustment to adjust the next workout")
    void testTrainer_RecommendNextWorkoutAdjustment() {
        AuthenticatedUser trainer = new AuthenticatedUser(200L, "trainer@rtms.com", "HEAD_TRAINER");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("THUNDER");
        horse.setRegistrationNumber("VN-THUNDER");
        horse.setTrainingDecision(TrainingDecision.ALLOWED);
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        TrainingWorkout w1 = new TrainingWorkout();
        w1.setActualDistanceMeters(new BigDecimal("1000"));
        w1.setMaxHeartRate(225);     // over the app's safe maximum of 220 bpm
        when(trainingWorkoutRepository.findPastWorkouts(eq(10L), any())).thenReturn(List.of(ranYesterday(w1)));

        Object result = toolExecutionService.executeTool("recommendNextWorkoutAdjustment", Map.of("horseId", 10L), trainer);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("THUNDER", map.get("horseName"));
        assertTrue(map.get("fitnessReview").toString().contains("Heart rate exceeds the system safety limits"));
        assertTrue(map.get("nextRecommendedWorkout").toString().contains("Reduce the distance by 30%"));
        assertEquals("/trainer/plans", map.get("actionUrl"));
    }

    @Test
    @DisplayName("HEAD_TRAINER calls recommendNextWorkoutAdjustment for a BLOCKED horse without contradicting its status")
    void testTrainer_RecommendNextWorkoutAdjustment_Blocked() {
        AuthenticatedUser trainer = new AuthenticatedUser(200L, "trainer@rtms.com", "HEAD_TRAINER");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("THUNDER");
        horse.setRegistrationNumber("VN-THUNDER");
        horse.setTrainingDecision(TrainingDecision.BLOCKED);
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        Object result = toolExecutionService.executeTool("recommendNextWorkoutAdjustment", Map.of("horseId", 10L), trainer);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertTrue(map.get("nextRecommendedWorkout").toString().contains("Complete rest in the stall"));
    }

@Test
    @DisplayName("HEAD_TRAINER calls recommendTrainerStrategy for comprehensive distance, track-surface, and workout recommendations")
    void testTrainer_RecommendTrainerStrategy() {
        AuthenticatedUser trainer = new AuthenticatedUser(200L, "trainer@rtms.com", "HEAD_TRAINER");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("THUNDER");
        horse.setRegistrationNumber("VN-THUNDER");
        horse.setTrainingDecision(TrainingDecision.ALLOWED);
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        HorseSpecialtyResponse specialty = new HorseSpecialtyResponse(
                3, new BigDecimal("7.0"),
                List.of(DistanceBand.SPRINT),
                SurfaceType.TURF,
                List.of()
        );
        when(horseSpecialtyService.getSpecialties(List.of(10L))).thenReturn(Map.of(10L, specialty));

        TrainingWorkout w1 = new TrainingWorkout();
        w1.setTopSpeedKmh(new BigDecimal("60.5"));
        w1.setActualDistanceMeters(new BigDecimal("1000"));
        w1.setMaxHeartRate(125);
        w1.setStatus(WorkoutStatus.RAN);
        when(trainingWorkoutRepository.findPastWorkouts(eq(10L), any())).thenReturn(List.of(ranYesterday(w1)));

        Object result = toolExecutionService.executeTool("recommendTrainerStrategy", Map.of("horseId", 10L), trainer);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("THUNDER", map.get("tenNgua"));
        assertEquals("VN-THUNDER", map.get("maDangKy"));
        assertTrue(map.containsKey("chienLuocCuLyVaSanDua"));
        assertTrue(map.containsKey("dieuChinhBaiTapKeTiep"));
    }
}
