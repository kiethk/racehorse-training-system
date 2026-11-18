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
import com.rtms.backend.training.enums.RacingReadinessStatus;
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

class AiToolExecutionServiceManagerAdmissionTest extends AiToolExecutionServiceTestSupport {

    // =================================================================
    // CLUB_MANAGER Tests
    // =================================================================

    @Test
    @DisplayName("CLUB_MANAGER calls getAdmissionPipelineDetails to retrieve horse admission application details")
    void testManager_GetAdmissionPipelineDetails() {
        AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@rtms.com", "CLUB_MANAGER");

        AdmissionApplication app = new AdmissionApplication();
        app.setId(123L);
        app.setOwnerId(50L);
        app.setStatus(AdmissionStatus.MANAGER_REVIEW);
        app.setSubmittedAt(LocalDateTime.of(2026, 10, 20, 10, 0));
        app.setVeterinarianId(8L);
        app.setTrainerId(5L);

        when(admissionApplicationRepository.findAll()).thenReturn(List.of(app));

        CandidateHorseProfile profile = new CandidateHorseProfile();
        profile.setId(1L);
        profile.setAdmissionId(123L);
        profile.setName("PEGASUS JUNIOR");
        when(candidateHorseProfileRepository.findByAdmissionId(123L)).thenReturn(Optional.of(profile));

        User owner = new User();
        owner.setId(50L);
        owner.setFullName("Horse Owner Carter");
        when(userRepository.findById(50L)).thenReturn(Optional.of(owner));

        User vet = new User();
        vet.setId(8L);
        vet.setFullName("Dr. Hoang");
        when(userRepository.findById(8L)).thenReturn(Optional.of(vet));

        User trainer = new User();
        trainer.setId(5L);
        trainer.setFullName("Trainer Park");
        when(userRepository.findById(5L)).thenReturn(Optional.of(trainer));

        Object result = toolExecutionService.executeTool("getAdmissionPipelineDetails", Map.of("status", "PENDING"), manager);
        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());

        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("PEGASUS JUNIOR", item.get("horseName"));
        assertEquals("Horse Owner Carter", item.get("ownerName"));
        assertTrue(item.get("currentStage").toString().contains("MANAGER_REVIEW"));
        assertEquals("Dr. Hoang", item.get("assignedVet"));
        assertEquals("Trainer Park", item.get("designatedTrainer"));
        assertEquals("/manager/admissions/123", item.get("navigationUrl"));
        assertEquals("/manager/admissions/123", item.get("actionUrl"));
    }

    @Test
    @DisplayName("CLUB_MANAGER calls getAdmissionPipelineOverview to view application counts and admission details")
    void testManager_GetAdmissionPipelineOverview() {
        AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@rtms.com", "CLUB_MANAGER");

        AdmissionApplication app = new AdmissionApplication();
        app.setId(123L);
        app.setOwnerId(50L);
        app.setStatus(AdmissionStatus.MANAGER_REVIEW);
        app.setSubmittedAt(LocalDateTime.of(2026, 10, 20, 10, 0));
        when(admissionApplicationRepository.findAll()).thenReturn(List.of(app));
        when(admissionApplicationRepository.findByStatus(AdmissionStatus.MANAGER_REVIEW)).thenReturn(List.of(app));

        CandidateHorseProfile profile = new CandidateHorseProfile();
        profile.setId(1L);
        profile.setAdmissionId(123L);
        profile.setName("PEGASUS JUNIOR");
        when(candidateHorseProfileRepository.findByAdmissionId(123L)).thenReturn(Optional.of(profile));

        Object result = toolExecutionService.executeTool("getAdmissionPipelineOverview", Map.of("status", "PENDING"), manager);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertTrue(map.containsKey("tongQuanSoLuong"));
        assertTrue(map.containsKey("chiTietCacDon"));
        assertEquals("/manager/admissions", map.get("actionUrl"));
    }
}
