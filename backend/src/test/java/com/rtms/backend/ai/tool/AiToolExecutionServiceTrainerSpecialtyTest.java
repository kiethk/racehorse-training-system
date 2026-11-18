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

class AiToolExecutionServiceTrainerSpecialtyTest extends AiToolExecutionServiceTestSupport {

    // =================================================================
    // HEAD_TRAINER Tests
    // =================================================================

    @Test
    @DisplayName("HEAD_TRAINER calls getHorseSpecialtyAnalysis to analyze horse specialties with surface and average rating")
    void testTrainer_GetHorseSpecialtyAnalysis() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@rtms.com", "HEAD_TRAINER");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("RED RUM");
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        HorseSpecialtyResponse specialty = new HorseSpecialtyResponse(
                3, new java.math.BigDecimal("7.0"),
                List.of(DistanceBand.SPRINT),
                SurfaceType.TURF,
                List.of(new DistanceStat(DistanceBand.SPRINT, 5, new BigDecimal("8.5"), new BigDecimal("62.0")))
        );
        when(horseSpecialtyService.getSpecialties(List.of(10L)))
                .thenReturn(Map.of(10L, specialty));

        Object result = toolExecutionService.executeTool(
                "getHorseSpecialtyAnalysis",
                Map.of("horseId", 10L),
                trainer
        );

        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("RED RUM", map.get("horseName"));
        assertEquals(1, map.get("specialtyCount"));
        assertEquals(new BigDecimal("8.5"), map.get("averageRating"));
        assertEquals(new BigDecimal("8.5"), map.get("bestRating"));
        assertEquals(10L, map.get("horseId"));
        assertTrue(map.get("preferredSurface").toString().contains("TURF"));
    }

    @Test
    @DisplayName("HEAD_TRAINER calls getHorseSpecialtyAnalysis with horseName='all' to rank horses by specialty count (sortBy='count')")
    void testTrainer_GetHorseSpecialtyAnalysis_FleetRanking_SortByCount() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@rtms.com", "HEAD_TRAINER");

        Horse h1 = new Horse();
        h1.setId(10L);
        h1.setName("ALPHA SPRINT");
        h1.setRegistrationNumber("VN-010");
        h1.setCurrentStatus(HorseStatus.ELIGIBLE);

        Horse h2 = new Horse();
        h2.setId(20L);
        h2.setName("BETA DUAL");
        h2.setRegistrationNumber("VN-020");
        h2.setCurrentStatus(HorseStatus.ELIGIBLE);

        when(horseRepository.findManagedHorsesByTrainerIdAndStatusIn(eq(5L), anyCollection()))
                .thenReturn(List.of(h1, h2));

        // h1 has 1 specialty (SPRINT, rating 9.5), h2 has 2 specialties (MILE 7.1, MIDDLE 7.3 => avg 7.2)
        HorseSpecialtyResponse spec1 = new HorseSpecialtyResponse(
                3, new java.math.BigDecimal("7.0"), List.of(DistanceBand.SPRINT), SurfaceType.TURF,
                List.of(new DistanceStat(DistanceBand.SPRINT, 5, new BigDecimal("9.5"), new BigDecimal("64.0"))));
        HorseSpecialtyResponse spec2 = new HorseSpecialtyResponse(
                3, new java.math.BigDecimal("7.0"), List.of(DistanceBand.MILE, DistanceBand.MIDDLE), SurfaceType.DIRT,
                List.of(
                        new DistanceStat(DistanceBand.MILE, 3, new BigDecimal("7.1"), new BigDecimal("58.0")),
                        new DistanceStat(DistanceBand.MIDDLE, 3, new BigDecimal("7.3"), new BigDecimal("56.0"))
                ));

        when(horseSpecialtyService.getSpecialties(anyCollection()))
                .thenReturn(Map.of(10L, spec1, 20L, spec2));

        Object result = toolExecutionService.executeTool(
                "getHorseSpecialtyAnalysis",
                Map.of("horseName", "all", "sortBy", "count"),
                trainer
        );

        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(2, map.get("totalHorses"));
        assertEquals(2, map.get("horsesWithSpecialties"));
        assertEquals("COUNT", map.get("sortBy"));
        assertTrue(map.containsKey("rankedHorseList"));

        List<?> rankedList = (List<?>) map.get("rankedHorseList");
        assertEquals(2, rankedList.size());

        // Top horse should be BETA DUAL (2 specialties)
        Map<?, ?> top = (Map<?, ?>) rankedList.get(0);
        assertEquals(1, top.get("rank"));
        assertEquals("BETA DUAL", top.get("horseName"));
        assertEquals(2, top.get("specialtyCount"));
        assertEquals(new BigDecimal("7.2"), top.get("averageRating"));

        // Second horse should be ALPHA SPRINT (1 specialty)
        Map<?, ?> second = (Map<?, ?>) rankedList.get(1);
        assertEquals(2, second.get("rank"));
        assertEquals("ALPHA SPRINT", second.get("horseName"));
        assertEquals(1, second.get("specialtyCount"));
        assertEquals(new BigDecimal("9.5"), second.get("averageRating"));
    }

    @Test
    @DisplayName("HEAD_TRAINER calls getHorseSpecialtyAnalysis with sortBy='rating' to rank by average specialty performance")
    void testTrainer_GetHorseSpecialtyAnalysis_FleetRanking_SortByRating() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@rtms.com", "HEAD_TRAINER");

        Horse h1 = new Horse();
        h1.setId(10L);
        h1.setName("ALPHA SPRINT");
        h1.setRegistrationNumber("VN-010");
        h1.setCurrentStatus(HorseStatus.ELIGIBLE);

        Horse h2 = new Horse();
        h2.setId(20L);
        h2.setName("BETA DUAL");
        h2.setRegistrationNumber("VN-020");
        h2.setCurrentStatus(HorseStatus.ELIGIBLE);

        when(horseRepository.findManagedHorsesByTrainerIdAndStatusIn(eq(5L), anyCollection()))
                .thenReturn(List.of(h1, h2));

        // h1 has 1 specialty (SPRINT, rating 9.5), h2 has 2 specialties (MILE 7.1, MIDDLE 7.3 => avg 7.2)
        HorseSpecialtyResponse spec1 = new HorseSpecialtyResponse(
                3, new java.math.BigDecimal("7.0"), List.of(DistanceBand.SPRINT), SurfaceType.TURF,
                List.of(new DistanceStat(DistanceBand.SPRINT, 5, new BigDecimal("9.5"), new BigDecimal("64.0"))));
        HorseSpecialtyResponse spec2 = new HorseSpecialtyResponse(
                3, new java.math.BigDecimal("7.0"), List.of(DistanceBand.MILE, DistanceBand.MIDDLE), SurfaceType.DIRT,
                List.of(
                        new DistanceStat(DistanceBand.MILE, 3, new BigDecimal("7.1"), new BigDecimal("58.0")),
                        new DistanceStat(DistanceBand.MIDDLE, 3, new BigDecimal("7.3"), new BigDecimal("56.0"))
                ));

        when(horseSpecialtyService.getSpecialties(anyCollection()))
                .thenReturn(Map.of(10L, spec1, 20L, spec2));

        Object result = toolExecutionService.executeTool(
                "getHorseSpecialtyAnalysis",
                Map.of("horseName", "all", "sortBy", "rating"),
                trainer
        );

        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(2, map.get("totalHorses"));
        assertEquals("RATING", map.get("sortBy"));

        List<?> rankedList = (List<?>) map.get("rankedHorseList");
        assertEquals(2, rankedList.size());

        // In sortBy='rating', ALPHA SPRINT (9.5) MUST be Rank 1!
        Map<?, ?> top = (Map<?, ?>) rankedList.get(0);
        assertEquals(1, top.get("rank"));
        assertEquals("ALPHA SPRINT", top.get("horseName"));
        assertEquals(new BigDecimal("9.5"), top.get("averageRating"));

        // BETA DUAL (7.2) is Rank 2
        Map<?, ?> second = (Map<?, ?>) rankedList.get(1);
        assertEquals(2, second.get("rank"));
        assertEquals("BETA DUAL", second.get("horseName"));
        assertEquals(new BigDecimal("7.2"), second.get("averageRating"));
    }
}
