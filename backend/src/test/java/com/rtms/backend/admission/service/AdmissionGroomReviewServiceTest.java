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
                groomProfileRepository);
        lenient().when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Groom reject: Admission REJECTED và không tạo Horse")
    void review_reject_success() {
        AdmissionApplication admission = admission();
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(stableStallRepository.isQuarantineGroom(7L)).thenReturn(true);
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
    @DisplayName("Groom approve thiếu capacity: Admission WAITING_FOR_STALL và chưa tạo Horse")
    void review_approveWithoutCapacity_waitingForStall() {
        AdmissionApplication admission = admission();
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(stableStallRepository.isQuarantineGroom(7L)).thenReturn(true);
        when(stableStallRepository.countAvailableQuarantineStalls()).thenReturn(0L);
        when(stableStallRepository.countAvailableRegularStalls()).thenReturn(10L);
        when(stableStallRepository.countOccupiedQuarantineStalls()).thenReturn(1L);
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 7L, request(ReviewDecision.APPROVED, "OK"));

        assertEquals(AdmissionStatus.WAITING_FOR_STALL, result.getStatus());
        assertEquals(ReviewDecision.APPROVED, result.getGroomDecision());
        assertNull(result.getHorseId());
        verify(stableStallRepository).lockAdmissionCapacityStallsForUpdate();
        verify(admissionApplicationRepository, never()).findNextWaitingForStallForUpdate();
        verifyNoInteractions(candidateHorseProfileRepository, horseRepository, horsePedigreeRepository,
                careScheduleRepository, notificationService);
    }

    @Test
    @DisplayName("Groom approve thiếu regular reserve vẫn duyệt nhưng chờ stall")
    void review_approveWithoutRegularReserve_waitingForStall() {
        AdmissionApplication admission = admission();
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(stableStallRepository.isQuarantineGroom(7L)).thenReturn(true);
        when(stableStallRepository.countAvailableQuarantineStalls()).thenReturn(2L);
        when(stableStallRepository.countAvailableRegularStalls()).thenReturn(1L);
        when(stableStallRepository.countOccupiedQuarantineStalls()).thenReturn(1L);
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 7L,
                request(ReviewDecision.APPROVED, "Documents verified"));

        assertEquals(AdmissionStatus.WAITING_FOR_STALL, result.getStatus());
        assertEquals(ReviewDecision.APPROVED, result.getGroomDecision());
        verifyNoInteractions(candidateHorseProfileRepository, horseRepository, horsePedigreeRepository,
                careScheduleRepository, notificationService);
    }

    @Test
    @DisplayName("BR-02: approve without feedback is allowed and stores no feedback")
    void review_approveWithoutFeedback_isAllowed() {
        AdmissionApplication admission = admission();
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(stableStallRepository.isQuarantineGroom(7L)).thenReturn(true);
        when(stableStallRepository.countAvailableQuarantineStalls()).thenReturn(0L);
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 7L, request(ReviewDecision.APPROVED, "  \t "));

        assertEquals(AdmissionStatus.WAITING_FOR_STALL, result.getStatus());
        assertNull(result.getGroomFeedback());
    }

    @Test
    @DisplayName("BR-02: reject requires non-blank feedback even when the client skips its own check")
    void review_rejectWithBlankFeedback_isRejected() {
        AdmissionApplication admission = admission();
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(stableStallRepository.isQuarantineGroom(7L)).thenReturn(true);

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
        when(stableStallRepository.isQuarantineGroom(7L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.review(1L, 7L, request(ReviewDecision.REJECTED, "x".repeat(2001))));

        assertEquals(400, exception.getStatusCode().value());
        verify(admissionApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("BR-03: only quarantine grooms may review new admissions")
    void review_byNonQuarantineGroom_isForbidden() {
        AdmissionApplication admission = admission();
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(stableStallRepository.isQuarantineGroom(8L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.review(1L, 8L, request(ReviewDecision.APPROVED, null)));

        assertEquals(403, exception.getStatusCode().value());
        verify(admissionApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Groom review trims feedback before persisting")
    void review_trimsFeedback() {
        AdmissionApplication admission = admission();
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(stableStallRepository.isQuarantineGroom(7L)).thenReturn(true);
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 7L,
                request(ReviewDecision.REJECTED, "  Missing vaccination record  "));

        assertEquals("Missing vaccination record", result.getGroomFeedback());
    }

    @Test
    void reviewApprovalJoinsQueueAndReservesFairStallWithoutCreatingHorse() {
        AdmissionApplication admission = admission();
        StableStall qStall = stall(99L);
        qStall.setGroomId(12L);
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(stableStallRepository.isQuarantineGroom(7L)).thenReturn(true);
        when(stableStallRepository.countAvailableQuarantineStalls()).thenReturn(1L, 0L);
        when(stableStallRepository.countAvailableRegularStalls()).thenReturn(2L);
        when(stableStallRepository.countOccupiedQuarantineStalls()).thenReturn(1L);
        when(admissionApplicationRepository.findNextWaitingForStallForUpdate()).thenReturn(Optional.of(admission));
        when(stableStallRepository.findFairAvailableStallForUpdate("QUARANTINE")).thenReturn(Optional.of(qStall));
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 7L, request(ReviewDecision.APPROVED, "Ready"));

        assertEquals(AdmissionStatus.WAITING_FOR_ARRIVAL, result.getStatus());
        assertEquals(StallStatus.RESERVED, qStall.getStatus());
        assertEquals(99L, result.getQuarantineStallId());
        assertNotNull(result.getArrivalDeadlineAt());
        assertNull(result.getHorseId());
        verify(groomProfileRepository).touchLastHorseAssignedAt(eq(12L), any());
        verify(notificationService).createNotification(eq(12L), anyString(), anyString(),
                eq(NotificationTypes.ADMISSION_GROOM_ASSIGNED), eq(NotificationTypes.REFERENCE_ADMISSION), eq(1L),
                eq("12:ADMISSION_1:ADMISSION_GROOM_ASSIGNED:stall:99"));
        verifyNoInteractions(horseRepository, horsePedigreeRepository, candidateHorseProfileRepository,
                careScheduleRepository);
    }

    @Test
    @DisplayName("BR-01: allocation serves the queue oldest first and stops without error when capacity runs out")
    void allocateWaitingAdmissionsServesQueueUntilCapacityRunsOut() {
        AdmissionApplication oldest = admission();
        oldest.setStatus(AdmissionStatus.WAITING_FOR_STALL);
        AdmissionApplication next = admission();
        next.setId(2L);
        next.setStatus(AdmissionStatus.WAITING_FOR_STALL);
        StableStall first = stall(98L);
        first.setGroomId(12L);
        StableStall second = stall(99L);
        second.setGroomId(13L);
        when(stableStallRepository.countAvailableQuarantineStalls()).thenReturn(2L, 1L, 0L);
        when(stableStallRepository.countAvailableRegularStalls()).thenReturn(10L);
        when(stableStallRepository.countOccupiedQuarantineStalls()).thenReturn(0L, 1L);
        when(admissionApplicationRepository.findNextWaitingForStallForUpdate())
                .thenReturn(Optional.of(oldest), Optional.of(next));
        when(stableStallRepository.findFairAvailableStallForUpdate("QUARANTINE"))
                .thenReturn(Optional.of(first), Optional.of(second));

        int allocated = service.allocateWaitingAdmissions();

        assertEquals(2, allocated);
        assertEquals(98L, oldest.getQuarantineStallId());
        assertEquals(99L, next.getQuarantineStallId());
        assertEquals(AdmissionStatus.WAITING_FOR_ARRIVAL, oldest.getStatus());
        assertEquals(AdmissionStatus.WAITING_FOR_ARRIVAL, next.getStatus());
        var order = inOrder(admissionApplicationRepository, stableStallRepository);
        order.verify(admissionApplicationRepository).flush();
        order.verify(stableStallRepository).lockAdmissionCapacityStallsForUpdate();
    }

    @Test
    @DisplayName("BR-01: an empty queue or a missing groom stall ends allocation quietly")
    void allocateWaitingAdmissionsWithoutWorkReturnsZero() {
        when(stableStallRepository.countAvailableQuarantineStalls()).thenReturn(3L);
        when(stableStallRepository.countAvailableRegularStalls()).thenReturn(10L);
        when(stableStallRepository.countOccupiedQuarantineStalls()).thenReturn(0L);
        when(admissionApplicationRepository.findNextWaitingForStallForUpdate()).thenReturn(Optional.empty());

        assertEquals(0, service.allocateWaitingAdmissions());

        AdmissionApplication waiting = admission();
        waiting.setStatus(AdmissionStatus.WAITING_FOR_STALL);
        when(admissionApplicationRepository.findNextWaitingForStallForUpdate()).thenReturn(Optional.of(waiting));
        when(stableStallRepository.findFairAvailableStallForUpdate("QUARANTINE")).thenReturn(Optional.empty());

        assertEquals(0, service.allocateWaitingAdmissions());
        assertEquals(AdmissionStatus.WAITING_FOR_STALL, waiting.getStatus());
        verifyNoInteractions(notificationService, groomProfileRepository);
    }

    @Test
    @DisplayName("BR-01: expiring a reservation hands the freed stall to the queue")
    void expiringReservationTriggersAllocation() {
        AdmissionApplication overdue = awaitingArrival();
        overdue.setArrivalDeadlineAt(LocalDateTime.now().minusMinutes(1));
        when(admissionApplicationRepository.findByStatusAndArrivalDeadlineAtLessThanEqualOrderByArrivalDeadlineAtAscIdAsc(
                eq(AdmissionStatus.WAITING_FOR_ARRIVAL), any())).thenReturn(List.of(overdue));
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(overdue));
        StableStall reserved = stall(99L);
        reserved.setStatus(StallStatus.RESERVED);
        when(stableStallRepository.findQuarantineStallByIdForUpdate(99L)).thenReturn(Optional.of(reserved));
        when(stableStallRepository.countAvailableQuarantineStalls()).thenReturn(0L);

        service.expireOverdueArrivalReservations();

        assertEquals(AdmissionStatus.ARRIVAL_EXPIRED, overdue.getStatus());
        assertEquals(StallStatus.AVAILABLE, reserved.getStatus());
        verify(stableStallRepository).lockAdmissionCapacityStallsForUpdate();
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
