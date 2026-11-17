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
import com.rtms.backend.training.entity.RacingReadinessAssessment;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.DistanceBand;
import com.rtms.backend.training.enums.SessionStatus;
import com.rtms.backend.training.enums.RacingReadinessStatus;
import com.rtms.backend.training.enums.SurfaceType;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.enums.CourseRunStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.CourseRunRepository;
import com.rtms.backend.training.repository.RacingReadinessAssessmentRepository;
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

class AiToolExecutionServiceManagerAnalysisTest extends AiToolExecutionServiceTestSupport {

    // =================================================================
    // CLUB_MANAGER Tests
    // =================================================================

    @Test
    @DisplayName("CLUB_MANAGER calls getHorseSpecialtyAnalysis to rank all racehorses in the club")
    void testManager_GetHorseSpecialtyAnalysis_FleetRanking() {
        AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@rtms.com", "CLUB_MANAGER");

        Horse h1 = new Horse();
        h1.setId(101L);
        h1.setName("ZEUS");
        h1.setRegistrationNumber("VN-101");
        h1.setCurrentStatus(HorseStatus.ELIGIBLE);

        when(horseRepository.findAll()).thenReturn(List.of(h1));

        HorseSpecialtyResponse spec = new HorseSpecialtyResponse(
                3, new java.math.BigDecimal("7.0"), List.of(DistanceBand.STAYER), SurfaceType.TURF,
                List.of(new DistanceStat(DistanceBand.STAYER, 4, new BigDecimal("8.8"), new BigDecimal("60.0"))));
        when(horseSpecialtyService.getSpecialties(anyCollection()))
                .thenReturn(Map.of(101L, spec));

        Object result = toolExecutionService.executeTool(
                "getHorseSpecialtyAnalysis",
                Map.of(),
                manager
        );

        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(1, map.get("totalHorses"));
        assertEquals(1, map.get("horsesWithSpecialties"));
        List<?> rankedList = (List<?>) map.get("rankedHorseList");
        assertEquals(1, rankedList.size());
        Map<?, ?> top = (Map<?, ?>) rankedList.get(0);
        assertEquals("ZEUS", top.get("horseName"));
        assertEquals(1, top.get("specialtyCount"));
        assertEquals(new BigDecimal("8.8"), top.get("averageRating"));
    }

    @Test
    @DisplayName("CLUB_MANAGER calls analyzeStableCapacityAndStallBottlenecks to analyze stable capacity and stall bottlenecks")
    void testManager_AnalyzeStableCapacityAndStallBottlenecks() {
        AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@rtms.com", "CLUB_MANAGER");

        when(stableStallRepository.count()).thenReturn(50L);
        when(stableStallRepository.countAvailableRegularStalls()).thenReturn(5L);
        when(stableStallRepository.countAvailableQuarantineStalls()).thenReturn(2L);
        when(stableStallRepository.countOccupiedQuarantineStalls()).thenReturn(8L);

        AdmissionApplication app = new AdmissionApplication();
        app.setStatus(AdmissionStatus.VET_REVIEW);
        when(admissionApplicationRepository.findAll()).thenReturn(List.of(app));

        Object result = toolExecutionService.executeTool("analyzeStableCapacityAndStallBottlenecks", Map.of(), manager);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(50L, map.get("totalStalls"));
        assertEquals(7L, map.get("vacantStalls"));
        assertEquals(8L, map.get("quarantineCasesCount"));
        assertNotNull(map.get("bottleneckForecast"));
        assertEquals("/manager/stable", map.get("actionUrl"));
    }

    @Test
    @DisplayName("CLUB_MANAGER calls recommendGroomWorkloadRebalancing to recommend a rebalancing of groom workloads")
    void testManager_RecommendGroomWorkloadRebalancing() {
        AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@rtms.com", "CLUB_MANAGER");

        User g1 = new User();
        g1.setId(10L);
        g1.setFullName("Groom Nam");
        Role r = new Role();
        r.setName("GROOM");
        g1.setRole(r);
        g1.setActive(true);

        User g2 = new User();
        g2.setId(20L);
        g2.setFullName("Groom Tony");
        g2.setRole(r);
        g2.setActive(true);

        when(userRepository.findAll()).thenReturn(List.of(g1, g2));
        when(stableStallRepository.findByGroomId(10L)).thenReturn(List.of(new StableStall(), new StableStall(), new StableStall(), new StableStall(), new StableStall(), new StableStall()));
        when(stableStallRepository.findByGroomId(20L)).thenReturn(List.of(new StableStall()));

        Object result = toolExecutionService.executeTool("recommendGroomWorkloadRebalancing", Map.of(), manager);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(2, map.get("totalGrooms"));
        assertTrue(map.containsKey("groomWorkloadList"));
        assertNotNull(map.get("proposedStallTransfer"));
        assertEquals("/manager/staff", map.get("actionUrl"));
    }
}
