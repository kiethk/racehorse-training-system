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

class AiToolExecutionServiceTrainerOperationsTest extends AiToolExecutionServiceTestSupport {

    // =================================================================
    // HEAD_TRAINER Tests
    // =================================================================

    @Test
    @DisplayName("HEAD_TRAINER calls getTrainerAssignedGrooms to retrieve collaborating grooms")
    void testTrainer_GetTrainerAssignedGrooms() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@rtms.com", "HEAD_TRAINER");

        Area area = new Area();
        area.setId(1L);
        area.setName("Training Area A");
        area.setTrainerId(5L);
        when(areaRepository.findAll()).thenReturn(List.of(area));

        StableStall stall = new StableStall();
        stall.setId(10L);
        stall.setAreaId(1L);
        stall.setGroomId(7L);
        stall.setStallCode("A-01");
        when(stableStallRepository.findByAreaIdInOrdered(List.of(1L))).thenReturn(List.of(stall));

        User groomUser = new User();
        groomUser.setId(7L);
        groomUser.setFullName("John Groom");
        groomUser.setPhone("0988123456");
        when(userRepository.findById(7L)).thenReturn(Optional.of(groomUser));

        Object result = toolExecutionService.executeTool("getTrainerAssignedGrooms", Map.of(), trainer);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(1, map.get("totalCollaboratingGrooms"));
        List<?> groomList = (List<?>) map.get("groomList");
        assertNotNull(groomList);
        assertEquals(1, groomList.size());
        Map<?, ?> groomItem = (Map<?, ?>) groomList.get(0);
        assertEquals("0988123456", groomItem.get("phone"));
        assertEquals("No email available", groomItem.get("email"));
    }

    @Test
    @DisplayName("HEAD_TRAINER calls getAssignedAdmissionReviews to retrieve applications awaiting review")
    void testTrainer_GetAssignedAdmissionReviews() {
        AuthenticatedUser trainer = new AuthenticatedUser(200L, "trainer@rtms.com", "HEAD_TRAINER");

        AdmissionApplication app = new AdmissionApplication();
        app.setId(301L);
        app.setTrainerId(200L);
        app.setStatus(AdmissionStatus.TRAINER_REVIEW);
        app.setSubmittedAt(LocalDateTime.of(2026, 10, 15, 9, 0));
        when(admissionApplicationRepository.findAllByOrderBySubmittedAtDesc()).thenReturn(List.of(app));

        CandidateHorseProfile cand = new CandidateHorseProfile();
        cand.setAdmissionId(301L);
        cand.setName("WIND RUNNER");
        cand.setBreed("Thoroughbred");
        when(candidateHorseProfileRepository.findByAdmissionIdIn(any())).thenReturn(List.of(cand));

        Object result = toolExecutionService.executeTool("getAssignedAdmissionReviews", Map.of(), trainer);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> response = (Map<?, ?>) result;
        assertEquals(1, response.get("pendingReviewCount"));
        List<?> list = (List<?>) response.get("applicationList");
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("WIND RUNNER", item.get("horseName"));
        assertEquals("Thoroughbred", item.get("breed"));
        assertEquals("/trainer/admissions/301", item.get("actionUrl"));
    }

    @Test
    @DisplayName("HEAD_TRAINER calls getCourseCurriculumOverview to retrieve the course and exercise catalog")
    void testTrainer_GetCourseCurriculumOverview() {
        AuthenticatedUser trainer = new AuthenticatedUser(200L, "trainer@rtms.com", "HEAD_TRAINER");

        Course c = new Course();
        c.setId(1L);
        c.setName("High-Speed Course");
        c.setTargetGoal("Improve acceleration");
        c.setTotalSessions(12);
        when(courseRepository.findAll()).thenReturn(List.of(c));

        Exercise s = new Exercise();
        s.setId(10L);
        s.setName("400m Sprint");
        s.setCategory(ExerciseCategory.SPEED);
        s.setSurfaceType(SurfaceType.TURF);
        s.setTargetDistanceMeters(BigDecimal.valueOf(400));
        s.setIntensityLevel(IntensityLevel.HIGH);
        s.setDurationMinutes(45);
        when(exerciseRepository.findAll()).thenReturn(List.of(s));

        Object result = toolExecutionService.executeTool("getCourseCurriculumOverview", Map.of(), trainer);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertTrue(map.containsKey("courseCurriculumList"));
        assertTrue(map.containsKey("technicalExerciseList"));
        assertEquals("/trainer/courses", map.get("actionUrl"));
    }
}
