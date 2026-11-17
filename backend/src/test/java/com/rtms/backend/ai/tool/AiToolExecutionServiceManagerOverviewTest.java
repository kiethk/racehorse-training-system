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

class AiToolExecutionServiceManagerOverviewTest extends AiToolExecutionServiceTestSupport {

    // =================================================================
    // CLUB_MANAGER Tests
    // =================================================================

    @Test
    @DisplayName("CLUB_MANAGER calls getClubHealthOverview to review the health of the entire herd")
    void testManager_GetClubHealthOverview() {
        AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@rtms.com", "CLUB_MANAGER");

        Horse h1 = new Horse();
        h1.setId(1L);
        h1.setTrainingDecision(TrainingDecision.ALLOWED);

        Horse h2 = new Horse();
        h2.setId(2L);
        h2.setTrainingDecision(TrainingDecision.BLOCKED);

        when(horseRepository.findAll()).thenReturn(List.of(h1, h2));
        when(horseIncidentReportRepository.findByReportedAtAfterOrderByReportedAtDesc(any(LocalDateTime.class)))
                .thenReturn(List.of(new HorseIncidentReport()));

        Object result = toolExecutionService.executeTool("getClubHealthOverview", Map.of(), manager);

        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(2, map.get("totalClubHorses"));
        assertEquals(1L, map.get("allowedTrainingHorses"));
        assertEquals(1L, map.get("medicallyBlockedHorsesCount"));
        assertEquals(1, map.get("incidentsLast7DaysCount"));
    }

    @Test
    @DisplayName("CLUB_MANAGER calls getSystemStaffOverview to retrieve the staff directory")
    void testManager_GetSystemStaffOverview() {
        AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@rtms.com", "CLUB_MANAGER");

        User u1 = new User();
        u1.setId(10L);
        u1.setFullName("John Trainer");
        u1.setEmail("trainer@rtms.com");
        u1.setPhone("0901234567");
        u1.setActive(true);
        Role r1 = new Role();
        r1.setName("HEAD_TRAINER");
        u1.setRole(r1);

        User u2 = new User();
        u2.setId(20L);
        u2.setFullName("Jane Veterinarian");
        u2.setEmail("vet@rtms.com");
        u2.setPhone("0907654321");
        u2.setActive(false);
        Role r2 = new Role();
        r2.setName("VETERINARIAN");
        u2.setRole(r2);

        User u3 = new User();
        u3.setId(30L);
        u3.setFullName("Michael Groom");
        u3.setEmail("groom@rtms.com");
        u3.setPhone("0909999888");
        u3.setActive(true);
        Role r3 = new Role();
        r3.setName("GROOM");
        u3.setRole(r3);

        when(userRepository.findAll()).thenReturn(List.of(u1, u2, u3));

        Object result = toolExecutionService.executeTool("getSystemStaffOverview", Map.of(), manager);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> response = (Map<?, ?>) result;

        // Verify summary counts
        assertEquals(3, response.get("totalStaff"));

        // Verify role breakdown
        Map<?, ?> roleBreakdown = (Map<?, ?>) response.get("roleBreakdown");
        assertNotNull(roleBreakdown);
        assertEquals(1, roleBreakdown.get("HEAD_TRAINER"));
        assertEquals(1, roleBreakdown.get("VETERINARIAN"));
        assertEquals(1, roleBreakdown.get("GROOM"));

        // Verify staff list
        List<?> list = (List<?>) response.get("staffList");
        assertEquals(3, list.size());

        Map<?, ?> item1 = (Map<?, ?>) list.get(0);
        assertEquals("John Trainer", item1.get("fullName"));
        assertEquals("HEAD_TRAINER", item1.get("role"));
        assertEquals("Head Trainer", item1.get("roleTitle"));
        assertEquals("trainer@rtms.com", item1.get("email"));
        assertEquals("0901234567", item1.get("phone"));
        assertEquals("Active", item1.get("activeStatus"));
        assertNotNull(item1.get("specialtyInfo"));

        Map<?, ?> item2 = (Map<?, ?>) list.get(1);
        assertEquals("Locked", item2.get("activeStatus"));
        assertEquals("VETERINARIAN", item2.get("role"));
        assertEquals("Veterinarian", item2.get("roleTitle"));

        // Filter by role & isActive
        Object filteredResult = toolExecutionService.executeTool(
                "getSystemStaffOverview",
                Map.of("role", "HEAD_TRAINER", "isActive", true),
                manager
        );
        assertTrue(filteredResult instanceof Map<?, ?>);
        Map<?, ?> filteredResponse = (Map<?, ?>) filteredResult;
        assertEquals(1, filteredResponse.get("totalStaff"));
        List<?> filteredList = (List<?>) filteredResponse.get("staffList");
        assertEquals(1, filteredList.size());
        assertEquals("John Trainer", ((Map<?, ?>) filteredList.get(0)).get("fullName"));
    }

