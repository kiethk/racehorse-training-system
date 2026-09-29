package com.rtms.backend.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.dto.CompleteVetExamRequest;
import com.rtms.backend.dto.VetExamResponse;
import com.rtms.backend.dto.VetReviewRequest;
import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.StableStall;
import com.rtms.backend.entity.VetExam;
import com.rtms.backend.enums.*;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.StableStallRepository;
import com.rtms.backend.repository.VetExamRepository;
import jakarta.validation.Validation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdmissionReviewServiceTest {
    @Mock AdmissionApplicationRepository admissions;
    @Mock VetExamRepository exams;
    @Mock HorseRepository horses;
    @Mock StableStallRepository stalls;
    @Mock VetExamService vetExamService;

    AdmissionReviewService service;
    AdmissionApplication admission;
    VetExam exam;
    Horse horse;
    StableStall quarantine;

    @BeforeEach
    void setUp() {
        service = new AdmissionReviewService(admissions, exams, horses, stalls, vetExamService);
        admission = new AdmissionApplication();
        admission.setId(1L);
        admission.setHorseId(10L);
        admission.setQuarantineStallId(99L);
        admission.setStatus(AdmissionStatus.VET_REVIEW);

        exam = new VetExam();
        exam.setId(20L);
        exam.setAdmissionId(1L);
        exam.setHorseId(10L);
        exam.setExamType(VetExamType.INITIAL);
        exam.setStatus(VetExamStatus.SCHEDULED);
        exam.setAssignedVetId(5L);

        horse = new Horse();
        horse.setId(10L);
        horse.setCurrentStatus(HorseStatus.CANDIDATE);
        horse.setCurrentStallId(99L);

        quarantine = new StableStall();
        quarantine.setId(99L);
        quarantine.setStallCode("Q-1");
    }

    @Test
    void approvedExamUsesCompatibilityAdapterAndReturnsTrainerReviewState() {
        mockInitialLookup();
        when(vetExamService.start(20L, 5L)).thenReturn(examResponse(VetExamStatus.IN_PROGRESS, null));
        when(vetExamService.complete(eq(20L), any(), eq(5L))).thenAnswer(invocation -> {
            admission.setStatus(AdmissionStatus.TRAINER_REVIEW);
            admission.setVetDecision(VetDecision.APPROVED);
            admission.setVetReviewedAt(LocalDateTime.now());
            return examResponse(VetExamStatus.COMPLETED, 30L);
        });

        var response = service.reviewByVet(1L, request(VetDecision.APPROVED), 5L);

        assertEquals(AdmissionStatus.TRAINER_REVIEW, response.status());
        assertEquals(HorseStatus.CANDIDATE, response.horseStatus());
        assertEquals(99L, response.quarantineStallId());
        assertEquals(VetExamStatus.COMPLETED, response.initialExamStatus());
        assertEquals(30L, response.healthRecordId());
        assertEquals(20L, response.vetExamId());
        verify(vetExamService).start(20L, 5L);
    }

    @Test
    void recheckUsesCurrentFollowUpAndDoesNotRestartInProgressExam() {
        admission.setStatus(AdmissionStatus.PENDING_RECHECK);
        exam.setExamType(VetExamType.FOLLOW_UP);
        exam.setStatus(VetExamStatus.IN_PROGRESS);
        when(admissions.findById(1L)).thenReturn(Optional.of(admission));
        when(exams.findFirstByAdmissionIdAndExamTypeOrderByCreatedAtDesc(1L, VetExamType.FOLLOW_UP))
                .thenReturn(Optional.of(exam));
        when(vetExamService.complete(eq(20L), any(), eq(5L))).thenAnswer(invocation -> {
            admission.setVetDecision(VetDecision.RECHECK_REQUIRED);
            admission.setVetReviewedAt(LocalDateTime.now());
            horse.setTrainingLocked(true);
            return examResponse(VetExamStatus.COMPLETED, 31L);
        });
        when(horses.findById(10L)).thenReturn(Optional.of(horse));
        when(stalls.findById(99L)).thenReturn(Optional.of(quarantine));
        VetReviewRequest request = request(VetDecision.RECHECK_REQUIRED);
        request.setFollowUpDate(LocalDate.now().plusDays(3));

        var response = service.reviewByVet(1L, request, 5L);

        assertEquals(AdmissionStatus.PENDING_RECHECK, response.status());
        assertEquals(VetDecision.RECHECK_REQUIRED, response.decision());
        verify(vetExamService, never()).start(anyLong(), anyLong());
        ArgumentCaptor<CompleteVetExamRequest> completion = ArgumentCaptor.forClass(CompleteVetExamRequest.class);
        verify(vetExamService).complete(eq(20L), completion.capture(), eq(5L));
        assertEquals(request.getFollowUpDate(), completion.getValue().getFollowUpDate());
    }

    @Test
    void rejectedMapsLegacyFeedbackToRequiredRejectionReason() {
        exam.setStatus(VetExamStatus.IN_PROGRESS);
        mockInitialLookup();
        when(vetExamService.complete(eq(20L), any(), eq(5L))).thenAnswer(invocation -> {
            CompleteVetExamRequest completion = invocation.getArgument(1);
            admission.setStatus(AdmissionStatus.REJECTED);
            admission.setVetDecision(VetDecision.REJECTED);
            admission.setVetFeedback(completion.getRejectionReason());
            admission.setVetReviewedAt(LocalDateTime.now());
            horse.setCurrentStatus(HorseStatus.REJECTED);
            horse.setCurrentStallId(null);
            return examResponse(VetExamStatus.COMPLETED, 32L);
        });
        VetReviewRequest request = request(VetDecision.REJECTED);
        request.setFeedback("Not safe for training");

        var response = service.reviewByVet(1L, request, 5L);

        assertEquals(AdmissionStatus.REJECTED, response.status());
        assertEquals("Not safe for training", response.feedback());
        ArgumentCaptor<CompleteVetExamRequest> completion = ArgumentCaptor.forClass(CompleteVetExamRequest.class);
        verify(vetExamService).complete(eq(20L), completion.capture(), eq(5L));
        assertEquals("Not safe for training", completion.getValue().getRejectionReason());
    }

    @Test
    void assignedVetFailureFromStateMachineIsPreserved() {
        when(admissions.findById(1L)).thenReturn(Optional.of(admission));
        when(exams.findFirstByAdmissionIdAndExamTypeOrderByCreatedAtDesc(1L, VetExamType.INITIAL))
                .thenReturn(Optional.of(exam));
        when(vetExamService.start(20L, 6L)).thenThrow(new ApiException(
                HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the assigned veterinarian can start this exam"));

        ApiException error = assertThrows(ApiException.class,
                () -> service.reviewByVet(1L, request(VetDecision.APPROVED), 6L));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
        assertEquals("FORBIDDEN", error.getErrorCode());
        verify(vetExamService, never()).complete(anyLong(), any(), anyLong());
    }

    @Test
    void requestValidationRequiresExamAndDecisionSpecificDetails() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            VetReviewRequest approved = request(VetDecision.APPROVED);
            assertTrue(validator.validate(approved).isEmpty());
            approved.setFindings(" ");
            assertFalse(validator.validate(approved).isEmpty());

            VetReviewRequest rejected = request(VetDecision.REJECTED);
            assertFalse(validator.validate(rejected).isEmpty());
            rejected.setRejectionReason("Unsafe condition");
            assertTrue(validator.validate(rejected).isEmpty());

            VetReviewRequest recheck = request(VetDecision.RECHECK_REQUIRED);
            assertFalse(validator.validate(recheck).isEmpty());
            recheck.setFollowUpDate(LocalDate.now().plusDays(1));
            assertTrue(validator.validate(recheck).isEmpty());
        }
    }

    private void mockInitialLookup() {
        when(admissions.findById(1L)).thenReturn(Optional.of(admission));
        when(exams.findFirstByAdmissionIdAndExamTypeOrderByCreatedAtDesc(1L, VetExamType.INITIAL))
                .thenReturn(Optional.of(exam));
        when(horses.findById(10L)).thenReturn(Optional.of(horse));
        when(stalls.findById(99L)).thenReturn(Optional.of(quarantine));
    }

    private VetReviewRequest request(VetDecision decision) {
        VetReviewRequest request = new VetReviewRequest();
        request.setDecision(decision);
        request.setPhysicalExamConfirmed(true);
        request.setFindings("Clinical findings");
        return request;
    }

    private VetExamResponse examResponse(VetExamStatus status, Long healthRecordId) {
        return new VetExamResponse(20L, 10L, 1L, exam.getExamType(), status,
                exam.getExamType().getDefaultPriority(), "Exam", 7L, 5L, null,
                null, LocalDateTime.now(), 30, healthRecordId, LocalDateTime.now().minusDays(1));
    }
}
