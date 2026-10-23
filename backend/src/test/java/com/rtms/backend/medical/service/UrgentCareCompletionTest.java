package com.rtms.backend.medical.service;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.Role;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.identity.repository.VeterinarianProfileRepository;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.enums.CareSchedulePurpose;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.medical.repository.HorseHealthMetricRepository;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.notification.service.NotificationTypes;
import com.rtms.backend.incident.entity.HorseIncidentReport;
import com.rtms.backend.incident.enums.IncidentStatus;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.service.TrainingWorkoutService;
import com.rtms.backend.training.service.TrainingDecisionService;


import com.rtms.backend.config.ApiException;
import com.rtms.backend.medical.dto.CancelCareScheduleRequest;
import com.rtms.backend.medical.dto.CompleteCareScheduleRequest;
import com.rtms.backend.medical.dto.CreateNextScheduleRequest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrgentCareCompletionTest {

    @Mock private CareScheduleRepository careScheduleRepository;
    @Mock private HorseRepository horseRepository;
    @Mock private HealthRecordRepository healthRecordRepository;
    @Mock private HorseHealthMetricRepository metricRepository;
    @Mock private AdmissionApplicationRepository admissionRepository;
    @Mock private StableStallRepository stallRepository;
    @Mock private UserRepository userRepository;
    @Mock private HorseIncidentReportRepository incidentReportRepository;
    @Mock private VeterinarianProfileRepository veterinarianProfileRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private EntityManager entityManager;
    @Mock private NotificationService notificationService;
    @Mock private TrainingWorkoutService workoutService;

    private CareScheduleService careScheduleService;

    private Horse horse;
    private User vet;
    private CareSchedule urgentSchedule;
    private HorseIncidentReport incident;

    @BeforeEach
    void setUp() {
        // TrainingDecisionService thật: test kiểm tra luôn việc chặn tập có hủy buổi tập hay không.
        careScheduleService = new CareScheduleService(
                careScheduleRepository, horseRepository, healthRecordRepository, metricRepository,
                admissionRepository, stallRepository, userRepository, incidentReportRepository,
                veterinarianProfileRepository, auditLogRepository, eventPublisher, entityManager,
                notificationService,
                new TrainingDecisionService(horseRepository, careScheduleRepository, workoutService),
                org.mockito.Mockito.mock(PreventiveVaccinationService.class));

        horse = new Horse();
        horse.setId(10L);
        horse.setName("Thunder");
        horse.setCurrentStatus(HorseStatus.ELIGIBLE);
        horse.setTrainingDecision(TrainingDecision.ALLOWED);

        vet = new User();
        vet.setId(20L);
        vet.setFullName("Dr. Smith");
        vet.setEmail("dr_smith@example.com");
        vet.setActive(true);
        Role role = new Role();
        role.setName("VETERINARIAN");
        vet.setRole(role);

        incident = new HorseIncidentReport();
        incident.setId(30L);
        incident.setStatus(IncidentStatus.IN_REVIEW);

        urgentSchedule = new CareSchedule();
        urgentSchedule.setId(100L);
        urgentSchedule.setHorseId(horse.getId());
        urgentSchedule.setVeterinarianId(vet.getId());
        urgentSchedule.setCareType(CareType.URGENT);
        urgentSchedule.setStatus(CareScheduleStatus.SCHEDULED);
        urgentSchedule.setSourceIncidentId(incident.getId());
    }

    @Test
    @DisplayName("createSchedule URGENT: chặn tập, ghi lý do mặc định, hủy buổi tập tương lai")
    void testCreateUrgentSchedule_blocksAndCancelsTraining() {
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));

        careScheduleService.createSchedule(10L, CareType.URGENT, null, LocalDateTime.now(), 30L);

        assertEquals(TrainingDecision.BLOCKED, horse.getTrainingDecision());
        assertEquals("Urgent veterinary care pending", horse.getTrainingDecisionReason());
        assertFalse(horse.canTrain());
        verify(workoutService).cancelFutureTrainingForHorse(eq(10L), anyString());
    }

    @Test
    @DisplayName("startCareSchedule chuyển SCHEDULED -> IN_PROGRESS")
    void testStartCareSchedule_success() {
        when(userRepository.findById(vet.getId())).thenReturn(Optional.of(vet));
        when(careScheduleRepository.findById(urgentSchedule.getId())).thenReturn(Optional.of(urgentSchedule));
        when(horseRepository.findByIdForUpdate(horse.getId())).thenReturn(Optional.of(horse));
        when(careScheduleRepository.findByIdForUpdate(urgentSchedule.getId())).thenReturn(Optional.of(urgentSchedule));
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));

        var response = careScheduleService.startCareSchedule(urgentSchedule.getId(), vet.getId());

        assertEquals(CareScheduleStatus.IN_PROGRESS, response.status());
        assertEquals(CareScheduleStatus.IN_PROGRESS, urgentSchedule.getStatus());
    }

    @Test
    @DisplayName("Khám xong kết luận ALLOWED: mở chặn tập, xóa lý do, đóng sự cố")
    void testCompleteUrgentSchedule_allowed_releasesBlock() {
        urgentSchedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        horse.setTrainingDecision(TrainingDecision.BLOCKED);
        horse.setTrainingDecisionReason("Urgent veterinary care pending");

        stubCompletion();
        when(incidentReportRepository.findById(incident.getId())).thenReturn(Optional.of(incident));

        var response = careScheduleService.completeCareSchedule(urgentSchedule.getId(),
                completeRequest(TrainingDecision.ALLOWED), vet.getId());

        assertEquals(CareScheduleStatus.COMPLETED, response.status());
        assertEquals(TrainingDecision.ALLOWED, horse.getTrainingDecision());
        assertNull(horse.getTrainingDecisionReason());
        assertTrue(horse.canTrain());
        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
        assertNotNull(incident.getHandlerNote(), "the reporting Groom should be told the outcome");
        assertTrue(incident.getHandlerNote().startsWith("Diagnosis: "));
        assertTrue(incident.getHandlerNote().endsWith("Training: allowed"));
        verify(healthRecordRepository).save(any(HealthRecord.class));
        verify(workoutService, never()).cancelFutureTrainingForHorse(anyLong(), anyString());
    }

    @Test
    @DisplayName("Trạng thái khóa cũ được giữ lại: đóng ca khẩn cấp cuối vẫn BLOCKED vì lý do trước đó")
    void testCompleteUrgentSchedule_restoresPreviousBlock() {
        HealthRecord previous = new HealthRecord();
        previous.setTrainingDecision(TrainingDecision.BLOCKED);
        previous.setRestrictionDetails("Injury rest");
        when(healthRecordRepository.findTopByHorseIdAndRecordTypeNotAndTrainingDecisionIsNotNullOrderByExaminedAtDesc(
                horse.getId(), CareType.URGENT.name())).thenReturn(Optional.of(previous));

        urgentSchedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        horse.setTrainingDecision(TrainingDecision.BLOCKED);
        stubCompletion();
        incident.setGroomId(55L);
        when(incidentReportRepository.findByHorseIdAndStatusIn(eq(horse.getId()), anyCollection()))
                .thenReturn(java.util.List.of(incident));

        careScheduleService.completeCareSchedule(urgentSchedule.getId(),
                completeRequest(TrainingDecision.ALLOWED), vet.getId());

        assertEquals(TrainingDecision.BLOCKED, horse.getTrainingDecision());
        assertEquals("Injury rest", horse.getTrainingDecisionReason());
        assertFalse(horse.canTrain());
        // Groom and Trainer must be told the effective state, not the raw ALLOWED conclusion.
        assertTrue(incident.getHandlerNote().contains("Training: blocked"));
        assertTrue(incident.getHandlerNote().contains("Injury rest"));
        verify(notificationService).sendAssignmentNotification(
                eq(55L),
                eq(NotificationTypes.REFERENCE_CARE_SCHEDULE),
                eq(urgentSchedule.getId()),
                eq(horse.getId()),
                eq(NotificationTypes.URGENT_CASE_CLOSED),
                anyString(),
                contains("BLOCKED")
        );
        verify(notificationService).sendAssignmentNotification(
                eq(55L),
                eq(NotificationTypes.REFERENCE_CARE_SCHEDULE),
                eq(urgentSchedule.getId()),
                eq(horse.getId()),
                eq(NotificationTypes.URGENT_CASE_CLOSED),
                anyString(),
                contains("Injury rest")
        );
    }

    @Test
    @DisplayName("Đóng ca Urgent resolve toàn bộ 3 báo cáo mở của ngựa, không đụng báo cáo ngựa khác")
    void testCompleteUrgentSchedule_resolvesAllOpenIncidentsOfHorse() {
        urgentSchedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        horse.setTrainingDecision(TrainingDecision.BLOCKED);
        HorseIncidentReport second = incidentOf(31L, IncidentStatus.REPORTED);
        HorseIncidentReport third = incidentOf(32L, IncidentStatus.IN_REVIEW);
        HorseIncidentReport otherHorse = incidentOf(99L, IncidentStatus.REPORTED);
        otherHorse.setHorseId(77L);

        incident.setGroomId(55L);
        when(horseRepository.findTrainerIdByHorseId(horse.getId())).thenReturn(Optional.of(88L));
        stubCompletion();
        when(incidentReportRepository.findByHorseIdAndStatusIn(eq(horse.getId()), anyCollection()))
                .thenReturn(java.util.List.of(incident, second, third));

        careScheduleService.completeCareSchedule(urgentSchedule.getId(),
                completeRequest(TrainingDecision.ALLOWED), vet.getId());

        for (HorseIncidentReport r : java.util.List.of(incident, second, third)) {
            assertEquals(IncidentStatus.RESOLVED, r.getStatus());
            assertEquals(vet.getId(), r.getHandledById());
            assertNotNull(r.getHandledAt());
        }
        assertEquals(IncidentStatus.REPORTED, otherHorse.getStatus());
        verify(incidentReportRepository).saveAll(argThat(list -> {
            int n = 0;
            for (Object ignored : list) n++;
            return n == 3;
        }));
        verify(notificationService, atLeastOnce()).sendAssignmentNotification(
                anyLong(),
                eq(NotificationTypes.REFERENCE_CARE_SCHEDULE),
                eq(urgentSchedule.getId()),
                eq(horse.getId()),
                eq(NotificationTypes.URGENT_CASE_CLOSED),
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("Báo nhầm (false alarm) vẫn đóng ca bằng HealthRecord và resolve incident")
    void testCompleteUrgentSchedule_falseAlarmStillResolvesIncidents() {
        urgentSchedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        stubCompletion();
        when(incidentReportRepository.findByHorseIdAndStatusIn(eq(horse.getId()), anyCollection()))
                .thenReturn(java.util.List.of(incident));

        var response = careScheduleService.completeCareSchedule(urgentSchedule.getId(),
                completeRequest(TrainingDecision.ALLOWED), vet.getId());

        assertEquals(CareScheduleStatus.COMPLETED, response.status());
        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
        verify(healthRecordRepository, times(1)).save(any(HealthRecord.class));
    }

    @Test
    @DisplayName("Incident nguồn ngoài kết quả query vẫn được resolve (không bỏ sót)")
    void testCompleteUrgentSchedule_resolvesSourceIncidentEvenIfNotInQuery() {
        urgentSchedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        stubCompletion();
        when(incidentReportRepository.findByHorseIdAndStatusIn(eq(horse.getId()), anyCollection()))
                .thenReturn(java.util.List.of());
        when(incidentReportRepository.findById(incident.getId())).thenReturn(Optional.of(incident));

        careScheduleService.completeCareSchedule(urgentSchedule.getId(),
                completeRequest(TrainingDecision.ALLOWED), vet.getId());

        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
    }

    @Test
    @DisplayName("BR-URG-13: alert trả trạng thái tập trước ca khẩn cấp để Vet thấy cảnh báo báo nhầm")
    void testGetUrgentCase_exposesPreviousTrainingDecision() {
        HealthRecord previous = new HealthRecord();
        previous.setTrainingDecision(TrainingDecision.BLOCKED);
        previous.setRestrictionDetails("Injury rest");
        when(healthRecordRepository.findTopByHorseIdAndRecordTypeNotAndTrainingDecisionIsNotNullOrderByExaminedAtDesc(
                horse.getId(), CareType.URGENT.name())).thenReturn(Optional.of(previous));
        when(careScheduleRepository.findById(urgentSchedule.getId())).thenReturn(Optional.of(urgentSchedule));
        when(horseRepository.findById(horse.getId())).thenReturn(Optional.of(horse));

        var alert = careScheduleService.getUrgentCase(urgentSchedule.getId(), vet.getId());

        assertEquals(TrainingDecision.BLOCKED, alert.previousTrainingDecision());
        assertEquals("Injury rest", alert.previousTrainingDecisionReason());
    }

    @Test
    @DisplayName("Báo cáo gộp vào ca khẩn cấp hiển thị đủ cho Thú y được giao, kể cả báo cáo nguồn")
    void testGetUrgentCaseReports_includesMergedReports() {
        incident.setCareScheduleId(urgentSchedule.getId());
        incident.setReportedAt(LocalDateTime.now().minusHours(1));
        HorseIncidentReport merged = incidentOf(31L, IncidentStatus.REPORTED);
        merged.setCareScheduleId(urgentSchedule.getId());
        merged.setReportedAt(LocalDateTime.now());

        when(careScheduleRepository.findById(urgentSchedule.getId())).thenReturn(Optional.of(urgentSchedule));
        when(incidentReportRepository.findByCareScheduleIdOrderByReportedAtAsc(urgentSchedule.getId()))
                .thenReturn(java.util.List.of(incident, merged));

        var reports = careScheduleService.getUrgentCaseReports(urgentSchedule.getId(), vet.getId());

        assertEquals(java.util.List.of(30L, 31L), reports.stream().map(r -> r.id()).toList());
    }

    @Test
    @DisplayName("Báo cáo nguồn cũ chưa gắn care_schedule_id vẫn được trả về")
    void testGetUrgentCaseReports_fallsBackToSourceIncident() {
        incident.setReportedAt(LocalDateTime.now());
        when(careScheduleRepository.findById(urgentSchedule.getId())).thenReturn(Optional.of(urgentSchedule));
        when(incidentReportRepository.findByCareScheduleIdOrderByReportedAtAsc(urgentSchedule.getId()))
                .thenReturn(java.util.List.of());
        when(incidentReportRepository.findById(incident.getId())).thenReturn(Optional.of(incident));

        var reports = careScheduleService.getUrgentCaseReports(urgentSchedule.getId(), vet.getId());

        assertEquals(1, reports.size());
        assertEquals(incident.getId(), reports.get(0).id());
    }

    @Test
    @DisplayName("Thú y không được giao ca không xem được báo cáo của ca")
    void testGetUrgentCaseReports_rejectsOtherVeterinarian() {
        when(careScheduleRepository.findById(urgentSchedule.getId())).thenReturn(Optional.of(urgentSchedule));

        ApiException error = assertThrows(ApiException.class,
                () -> careScheduleService.getUrgentCaseReports(urgentSchedule.getId(), 999L));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
        verify(incidentReportRepository, never()).findByCareScheduleIdOrderByReportedAtAsc(anyLong());
    }

    private HorseIncidentReport incidentOf(Long id, IncidentStatus status) {
        HorseIncidentReport r = new HorseIncidentReport();
        r.setId(id);
        r.setHorseId(horse.getId());
        r.setStatus(status);
        return r;
    }

    @Test
    @DisplayName("Kết luận BLOCKED mà không có lịch khám lại -> 400 FOLLOW_UP_REQUIRED, không ghi gì")
    void testCompleteUrgentSchedule_blockedWithoutFollowUp_rejected() {
        CompleteCareScheduleRequest req = completeRequest(TrainingDecision.BLOCKED);
        req.setRestrictionDetails("Walk only for 5 days");

        ApiException ex = assertThrows(ApiException.class,
                () -> careScheduleService.completeCareSchedule(urgentSchedule.getId(), req, vet.getId()));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("FOLLOW_UP_REQUIRED", ex.getErrorCode());
        verifyNoInteractions(healthRecordRepository, workoutService);
    }

    @Test
    @DisplayName("Kết luận BLOCKED + lịch khám lại: chặn tập, hủy buổi tập, tạo lịch ROUTINE nối với ca vừa khám")
    void testCompleteUrgentSchedule_blockedWithFollowUp_blocksAndSchedulesRecheck() {
        urgentSchedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        stubCompletion();
        when(careScheduleRepository.saveAndFlush(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));

        LocalDateTime recheckAt = LocalDateTime.now().plusDays(5).withHour(14).withMinute(0).withSecond(0).withNano(0);
        CreateNextScheduleRequest next = new CreateNextScheduleRequest();
        next.setHorseId(horse.getId());
        next.setCareType(CareType.ROUTINE);
        next.setScheduledAt(recheckAt);
        next.setDescription("Khám lại gân chân trước phải");

        CompleteCareScheduleRequest req = completeRequest(TrainingDecision.BLOCKED);
        req.setRestrictionDetails("  Walk only for 5 days  ");
        req.setNextSchedule(next);

        careScheduleService.completeCareSchedule(urgentSchedule.getId(), req, vet.getId());

        assertEquals(TrainingDecision.BLOCKED, horse.getTrainingDecision());
        assertEquals("Walk only for 5 days", horse.getTrainingDecisionReason());
        verify(workoutService).cancelFutureTrainingForHorse(eq(10L), contains("Walk only for 5 days"));

        ArgumentCaptor<CareSchedule> followUp = ArgumentCaptor.forClass(CareSchedule.class);
        verify(careScheduleRepository).saveAndFlush(followUp.capture());
        assertEquals(CareType.ROUTINE, followUp.getValue().getCareType());
        assertEquals(100L, followUp.getValue().getSourceScheduleId());
        assertEquals(recheckAt, followUp.getValue().getRequestedAt());
    }

    @Test
    @DisplayName("preservedUrgentLock: còn ca khẩn cấp khác chưa khám thì kết luận ALLOWED không mở chặn")
    void testCompleteUrgentSchedule_anotherUrgentPending_keepsBlock() {
        urgentSchedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        horse.setTrainingDecision(TrainingDecision.BLOCKED);
        horse.setTrainingDecisionReason("Urgent veterinary care pending");

        stubCompletion();
        when(careScheduleRepository.existsByHorseIdAndCareTypeAndStatusInAndIdNot(
                eq(10L), eq(CareType.URGENT), anyList(), eq(100L))).thenReturn(true);
        incident.setGroomId(55L);
        when(incidentReportRepository.findByHorseIdAndStatusIn(eq(horse.getId()), anyCollection()))
                .thenReturn(java.util.List.of(incident));

        careScheduleService.completeCareSchedule(urgentSchedule.getId(),
                completeRequest(TrainingDecision.ALLOWED), vet.getId());

        assertEquals(TrainingDecision.BLOCKED, horse.getTrainingDecision());
        assertEquals("Urgent veterinary care pending", horse.getTrainingDecisionReason());
        // Bệnh án vẫn ghi kết luận của lần khám này.
        ArgumentCaptor<HealthRecord> record = ArgumentCaptor.forClass(HealthRecord.class);
        verify(healthRecordRepository).save(record.capture());
        assertEquals(TrainingDecision.ALLOWED, record.getValue().getTrainingDecision());
        // Groom được báo đúng trạng thái hiệu lực: ngựa vẫn bị khóa vì ca khác còn mở.
        assertTrue(incident.getHandlerNote().contains("Training: blocked"));
        assertTrue(incident.getHandlerNote().contains("Urgent veterinary care pending"));
        verify(notificationService).sendAssignmentNotification(
                eq(55L),
                eq(NotificationTypes.REFERENCE_CARE_SCHEDULE),
                eq(urgentSchedule.getId()),
                eq(horse.getId()),
                eq(NotificationTypes.URGENT_CASE_CLOSED),
                anyString(),
                contains("BLOCKED")
        );
    }

    @Test
    @DisplayName("Gọi hoàn tất lại bởi đúng Vet là idempotent, không ghi bệnh án lần hai")
    void testCompleteUrgentSchedule_idempotent() {
        urgentSchedule.setStatus(CareScheduleStatus.COMPLETED);

        when(careScheduleRepository.findById(urgentSchedule.getId())).thenReturn(Optional.of(urgentSchedule));
        when(horseRepository.findByIdForUpdate(horse.getId())).thenReturn(Optional.of(horse));
        when(careScheduleRepository.findByIdForUpdate(urgentSchedule.getId())).thenReturn(Optional.of(urgentSchedule));

        var response = careScheduleService.completeCareSchedule(urgentSchedule.getId(),
                completeRequest(TrainingDecision.ALLOWED), vet.getId());

        assertEquals(CareScheduleStatus.COMPLETED, response.status());
        verify(healthRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Không cho Manager hủy lần khám cuối cùng của ngựa đang bị chặn tập (409 LAST_FOLLOW_UP)")
    void testCancel_lastPendingExamOfBlockedHorse_rejected() {
        horse.setTrainingDecision(TrainingDecision.BLOCKED);
        CareSchedule recheck = new CareSchedule();
        recheck.setId(200L);
        recheck.setHorseId(horse.getId());
        recheck.setCareType(CareType.ROUTINE);
        recheck.setStatus(CareScheduleStatus.SCHEDULED);

        when(careScheduleRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(recheck));
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager()));
        when(horseRepository.findById(horse.getId())).thenReturn(Optional.of(horse));
        when(careScheduleRepository.existsByHorseIdAndPurposeAndStatusInAndIdNot(
                eq(10L), eq(CareSchedulePurpose.FOLLOW_UP), anyList(), eq(200L)))
                .thenReturn(false);

        CancelCareScheduleRequest req = new CancelCareScheduleRequest();
        req.setReason("Vet on leave");

        ApiException ex = assertThrows(ApiException.class,
                () -> careScheduleService.cancelCareSchedule(200L, req, 1L));

        assertEquals("LAST_FOLLOW_UP", ex.getErrorCode());
        assertEquals(CareScheduleStatus.SCHEDULED, recheck.getStatus());
        verify(careScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Hủy được nếu ngựa bị chặn nhưng còn lần khám khác đang chờ")
    void testCancel_blockedHorseWithAnotherPendingExam_allowed() {
        horse.setTrainingDecision(TrainingDecision.BLOCKED);
        CareSchedule recheck = new CareSchedule();
        recheck.setId(200L);
        recheck.setHorseId(horse.getId());
        recheck.setCareType(CareType.ROUTINE);
        recheck.setStatus(CareScheduleStatus.SCHEDULED);

        when(careScheduleRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(recheck));
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager()));
        when(horseRepository.findById(horse.getId())).thenReturn(Optional.of(horse));
        when(careScheduleRepository.existsByHorseIdAndPurposeAndStatusInAndIdNot(
                eq(10L), eq(CareSchedulePurpose.FOLLOW_UP), anyList(), eq(200L)))
                .thenReturn(true);

        CancelCareScheduleRequest req = new CancelCareScheduleRequest();
        req.setReason("Duplicate booking");

        careScheduleService.cancelCareSchedule(200L, req, 1L);

        assertEquals(CareScheduleStatus.CANCELLED, recheck.getStatus());
    }

    private void stubCompletion() {
        when(careScheduleRepository.findById(urgentSchedule.getId())).thenReturn(Optional.of(urgentSchedule));
        when(horseRepository.findByIdForUpdate(horse.getId())).thenReturn(Optional.of(horse));
        when(careScheduleRepository.findByIdForUpdate(urgentSchedule.getId())).thenReturn(Optional.of(urgentSchedule));
        when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> {
            HealthRecord hr = i.getArgument(0);
            hr.setId(999L);
            return hr;
        });
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));
    }

    private CompleteCareScheduleRequest completeRequest(TrainingDecision decision) {
        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Examined");
        req.setDiagnosis("Diagnosis");
        req.setTrainingDecision(decision);
        return req;
    }

    private User manager() {
        Role role = new Role();
        role.setName("CLUB_MANAGER");
        User manager = new User();
        manager.setId(1L);
        manager.setRole(role);
        return manager;
    }
}
