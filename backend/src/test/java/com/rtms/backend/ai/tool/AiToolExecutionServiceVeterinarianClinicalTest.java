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

class AiToolExecutionServiceVeterinarianClinicalTest extends AiToolExecutionServiceTestSupport {

    // =================================================================
    // VETERINARIAN Tests
    // =================================================================

    @Test
    @DisplayName("VETERINARIAN calls getPendingAdmissionExams to retrieve applications awaiting screening")
    void testVet_GetPendingAdmissionExams() {
        AuthenticatedUser vet = new AuthenticatedUser(300L, "vet@rtms.com", "VETERINARIAN");

        AdmissionApplication app = new AdmissionApplication();
        app.setId(401L);
        app.setStatus(AdmissionStatus.VET_REVIEW);
        app.setSubmittedAt(LocalDateTime.of(2026, 10, 18, 8, 30));
        when(admissionApplicationRepository.findAllByOrderBySubmittedAtDesc()).thenReturn(List.of(app));

        CandidateHorseProfile cand = new CandidateHorseProfile();
        cand.setAdmissionId(401L);
        cand.setName("SILVER BULLET");
        when(candidateHorseProfileRepository.findByAdmissionIdIn(any())).thenReturn(List.of(cand));
        CareSchedule initialCs = new CareSchedule();
        initialCs.setCareType(CareType.INITIAL);
        initialCs.setAdmissionId(401L);
        initialCs.setVeterinarianId(300L);
        when(careScheduleRepository.findByVeterinarianId(300L)).thenReturn(List.of(initialCs));

        Object result = toolExecutionService.executeTool("getPendingAdmissionExams", Map.of(), vet);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> response = (Map<?, ?>) result;
        assertEquals(1, response.get("pendingExamCount"));
        List<?> list = (List<?>) response.get("examList");
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("SILVER BULLET", item.get("horseName"));
        assertEquals("Awaiting veterinary screening examination", item.get("status"));
        assertEquals("/veterinarian/admissions?id=401", item.get("actionUrl"));
    }

    @Test
    @DisplayName("VETERINARIAN calls analyzeIncidentPatternsAndRecurringRisks to analyze chronic risks")
    void testVet_AnalyzeIncidentPatternsAndRecurringRisks() {
        AuthenticatedUser vet = new AuthenticatedUser(300L, "vet@rtms.com", "VETERINARIAN");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("LIGHTNING");
        horse.setRegistrationNumber("VN-LIGHTNING");
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        HorseIncidentReport ir = new HorseIncidentReport();
        ir.setTitle("Mild tendonitis");
        when(horseIncidentReportRepository.findByHorseId(10L)).thenReturn(List.of(ir));

        HealthRecord hr = new HealthRecord();
        hr.setDiagnosis("Hock arthritis");
        when(healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(10L)).thenReturn(List.of(hr));

        Object result = toolExecutionService.executeTool("analyzeIncidentPatternsAndRecurringRisks", Map.of("horseId", 10L), vet);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("LIGHTNING", map.get("horseName"));
        assertEquals(1, map.get("totalIncidents"));
        assertEquals(1, map.get("totalExamsCount"));
        assertTrue(map.get("commonConditions").toString().contains("arthritis"));
        assertEquals("/veterinarian/health?horseId=10", map.get("actionUrl"));
    }

    @Test
    @DisplayName("VETERINARIAN calls recommendClinicalInterventionPlan to recommend a RAG-based treatment plan")
    void testVet_RecommendClinicalInterventionPlan() {
        AuthenticatedUser vet = new AuthenticatedUser(300L, "vet@rtms.com", "VETERINARIAN");

        ClinicalChunkResponseDto chunk = new ClinicalChunkResponseDto(
                1L, "Equine colic treatment protocol", "Digestive system",
                "Insert a nasogastric tube and administer Flunixin Meglumine", 0.88, Map.of()
        );
        when(clinicalRetrievalService.retrieveSimilarClinicalRecords(anyString(), any(), anyInt()))
                .thenReturn(List.of(chunk));

        Object result = toolExecutionService.executeTool(
                "recommendClinicalInterventionPlan",
                Map.of("symptoms", "The horse is pawing the ground, refusing to eat, and showing signs of abdominal pain"),
                vet
        );
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertTrue(map.get("recommendedIntervention").toString().contains("Step 1"));
        assertNotNull(map.get("requiredTests"));
        assertNotNull(map.get("minObservationTime"));
        assertEquals("/veterinarian/urgent", map.get("actionUrl"));
    }

