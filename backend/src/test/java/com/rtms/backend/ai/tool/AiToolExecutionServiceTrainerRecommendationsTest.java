package com.rtms.backend.ai.tool;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.ai.dto.ClinicalChunkResponseDto;
import com.rtms.backend.ai.rag.service.ClinicalRetrievalService;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.enums.HorseSex;
import com.rtms.backend.medical.entity.TreatmentPlan;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.enums.CareSchedulePurpose;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.notification.dto.HorseAlertResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.dto.TodayTaskItemResponse;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.enums.AreaType;
import com.rtms.backend.incident.enums.IncidentType;

import com.rtms.backend.stable.enums.StallStatus;
import com.rtms.backend.stable.enums.TaskSource;
import com.rtms.backend.stable.enums.GroomTaskType;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.GroomDailyTaskRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.stable.service.GroomDailyTaskService;
import com.rtms.backend.training.repository.projection.SessionHorseNameRow;
import com.rtms.backend.training.repository.projection.SessionRow;
import com.rtms.backend.training.dto.*;
import com.rtms.backend.training.entity.Course;
//
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.DistanceBand;
import com.rtms.backend.training.enums.SessionStatus;
//
import com.rtms.backend.training.enums.SurfaceType;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.enums.CourseRunStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.CourseRunRepository;
//
import com.rtms.backend.training.repository.TrainingSessionRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import com.rtms.backend.training.service.HorsePerformanceService;
import com.rtms.backend.training.service.HorseSpecialtyService;
import com.rtms.backend.audit.entity.AuditLog;
import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.admission.entity.CandidateHorseProfile;
import com.rtms.backend.admission.repository.CandidateHorseProfileRepository;
import com.rtms.backend.training.entity.Exercise;
import com.rtms.backend.training.enums.IntensityLevel;
import com.rtms.backend.training.enums.ExerciseCategory;
import com.rtms.backend.training.repository.ExerciseRepository;
import com.rtms.backend.medical.entity.HorseHealthMetric;
import com.rtms.backend.medical.repository.HorseHealthMetricRepository;
import com.rtms.backend.horse.entity.HorsePedigree;
import com.rtms.backend.horse.repository.HorsePedigreeRepository;
import com.rtms.backend.incident.entity.HorseIncidentReport;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.medical.repository.TreatmentPlanRepository;
import com.rtms.backend.medical.repository.InjuryRecordRepository;
import com.rtms.backend.identity.repository.VeterinarianProfileRepository;
import com.rtms.backend.identity.repository.TrainerProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.stable.entity.GroomDailyTask;
import com.rtms.backend.stable.repository.GroomTaskCompletionRepository;
import com.rtms.backend.identity.entity.Role;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.rtms.backend.training.dto.HorseAttendanceResponse;
import com.rtms.backend.training.repository.projection.PastWorkoutRow;
import com.rtms.backend.training.enums.SessionStatus;
import com.rtms.backend.training.service.HorseAttendanceService;
import com.rtms.backend.training.service.TrainingSessionService;
import com.rtms.backend.training.service.TrainingGroupService;
import com.rtms.backend.training.dto.GroupMemberResponse;
import com.rtms.backend.training.dto.SessionSummaryResponse;
import com.rtms.backend.training.dto.SessionDetailResponse;
import com.rtms.backend.training.dto.SessionHorseResponse;
import com.rtms.backend.training.dto.TrainingGroupResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
                3, new java.math.BigDecimal("7.0"),
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
                3, new java.math.BigDecimal("7.0"),
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
