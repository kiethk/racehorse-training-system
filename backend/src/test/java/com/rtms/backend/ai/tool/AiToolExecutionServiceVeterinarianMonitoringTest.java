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

class AiToolExecutionServiceVeterinarianMonitoringTest extends AiToolExecutionServiceTestSupport {

    // =================================================================
    // VETERINARIAN Tests
    // =================================================================

    @Test
    @DisplayName("VETERINARIAN calls getVetExaminationOverview to review horses currently being examined, awaiting examination, and examined")
    void testVet_GetVetExaminationOverview() {
        AuthenticatedUser vet = new AuthenticatedUser(8L, "vet@test.com", "VETERINARIAN");

        CareSchedule cs1 = new CareSchedule();
        cs1.setId(1L);
        cs1.setHorseId(10L);
        cs1.setStatus(CareScheduleStatus.IN_PROGRESS);
        cs1.setVeterinarianId(8L);

        CareSchedule cs2 = new CareSchedule();
        cs2.setId(2L);
        cs2.setHorseId(11L);
        cs2.setStatus(CareScheduleStatus.SCHEDULED);
        cs2.setVeterinarianId(8L);

        when(careScheduleRepository.findByVeterinarianId(8L)).thenReturn(List.of(cs1, cs2));

        Object result = toolExecutionService.executeTool("getVetExaminationOverview", Map.of(), vet);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(1, map.get("inProgressExamsCount"));
        assertEquals(1, map.get("pendingScheduledExamsCount"));
    }

    @Test
    @DisplayName("VETERINARIAN calls getQuarantineStatus: quarantined horses are always BLOCKED and cannot train")
    void testVet_GetQuarantineStatus() {
        AuthenticatedUser vet = new AuthenticatedUser(8L, "vet@test.com", "VETERINARIAN");

        Area qArea = new Area();
        qArea.setId(99L);
        qArea.setCode("Q-A");
        qArea.setType(AreaType.QUARANTINE);
        when(areaRepository.findByType(AreaType.QUARANTINE)).thenReturn(List.of(qArea));

        StableStall qStall = new StableStall();
        qStall.setId(901L);
        qStall.setAreaId(99L);
        qStall.setStallCode("Q-01");
        when(stableStallRepository.findByAreaIdInOrdered(List.of(99L))).thenReturn(List.of(qStall));

        Horse qHorse = new Horse();
        qHorse.setId(10L);
        qHorse.setName("QUARANTINE HORSE");
        qHorse.setCurrentStallId(901L);
        qHorse.setTrainingDecision(TrainingDecision.BLOCKED);
        when(horseRepository.findByCurrentStallIdIn(List.of(901L))).thenReturn(List.of(qHorse));
        when(horseRepository.findAll()).thenReturn(List.of(qHorse));

        Object result = toolExecutionService.executeTool("getQuarantineStatus", Map.of(), vet);

        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(1, map.get("quarantinedOrRestrictedHorsesCount"));

        List<?> list = (List<?>) map.get("quarantinedHorseList");
        Map<?, ?> horseItem = (Map<?, ?>) list.get(0);
        assertEquals("BLOCKED", horseItem.get("trainingDecision"));
        assertEquals(false, horseItem.get("isTrainingAllowed"));
    }

    @Test
    @DisplayName("VETERINARIAN calls getUrgentCareAlerts to retrieve urgent care cases")
    void testVet_GetUrgentCareAlerts() {
        AuthenticatedUser vet = new AuthenticatedUser(300L, "vet@rtms.com", "VETERINARIAN");

        CareSchedule cs = new CareSchedule();
        cs.setId(501L);
        cs.setHorseId(10L);
        cs.setCareType(CareType.URGENT);
        cs.setStatus(CareScheduleStatus.SCHEDULED);
        cs.setDescription("Horse developed a sudden high fever");
        cs.setScheduledAt(LocalDateTime.of(2026, 10, 20, 14, 0));
        cs.setVeterinarianId(300L);
        when(careScheduleRepository.findByVeterinarianId(300L)).thenReturn(List.of(cs));

        Horse h = new Horse();
        h.setId(10L);
        h.setName("LIGHTNING");
        when(horseRepository.findAll()).thenReturn(List.of(h));

        Object result = toolExecutionService.executeTool("getUrgentCareAlerts", Map.of(), vet);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> response = (Map<?, ?>) result;
        assertEquals(1, response.get("totalUrgentAlerts"));
        List<?> list = (List<?>) response.get("alertList");
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("LIGHTNING", item.get("horseName"));
        assertEquals("Urgent", item.get("severity"));
        assertEquals("Horse developed a sudden high fever", item.get("symptoms"));
        assertEquals("/veterinarian/urgent/501", item.get("actionUrl"));
    }

