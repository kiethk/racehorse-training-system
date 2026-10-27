package com.rtms.backend.admission.service;
import com.rtms.backend.medical.service.CareScheduleService;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.notification.service.NotificationTypes;


import com.rtms.backend.admission.dto.GroomAdmissionReviewRequest;
import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.entity.CandidateHorseProfile;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.entity.HorsePedigree;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.enums.HorseSex;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.admission.enums.ReviewDecision;
import com.rtms.backend.stable.enums.StallStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.admission.repository.CandidateHorseProfileRepository;
import com.rtms.backend.horse.repository.HorsePedigreeRepository;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.horse.service.PedigreeBirthDateValidator;
import com.rtms.backend.identity.repository.GroomProfileRepository;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdmissionGroomReviewServiceTest {

    @Mock
    private AdmissionApplicationRepository admissionApplicationRepository;

    @Mock
    private CandidateHorseProfileRepository candidateHorseProfileRepository;

    @Mock
    private StableStallRepository stableStallRepository;

    @Mock
    private HorseRepository horseRepository;

    @Mock
    private HorsePedigreeRepository horsePedigreeRepository;

    @Mock
    private CareScheduleRepository careScheduleRepository;

    @Mock
    private CareScheduleService careScheduleService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private GroomProfileRepository groomProfileRepository;

    @Mock
    private AdmissionGroomPoolService groomPool;

    @Mock
    private AdmissionGroomPoolNotifier poolNotifier;

    private AdmissionGroomReviewService service;

    @BeforeEach
    void setUp() {
        service = new AdmissionGroomReviewService(
                admissionApplicationRepository,
                candidateHorseProfileRepository,
                stableStallRepository,
                horseRepository,
                horsePedigreeRepository,
                careScheduleRepository,
                careScheduleService,
                notificationService,
                new PedigreeBirthDateValidator(horseRepository),
                groomProfileRepository,
                groomPool,
                poolNotifier);
        lenient().when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ── Pool helpers ───────────────────────────────────────────────────────

    private static final LocalDateTime CUTOFF = LocalDateTime.now().minusHours(24);

    private void inPool(Long groomId) {
        when(groomPool.viewFor(groomId)).thenReturn(new AdmissionGroomPoolService.PoolView(true, true, CUTOFF));
    }

    private void outOfPool(Long groomId) {
        when(groomPool.viewFor(groomId)).thenReturn(new AdmissionGroomPoolService.PoolView(false, false, CUTOFF));
    }

    /** Pool head for the approving groom, plus capacity counts that pass the reserve policy. */
    private void oldestAndCapacity(Long oldestId) {
        when(admissionApplicationRepository.findPoolIdsInApprovalOrder(anyBoolean(), anyBoolean(), any(),
                eq(AdmissionStatus.GROOM_REVIEW), any())).thenReturn(List.of(oldestId));
        lenient().when(stableStallRepository.countAvailableQuarantineStalls()).thenReturn(2L);
        lenient().when(stableStallRepository.countAvailableRegularStalls()).thenReturn(5L);
        lenient().when(stableStallRepository.countOccupiedQuarantineStalls()).thenReturn(1L);
    }

    private static void assertApiError(ApiException error, int status, String code) {
        assertEquals(status, error.getStatus().value());
        assertEquals(code, error.getErrorCode());
    }

    // ── Reject ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Groom reject: Admission REJECTED và không tạo Horse")
    void review_reject_success() {
        AdmissionApplication admission = admission();
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        inPool(7L);
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 7L, request(ReviewDecision.REJECTED, "Missing documents"));

        assertEquals(AdmissionStatus.REJECTED, result.getStatus());
        assertEquals(7L, result.getGroomId());
        assertEquals(ReviewDecision.REJECTED, result.getGroomDecision());
        assertEquals("Missing documents", result.getGroomFeedback());
        assertNotNull(result.getGroomReviewedAt());
        assertNull(result.getHorseId());
        verify(stableStallRepository, never()).lockAdmissionCapacityStallsForUpdate();
        verifyNoInteractions(candidateHorseProfileRepository, horseRepository, horsePedigreeRepository,
                careScheduleRepository, notificationService);
    }

    @Test
    @DisplayName("D8.6: a groom may reject an admission that is not the oldest in their pool")
    void review_rejectOutOfOrder_isAllowed() {
        AdmissionApplication admission = admission();
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        inPool(7L);
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.review(1L, 7L, request(ReviewDecision.REJECTED, "Incomplete pedigree"));

        assertEquals(AdmissionStatus.REJECTED, admission.getStatus());
        verify(admissionApplicationRepository, never()).findPoolIdsInApprovalOrder(
                anyBoolean(), anyBoolean(), any(), any(), any());
    }

    @Test
    @DisplayName("BR-02: reject requires non-blank feedback even when the client skips its own check")
    void review_rejectWithBlankFeedback_isRejected() {
        AdmissionApplication admission = admission();
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        inPool(7L);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.review(1L, 7L, request(ReviewDecision.REJECTED, "  \t ")));

        assertEquals(400, exception.getStatusCode().value());
        verify(admissionApplicationRepository, never()).save(any());
        assertEquals(AdmissionStatus.GROOM_REVIEW, admission.getStatus());
    }

    @Test
    @DisplayName("BR-02: feedback longer than 2000 characters is rejected")
    void review_tooLongFeedback_isRejected() {
        AdmissionApplication admission = admission();
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        inPool(7L);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.review(1L, 7L, request(ReviewDecision.REJECTED, "x".repeat(2001))));

        assertEquals(400, exception.getStatusCode().value());
        verify(admissionApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("D11: a groom outside the pool can neither approve nor reject")
    void review_outsidePool_isForbidden() {
        AdmissionApplication admission = admission();
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        outOfPool(8L);

        ApiException approve = assertThrows(ApiException.class,
                () -> service.review(1L, 8L, request(ReviewDecision.APPROVED, null)));
        ApiException reject = assertThrows(ApiException.class,
                () -> service.review(1L, 8L, request(ReviewDecision.REJECTED, "No")));

        assertApiError(approve, 403, "NOT_IN_GROOM_POOL");
        assertApiError(reject, 403, "NOT_IN_GROOM_POOL");
        verify(admissionApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Groom review trims feedback before persisting")
    void review_trimsFeedback() {
        AdmissionApplication admission = admission();
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        inPool(7L);
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 7L,
                request(ReviewDecision.REJECTED, "  Missing vaccination record  "));

        assertEquals("Missing vaccination record", result.getGroomFeedback());
    }

    // ── Approve = claim ────────────────────────────────────────────────────

    @Test
    @DisplayName("D1: approving reserves the reviewer's own first free quarantine stall")
    void review_approve_reservesReviewersOwnStall() {
        AdmissionApplication admission = admission();
        StableStall own = stall(42L);
        own.setGroomId(7L);
        own.setStallCode("QA-01");
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        inPool(7L);
        oldestAndCapacity(1L);
        when(stableStallRepository.findFirstAvailableQuarantineStallOfGroomForUpdate(7L)).thenReturn(Optional.of(own));
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 7L, request(ReviewDecision.APPROVED, "  Ready  "));

        assertEquals(AdmissionStatus.WAITING_FOR_ARRIVAL, result.getStatus());
        assertEquals(42L, result.getQuarantineStallId());
        assertEquals(StallStatus.RESERVED, own.getStatus());
        assertNotNull(result.getArrivalDeadlineAt());
        assertEquals(7L, result.getGroomId());
        assertEquals(ReviewDecision.APPROVED, result.getGroomDecision());
        assertEquals("Ready", result.getGroomFeedback());
        assertNull(result.getHorseId());
        verify(groomProfileRepository).touchLastHorseAssignedAt(eq(7L), any());
        verify(notificationService).createNotification(eq(7L), anyString(), anyString(),
                eq(NotificationTypes.ADMISSION_GROOM_ASSIGNED), eq(NotificationTypes.REFERENCE_ADMISSION), eq(1L),
                eq("7:ADMISSION_1:ADMISSION_GROOM_ASSIGNED:stall:42"));
        verify(stableStallRepository, never()).findFairAvailableStallForUpdate(anyString());
        verifyNoInteractions(horseRepository, horsePedigreeRepository, candidateHorseProfileRepository,
                careScheduleRepository);
    }

    @Test
    @DisplayName("The stall lock is taken before the pool is read")
    void review_approve_locksStallsBeforeReadingPool() {
        AdmissionApplication admission = admission();
        StableStall own = stall(42L);
        own.setGroomId(7L);
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        inPool(7L);
        oldestAndCapacity(1L);
        when(stableStallRepository.findFirstAvailableQuarantineStallOfGroomForUpdate(7L)).thenReturn(Optional.of(own));
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.review(1L, 7L, request(ReviewDecision.APPROVED, null));

        var order = inOrder(admissionApplicationRepository, stableStallRepository, groomPool);
        order.verify(admissionApplicationRepository).findByIdForUpdate(1L);
        order.verify(stableStallRepository).lockAdmissionCapacityStallsForUpdate();
        order.verify(groomPool).viewFor(7L);
        order.verify(stableStallRepository).findFirstAvailableQuarantineStallOfGroomForUpdate(7L);
    }

    @Test
    @DisplayName("D8.6: only the oldest admission in the groom's pool can be approved")
    void review_approveNotOldest_isConflict() {
        AdmissionApplication admission = admission();
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        inPool(7L);
        oldestAndCapacity(99L);

        ApiException error = assertThrows(ApiException.class,
                () -> service.review(1L, 7L, request(ReviewDecision.APPROVED, null)));

        assertApiError(error, 409, "OLDER_ADMISSION_FIRST");
        assertEquals(AdmissionStatus.GROOM_REVIEW, admission.getStatus());
        assertNull(admission.getGroomId());
        verify(stableStallRepository, never()).findFirstAvailableQuarantineStallOfGroomForUpdate(any());
        verify(admissionApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("D4: approval is blocked when the regular-stall reserve is too low")
    void review_approveWithoutRegularReserve_isConflict() {
        AdmissionApplication admission = admission();
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        inPool(7L);
        oldestAndCapacity(1L);
        when(stableStallRepository.countAvailableRegularStalls()).thenReturn(1L);

        ApiException error = assertThrows(ApiException.class,
                () -> service.review(1L, 7L, request(ReviewDecision.APPROVED, null)));

        assertApiError(error, 409, "REGULAR_RESERVE_INSUFFICIENT");
        verify(admissionApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("A groom whose own stalls are all taken gets NO_QUARANTINE_STALL")
    void review_approveWithoutOwnFreeStall_isConflict() {
        AdmissionApplication admission = admission();
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        inPool(7L);
        oldestAndCapacity(1L);
        when(stableStallRepository.findFirstAvailableQuarantineStallOfGroomForUpdate(7L)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> service.review(1L, 7L, request(ReviewDecision.APPROVED, null)));

        assertApiError(error, 409, "NO_QUARANTINE_STALL");
        assertEquals(AdmissionStatus.GROOM_REVIEW, admission.getStatus());
        verify(admissionApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("D8.4: after the fallback window an eligible but busier groom claims it into their own stall")
    void review_approveAfterFallback_byEligibleGroom() {
        AdmissionApplication admission = admission();
        admission.setGroomReviewStartedAt(CUTOFF.minusHours(1));
        StableStall own = stall(43L);
        own.setGroomId(8L);
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(groomPool.viewFor(8L)).thenReturn(new AdmissionGroomPoolService.PoolView(false, true, CUTOFF));
        oldestAndCapacity(1L);
        when(stableStallRepository.findFirstAvailableQuarantineStallOfGroomForUpdate(8L)).thenReturn(Optional.of(own));
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.review(1L, 8L, request(ReviewDecision.APPROVED, null));

        assertEquals(43L, admission.getQuarantineStallId());
        assertEquals(8L, admission.getGroomId());
    }

    @Test
    @DisplayName("Before the fallback window a busier groom is not in the pool")
    void review_approveBeforeFallback_byBusierGroom_isForbidden() {
        AdmissionApplication admission = admission();
        admission.setGroomReviewStartedAt(CUTOFF.plusHours(1));
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(groomPool.viewFor(8L)).thenReturn(new AdmissionGroomPoolService.PoolView(false, true, CUTOFF));

        ApiException error = assertThrows(ApiException.class,
                () -> service.review(1L, 8L, request(ReviewDecision.APPROVED, null)));

        assertApiError(error, 403, "NOT_IN_GROOM_POOL");
    }

    @Test
    @DisplayName("An admission that already left GROOM_REVIEW cannot be reviewed again")
    void review_alreadyClaimed_isConflict() {
        AdmissionApplication admission = awaitingArrival();
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.review(1L, 7L, request(ReviewDecision.APPROVED, null)));

        assertEquals(409, error.getStatusCode().value());
        verifyNoInteractions(groomPool);
    }

    @Test
    @DisplayName("Reopening an expired arrival returns it to the groom pool as a fresh review")
    void reopenExpiredArrival_returnsToGroomReview() {
        AdmissionApplication expired = admission();
        expired.setStatus(AdmissionStatus.ARRIVAL_EXPIRED);
        expired.setGroomId(7L);
        expired.setGroomDecision(ReviewDecision.APPROVED);
        expired.setGroomFeedback("Ready");
        expired.setGroomReviewedAt(LocalDateTime.now().minusDays(20));
        expired.setGroomReviewStartedAt(LocalDateTime.now().minusDays(21));
        expired.setGroomPoolWidenedAt(LocalDateTime.now().minusDays(20));
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(expired));
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        LocalDateTime before = LocalDateTime.now();

        AdmissionApplication result = service.reopenExpiredArrival(1L);

        assertEquals(AdmissionStatus.GROOM_REVIEW, result.getStatus());
        assertNull(result.getGroomId());
        assertNull(result.getGroomDecision());
        assertNull(result.getGroomFeedback());
        assertNull(result.getGroomReviewedAt());
        assertNull(result.getGroomPoolWidenedAt());
        assertFalse(result.getGroomReviewStartedAt().isBefore(before));
        verify(stableStallRepository, never()).lockAdmissionCapacityStallsForUpdate();
        verify(poolNotifier).notifyReopened(result);
    }

    @Test
    @DisplayName("D14: a RESERVED stall changing hands tells its new groom about the horse on its way")
    void reservedStallChangingHandsNotifiesNewGroom() {
        AdmissionApplication arriving = awaitingArrival();
        StableStall reserved = stall(99L);
        reserved.setStatus(StallStatus.RESERVED);
        reserved.setGroomId(8L);
        reserved.setStallCode("QA-02");
        StableStall regular = stall(20L);
        regular.setGroomId(9L);
        when(admissionApplicationRepository.findByQuarantineStallIdInAndStatus(
                java.util.Set.of(99L), AdmissionStatus.WAITING_FOR_ARRIVAL)).thenReturn(List.of(arriving));

        service.notifyReservedStallsChangedHands(List.of(reserved, regular));

        verify(notificationService).createNotification(eq(8L), anyString(), contains("QA-02"),
                eq(NotificationTypes.ADMISSION_GROOM_ASSIGNED), eq(NotificationTypes.REFERENCE_ADMISSION), eq(1L),
                eq("8:ADMISSION_1:ADMISSION_GROOM_ASSIGNED:stall:99"));
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    @DisplayName("Stalls that are not RESERVED never trigger an admission lookup")
    void nonReservedStallsChangingHandsAreIgnored() {
        StableStall available = stall(99L);
        available.setGroomId(8L);

        service.notifyReservedStallsChangedHands(List.of(available));

        verify(admissionApplicationRepository, never()).findByQuarantineStallIdInAndStatus(any(), any());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Expiring a reservation frees the stall and touches no other admission")
    void expiringReservationReleasesStall() {
        AdmissionApplication overdue = awaitingArrival();
        overdue.setArrivalDeadlineAt(LocalDateTime.now().minusMinutes(1));
        when(admissionApplicationRepository.findByStatusAndArrivalDeadlineAtLessThanEqualOrderByArrivalDeadlineAtAscIdAsc(
                eq(AdmissionStatus.WAITING_FOR_ARRIVAL), any())).thenReturn(List.of(overdue));
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(overdue));
        StableStall reserved = stall(99L);
        reserved.setStatus(StallStatus.RESERVED);
        when(stableStallRepository.findQuarantineStallByIdForUpdate(99L)).thenReturn(Optional.of(reserved));

        service.expireOverdueArrivalReservations();

        assertEquals(AdmissionStatus.ARRIVAL_EXPIRED, overdue.getStatus());
        assertEquals(StallStatus.AVAILABLE, reserved.getStatus());
        verify(stableStallRepository, never()).lockAdmissionCapacityStallsForUpdate();
        verify(admissionApplicationRepository).save(overdue);
    }

    @Test
    @DisplayName("BR-03: only the groom of the reserved quarantine stall confirms arrival")
    void confirmArrivalByAnotherGroomIsForbidden() {
        AdmissionApplication admission = awaitingArrival();
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        StableStall reserved = stall(99L);
        reserved.setStatus(StallStatus.RESERVED);
        reserved.setGroomId(7L);
        when(stableStallRepository.findQuarantineStallByIdForUpdate(99L)).thenReturn(Optional.of(reserved));

        var error = assertThrows(ResponseStatusException.class, () -> service.confirmArrival(1L, 8L));

        assertEquals(403, error.getStatusCode().value());
        verify(horseRepository, never()).save(any());
    }

    @Test
    void confirmArrivalCreatesHorseAndInitialExamForThisAdmission() {
        AdmissionApplication admission = awaitingArrival();
        mockArrival(admission);
        when(horseRepository.save(any(Horse.class))).thenAnswer(inv -> {
            Horse horse = inv.getArgument(0);
            horse.setId(20L);
            return horse;
        });
        CareSchedule schedule = new CareSchedule();
        schedule.setId(100L);
        schedule.setHorseId(20L);
        schedule.setAdmissionId(1L);
        schedule.setCareType(CareType.INITIAL);
        schedule.setVeterinarianId(15L);
        when(careScheduleRepository.findFirstByAdmissionIdAndCareTypeOrderByCreatedAtDesc(1L, CareType.INITIAL))
                .thenReturn(Optional.empty(), Optional.of(schedule));
        when(careScheduleRepository.save(any(CareSchedule.class))).thenReturn(schedule);

        AdmissionApplication result = service.confirmArrival(1L, 7L);

        assertEquals(AdmissionStatus.VET_REVIEW, result.getStatus());
        assertEquals(20L, result.getHorseId());
        var horse = ArgumentCaptor.forClass(Horse.class);
        verify(horseRepository).save(horse.capture());
        assertEquals(5L, horse.getValue().getOwnerId());
        assertEquals("999999TST000001", horse.getValue().getRegistrationNumber());
        assertEquals(HorseSex.FEMALE, horse.getValue().getSex());
        assertEquals(HorseStatus.CANDIDATE, horse.getValue().getCurrentStatus());
        assertFalse(horse.getValue().canTrain());
        var initial = ArgumentCaptor.forClass(CareSchedule.class);
        verify(careScheduleRepository).save(initial.capture());
        assertEquals(1L, initial.getValue().getAdmissionId());
        assertEquals(20L, initial.getValue().getHorseId());
        verify(careScheduleService).assignRequestedSchedule(schedule);
        verify(careScheduleService, never()).assignRequestedSchedules();
        verify(notificationService).sendAssignmentNotification(eq(15L), eq(1L), eq(20L),
                eq(NotificationTypes.ADMISSION_VET_ASSIGNED), anyString(), anyString());
    }

    @Test
    void confirmArrivalPreservesExistingExamAssignmentAndRejectsRepeatConfirmation() {
        AdmissionApplication admission = awaitingArrival();
        mockArrival(admission);
        when(horseRepository.save(any(Horse.class))).thenAnswer(inv -> {
            Horse horse = inv.getArgument(0);
            horse.setId(20L);
            return horse;
        });
        CareSchedule schedule = new CareSchedule();
        schedule.setId(100L);
        schedule.setHorseId(20L);
        schedule.setAdmissionId(1L);
        schedule.setCareType(CareType.INITIAL);
        schedule.setVeterinarianId(15L);
        when(careScheduleRepository.findFirstByAdmissionIdAndCareTypeOrderByCreatedAtDesc(1L, CareType.INITIAL))
                .thenReturn(Optional.of(schedule));

        service.confirmArrival(1L, 7L);
        ResponseStatusException repeated = assertThrows(ResponseStatusException.class,
                () -> service.confirmArrival(1L, 7L));

        assertEquals(409, repeated.getStatusCode().value());
        verify(careScheduleRepository, never()).save(any());
        verify(horseRepository, times(1)).save(any());
        verify(notificationService).sendAssignmentNotification(eq(15L), eq(1L), eq(20L),
                eq(NotificationTypes.ADMISSION_VET_ASSIGNED), anyString(), anyString());
    }

    @Test
    void confirmArrivalReusesRejectedHorseWithSameOwner() {
        assertReadmissionUpdatesCurrentHorseOnly(5L);
    }

    @Test
    void confirmArrivalReusesRejectedHorseWithNewOwner() {
        assertReadmissionUpdatesCurrentHorseOnly(42L);
    }

    @Test
    void confirmArrivalRechecksParentBirthDateBeforeUpdatingHorseOrStall() {
        AdmissionApplication admission = awaitingArrival();
        mockArrival(admission);
        Horse sire = new Horse();
        sire.setDateOfBirth(LocalDate.of(2022, 1, 1));
        when(horseRepository.findByRegistrationNumber("999999SIR000001")).thenReturn(List.of(sire));

        var error = assertThrows(ResponseStatusException.class, () -> service.confirmArrival(1L, 7L));

        assertEquals(400, error.getStatusCode().value());
        assertTrue(error.getReason().contains("sire"));
        verify(stableStallRepository, never()).save(any());
        verify(horseRepository, never()).save(any());
        verify(admissionApplicationRepository, never()).save(any());
        verifyNoInteractions(horsePedigreeRepository, careScheduleRepository, notificationService);
    }

    @Test
    void confirmArrivalStillRejectsAnActiveHorseWithSameUeln() {
        AdmissionApplication admission = awaitingArrival();
        mockArrival(admission);
        Horse existingHorse = new Horse();
        existingHorse.setId(20L);
        existingHorse.setOwnerId(42L);
        existingHorse.setCurrentStatus(HorseStatus.ELIGIBLE);
        when(horseRepository.findByRegistrationNumberForUpdate("999999TST000001"))
                .thenReturn(List.of(existingHorse));

        assertThrows(ResponseStatusException.class, () -> service.confirmArrival(1L, 7L));
        assertEquals(42L, existingHorse.getOwnerId());
        verify(horseRepository, never()).save(any());
        verify(admissionApplicationRepository, never()).save(any());
    }

    @Test
    void legacyAdmissionDoesNotEraseKnownSexOnReadmission() {
        AdmissionApplication admission = awaitingArrival();
        mockArrival(admission);
        CandidateHorseProfile legacy = candidate();
        legacy.setSex(null);
        when(candidateHorseProfileRepository.findByAdmissionId(1L)).thenReturn(Optional.of(legacy));
        Horse existing = new Horse();
        existing.setId(20L);
        existing.setCurrentStatus(HorseStatus.REJECTED);
        existing.setSex(HorseSex.FEMALE);
        when(horseRepository.findByRegistrationNumberForUpdate("999999TST000001")).thenReturn(List.of(existing));
        when(horseRepository.save(existing)).thenReturn(existing);

        service.confirmArrival(1L, 7L);

        assertEquals(HorseSex.FEMALE, existing.getSex());
        assertNull(legacy.getSex());
        verify(candidateHorseProfileRepository, never()).save(any());
    }

    @Test
    void legacyAdmissionCanCreateHorseWithUnknownSex() {
        AdmissionApplication admission = awaitingArrival();
        mockArrival(admission);
        CandidateHorseProfile legacy = candidate();
        legacy.setSex(null);
        when(candidateHorseProfileRepository.findByAdmissionId(1L)).thenReturn(Optional.of(legacy));
        when(horseRepository.save(any(Horse.class))).thenAnswer(inv -> {
            Horse horse = inv.getArgument(0);
            horse.setId(20L);
            return horse;
        });

        service.confirmArrival(1L, 7L);

        var saved = ArgumentCaptor.forClass(Horse.class);
        verify(horseRepository).save(saved.capture());
        assertNull(saved.getValue().getSex());
    }

    @Test
    void confirmArrivalRejectsQuarantineStallStillLinkedToAnotherHorse() {
        AdmissionApplication admission = awaitingArrival();
        mockArrival(admission);
        Horse squatter = new Horse();
        squatter.setId(77L);
        when(horseRepository.findByCurrentStallId(99L)).thenReturn(Optional.of(squatter));

        var error = assertThrows(ResponseStatusException.class, () -> service.confirmArrival(1L, 7L));

        assertEquals(409, error.getStatusCode().value());
        verify(stableStallRepository, never()).save(any());
        verify(horseRepository, never()).save(any());
        verifyNoInteractions(careScheduleRepository, notificationService);
    }

    @Test
    void confirmArrivalRejectsReadmittedHorseWithAnotherActiveAdmission() {
        AdmissionApplication admission = awaitingArrival();
        mockArrival(admission);
        Horse rejected = new Horse();
        rejected.setId(20L);
        rejected.setCurrentStatus(HorseStatus.REJECTED);
        when(horseRepository.findByRegistrationNumberForUpdate("999999TST000001")).thenReturn(List.of(rejected));
        when(admissionApplicationRepository.existsByHorseIdAndIdNotAndStatusNotIn(eq(20L), eq(1L), any()))
                .thenReturn(true);

        var error = assertThrows(ResponseStatusException.class, () -> service.confirmArrival(1L, 7L));

        assertEquals(409, error.getStatusCode().value());
        verify(horseRepository, never()).save(any());
        verifyNoInteractions(careScheduleRepository, notificationService);
    }

    private void assertReadmissionUpdatesCurrentHorseOnly(Long previousOwnerId) {
        AdmissionApplication admission = awaitingArrival();
        mockArrival(admission);
        Horse existingHorse = new Horse();
        existingHorse.setId(20L);
        existingHorse.setOwnerId(previousOwnerId);
        existingHorse.setCurrentStatus(HorseStatus.REJECTED);
        existingHorse.setRegistrationNumber("999999TST000001");
        when(horseRepository.findByRegistrationNumberForUpdate("999999TST000001"))
                .thenReturn(List.of(existingHorse));
        when(horseRepository.save(existingHorse)).thenReturn(existingHorse);

        AdmissionApplication result = service.confirmArrival(1L, 7L);

        assertEquals(20L, result.getHorseId());
        assertEquals(5L, existingHorse.getOwnerId());
        assertEquals(HorseSex.FEMALE, existingHorse.getSex());
        assertEquals("999999TST000001", existingHorse.getRegistrationNumber());
        assertEquals(HorseStatus.CANDIDATE, existingHorse.getCurrentStatus());
        assertEquals(99L, existingHorse.getCurrentStallId());
        assertFalse(existingHorse.canTrain());
        verify(horseRepository).save(same(existingHorse));
        verify(admissionApplicationRepository).findByIdForUpdate(1L);
        verify(admissionApplicationRepository).save(same(admission));
        verify(admissionApplicationRepository).existsByHorseIdAndIdNotAndStatusNotIn(eq(20L), eq(1L), any());
        verifyNoMoreInteractions(admissionApplicationRepository);
        verify(candidateHorseProfileRepository).findByAdmissionId(1L);
        verifyNoMoreInteractions(candidateHorseProfileRepository);
    }

    private AdmissionApplication awaitingArrival() {
        AdmissionApplication admission = admission();
        admission.setStatus(AdmissionStatus.WAITING_FOR_ARRIVAL);
        admission.setGroomId(7L);
        admission.setQuarantineStallId(99L);
        admission.setArrivalDeadlineAt(LocalDateTime.now().plusDays(1));
        return admission;
    }

    private void mockArrival(AdmissionApplication admission) {
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(candidateHorseProfileRepository.findByAdmissionId(1L)).thenReturn(Optional.of(candidate()));
        StableStall reserved = stall(99L);
        reserved.setStatus(StallStatus.RESERVED);
        reserved.setGroomId(7L);
        when(stableStallRepository.findQuarantineStallByIdForUpdate(99L)).thenReturn(Optional.of(reserved));
        lenient().when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private AdmissionApplication admission() {
        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(1L);
        admission.setOwnerId(5L);
        admission.setStatus(AdmissionStatus.GROOM_REVIEW);
        admission.setGroomReviewStartedAt(LocalDateTime.now().minusHours(1));
        return admission;
    }

    private CandidateHorseProfile candidate() {
        CandidateHorseProfile candidate = new CandidateHorseProfile();
        candidate.setId(11L);
        candidate.setAdmissionId(1L);
        candidate.setName("QABALAH MERCURY");
        candidate.setBreed("Arabian");
        candidate.setSex(HorseSex.FEMALE);
        candidate.setDateOfBirth(LocalDate.of(2021, 3, 4));
        candidate.setRegistrationNumber("999999TST000001");
        candidate.setRegistryName("SIRE");
        candidate.setSireName("Mercury Sire");
        candidate.setSireRegistrationNumber("999999SIR000001");
        candidate.setDamName("Mercury Dam");
        candidate.setDamRegistrationNumber("999999DAM000001");
        candidate.setPedigreeNotes("Clean pedigree snapshot");
        return candidate;
    }

    private StableStall stall(Long id) {
        StableStall stall = new StableStall();
        stall.setId(id);
        stall.setStatus(StallStatus.AVAILABLE);
        return stall;
    }

    private GroomAdmissionReviewRequest request(ReviewDecision decision, String feedback) {
        GroomAdmissionReviewRequest request = new GroomAdmissionReviewRequest();
        request.setDecision(decision);
        request.setFeedback(feedback);
        return request;
    }
}
