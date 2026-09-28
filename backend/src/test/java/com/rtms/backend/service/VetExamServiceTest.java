package com.rtms.backend.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.dto.CompleteVetExamRequest;
import com.rtms.backend.dto.CreateVetExamRequest;
import com.rtms.backend.entity.*;
import com.rtms.backend.enums.*;
import com.rtms.backend.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VetExamServiceTest {
    @Mock VetExamRepository exams;
    @Mock HorseRepository horses;
    @Mock AdmissionApplicationRepository admissions;
    @Mock StableStallRepository stalls;
    @Mock HealthRecordRepository records;
    @Mock HorseHealthMetricRepository metrics;
    @Mock UserRepository users;

    VetExamService service;
    VetExam exam;
    AdmissionApplication admission;
    Horse horse;
    StableStall quarantine;
    StableStall regular;

    @BeforeEach
    void setUp() {
        service = new VetExamService(exams, horses, admissions, stalls, records, metrics, users);
        exam = new VetExam();
        exam.setId(20L);
        exam.setHorseId(10L);
        exam.setAdmissionId(1L);
        exam.setExamType(VetExamType.INITIAL);
        exam.setStatus(VetExamStatus.IN_PROGRESS);
        exam.setAssignedVetId(5L);
        exam.setScheduledAt(LocalDateTime.now().minusMinutes(10));
        exam.setDurationMinutes(30);

        admission = new AdmissionApplication();
        admission.setId(1L);
        admission.setHorseId(10L);
        admission.setQuarantineStallId(99L);
        admission.setStatus(AdmissionStatus.VET_REVIEW);

        horse = new Horse();
        horse.setId(10L);
        horse.setCurrentStatus(HorseStatus.CANDIDATE);
        horse.setCurrentStallId(99L);
        horse.setTrainingLocked(true);

        quarantine = stall(99L, "Q-1", StallStatus.OCCUPIED);
        regular = stall(100L, "R-1", StallStatus.AVAILABLE);
    }

    @Test
    void approvedIsFinalAndMovesHorseToReservedRegularStall() {
        mockCompletion();
        when(stalls.findFirstAvailableRegularStallForUpdate()).thenReturn(Optional.of(regular));
        when(stalls.findQuarantineStallByIdForUpdate(99L)).thenReturn(Optional.of(quarantine));

        var result = service.complete(20L, completion(VetDecision.APPROVED), 5L);

        assertEquals(VetExamStatus.COMPLETED, result.status());
        assertEquals(30L, result.healthRecordId());
        assertEquals(AdmissionStatus.APPROVED, admission.getStatus());
        assertEquals(VetDecision.APPROVED, admission.getVetDecision());
        assertEquals(HorseStatus.ELIGIBLE, horse.getCurrentStatus());
        assertEquals(100L, horse.getCurrentStallId());
        assertFalse(horse.isTrainingLocked());
        assertEquals(StallStatus.OCCUPIED, regular.getStatus());
        assertEquals(StallStatus.AVAILABLE, quarantine.getStatus());
        assertEquals(30L, exam.getHealthRecordId());

        ArgumentCaptor<HealthRecord> saved = ArgumentCaptor.forClass(HealthRecord.class);
        verify(records).save(saved.capture());
        assertEquals(VetDecision.APPROVED, saved.getValue().getVetDecision());
    }

    @Test
    void approvalWithoutRegularStallRollsBackBeforeHealthRecord() {
        mockCompletionLookups();
        when(stalls.findFirstAvailableRegularStallForUpdate()).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> service.complete(20L, completion(VetDecision.APPROVED), 5L));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        assertTrue(error.getMessage().contains("No regular stall"));
        assertEquals(VetExamStatus.IN_PROGRESS, exam.getStatus());
        verify(records, never()).save(any());
    }

    @Test
    void recheckKeepsQuarantineAndCreatesNewFollowUp() {
        mockCompletion();
        when(stalls.findQuarantineStallByIdForUpdate(99L)).thenReturn(Optional.of(quarantine));
        CompleteVetExamRequest request = completion(VetDecision.RECHECK_REQUIRED);
        request.setFollowUpDate(LocalDate.now().plusDays(4));

        service.complete(20L, request, 5L);

        assertEquals(AdmissionStatus.PENDING_RECHECK, admission.getStatus());
        assertEquals(HorseStatus.CANDIDATE, horse.getCurrentStatus());
        assertEquals(99L, horse.getCurrentStallId());
        assertTrue(horse.isTrainingLocked());
        assertEquals(LocalDate.now().plusDays(4), horse.getTrainingLockReviewDate());
        assertEquals(StallStatus.OCCUPIED, quarantine.getStatus());

        ArgumentCaptor<VetExam> followUp = ArgumentCaptor.forClass(VetExam.class);
        verify(exams).save(followUp.capture());
        assertEquals(VetExamType.FOLLOW_UP, followUp.getValue().getExamType());
        assertEquals(VetExamStatus.REQUESTED, followUp.getValue().getStatus());
        assertEquals(5L, followUp.getValue().getPreferredVetId());
        assertEquals(1L, followUp.getValue().getAdmissionId());
    }

    @Test
    void rejectedRequiresReason() {
        CompleteVetExamRequest request = completion(VetDecision.REJECTED);
        ApiException error = assertThrows(ApiException.class,
                () -> service.complete(20L, request, 5L));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verifyNoInteractions(exams, horses, admissions, stalls, records);
    }

    @Test
    void rejectedReleasesQuarantineAndLocksTraining() {
        mockCompletion();
        when(stalls.findQuarantineStallByIdForUpdate(99L)).thenReturn(Optional.of(quarantine));
        CompleteVetExamRequest request = completion(VetDecision.REJECTED);
        request.setRejectionReason("Cardiac risk");

        service.complete(20L, request, 5L);

        assertEquals(AdmissionStatus.REJECTED, admission.getStatus());
        assertEquals(HorseStatus.REJECTED, horse.getCurrentStatus());
        assertNull(horse.getCurrentStallId());
        assertTrue(horse.isTrainingLocked());
        assertEquals("Cardiac risk", horse.getTrainingLockReason());
        assertEquals(StallStatus.AVAILABLE, quarantine.getStatus());
    }

    @Test
    void publicCreateOnlyAcceptsUrgentRequests() {
        ApiException error = assertThrows(ApiException.class, () -> service.createExam(
                new CreateVetExamRequest(10L, VetExamType.ROUTINE, "Annual check", null), 7L));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verifyNoInteractions(horses, exams);
    }

    @Test
    void queuePageSizeIsCappedAtFifty() {
        when(exams.findAll(any(Pageable.class))).thenReturn(Page.empty());

        service.list(null, null, PageRequest.of(0, 100));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(exams).findAll(pageable.capture());
        assertEquals(50, pageable.getValue().getPageSize());
    }

    @Test
    void schedulerUsesGlobalLockAndAssignsUrgentImmediately() {
        VetExam urgent = new VetExam();
        urgent.setId(40L);
        urgent.setHorseId(10L);
        urgent.setExamType(VetExamType.URGENT);
        urgent.setStatus(VetExamStatus.REQUESTED);
        urgent.setDurationMinutes(30);
        User vet = new User();
        vet.setId(5L);
        when(exams.trySchedulerLock()).thenReturn(true);
        when(users.findActiveVeterinarians()).thenReturn(List.of(vet));
        when(exams.lockNextRequestedBatch(100)).thenReturn(List.of(urgent));
        when(exams.findVetSchedule(eq(5L), anyCollection(), any(), any())).thenReturn(List.of());
        when(exams.findHorseSchedule(eq(10L), anyCollection(), any(), any())).thenReturn(List.of());

        assertEquals(1, service.scheduleRequestedBatch(100));
        assertEquals(VetExamStatus.SCHEDULED, urgent.getStatus());
        assertEquals(5L, urgent.getAssignedVetId());
        assertNotNull(urgent.getScheduledAt());
        verify(exams).trySchedulerLock();
    }

    private void mockCompletionLookups() {
        when(exams.findByIdForUpdate(20L)).thenReturn(Optional.of(exam));
        when(horses.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(admissions.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
    }

    private void mockCompletion() {
        mockCompletionLookups();
        when(records.save(any())).thenAnswer(invocation -> {
            HealthRecord record = invocation.getArgument(0);
            record.setId(30L);
            return record;
        });
    }

    private CompleteVetExamRequest completion(VetDecision decision) {
        CompleteVetExamRequest request = new CompleteVetExamRequest();
        request.setVetDecision(decision);
        request.setFindings("Clinical findings");
        return request;
    }

    private StableStall stall(Long id, String code, StallStatus status) {
        StableStall value = new StableStall();
        value.setId(id);
        value.setStallCode(code);
        value.setStatus(status);
        return value;
    }
}