    @Test
    @DisplayName("VETERINARIAN calls getHorseVitalSignsTrend to retrieve vital signs")
    void testVet_GetHorseVitalSignsTrend() {
        AuthenticatedUser vet = new AuthenticatedUser(300L, "vet@rtms.com", "VETERINARIAN");

        Horse h = new Horse();
        h.setId(10L);
        h.setName("LIGHTNING");
        when(horseRepository.findById(10L)).thenReturn(Optional.of(h));

        HorseHealthMetric metric = new HorseHealthMetric();
        metric.setHorseId(10L);
        metric.setWeight(BigDecimal.valueOf(480));
        metric.setTemperature(BigDecimal.valueOf(38.2));
        metric.setHeartRate(BigDecimal.valueOf(36));
        metric.setRespiratoryRate(BigDecimal.valueOf(12));
        metric.setRecordedAt(LocalDateTime.of(2026, 10, 20, 7, 0));
        when(horseHealthMetricRepository.findByHorseIdOrderByRecordedAtDesc(10L)).thenReturn(List.of(metric));

        Object result = toolExecutionService.executeTool("getHorseVitalSignsTrend", Map.of("horseId", 10L), vet);
        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("LIGHTNING", item.get("horseName"));
        assertEquals("480 kg", item.get("weight"));
        assertEquals("38.2 °C", item.get("temperature"));
        assertEquals("36 bpm", item.get("heartRate"));
        assertEquals("12 breaths/min", item.get("respiratoryRate"));
    }

    @Test
    @DisplayName("VETERINARIAN calls getHorseVitalSignsTrend without a horse or with all to retrieve the latest records")
    void testVet_GetHorseVitalSignsTrend_AllHorsesWhenEmptyOrAll() {
        AuthenticatedUser vet = new AuthenticatedUser(300L, "vet@rtms.com", "VETERINARIAN");

        HorseHealthMetric m1 = new HorseHealthMetric();
        m1.setHorseId(1L);
        m1.setRecordedAt(LocalDateTime.of(2026, 10, 20, 8, 0));
        m1.setTemperature(BigDecimal.valueOf(38.4));
        m1.setHeartRate(BigDecimal.valueOf(38));

        HorseHealthMetric m2 = new HorseHealthMetric();
        m2.setHorseId(2L);
        m2.setRecordedAt(LocalDateTime.of(2026, 10, 20, 9, 0));
        m2.setTemperature(BigDecimal.valueOf(38.1));
        m2.setHeartRate(BigDecimal.valueOf(35));

        Horse h1 = new Horse();
        h1.setId(1L);
        h1.setName("BELLA");

        Horse h2 = new Horse();
        h2.setId(2L);
        h2.setName("LIGHTNING");

        when(horseHealthMetricRepository.findAll()).thenReturn(List.of(m1, m2));
        when(horseRepository.findById(1L)).thenReturn(Optional.of(h1));
        when(horseRepository.findById(2L)).thenReturn(Optional.of(h2));

        // 1. No parameter
        Object resultEmpty = toolExecutionService.executeTool("getHorseVitalSignsTrend", Map.of(), vet);
        assertNotNull(resultEmpty);
        assertTrue(resultEmpty instanceof List<?>);
        List<?> listEmpty = (List<?>) resultEmpty;
        assertEquals(2, listEmpty.size());
        Map<?, ?> first = (Map<?, ?>) listEmpty.get(0);
        // m2 has a recordedAt of 9:00 > 8:00, so it appears first
        assertEquals("LIGHTNING", first.get("horseName"));
        assertEquals("38.1 °C", first.get("temperature"));

        // 2. Pass horseName = "all"
        Object resultAll = toolExecutionService.executeTool("getHorseVitalSignsTrend", Map.of("horseName", "all"), vet);
        assertNotNull(resultAll);
        assertEquals(2, ((List<?>) resultAll).size());
    }
}
