package com.rtms.backend.medical.service;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.identity.repository.VeterinarianProfileRepository;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.medical.dto.CompleteCareScheduleRequest;
import com.rtms.backend.medical.dto.CreateNextScheduleRequest;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.enums.CareSchedulePurpose;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.medical.repository.HorseHealthMetricRepository;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.service.TrainingDecisionService;
import com.rtms.backend.training.service.TrainingWorkoutService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AUTO cadence: a completed routine follow-up without a vet-supplied date chains to the next one
 * 14 days after the completed visit. MANUAL (an explicit nextSchedule) always wins.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoutineCadenceTest {

    @Mock CareScheduleRepository schedules;
    @Mock HorseRepository horses;
    @Mock HealthRecordRepository healthRecords;
    @Mock HorseHealthMetricRepository metrics;
    @Mock AdmissionApplicationRepository admissions;
    @Mock StableStallRepository stalls;
    @Mock UserRepository users;
    @Mock HorseIncidentReportRepository incidents;
    @Mock VeterinarianProfileRepository profiles;
    @Mock AuditLogRepository audits;
    @Mock ApplicationEventPublisher events;
    @Mock EntityManager entityManager;
    @Mock NotificationService notifications;
    @Mock TrainingWorkoutService workouts;
    @Mock PreventiveVaccinationService vaccinations;

    private CareScheduleService service;
    private Horse horse;
    private User vet;

    @BeforeEach
    void setUp() {
        service = new CareScheduleService(schedules, horses, healthRecords, metrics, admissions, stalls, users,
                incidents, profiles, audits, events, entityManager, notifications,
                new TrainingDecisionService(horses, schedules, workouts), vaccinations,
                org.mockito.Mockito.mock(com.rtms.backend.medical.repository.InjuryRecordRepository.class));

        horse = new Horse();
        horse.setId(10L);
        horse.setName("Thunder");
        horse.setCurrentStatus(HorseStatus.ELIGIBLE);
        horse.setTrainingDecision(TrainingDecision.ALLOWED);

        vet = new User();
        vet.setId(20L);
        vet.setFullName("Dr. Smith");
        vet.setActive(true);

        when(schedules.save(any(CareSchedule.class))).thenAnswer(call -> call.getArgument(0));
        when(schedules.saveAndFlush(any(CareSchedule.class))).thenAnswer(call -> {
            CareSchedule saved = call.getArgument(0);
            saved.setId(900L);
            return saved;
        });
        when(healthRecords.save(any(HealthRecord.class))).thenAnswer(call -> {
            HealthRecord record = call.getArgument(0);
            record.setId(999L);
            return record;
        });
        when(horses.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
    }

    // ---- 1. AUTO chain ----

    @Test
    @DisplayName("ROUTINE + FOLLOW_UP hoàn tất không có ngày -> đúng một lịch AUTO tại completedAt + 14 ngày")
    void routineFollowUpWithoutDateChainsFourteenDaysAfterCompletion() {
        CareSchedule exam = routineExam(CareScheduleStatus.IN_PROGRESS);
        stubCompletion(exam);

        service.completeCareSchedule(exam.getId(), request(TrainingDecision.ALLOWED), vet.getId());

        ArgumentCaptor<CareSchedule> chain = ArgumentCaptor.forClass(CareSchedule.class);
        verify(schedules, times(1)).saveAndFlush(chain.capture());
        CareSchedule next = chain.getValue();
        assertNotNull(exam.getCompletedAt(), "ca đã khám phải có completedAt đã lưu");
        assertEquals(exam.getCompletedAt().plusDays(14), next.getRequestedAt(),
                "lịch kế tiếp neo vào completedAt đã lưu, không neo vào now()");
        assertEquals("completion:" + exam.getId(), next.getIdempotencyKey());
        assertEquals(exam.getId(), next.getSourceScheduleId());
        assertEquals(CareType.ROUTINE, next.getCareType());
        assertEquals(CareSchedulePurpose.FOLLOW_UP, next.getPurpose());
        assertEquals("Routine follow-up recheck examination", next.getDescription());
    }

    // ---- 2. Double submit ----

    @Test
    @DisplayName("Bấm hoàn tất lần hai trên ca đã COMPLETED -> trả về đúng lịch cũ, không tạo trùng, không IDEMPOTENCY_CONFLICT")
    void doubleSubmitOnCompletedRoutineVisitResolvesTheSameRow() {
        CareSchedule exam = routineExam(CareScheduleStatus.COMPLETED);
        exam.setCompletedAt(LocalDateTime.now().minusDays(1));
        LocalDateTime expectedRequestedAt = exam.getCompletedAt().plusDays(14);

        CareSchedule existing = new CareSchedule();
        existing.setId(900L);
        existing.setHorseId(horse.getId());
        existing.setSourceScheduleId(exam.getId());
        existing.setIdempotencyKey("completion:" + exam.getId());
        existing.setRequestedAt(expectedRequestedAt);
        existing.setDescription("Routine follow-up recheck examination");
        existing.setRequestFingerprint(fingerprint(horse.getId(), null, expectedRequestedAt, existing.getDescription()));

        when(schedules.findById(exam.getId())).thenReturn(Optional.of(exam));
        when(schedules.findByIdForUpdate(exam.getId())).thenReturn(Optional.of(exam));
        when(schedules.findBySourceScheduleId(exam.getId())).thenReturn(Optional.of(existing));

        var response = service.completeCareSchedule(exam.getId(), request(TrainingDecision.ALLOWED), vet.getId());

        assertEquals(CareScheduleStatus.COMPLETED, response.status());
        verify(schedules, never()).saveAndFlush(any());
        verify(healthRecords, never()).save(any());
    }

    // ---- 3. BLOCKED vẫn bắt buộc có ngày ----

    @Test
    @DisplayName("Kết luận BLOCKED mà không có lịch khám lại -> 400 FOLLOW_UP_REQUIRED, không tạo lịch AUTO")
    void blockedWithoutNextScheduleIsStillRejected() {
        CareSchedule exam = routineExam(CareScheduleStatus.IN_PROGRESS);
        CompleteCareScheduleRequest req = request(TrainingDecision.BLOCKED);
        req.setRestrictionDetails("Walk only for 5 days");

        ApiException error = assertThrows(ApiException.class,
                () -> service.completeCareSchedule(exam.getId(), req, vet.getId()));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertEquals("FOLLOW_UP_REQUIRED", error.getErrorCode());
        verify(schedules, never()).saveAndFlush(any());
        verify(healthRecords, never()).save(any());
    }

    // ---- 4. INITIAL / URGENT không nối chuỗi ----

    @Test
    @DisplayName("Ca INITIAL hoàn tất không có lịch khám lại -> không tạo lịch AUTO")
    void initialExamDoesNotChain() {
        CareSchedule exam = routineExam(CareScheduleStatus.IN_PROGRESS);
        exam.setCareType(CareType.INITIAL);
        exam.setAdmissionId(500L);
        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(500L);
        admission.setHorseId(horse.getId());
        admission.setStatus(AdmissionStatus.VET_REVIEW);
        when(admissions.findByIdForUpdate(500L)).thenReturn(Optional.of(admission));
        stubCompletion(exam);

        service.completeCareSchedule(exam.getId(), request(TrainingDecision.ALLOWED), vet.getId());

        assertEquals(CareScheduleStatus.COMPLETED, exam.getStatus());
        verify(schedules, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Ca URGENT hoàn tất không có lịch khám lại -> không tạo lịch AUTO")
    void urgentExamDoesNotChain() {
        CareSchedule exam = routineExam(CareScheduleStatus.IN_PROGRESS);
        exam.setCareType(CareType.URGENT);
        stubCompletion(exam);

        service.completeCareSchedule(exam.getId(), request(TrainingDecision.ALLOWED), vet.getId());

        assertEquals(CareScheduleStatus.COMPLETED, exam.getStatus());
        verify(schedules, never()).saveAndFlush(any());
    }

    // ---- 5. MANUAL thắng AUTO ----

    @Test
    @DisplayName("Thú y chọn ngày -> lịch MANUAL giữ nguyên, không có lịch AUTO thứ hai")
    void manualNextScheduleStaysManual() {
        CareSchedule exam = routineExam(CareScheduleStatus.IN_PROGRESS);
        stubCompletion(exam);

        LocalDateTime manualAt = LocalDateTime.now().plusDays(5).withSecond(0).withNano(0);
        CreateNextScheduleRequest next = new CreateNextScheduleRequest();
        next.setCareType(CareType.ROUTINE);
        next.setScheduledAt(manualAt);
        next.setDescription("Post-treatment clinical reassessment");
        CompleteCareScheduleRequest req = request(TrainingDecision.ALLOWED);
        req.setNextSchedule(next);

        service.completeCareSchedule(exam.getId(), req, vet.getId());

        ArgumentCaptor<CareSchedule> created = ArgumentCaptor.forClass(CareSchedule.class);
        verify(schedules, times(1)).saveAndFlush(created.capture());
        assertEquals(manualAt, created.getValue().getRequestedAt());
        assertEquals("Post-treatment clinical reassessment", created.getValue().getDescription());
        assertEquals("completion:" + exam.getId(), created.getValue().getIdempotencyKey());
    }

    // ---- 6. PREVENTIVE không nối chuỗi ----

    @Test
    @DisplayName("Ca PREVENTIVE hoàn tất -> không nối chuỗi, mốc tiêm phòng tự lo")
    void preventiveVisitDoesNotChain() {
        CareSchedule exam = routineExam(CareScheduleStatus.IN_PROGRESS);
        exam.setPurpose(CareSchedulePurpose.PREVENTIVE);
        stubCompletion(exam);

        service.completeCareSchedule(exam.getId(), request(TrainingDecision.ALLOWED), vet.getId());

        assertEquals(CareScheduleStatus.COMPLETED, exam.getStatus());
        verify(schedules, never()).saveAndFlush(any());
        verify(vaccinations).refreshMilestone(horse.getId(), 999L, vet.getId(), CareType.ROUTINE);
    }

    // ---- helpers ----

    private CareSchedule routineExam(CareScheduleStatus status) {
        CareSchedule exam = new CareSchedule();
        exam.setId(100L);
        exam.setHorseId(horse.getId());
        exam.setVeterinarianId(vet.getId());
        exam.setCareType(CareType.ROUTINE);
        exam.setPurpose(CareSchedulePurpose.FOLLOW_UP);
        exam.setStatus(status);
        return exam;
    }

    private void stubCompletion(CareSchedule exam) {
        when(schedules.findById(exam.getId())).thenReturn(Optional.of(exam));
        when(schedules.findByIdForUpdate(exam.getId())).thenReturn(Optional.of(exam));
        when(schedules.findBySourceScheduleId(exam.getId())).thenReturn(Optional.empty());
        when(schedules.existsByHorseIdAndCareTypeAndStatusIn(eq(horse.getId()), any(), any()))
                .thenReturn(false);
        when(schedules.existsByHorseIdAndCareTypeAndStatusInAndIdNot(eq(horse.getId()), any(), any(), anyLong()))
                .thenReturn(false);
        when(incidents.findByHorseIdAndStatusIn(eq(horse.getId()), any())).thenReturn(List.of());
        when(horses.findTrainerIdByHorseId(horse.getId())).thenReturn(Optional.empty());
    }

    private static CompleteCareScheduleRequest request(TrainingDecision decision) {
        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Examined");
        req.setDiagnosis("Healthy");
        req.setTrainingDecision(decision);
        return req;
    }

    private static String fingerprint(Long horseId, Long admissionId, LocalDateTime requestedAt, String description) {
        String value = horseId + "|" + admissionId + "|ROUTINE|" + requestedAt + "|" + description;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    @Test
    @DisplayName("Lịch AUTO nằm trong cửa sổ xếp Vet 14 ngày nên được gán ngay")
    void autoChainStaysInsideTheAssignmentHorizon() {
        CareSchedule exam = routineExam(CareScheduleStatus.IN_PROGRESS);
        stubCompletion(exam);

        service.completeCareSchedule(exam.getId(), request(TrainingDecision.ALLOWED), vet.getId());

        ArgumentCaptor<CareSchedule> chain = ArgumentCaptor.forClass(CareSchedule.class);
        verify(schedules).saveAndFlush(chain.capture());
        LocalDateTime horizon = LocalDateTime.now().plusDays(CareScheduleService.ASSIGNMENT_HORIZON_DAYS);
        assertTrue(chain.getValue().getRequestedAt().isBefore(horizon),
                "requestedAt phải nhỏ hơn now + 14 ngày để được xếp Vet ngay");
        assertTrue(!CareScheduleService.isBeyondAssignmentHorizon(chain.getValue(), horizon));
    }
}