    @Test
    @DisplayName("VETERINARIAN calls evaluateTrainingResumptionReadiness to assess readiness to resume training")
    void testVet_EvaluateTrainingResumptionReadiness() {
        AuthenticatedUser vet = new AuthenticatedUser(300L, "vet@rtms.com", "VETERINARIAN");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("LIGHTNING");
        horse.setRegistrationNumber("VN-LIGHTNING");
        horse.setTrainingDecision(TrainingDecision.ALLOWED);
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        HorseHealthMetric m = new HorseHealthMetric();
        m.setTemperature(new BigDecimal("37.8"));
        m.setHeartRate(new BigDecimal("38"));
        when(horseHealthMetricRepository.findByHorseIdOrderByRecordedAtDesc(10L)).thenReturn(List.of(m));

        Object result = toolExecutionService.executeTool("evaluateTrainingResumptionReadiness", Map.of("horseId", 10L), vet);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("LIGHTNING", map.get("horseName"));
        assertEquals("85/100", map.get("readinessScore"));
        assertEquals("Fit for light exercise", map.get("vetRecommendations"));
        assertEquals("/veterinarian/preventive-care", map.get("actionUrl"));
    }

    @Test
    @DisplayName("VETERINARIAN calls getUrgentCareAndIncidentAlerts to retrieve urgent-care and stable-incident alerts for the past 48 hours")
    void testVet_GetUrgentCareAndIncidentAlerts() {
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

        HorseIncidentReport rep = new HorseIncidentReport();
        rep.setId(701L);
        rep.setHorseId(10L);
        rep.setTitle("Minor abrasion on the left foreleg");
        rep.setReportedAt(LocalDateTime.now().minusHours(12));
        when(horseIncidentReportRepository.findByReportedAtAfterOrderByReportedAtDesc(any())).thenReturn(List.of(rep));

        Object result = toolExecutionService.executeTool("getUrgentCareAndIncidentAlerts", Map.of(), vet);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertTrue(map.containsKey("caTrucCapCuu"));
        assertTrue(map.containsKey("suCoChuongTrai48h"));
        assertEquals("/veterinarian/urgent", map.get("actionUrl"));
    }

    @Test
    @DisplayName("VETERINARIAN calls getActivePrescriptions to correctly deserialize snake_case JSON from treatment_plans.instructions")
    void testVeterinarian_GetActivePrescriptions_DeserializesInstructionsJson() {
        AuthenticatedUser vet = new AuthenticatedUser(300L, "vet@rtms.com", "VETERINARIAN");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("DARK ANGEL");
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        TreatmentPlan plan = new TreatmentPlan();
        plan.setId(1L);
        plan.setHealthRecordId(5L);
        plan.setTreatmentName("Seven-day anti-inflammatory treatment");
        plan.setStartDate(LocalDate.of(2026, 11, 1));
        plan.setEndDate(LocalDate.of(2026, 11, 8));
        plan.setStatus("ACTIVE");
        plan.setInstructions("[{\"medication_name\":\"Phenylbutazone\",\"dosage\":\"2.2 mg/kg\",\"frequency\":\"BID\",\"route\":\"ORAL\",\"duration\":7,\"withdrawal_days\":14,\"notes\":\"Mixed with oats\"}]");

        when(treatmentPlanRepository.findActivePlansByHorseId(10L)).thenReturn(List.of(plan));

        Object result = toolExecutionService.executeTool("getActivePrescriptions", Map.of("horseId", 10L), vet);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(1, map.get("totalPrescriptions"));
        List<?> list = (List<?>) map.get("treatmentList");
        assertEquals(1, list.size());

        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("DARK ANGEL", item.get("horseName"));
        assertEquals("Phenylbutazone", item.get("medicationName"));
        assertEquals("2.2 mg/kg", item.get("dosage"));
        assertEquals("BID", item.get("frequency"));
        assertEquals("ORAL", item.get("route"));
        assertEquals(7, item.get("duration"));
        assertEquals(14, item.get("withdrawalDays"));
        assertEquals("Mixed with oats", item.get("notes"));
    }
}
