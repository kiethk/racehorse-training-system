package com.rtms.backend.admission.service;
import com.rtms.backend.common.storage.FileStorageService;
import com.rtms.backend.admission.dto.VetAdmissionQueueItemResponse;
import com.rtms.backend.admission.dto.VetReviewRequest;
import com.rtms.backend.admission.dto.VetReviewResponse;
import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.entity.CandidateHorseProfile;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.enums.ReviewDecision;
import com.rtms.backend.admission.service.AdmissionGroomReviewService;
import com.rtms.backend.admission.service.AdmissionNotificationService;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.repository.AdmissionDocumentRepository;
import com.rtms.backend.admission.repository.CandidateHorseProfileRepository;
import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.medical.entity.InjuryRecord;
import com.rtms.backend.medical.repository.InjuryRecordRepository;
import com.rtms.backend.medical.repository.PrescriptionRepository;
import com.rtms.backend.medical.repository.TreatmentPlanRepository;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.enums.StallStatus;
import com.rtms.backend.identity.repository.VeterinarianProfileRepository;
import com.rtms.backend.medical.dto.CareScheduleResponse;
import com.rtms.backend.medical.dto.CompleteCareScheduleRequest;
import com.rtms.backend.medical.dto.CreateNextScheduleRequest;
import com.rtms.backend.medical.dto.HorseHealthMetricRequest;
import com.rtms.backend.medical.dto.InjuryMarkerRequest;
import com.rtms.backend.medical.enums.HydrationStatus;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.medical.repository.HorseHealthMetricRepository;
import com.rtms.backend.medical.service.CareScheduleService;
import com.rtms.backend.medical.service.PreventiveVaccinationService;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.service.TrainingGroupService;
import com.rtms.backend.training.service.TrainingDecisionService;


