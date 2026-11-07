package com.rtms.backend.admission.service;

import com.rtms.backend.admission.dto.ManagerReviewRequest;
import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.admission.enums.ReviewDecision;
import com.rtms.backend.stable.enums.StallStatus;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdmissionManagerReviewServiceTest {

    @Mock
    private AdmissionApplicationRepository admissionApplicationRepository;

    @Mock
    private StableStallRepository stableStallRepository;

    @Mock
    private HorseRepository horseRepository;

    @Mock
    private CareScheduleRepository careScheduleRepository;

    @Mock
    private AdmissionGroomReviewService admissionGroomReviewService;

    @Mock
    private com.rtms.backend.medical.service.PreventiveStatusService preventiveStatusService;

    @Mock
    private com.rtms.backend.identity.repository.GroomProfileRepository groomProfileRepository;

    @Mock
    private AdmissionNotificationService admissionNotificationService;

    private AdmissionManagerReviewService service;

    // â”€â”€â”€ Shared helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private AdmissionApplication buildAdmission(AdmissionStatus status) {
        AdmissionApplication a = new AdmissionApplication();
        a.setId(1L);
        a.setStatus(status);
        a.setHorseId(10L);
        a.setQuarantineStallId(99L);
        return a;
    }

    private Horse buildHorse(HorseStatus status) {
        Horse h = new Horse();
        h.setId(10L);
        h.setCurrentStatus(status);
        h.setCurrentStallId(99L);
        return h;
    }

    private StableStall buildStall(Long id, StallStatus status) {
        StableStall s = new StableStall();
        s.setId(id);
        s.setStatus(status);
        return s;
    }

    private ManagerReviewRequest approveRequest(Long stallId) {
        ManagerReviewRequest r = new ManagerReviewRequest();
        r.setDecision(ReviewDecision.APPROVED);
        r.setStallId(stallId);
        return r;
    }

    private ManagerReviewRequest rejectRequest(String feedback) {
        ManagerReviewRequest r = new ManagerReviewRequest();
        r.setDecision(ReviewDecision.REJECTED);
        r.setFeedback(feedback);
        return r;
    }

    @BeforeEach
    void setUp() {
        service = new AdmissionManagerReviewService(
                admissionApplicationRepository,
                stableStallRepository,
                horseRepository,
                careScheduleRepository,
                admissionGroomReviewService,
                preventiveStatusService,
                groomProfileRepository,
                admissionNotificationService);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // APPROVE â€” manual stall selection
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Test
    @DisplayName("APPROVE thá»§ cÃ´ng: Admission=APPROVED, Horse=ELIGIBLE, chuá»“ng thÆ°á»ng=OCCUPIED, chuá»“ng Q=AVAILABLE")
    void approve_withSelectedStall_success() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.CANDIDATE);
        StableStall regularStall = buildStall(20L, StallStatus.AVAILABLE);
        StableStall qStall = buildStall(99L, StallStatus.OCCUPIED);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findAvailableRegularStallByIdForUpdate(20L)).thenReturn(Optional.of(regularStall));
        when(stableStallRepository.findById(99L)).thenReturn(Optional.of(qStall));
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 5L, approveRequest(20L));

        assertEquals(AdmissionStatus.APPROVED, result.getStatus());
        assertEquals(ReviewDecision.APPROVED, result.getManagerDecision());
        assertEquals(5L, result.getManagerId());
        assertNotNull(result.getManagerReviewedAt());

        assertEquals(HorseStatus.ELIGIBLE, horse.getCurrentStatus());
        assertEquals(20L, horse.getCurrentStallId());
        // Duyá»‡t xong: ELIGIBLE + ALLOWED -> Trainer láº­p káº¿ hoáº¡ch Ä‘Æ°á»£c ngay.
        assertEquals(TrainingDecision.ALLOWED, horse.getTrainingDecision());
        assertTrue(horse.canTrain());
        verify(horseRepository).save(horse);

        assertEquals(StallStatus.OCCUPIED, regularStall.getStatus());
        verify(stableStallRepository).save(regularStall);

        assertEquals(StallStatus.AVAILABLE, qStall.getStatus());
        verify(stableStallRepository).save(qStall);

        // PhÃ¡c Ä‘á»“ báº¯t buá»™c cÃ²n MISSING sau khÃ¡m nháº­p tráº¡i Ä‘Æ°á»£c háº¹n tiÃªm ngÃ y hÃ´m sau.
        verify(preventiveStatusService).upsertMilestone(10L, "preventive:approval:1", 5L,
                java.time.LocalDate.now().plusDays(1));
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // APPROVE â€” automatic stall selection (no stallId in request)
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Test
    @DisplayName("APPROVE tá»± Ä‘á»™ng: khÃ´ng truyá»n stallId, há»‡ thá»‘ng tá»± chá»n chuá»“ng REGULAR Ä‘áº§u tiÃªn")
    void approve_withAutoStall_success() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.CANDIDATE);
        StableStall regularStall = buildStall(21L, StallStatus.AVAILABLE);
        StableStall qStall = buildStall(99L, StallStatus.OCCUPIED);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findFairAvailableStallForUpdate("REGULAR")).thenReturn(Optional.of(regularStall));
        when(stableStallRepository.findById(99L)).thenReturn(Optional.of(qStall));
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 5L, approveRequest(null));

        assertEquals(AdmissionStatus.APPROVED, result.getStatus());
        assertEquals(HorseStatus.ELIGIBLE, horse.getCurrentStatus());
        assertEquals(21L, horse.getCurrentStallId());
        assertEquals(StallStatus.OCCUPIED, regularStall.getStatus());
        assertEquals(StallStatus.AVAILABLE, qStall.getStatus());
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // REJECT â€” full state verification
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Test
    @DisplayName("REJECT: Admission=REJECTED, Horse=REJECTED, currentStallId=null, chuá»“ng Q=AVAILABLE")
    void reject_success_admissionAndHorseRejected() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.CANDIDATE);
        StableStall qStall = buildStall(99L, StallStatus.OCCUPIED);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findById(99L)).thenReturn(Optional.of(qStall));
        when(careScheduleRepository.findByHorseIdAndStatusIn(eq(10L), any()))
                .thenReturn(List.of());
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 5L, rejectRequest("Not suitable"));

        assertEquals(AdmissionStatus.REJECTED, result.getStatus());
        assertEquals(ReviewDecision.REJECTED, result.getManagerDecision());
        assertEquals("Not suitable", result.getManagerFeedback());
        assertEquals(5L, result.getManagerId());
        assertNotNull(result.getManagerReviewedAt());

        assertEquals(HorseStatus.REJECTED, horse.getCurrentStatus());
        assertNull(horse.getCurrentStallId());
        verify(horseRepository).save(horse);

        assertEquals(StallStatus.AVAILABLE, qStall.getStatus());
        verify(stableStallRepository).save(qStall);
    }

    @Test
    @DisplayName("REJECT: chá»‰ lá»‹ch SCHEDULED bá»‹ CANCELLED")
    void reject_cancelsActiveSchedules_notCompleted() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.CANDIDATE);
        StableStall qStall = buildStall(99L, StallStatus.OCCUPIED);

        CareSchedule pending = new CareSchedule();
        pending.setId(1L);
        pending.setHorseId(10L);
        pending.setStatus(CareScheduleStatus.SCHEDULED);

        CareSchedule overdue = new CareSchedule();
        overdue.setId(2L);
        overdue.setHorseId(10L);
        overdue.setStatus(CareScheduleStatus.SCHEDULED);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findById(99L)).thenReturn(Optional.of(qStall));
        when(careScheduleRepository.findByHorseIdAndStatusIn(eq(10L), any()))
                .thenReturn(List.of());
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.review(1L, 5L, rejectRequest("Rejected"));

        // AD-025 rule 6: lá»‹ch chÆ°a diá»…n ra (SCHEDULED) cá»§a ngá»±a rá»›t tuyá»ƒn bá»‹ xÃ³a cá»©ng.
        verify(careScheduleRepository).deleteOpenSchedulesIfUnreferenced(
                10L, List.of(CareScheduleStatus.SCHEDULED));
        verify(careScheduleRepository, never()).saveAll(any());

        // COMPLETED schedules are never queried in this call â€” no interference
        verify(careScheduleRepository, never()).findByHorseIdOrderByScheduledAtAsc(any());
    }

    @Test
    @DisplayName("REJECT: khong co lich can xoa — deleteOpenSchedulesIfUnreferenced van duoc goi")
    void reject_noSchedulesToCancel_saveAllCalledWithEmptyList() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.CANDIDATE);
        StableStall qStall = buildStall(99L, StallStatus.OCCUPIED);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findById(99L)).thenReturn(Optional.of(qStall));
        when(careScheduleRepository.findByHorseIdAndStatusIn(eq(10L), any()))
                .thenReturn(List.of());
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.review(1L, 5L, rejectRequest("Rejected"));

        verify(careScheduleRepository).deleteOpenSchedulesIfUnreferenced(
                10L, List.of(CareScheduleStatus.SCHEDULED));
    }

    @Test
    @DisplayName("REJECT: chan tu choi khi ca kham dang REQUESTED, IN_PROGRESS, OVERDUE hoac PAUSED")
    void reject_doesNotCancelInvalidTransitions() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.CANDIDATE);
        StableStall qStall = buildStall(99L, StallStatus.OCCUPIED);

        CareSchedule requested = new CareSchedule();
        requested.setId(100L);
        requested.setHorseId(10L);
        requested.setStatus(CareScheduleStatus.SCHEDULED);

        CareSchedule inProgress = new CareSchedule();
        inProgress.setId(101L);
        inProgress.setHorseId(10L);
        inProgress.setStatus(CareScheduleStatus.IN_PROGRESS);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findById(99L)).thenReturn(Optional.of(qStall));
        when(careScheduleRepository.findByHorseIdAndStatusIn(eq(10L), any()))
                .thenReturn(List.of(requested, inProgress));

        assertThrows(IllegalStateException.class,
                () -> service.review(1L, 5L, rejectRequest("Rejected")));

        verify(careScheduleRepository, never()).deleteOpenSchedulesIfUnreferenced(any(), any());
    }
    // REJECT â€” validation failures
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Test
    @DisplayName("REJECT thiáº¿u feedback: nÃ©m IllegalArgumentException, khÃ´ng lÆ°u gÃ¬")
    void reject_missingFeedback_throwsAndNoSave() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.CANDIDATE);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));

        ManagerReviewRequest req = rejectRequest(null);

        assertThrows(IllegalArgumentException.class,
                () -> service.review(1L, 5L, req));

        verify(admissionApplicationRepository, never()).save(any());
        verify(horseRepository, never()).save(any());
        verify(stableStallRepository, never()).save(any());
    }

    @Test
    @DisplayName("REJECT feedback rá»—ng (blank): nÃ©m IllegalArgumentException")
    void reject_blankFeedback_throws() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.CANDIDATE);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));

        assertThrows(IllegalArgumentException.class,
                () -> service.review(1L, 5L, rejectRequest("   ")));
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // Admission state validation
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Test
    @DisplayName("Admission khÃ´ng á»Ÿ tráº¡ng thÃ¡i MANAGER_REVIEW: nÃ©m IllegalStateException")
    void review_admissionNotManagerReview_throws() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.VET_REVIEW);
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));

        assertThrows(IllegalStateException.class,
                () -> service.review(1L, 5L, approveRequest(20L)));

        verify(horseRepository, never()).findById(any());
        verify(admissionApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("APPROVE sau khi Admission Ä‘Ã£ APPROVED: nÃ©m IllegalStateException (khÃ´ng cho review láº§n 2)")
    void review_alreadyApproved_throws() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.APPROVED);
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));

        assertThrows(IllegalStateException.class,
                () -> service.review(1L, 5L, approveRequest(20L)));
    }

    @Test
    @DisplayName("REJECT sau khi Admission Ä‘Ã£ REJECTED: nÃ©m IllegalStateException (khÃ´ng cho review láº§n 2)")
    void review_alreadyRejected_throws() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.REJECTED);
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));

        assertThrows(IllegalStateException.class,
                () -> service.review(1L, 5L, rejectRequest("Again")));
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // Horse validation
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Test
    @DisplayName("horseId lÃ  null trong Admission: nÃ©m IllegalStateException")
    void review_horseIdNull_throws() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        admission.setHorseId(null);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));

        assertThrows(IllegalStateException.class,
                () -> service.review(1L, 5L, approveRequest(20L)));

        verify(horseRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Horse khÃ´ng tá»“n táº¡i: nÃ©m IllegalStateException")
    void review_horseNotFound_throws() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> service.review(1L, 5L, approveRequest(20L)));

        verify(admissionApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Horse khÃ´ng á»Ÿ tráº¡ng thÃ¡i CANDIDATE: nÃ©m IllegalStateException")
    void review_horseNotCandidate_throws() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.ELIGIBLE); // Already past CANDIDATE

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));

        assertThrows(IllegalStateException.class,
                () -> service.review(1L, 5L, approveRequest(20L)));

        verify(admissionApplicationRepository, never()).save(any());
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // Stall validation
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Test
    @DisplayName("Chuá»“ng REGULAR Ä‘Æ°á»£c chá»n thá»§ cÃ´ng khÃ´ng cÃ²n AVAILABLE hoáº·c chÆ°a cÃ³ Groom: 409, khÃ´ng lÆ°u gÃ¬")
    void approve_selectedStallUnavailable_throwsAndNoSave() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.CANDIDATE);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findAvailableRegularStallByIdForUpdate(20L)).thenReturn(Optional.empty());

        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.review(1L, 5L, approveRequest(20L)));
        assertEquals(409, error.getStatusCode().value());

        verify(admissionApplicationRepository, never()).save(any());
        verify(horseRepository, never()).save(any());
    }

    @Test
    @DisplayName("KhÃ´ng cÃ³ chuá»“ng REGULAR cÃ³ Groom nÃ o AVAILABLE cho tá»± Ä‘á»™ng chá»n: 409")
    void approve_noAvailableRegularStall_throws() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.CANDIDATE);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findFairAvailableStallForUpdate("REGULAR")).thenReturn(Optional.empty());

        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.review(1L, 5L, approveRequest(null)));
        assertEquals(409, error.getStatusCode().value());

        verify(admissionApplicationRepository, never()).save(any());
        verify(horseRepository, never()).save(any());
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // Decision validation
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Test
    @DisplayName("decision lÃ  null: nÃ©m IllegalArgumentException trÆ°á»›c khi cháº¡m tá»›i chuá»“ng")
    void review_nullDecision_throws() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.CANDIDATE);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));

        ManagerReviewRequest req = new ManagerReviewRequest();
        // decision left null

        assertThrows(IllegalArgumentException.class,
                () -> service.review(1L, 5L, req));

        verify(admissionApplicationRepository, never()).save(any());
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // Q stall absent (null quarantineStallId)
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Test
    @DisplayName("APPROVE khi Admission khÃ´ng cÃ³ Q stall: khÃ´ng cá»‘ gáº¯ng giáº£i phÃ³ng Q stall")
    void approve_noQuarantineStall_skipsQStallRelease() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        admission.setQuarantineStallId(null); // No Q stall assigned
        Horse horse = buildHorse(HorseStatus.CANDIDATE);
        horse.setCurrentStallId(null);
        StableStall regularStall = buildStall(20L, StallStatus.AVAILABLE);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findAvailableRegularStallByIdForUpdate(20L)).thenReturn(Optional.of(regularStall));
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 5L, approveRequest(20L));

        assertEquals(AdmissionStatus.APPROVED, result.getStatus());
        // findById for Q stall must never be called
        verify(stableStallRepository, never()).findById(any());
    }

    @Test
    @DisplayName("APPROVE giá»¯ nguyÃªn quyáº¿t Ä‘á»‹nh cháº·n táº­p (BLOCKED) cá»§a ThÃº y")
    void approve_withVetBlockedDecision_preservesBlock() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.CANDIDATE);
        horse.setTrainingDecision(TrainingDecision.BLOCKED);
        horse.setTrainingDecisionReason("Left forelimb tendon strain - rest prescribed by vet");

        StableStall regularStall = buildStall(20L, StallStatus.AVAILABLE);
        StableStall qStall = buildStall(99L, StallStatus.OCCUPIED);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findAvailableRegularStallByIdForUpdate(20L)).thenReturn(Optional.of(regularStall));
        when(stableStallRepository.findById(99L)).thenReturn(Optional.of(qStall));
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 5L, approveRequest(20L));

        assertEquals(AdmissionStatus.APPROVED, result.getStatus());
        assertEquals(HorseStatus.ELIGIBLE, horse.getCurrentStatus());
        assertEquals(20L, horse.getCurrentStallId());

        // Quyáº¿t Ä‘á»‹nh cháº·n táº­p cá»§a ThÃº y pháº£i giá»¯ nguyÃªn sau khi duyá»‡t.
        assertEquals(TrainingDecision.BLOCKED, horse.getTrainingDecision());
        assertEquals("Left forelimb tendon strain - rest prescribed by vet", horse.getTrainingDecisionReason());
        assertFalse(horse.canTrain());
        verify(horseRepository).save(horse);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // WAITING_FOR_STALL processing
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Test
    @DisplayName("Approve: releases Q stall, stamps the regular groom, notifies them and lets the stall queue move")
    void managerApprove_releasesQStall_andRunsAllocation() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.CANDIDATE);
        StableStall regularStall = buildStall(20L, StallStatus.AVAILABLE);
        regularStall.setGroomId(33L);
        StableStall quarantineStall = buildStall(99L, StallStatus.OCCUPIED);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findFairAvailableStallForUpdate("REGULAR")).thenReturn(Optional.of(regularStall));
        when(stableStallRepository.findById(99L)).thenReturn(Optional.of(quarantineStall));
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.review(1L, 5L, approveRequest(null));

        assertEquals(StallStatus.AVAILABLE, quarantineStall.getStatus());
        var order = inOrder(stableStallRepository);
        order.verify(stableStallRepository).lockAdmissionCapacityStallsForUpdate();
        order.verify(stableStallRepository).findFairAvailableStallForUpdate("REGULAR");
        verify(groomProfileRepository).touchLastHorseAssignedAt(org.mockito.ArgumentMatchers.eq(33L), any());
        verify(admissionGroomReviewService).notifyGroomAssigned(org.mockito.ArgumentMatchers.same(admission),
                org.mockito.ArgumentMatchers.same(regularStall), org.mockito.ArgumentMatchers.anyString());
        verify(admissionGroomReviewService).allocateWaitingAdmissions();
    }

    @Test
    @DisplayName("Reject: releases Q stall and lets the stall queue move")
    void managerReject_releasesQStall_andRunsAllocation() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.CANDIDATE);
        StableStall quarantineStall = buildStall(99L, StallStatus.OCCUPIED);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findById(99L)).thenReturn(Optional.of(quarantineStall));
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.review(1L, 5L, rejectRequest("  Not a good fit  "));

        assertEquals(StallStatus.AVAILABLE, quarantineStall.getStatus());
        assertEquals("Not a good fit", admission.getManagerFeedback());
        verify(admissionGroomReviewService).allocateWaitingAdmissions();
    }
}