    @Test
    @DisplayName("CLUB_MANAGER calls getClubHorsesDirectory to retrieve the racehorse directory with statuses and assigned trainers")
    void testManager_GetClubHorsesDirectory() {
        AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@rtms.com", "CLUB_MANAGER");

        Horse h1 = new Horse();
        h1.setId(100L);
        h1.setName("DARK ANGEL");
        h1.setRegistrationNumber("VN-100");
        h1.setOwnerId(50L);
        h1.setCurrentStallId(1L);
        h1.setCurrentStatus(HorseStatus.ELIGIBLE);
        h1.setTrainingDecision(TrainingDecision.ALLOWED);

        when(horseRepository.findAll()).thenReturn(List.of(h1));

        User owner = new User();
        owner.setId(50L);
        owner.setFullName("Horse Owner Lee");
        when(userRepository.findById(50L)).thenReturn(Optional.of(owner));

        StableStall stall = new StableStall();
        stall.setId(1L);
        stall.setAreaId(10L);
        stall.setStallCode("A-01");
        stall.setStallNumber(1);
        when(stableStallRepository.findById(1L)).thenReturn(Optional.of(stall));

        Area area = new Area();
        area.setId(10L);
        area.setTrainerId(5L);
        when(areaRepository.findById(10L)).thenReturn(Optional.of(area));

        User trainer = new User();
        trainer.setId(5L);
        trainer.setFullName("Trainer Park");
        when(userRepository.findById(5L)).thenReturn(Optional.of(trainer));

        Object result = toolExecutionService.executeTool("getClubHorsesDirectory", Map.of(), manager);
        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());

        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("DARK ANGEL", item.get("horseName"));
        assertEquals(100L, item.get("horseId"));
        assertEquals("VN-100", item.get("registrationNumber"));
        assertEquals("Horse Owner Lee", item.get("ownerName"));
        assertTrue(item.get("currentStatus").toString().contains("ACTIVE"));
        assertTrue(item.get("stallLocation").toString().contains("A-01"));
        assertEquals("Trainer Park", item.get("assignedTrainer"));
        assertTrue(item.containsKey("specialtyCount"));
        assertTrue(item.containsKey("specialties"));
    }

    @Test
    @DisplayName("CLUB_MANAGER calls getRecentAuditLogs to retrieve the system audit log")
    void testManager_GetRecentAuditLogs() {
        AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@rtms.com", "CLUB_MANAGER");

        AuditLog log = new AuditLog();
        log.setId(1L);
        log.setAction("Updated training plan");
        log.setEntityName("HorseTrainingPlan");
        log.setEntityId(10L);
        log.setHttpMethod("PUT");
        log.setRequestPath("/api/training/plans/10");
        log.setStatusCode(200);

        User actor = new User();
        actor.setId(5L);
        actor.setFullName("Trainer John B");
        Role r = new Role();
        r.setName("HEAD_TRAINER");
        actor.setRole(r);
        log.setUser(actor);

        when(auditLogRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        Object result = toolExecutionService.executeTool("getRecentAuditLogs", Map.of("limit", 10), manager);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> response = (Map<?, ?>) result;
        assertEquals(1, response.get("totalAuditLogs"));
        List<?> list = (List<?>) response.get("auditLogList");
        assertEquals(1, list.size());

        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("Updated training plan", item.get("action"));
        assertTrue(item.get("performedBy").toString().contains("Trainer John B"));
        assertTrue(item.get("changeDetails").toString().contains("HorseTrainingPlan"));
    }

    @Test
    @DisplayName("CLUB_MANAGER calls getAdmissionRequestsOverview to retrieve an overview of all horse admission applications")
    void testManager_GetAdmissionRequestsOverview() {
        AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@rtms.com", "CLUB_MANAGER");

        AdmissionApplication app1 = new AdmissionApplication();
        app1.setId(1L);
        app1.setStatus(AdmissionStatus.MANAGER_REVIEW);

        AdmissionApplication app2 = new AdmissionApplication();
        app2.setId(2L);
        app2.setStatus(AdmissionStatus.APPROVED);

        when(admissionApplicationRepository.findAll()).thenReturn(List.of(app1, app2));

        Object result = toolExecutionService.executeTool("getAdmissionRequestsOverview", Map.of(), manager);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);

        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(2L, map.get("totalApplications"));
        assertEquals(1L, map.get("pendingManagerReviewCount"));
        assertEquals(1L, map.get("approvedApplications"));
    }
}