import com.rtms.backend.config.ApiException;
import com.rtms.backend.event.InitialExamCompletedEvent;
import com.rtms.backend.security.AuthenticatedUser;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdmissionVetReviewWorkflowTest {

    @Mock
    private CareScheduleRepository careScheduleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private HorseRepository horseRepository;
    @Mock
    private HealthRecordRepository healthRecordRepository;
    @Mock
    private HorseHealthMetricRepository metricRepository;
    @Mock
    private AdmissionApplicationRepository admissionRepository;
    @Mock
    private StableStallRepository stallRepository;
    @Mock
    private HorseIncidentReportRepository incidentRepository;
    @Mock
    private VeterinarianProfileRepository vetProfileRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private EntityManager entityManager;
    @Mock
    private NotificationService notificationService;
    @Mock
    private TrainingGroupService workoutService;
    @Mock
    private CandidateHorseProfileRepository candidateProfileRepository;
    @Mock
    private AdmissionDocumentRepository admissionDocumentRepository;
    @Mock
    private FileStorageService fileStorage;
    @Mock
    private AdmissionNotificationService admissionNotificationService;
    @Mock
    private AdmissionGroomReviewService admissionGroomReviewService;

    private CareScheduleService careScheduleService;
    private AdmissionQueryService admissionQueryService;
    private AdmissionReviewService admissionReviewService;

    @BeforeEach
    void setUp() {
        careScheduleService = new CareScheduleService(
                careScheduleRepository,
                horseRepository,
                healthRecordRepository,
                metricRepository,
                admissionRepository,
                stallRepository,
                userRepository,
                incidentRepository,
                vetProfileRepository,
                auditLogRepository,
                eventPublisher,
                entityManager,
                notificationService,
                new TrainingDecisionService(horseRepository, careScheduleRepository, workoutService),
                mock(PreventiveVaccinationService.class),
                mock(InjuryRecordRepository.class),
                mock(TreatmentPlanRepository.class),
                mock(PrescriptionRepository.class)
        );

        admissionQueryService = new AdmissionQueryService(
                admissionRepository,
                candidateProfileRepository,
                admissionDocumentRepository,
                stallRepository,
                healthRecordRepository,
                fileStorage,
                userRepository,
                careScheduleRepository
        );

        admissionReviewService = new AdmissionReviewService(
                admissionRepository,
                careScheduleRepository,
                horseRepository,
                stallRepository,
                careScheduleService,
                healthRecordRepository,
                admissionNotificationService,
                admissionGroomReviewService
        );
    }

    @Test
    @DisplayName("Vet review thiếu kết luận tập luyện -> 400, không đụng tới đơn")
    void reviewByVet_missingTrainingDecision_throws400() {
        VetReviewRequest req = new VetReviewRequest();
        req.setPhysicalExamConfirmed(true);
        req.setFindings("Severe colic");

        ApiException ex = assertThrows(ApiException.class, () ->
                admissionReviewService.reviewByVet(1L, req, 15L));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verifyNoInteractions(admissionRepository);
    }

    @Test
    @DisplayName("Khám nhập học BLOCKED: Vet không từ chối đơn — đơn vẫn sang TRAINER_REVIEW kèm kết luận BLOCKED")
    void completeInitialExam_blocked_stillMovesToTrainerReview() {
        Long scheduleId = 100L;
        Long vetId = 15L;
        Long horseId = 20L;
        Long admissionId = 1L;

        CareSchedule schedule = new CareSchedule();
        schedule.setId(scheduleId);
        schedule.setHorseId(horseId);
        schedule.setAdmissionId(admissionId);
        schedule.setCareType(CareType.INITIAL);
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        schedule.setVeterinarianId(vetId);

        Horse horse = new Horse();
        horse.setId(horseId);
        horse.setCurrentStatus(HorseStatus.CANDIDATE);

        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(admissionId);
        admission.setHorseId(horseId);
        admission.setStatus(AdmissionStatus.VET_REVIEW);

        when(careScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(careScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule));
        when(admissionRepository.findByIdForUpdate(admissionId)).thenReturn(Optional.of(admission));
        when(admissionRepository.findById(admissionId)).thenReturn(Optional.of(admission));
        when(horseRepository.findByIdForUpdate(horseId)).thenReturn(Optional.of(horse));
        when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));
        when(careScheduleRepository.saveAndFlush(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));

        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Mild tendon heat");
        req.setDiagnosis("Tendon strain");
        req.setTrainingDecision(TrainingDecision.BLOCKED);
        req.setRestrictionDetails("Box rest 10 days");
        req.setMetrics(List.of(fullMetric()));
        req.setInjuryMarkers(List.of(injuryMarker()));
        CreateNextScheduleRequest next = new CreateNextScheduleRequest();
        next.setCareType(CareType.ROUTINE);
        next.setDescription("Recheck tendon");
        next.setScheduledDate(LocalDate.now().plusDays(10).toString());
        req.setNextSchedule(next);

        careScheduleService.completeCareSchedule(scheduleId, req, vetId);

        assertEquals(AdmissionStatus.TRAINER_REVIEW, admission.getStatus());
        assertEquals(TrainingDecision.BLOCKED, admission.getVetTrainingDecision());
        assertEquals(TrainingDecision.BLOCKED, horse.getTrainingDecision());
        assertEquals("Box rest 10 days", horse.getTrainingDecisionReason());
        verify(eventPublisher).publishEvent(new InitialExamCompletedEvent(admissionId));
    }

    @Test
    @DisplayName("Atomic complete and create next schedule inside same transaction")
    void completeCareSchedule_atomicNextSchedule_success() {
        Long scheduleId = 100L;
        Long vetId = 15L;
        Long horseId = 20L;
        Long admissionId = 1L;

        CareSchedule schedule = new CareSchedule();
        schedule.setId(scheduleId);
        schedule.setHorseId(horseId);
        schedule.setAdmissionId(admissionId);
        schedule.setCareType(CareType.INITIAL);
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        schedule.setVeterinarianId(vetId);

        Horse horse = new Horse();
        horse.setId(horseId);
        horse.setCurrentStatus(HorseStatus.CANDIDATE);

        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(admissionId);
        admission.setHorseId(horseId);
        admission.setStatus(AdmissionStatus.VET_REVIEW);

        when(careScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(careScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule));
        when(admissionRepository.findByIdForUpdate(admissionId)).thenReturn(Optional.of(admission));
        when(admissionRepository.findById(admissionId)).thenReturn(Optional.of(admission));
        when(horseRepository.findByIdForUpdate(horseId)).thenReturn(Optional.of(horse));
        when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));
        when(careScheduleRepository.saveAndFlush(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));

        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Initial exam clear");
        req.setDiagnosis("Good condition");
        req.setTrainingDecision(TrainingDecision.ALLOWED);
        req.setMetrics(List.of(fullMetric()));

        CreateNextScheduleRequest next = new CreateNextScheduleRequest();
        next.setCareType(CareType.ROUTINE);
        next.setDescription("Follow-up heart rate check");
        next.setScheduledDate(LocalDate.now().plusDays(3).toString());
        req.setNextSchedule(next);

        CareScheduleResponse res = careScheduleService.completeCareSchedule(scheduleId, req, vetId);

        assertNotNull(res);
        assertEquals(CareScheduleStatus.COMPLETED, schedule.getStatus());
        assertEquals(AdmissionStatus.TRAINER_REVIEW, admission.getStatus());
        assertEquals(TrainingDecision.ALLOWED, admission.getVetTrainingDecision());
        assertEquals(15L, admission.getVeterinarianId(), "Vet assignment must be preserved");
        // Gán Trainer chạy sau commit qua sự kiện, không gọi thẳng trong transaction của Vet.
        verify(eventPublisher).publishEvent(new InitialExamCompletedEvent(admissionId));
    }

    @Test
    @DisplayName("Ca ROUTINE trên ngựa đã APPROVED: gửi kèm admissionId cũ không bị lỗi 409 ADMISSION_MISMATCH")
    void completeCareSchedule_routineWithApprovedAdmission_success() {
        Long scheduleId = 101L;
        Long vetId = 15L;
        Long horseId = 20L;
        Long admissionId = 1L;

        CareSchedule schedule = new CareSchedule();
        schedule.setId(scheduleId);
        schedule.setHorseId(horseId);
        schedule.setAdmissionId(admissionId);
        schedule.setCareType(CareType.ROUTINE);
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        schedule.setVeterinarianId(vetId);

        Horse horse = new Horse();
        horse.setId(horseId);
        horse.setCurrentStatus(HorseStatus.ELIGIBLE);

        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(admissionId);
        admission.setHorseId(horseId);
        admission.setStatus(AdmissionStatus.APPROVED);

        when(careScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(careScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule));
        when(admissionRepository.findById(admissionId)).thenReturn(Optional.of(admission));
        when(admissionRepository.findByIdForUpdate(admissionId)).thenReturn(Optional.of(admission));
        when(horseRepository.findByIdForUpdate(horseId)).thenReturn(Optional.of(horse));
        when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));
        when(careScheduleRepository.saveAndFlush(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));

        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Routine check clear");
        req.setDiagnosis("Good condition");
        req.setTrainingDecision(TrainingDecision.ALLOWED);
        req.setMetrics(List.of(fullMetric()));

        CreateNextScheduleRequest next = new CreateNextScheduleRequest();
        next.setCareType(CareType.ROUTINE);
        next.setDescription("Next check");
        next.setScheduledDate(LocalDate.now().plusDays(14).toString());
        next.setAdmissionId(admissionId);
        req.setNextSchedule(next);

        CareScheduleResponse res = careScheduleService.completeCareSchedule(scheduleId, req, vetId);

        assertNotNull(res);
        assertEquals(CareScheduleStatus.COMPLETED, schedule.getStatus());
    }

    @Test
    @DisplayName("Vet Queue is filtered server-side by assigned veterinarian")
    void getVetQueue_filteredByAssignedVet() {
        Long vetId = 15L;
        Long otherVetId = 99L;

        CareSchedule cs = new CareSchedule();
        cs.setId(100L);
        cs.setAdmissionId(1L);
        cs.setHorseId(20L);
        cs.setVeterinarianId(vetId);
        cs.setStatus(CareScheduleStatus.SCHEDULED);
        cs.setCareType(CareType.INITIAL);
        cs.setScheduledAt(LocalDateTime.now().plusDays(1));

        var pageRequest = PageRequest.of(0, 10);
        when(careScheduleRepository.findVetQueuePage(
                eq(vetId), eq(false), eq(""), eq(""), isNull(), isNull(), isNull(), eq(""), eq(pageRequest)))
                .thenReturn(new PageImpl<>(List.of(cs), pageRequest, 1));

        AdmissionApplication adm = new AdmissionApplication();
        adm.setId(1L);
        adm.setOwnerId(5L);
        adm.setHorseId(20L);
        adm.setTrainerId(8L);
        adm.setStatus(AdmissionStatus.VET_REVIEW);
        adm.setSubmittedAt(LocalDateTime.now().minusDays(1));
        when(admissionRepository.findAllById(any())).thenReturn(List.of(adm));

        CandidateHorseProfile cand = new CandidateHorseProfile();
        cand.setAdmissionId(1L);
        cand.setName("Desert Star");
        cand.setBreed("Arabian");
        cand.setDateOfBirth(LocalDate.of(2021, 5, 10));
        when(candidateProfileRepository.findByAdmissionIdIn(any())).thenReturn(List.of(cand));

        User owner = new User();
        owner.setId(5L);
        owner.setFullName("John Owner");

        User trainer = new User();
        trainer.setId(8L);
        trainer.setFullName("Trainer Mike");
        when(userRepository.findAllById(any())).thenReturn(List.of(owner, trainer));

        Page<VetAdmissionQueueItemResponse> queue = admissionQueryService.getVetQueue(
                vetId, null, null, null, null, null, null, pageRequest);

        assertEquals(1, queue.getTotalElements());
        VetAdmissionQueueItemResponse item = queue.getContent().get(0);
        assertEquals(1L, item.admissionId());
        assertEquals("Desert Star", item.candidateName());
        assertEquals("Trainer Mike", item.trainerName());
        assertEquals(CareScheduleStatus.SCHEDULED, item.careSchedule().status());
    }


    @Test
    @DisplayName("getVetQueueSummary counts active, awaiting, inProgress, and approved")
    void getVetQueueSummary_countsAllBuckets() {
        Long vetId = 15L;
        List<CareScheduleStatus> activeStatuses = CareScheduleStatus.OPEN;

        when(careScheduleRepository.countByVeterinarianIdAndAdmissionIdIsNotNullAndStatusIn(vetId, activeStatuses))
                .thenReturn(5L);
        when(careScheduleRepository.countActiveVetQueueItems(vetId, activeStatuses))
                .thenReturn(3L);
        when(careScheduleRepository.countActiveVetQueueItems(
                vetId, List.of(CareScheduleStatus.SCHEDULED)))
                .thenReturn(2L);
        when(careScheduleRepository.countActiveVetQueueItems(
                vetId, List.of(CareScheduleStatus.IN_PROGRESS)))
                .thenReturn(1L);
        when(careScheduleRepository.countApprovedVetQueueItems(eq(vetId), anyCollection()))
                .thenReturn(2L);
        when(careScheduleRepository.countReviewedVetQueueItems(vetId))
                .thenReturn(1L);

        var summary = admissionQueryService.getVetQueueSummary(vetId);

        assertEquals(5L, summary.total());
        assertEquals(3L, summary.active());
        assertEquals(2L, summary.awaiting());
        assertEquals(1L, summary.inProgress());
        assertEquals(2L, summary.approved());
        assertEquals(1L, summary.reviewed());
    }

    @Test
    @DisplayName("assertVetAssignedOrManager throws 403 for unassigned veterinarian")
    void assertVetAssignedOrManager_unassigned_throws403() {
        Long admissionId = 1L;
        Long unauthorizedVetId = 99L;

        when(careScheduleRepository.existsByAdmissionIdAndVeterinarianId(admissionId, unauthorizedVetId)).thenReturn(false);
        when(admissionRepository.findById(admissionId)).thenReturn(Optional.of(new AdmissionApplication()));

        AuthenticatedUser vetUser = new AuthenticatedUser(unauthorizedVetId, "vet@rtms.com", "VETERINARIAN");

        ApiException ex = assertThrows(ApiException.class, () ->
                admissionQueryService.assertVetAssignedOrManager(admissionId, vetUser));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("FORBIDDEN", ex.getErrorCode());
    }

    @Test
    @DisplayName("Vet REJECT: đơn REJECTED, hủy lịch, trả chuồng, thông báo Owner+Manager, không tạo HealthRecord")
    void reviewByVet_reject_cleansUpAndNotifies() {
        Long vetId = 15L;
        Long horseId = 20L;
        Long admissionId = 1L;
        Long scheduleId = 100L;
        Long quarantineStallId = 99L;

        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(admissionId);
        admission.setHorseId(horseId);
        admission.setOwnerId(5L);
        admission.setQuarantineStallId(quarantineStallId);
        admission.setStatus(AdmissionStatus.VET_REVIEW);

        CareSchedule schedule = new CareSchedule();
        schedule.setId(scheduleId);
        schedule.setAdmissionId(admissionId);
        schedule.setHorseId(horseId);
        schedule.setCareType(CareType.INITIAL);
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        schedule.setVeterinarianId(vetId);

        Horse horse = new Horse();
        horse.setId(horseId);
        horse.setName("Desert Star");
        horse.setCurrentStatus(HorseStatus.CANDIDATE);
        horse.setCurrentStallId(quarantineStallId);

        StableStall stall = new StableStall();
        stall.setId(quarantineStallId);
        stall.setStallCode("Q-1");

        when(admissionRepository.findById(admissionId)).thenReturn(Optional.of(admission));
        when(careScheduleRepository.findFirstByAdmissionIdAndStatusOrderByCreatedAtDesc(
                admissionId, CareScheduleStatus.IN_PROGRESS)).thenReturn(Optional.of(schedule));
        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));
        when(stallRepository.findById(quarantineStallId)).thenReturn(Optional.of(stall));

        VetReviewRequest req = new VetReviewRequest();
        req.setDecision(ReviewDecision.REJECT);
        req.setRejectReason("Chronic lameness not suitable for racing");

        VetReviewResponse res = admissionReviewService.reviewByVet(admissionId, req, vetId);

        assertEquals(AdmissionStatus.REJECTED, res.status());
        assertEquals(AdmissionStatus.REJECTED, admission.getStatus());
        assertEquals("Chronic lameness not suitable for racing", admission.getVetFeedback());
        assertNull(admission.getVetTrainingDecision());
        // Lịch mở của ngựa được chuyển sang CANCELLED khi vet reject.
        verify(careScheduleRepository).cancelOpenSchedulesIfUnreferenced(
                eq(horseId), eq(CareScheduleStatus.OPEN), anyString(), any(LocalDateTime.class));
        assertEquals(HorseStatus.REJECTED, horse.getCurrentStatus());
        assertNull(horse.getCurrentStallId());
        assertEquals(StallStatus.AVAILABLE, stall.getStatus());
        verify(admissionNotificationService).notifyOwnerRejected(
                admission, "Desert Star", "Veterinary Clinical Review", "Chronic lameness not suitable for racing");
        verify(admissionNotificationService).notifyManagersOfAdmissionRejection(
                admission, "Desert Star", "Veterinary Clinical Review", "Chronic lameness not suitable for racing");
        verify(healthRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Vet REJECT thiếu lý do -> 400 VALIDATION_ERROR")
    void reviewByVet_reject_missingReason_throws400() {
        Long vetId = 15L;
        Long admissionId = 1L;
        Long scheduleId = 100L;

        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(admissionId);
        admission.setHorseId(20L);
        admission.setStatus(AdmissionStatus.VET_REVIEW);

        CareSchedule schedule = new CareSchedule();
        schedule.setId(scheduleId);
        schedule.setAdmissionId(admissionId);
        schedule.setHorseId(20L);
        schedule.setCareType(CareType.INITIAL);
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        schedule.setVeterinarianId(vetId);

        when(admissionRepository.findById(admissionId)).thenReturn(Optional.of(admission));
        when(careScheduleRepository.findFirstByAdmissionIdAndStatusOrderByCreatedAtDesc(
                admissionId, CareScheduleStatus.IN_PROGRESS)).thenReturn(Optional.of(schedule));

        VetReviewRequest req = new VetReviewRequest();
        req.setDecision(ReviewDecision.REJECT);

        ApiException ex = assertThrows(ApiException.class,
                () -> admissionReviewService.reviewByVet(admissionId, req, vetId));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("VALIDATION_ERROR", ex.getErrorCode());
        verify(careScheduleRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Vet REJECT bởi Thú y không được giao -> 403 FORBIDDEN")
    void reviewByVet_reject_otherVet_throws403() {
        Long assignedVetId = 15L;
        Long otherVetId = 99L;
        Long admissionId = 1L;
        Long scheduleId = 100L;

        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(admissionId);
        admission.setHorseId(20L);
        admission.setStatus(AdmissionStatus.VET_REVIEW);

        CareSchedule schedule = new CareSchedule();
        schedule.setId(scheduleId);
        schedule.setAdmissionId(admissionId);
        schedule.setHorseId(20L);
        schedule.setCareType(CareType.INITIAL);
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        schedule.setVeterinarianId(assignedVetId);

        when(admissionRepository.findById(admissionId)).thenReturn(Optional.of(admission));
        when(careScheduleRepository.findFirstByAdmissionIdAndStatusOrderByCreatedAtDesc(
                admissionId, CareScheduleStatus.IN_PROGRESS)).thenReturn(Optional.of(schedule));

        VetReviewRequest req = new VetReviewRequest();
        req.setDecision(ReviewDecision.REJECT);
        req.setRejectReason("Reason");

        ApiException ex = assertThrows(ApiException.class,
                () -> admissionReviewService.reviewByVet(admissionId, req, otherVetId));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("FORBIDDEN", ex.getErrorCode());
    }

    @Test
    @DisplayName("INITIAL thiếu chỉ số sinh hiệu thứ 6 -> 400 VALIDATION_ERROR")
    void completeCareSchedule_initialMissingVitals_throws400() {
        Long scheduleId = 100L;
        Long vetId = 15L;
        Long horseId = 20L;
        Long admissionId = 1L;

        CareSchedule schedule = new CareSchedule();
        schedule.setId(scheduleId);
        schedule.setHorseId(horseId);
        schedule.setAdmissionId(admissionId);
        schedule.setCareType(CareType.INITIAL);
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        schedule.setVeterinarianId(vetId);

        Horse horse = new Horse();
        horse.setId(horseId);
        horse.setCurrentStatus(HorseStatus.CANDIDATE);

        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(admissionId);
        admission.setHorseId(horseId);
        admission.setStatus(AdmissionStatus.VET_REVIEW);

        when(careScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(careScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule));
        when(admissionRepository.findByIdForUpdate(admissionId)).thenReturn(Optional.of(admission));
        when(horseRepository.findByIdForUpdate(horseId)).thenReturn(Optional.of(horse));

        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Findings");
        req.setDiagnosis("Diagnosis");
        req.setTrainingDecision(TrainingDecision.ALLOWED);
        HorseHealthMetricRequest m = fullMetric();
        m.setBodyConditionScore(null); // missing 6th vital
        req.setMetrics(List.of(m));

        ApiException ex = assertThrows(ApiException.class,
                () -> careScheduleService.completeCareSchedule(scheduleId, req, vetId));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("VALIDATION_ERROR", ex.getErrorCode());
        verify(healthRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("INITIAL chỉ số ngoài range -> 400 VALIDATION_ERROR")
    void completeCareSchedule_initialBadVitalRange_throws400() {
        Long scheduleId = 100L;
        Long vetId = 15L;
        Long horseId = 20L;
        Long admissionId = 1L;

        CareSchedule schedule = new CareSchedule();
        schedule.setId(scheduleId);
        schedule.setHorseId(horseId);
        schedule.setAdmissionId(admissionId);
        schedule.setCareType(CareType.INITIAL);
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        schedule.setVeterinarianId(vetId);

        Horse horse = new Horse();
        horse.setId(horseId);
        horse.setCurrentStatus(HorseStatus.CANDIDATE);

        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(admissionId);
        admission.setHorseId(horseId);
        admission.setStatus(AdmissionStatus.VET_REVIEW);

        when(careScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(careScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule));
        when(admissionRepository.findByIdForUpdate(admissionId)).thenReturn(Optional.of(admission));
        when(horseRepository.findByIdForUpdate(horseId)).thenReturn(Optional.of(horse));

        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Findings");
        req.setDiagnosis("Diagnosis");
        req.setTrainingDecision(TrainingDecision.ALLOWED);
        HorseHealthMetricRequest m = fullMetric();
        m.setTemperature(BigDecimal.valueOf(50)); // out of range
        req.setMetrics(List.of(m));

        ApiException ex = assertThrows(ApiException.class,
                () -> careScheduleService.completeCareSchedule(scheduleId, req, vetId));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("VALIDATION_ERROR", ex.getErrorCode());
    }

    @Test
    @DisplayName("URGENT chỉ gửi 2 chỉ số -> vẫn hoàn tất thành công")
    void completeCareSchedule_urgentWithPartialVitals_succeeds() {
        Long scheduleId = 100L;
        Long vetId = 20L;
        Long horseId = 10L;

        CareSchedule schedule = new CareSchedule();
        schedule.setId(scheduleId);
        schedule.setHorseId(horseId);
        schedule.setVeterinarianId(vetId);
        schedule.setCareType(CareType.URGENT);
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);

        Horse horse = new Horse();
        horse.setId(horseId);
        horse.setName("Thunder");
        horse.setCurrentStatus(HorseStatus.ELIGIBLE);
        horse.setTrainingDecision(TrainingDecision.BLOCKED);

        when(careScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(careScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule));
        when(horseRepository.findByIdForUpdate(horseId)).thenReturn(Optional.of(horse));
        when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> {
            HealthRecord hr = i.getArgument(0);
            hr.setId(999L);
            return hr;
        });
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));
        when(careScheduleRepository.saveAndFlush(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));

        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Examined");
        req.setDiagnosis("Diagnosis");
        req.setTrainingDecision(TrainingDecision.ALLOWED);
        CreateNextScheduleRequest next = new CreateNextScheduleRequest();
        next.setCareType(CareType.ROUTINE);
        next.setScheduledDate(LocalDate.now().plusDays(3).toString());
        next.setDescription("Recheck urgent case");
        req.setNextSchedule(next);
        HorseHealthMetricRequest m = new HorseHealthMetricRequest();
        m.setHeartRate(BigDecimal.valueOf(52));
        m.setTemperature(BigDecimal.valueOf(38.5));
        req.setMetrics(List.of(m));

        CareScheduleResponse res = careScheduleService.completeCareSchedule(scheduleId, req, vetId);

        assertEquals(CareScheduleStatus.COMPLETED, res.status());
        verify(healthRecordRepository).save(any(HealthRecord.class));
    }

    @Test
    @DisplayName("INITIAL + BLOCKED thiếu injury marker -> 400 VALIDATION_ERROR")
    void completeCareSchedule_initialBlockedWithoutMarkers_throws400() {
        Long scheduleId = 100L;
        Long vetId = 15L;
        Long horseId = 20L;
        Long admissionId = 1L;

        CareSchedule schedule = new CareSchedule();
        schedule.setId(scheduleId);
        schedule.setHorseId(horseId);
        schedule.setAdmissionId(admissionId);
        schedule.setCareType(CareType.INITIAL);
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        schedule.setVeterinarianId(vetId);

        Horse horse = new Horse();
        horse.setId(horseId);
        horse.setCurrentStatus(HorseStatus.CANDIDATE);

        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(admissionId);
        admission.setHorseId(horseId);
        admission.setStatus(AdmissionStatus.VET_REVIEW);

        when(careScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(careScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule));
        when(admissionRepository.findByIdForUpdate(admissionId)).thenReturn(Optional.of(admission));
        when(horseRepository.findByIdForUpdate(horseId)).thenReturn(Optional.of(horse));

        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Findings");
        req.setDiagnosis("Diagnosis");
        req.setTrainingDecision(TrainingDecision.BLOCKED);
        req.setRestrictionDetails("Box rest");
        req.setMetrics(List.of(fullMetric()));
        CreateNextScheduleRequest next = new CreateNextScheduleRequest();
        next.setCareType(CareType.ROUTINE);
        next.setDescription("Recheck");
        next.setScheduledDate(LocalDate.now().plusDays(3).toString());
        req.setNextSchedule(next);

        ApiException ex = assertThrows(ApiException.class,
                () -> careScheduleService.completeCareSchedule(scheduleId, req, vetId));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("VALIDATION_ERROR", ex.getErrorCode());
    }

    @Test
    @DisplayName("INITIAL + BLOCKED có injury marker -> lưu HealthCondition")
    void completeCareSchedule_initialBlockedWithMarkers_persistsInjury() {
        Long scheduleId = 100L;
        Long vetId = 15L;
        Long horseId = 20L;
        Long admissionId = 1L;

        CareSchedule schedule = new CareSchedule();
        schedule.setId(scheduleId);
        schedule.setHorseId(horseId);
        schedule.setAdmissionId(admissionId);
        schedule.setCareType(CareType.INITIAL);
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        schedule.setVeterinarianId(vetId);

        Horse horse = new Horse();
        horse.setId(horseId);
        horse.setCurrentStatus(HorseStatus.CANDIDATE);

        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(admissionId);
        admission.setHorseId(horseId);
        admission.setStatus(AdmissionStatus.VET_REVIEW);

        InjuryRecordRepository injuryRepo =
                mock(InjuryRecordRepository.class);
        when(injuryRepo.save(any(InjuryRecord.class))).thenAnswer(i -> i.getArgument(0));

        CareScheduleService serviceWithInjuryRepo = new CareScheduleService(
                careScheduleRepository,
                horseRepository,
                healthRecordRepository,
                metricRepository,
                admissionRepository,
                stallRepository,
                userRepository,
                incidentRepository,
                vetProfileRepository,
                auditLogRepository,
                eventPublisher,
                entityManager,
                notificationService,
                new TrainingDecisionService(horseRepository, careScheduleRepository, workoutService),
                mock(PreventiveVaccinationService.class),
                injuryRepo,
                mock(TreatmentPlanRepository.class),
                mock(PrescriptionRepository.class)
        );

        when(careScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(careScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule));
        when(admissionRepository.findByIdForUpdate(admissionId)).thenReturn(Optional.of(admission));
        when(admissionRepository.findById(admissionId)).thenReturn(Optional.of(admission));
        when(horseRepository.findByIdForUpdate(horseId)).thenReturn(Optional.of(horse));
        when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> {
            HealthRecord hr = i.getArgument(0);
            hr.setId(200L);
            return hr;
        });
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));
        when(careScheduleRepository.saveAndFlush(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));

        com.rtms.backend.medical.entity.HorseBodyRegion bodyRegion = new com.rtms.backend.medical.entity.HorseBodyRegion();
        bodyRegion.setId(1L);
        bodyRegion.setCode("foreleg_right");
        bodyRegion.setDisplayName("Right forelimb");
        when(entityManager.find(com.rtms.backend.medical.entity.HorseBodyRegion.class, 1L))
                .thenReturn(bodyRegion);

        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Findings");
        req.setDiagnosis("Diagnosis");
        req.setTrainingDecision(TrainingDecision.BLOCKED);
        req.setRestrictionDetails("Box rest");
        req.setMetrics(List.of(fullMetric()));
        req.setInjuryMarkers(List.of(injuryMarker()));
        CreateNextScheduleRequest next = new CreateNextScheduleRequest();
        next.setCareType(CareType.ROUTINE);
        next.setDescription("Recheck");
        next.setScheduledDate(LocalDate.now().plusDays(3).toString());
        req.setNextSchedule(next);

        serviceWithInjuryRepo.completeCareSchedule(scheduleId, req, vetId);

        verify(injuryRepo).save(argThat(ir ->
                ir.getHorseId().equals(horseId)
                        && "Right forelimb".equals(ir.getInjuryType())
                        && "MODERATE".equals(ir.getSeverity())));
    }

    private HorseHealthMetricRequest fullMetric() {
        HorseHealthMetricRequest m = new HorseHealthMetricRequest();
        m.setHeartRate(BigDecimal.valueOf(36));
        m.setTemperature(BigDecimal.valueOf(37.8));
        m.setWeight(BigDecimal.valueOf(450));
        m.setRespiratoryRate(BigDecimal.valueOf(12));
        m.setHydrationStatus(HydrationStatus.NORMAL);
        m.setBodyConditionScore(BigDecimal.valueOf(5.5));
        return m;
    }

    private InjuryMarkerRequest injuryMarker() {
        InjuryMarkerRequest marker = new InjuryMarkerRequest();
        marker.setBodyRegionId(1L);
        marker.setDescription("Superficial digital flexor tendon, right fore");
        marker.setDiagnosedAt(LocalDateTime.now());
        return marker;
    }
}
