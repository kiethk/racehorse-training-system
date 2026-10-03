package com.rtms.backend.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.dto.CareScheduleResponse;
import com.rtms.backend.dto.CompleteCareScheduleRequest;
import com.rtms.backend.dto.VetReviewRequest;
import com.rtms.backend.dto.VetReviewResponse;
import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.entity.CareSchedule;
import com.rtms.backend.entity.HealthRecord;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.StableStall;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.enums.CareScheduleStatus;
import com.rtms.backend.enums.CareType;
import com.rtms.backend.enums.HorseStatus;
import com.rtms.backend.enums.TrainingDecision;
import com.rtms.backend.enums.VetDecision;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.repository.CareScheduleRepository;
import com.rtms.backend.repository.HealthRecordRepository;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.StableStallRepository;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionReviewServiceTest {
    @Mock AdmissionApplicationRepository admissions;
    @Mock CareScheduleRepository careSchedules;
    @Mock CareScheduleService careScheduleService;
    @Mock HealthRecordRepository healthRecordRepository;
    @Mock HorseRepository horses;
    @Mock StableStallRepository stalls;

    AdmissionReviewService service;
    AdmissionApplication admission;
    CareSchedule schedule;
    Horse horse;
    StableStall quarantine;

    @BeforeEach
    void setUp() {
        service = new AdmissionReviewService(admissions, careSchedules, horses, stalls,
                careScheduleService, healthRecordRepository);
        admission = new AdmissionApplication();
        admission.setId(1L);
        admission.setHorseId(10L);
        admission.setQuarantineStallId(99L);
        admission.setStatus(AdmissionStatus.VET_REVIEW);

        schedule = new CareSchedule();
        schedule.setId(20L);
        schedule.setAdmissionId(1L);
        schedule.setHorseId(10L);
        schedule.setCareType(CareType.INITIAL);
        schedule.setStatus(CareScheduleStatus.SCHEDULED);
        schedule.setVeterinarianId(5L);

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
        mockScheduledLookup();
        HealthRecord record = new HealthRecord();
        record.setId(30L);
        record.setCareScheduleId(20L);
        when(healthRecordRepository.findByCareScheduleId(20L)).thenReturn(Optional.of(record));

        when(careScheduleService.completeCareSchedule(eq(20L), any(), eq(5L))).thenAnswer(invocation -> {
            admission.setStatus(AdmissionStatus.TRAINER_REVIEW);
            admission.setVetDecision(VetDecision.APPROVED);
            admission.setVetReviewedAt(LocalDateTime.now());
            schedule.setStatus(CareScheduleStatus.COMPLETED);
            return CareScheduleResponse.from(schedule);
        });

        var response = service.reviewByVet(1L, request(VetDecision.APPROVED), 5L);

        assertEquals(AdmissionStatus.TRAINER_REVIEW, response.status());
        assertEquals(HorseStatus.CANDIDATE, response.horseStatus());
        assertEquals(99L, response.quarantineStallId());
        assertEquals(CareScheduleStatus.COMPLETED, response.initialExamStatus());
        assertEquals(30L, response.healthRecordId());
        assertEquals(20L, response.vetExamId());
        verify(careScheduleService).startCareSchedule(20L, 5L);
    }

    @Test
    void recheckUsesInProgressScheduleAndDoesNotRestartExam() {
        // Simulate: admission is PENDING_RECHECK with a ROUTINE CareSchedule IN_PROGRESS
        admission.setStatus(AdmissionStatus.PENDING_RECHECK);
        CareSchedule routineSchedule = new CareSchedule();
        routineSchedule.setId(21L);
        routineSchedule.setAdmissionId(1L);
        routineSchedule.setHorseId(10L);
        routineSchedule.setCareType(CareType.ROUTINE);
        routineSchedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        routineSchedule.setVeterinarianId(5L);

        when(admissions.findById(1L)).thenReturn(Optional.of(admission));
        // findFirstByAdmissionIdAndStatusOrderByCreatedAtDesc returns the IN_PROGRESS routine schedule
        when(careSchedules.findFirstByAdmissionIdAndStatusOrderByCreatedAtDesc(1L, CareScheduleStatus.IN_PROGRESS))
                .thenReturn(Optional.of(routineSchedule));
        when(horses.findById(10L)).thenReturn(Optional.of(horse));
        when(stalls.findById(99L)).thenReturn(Optional.of(quarantine));
        when(healthRecordRepository.findByCareScheduleId(21L)).thenReturn(Optional.empty());

        when(careScheduleService.completeCareSchedule(eq(21L), any(), eq(5L))).thenAnswer(invocation -> {
            admission.setStatus(AdmissionStatus.TRAINER_REVIEW);
            admission.setVetDecision(VetDecision.APPROVED);
            admission.setVetReviewedAt(LocalDateTime.now());
            routineSchedule.setStatus(CareScheduleStatus.COMPLETED);
            return CareScheduleResponse.from(routineSchedule);
        });

        VetReviewRequest request = request(VetDecision.APPROVED);
        var response = service.reviewByVet(1L, request, 5L);

        assertEquals(AdmissionStatus.TRAINER_REVIEW, response.status());
        assertEquals(VetDecision.APPROVED, response.decision());
        verify(careScheduleService, never()).startCareSchedule(anyLong(), anyLong());
        ArgumentCaptor<CompleteCareScheduleRequest> completion = ArgumentCaptor.forClass(CompleteCareScheduleRequest.class);
        verify(careScheduleService).completeCareSchedule(eq(21L), completion.capture(), eq(5L));
        assertEquals(TrainingDecision.ALLOWED, completion.getValue().getTrainingDecision());
    }

    @Test
    void rejectedMapsLegacyFeedbackToBlockedTrainingDecision() {
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        // IN_PROGRESS found directly
        when(admissions.findById(1L)).thenReturn(Optional.of(admission));
        when(careSchedules.findFirstByAdmissionIdAndStatusOrderByCreatedAtDesc(1L, CareScheduleStatus.IN_PROGRESS))
                .thenReturn(Optional.of(schedule));
        when(horses.findById(10L)).thenReturn(Optional.of(horse));
        when(stalls.findById(99L)).thenReturn(Optional.of(quarantine));

        when(careScheduleService.completeCareSchedule(eq(20L), any(), eq(5L))).thenAnswer(invocation -> {
            CompleteCareScheduleRequest completion = invocation.getArgument(1);
            admission.setStatus(AdmissionStatus.REJECTED);
            admission.setVetDecision(VetDecision.REJECTED);
            admission.setVetFeedback(completion.getRestrictionDetails());
            admission.setVetReviewedAt(LocalDateTime.now());
            horse.setCurrentStatus(HorseStatus.REJECTED);
            horse.setCurrentStallId(null);
            schedule.setStatus(CareScheduleStatus.COMPLETED);
            return CareScheduleResponse.from(schedule);
        });
        when(healthRecordRepository.findByCareScheduleId(20L)).thenReturn(Optional.empty());

        VetReviewRequest request = request(VetDecision.REJECTED);
        request.setFeedback("Not safe for training");

        var response = service.reviewByVet(1L, request, 5L);

        assertEquals(AdmissionStatus.REJECTED, response.status());
        assertEquals("Not safe for training", response.feedback());
        ArgumentCaptor<CompleteCareScheduleRequest> completion = ArgumentCaptor.forClass(CompleteCareScheduleRequest.class);
        verify(careScheduleService).completeCareSchedule(eq(20L), completion.capture(), eq(5L));
        assertEquals(TrainingDecision.BLOCKED, completion.getValue().getTrainingDecision());
        assertEquals("Not safe for training", completion.getValue().getRestrictionDetails());
        assertTrue(completion.getValue().isRejectAdmission());
        assertEquals("Not safe for training", completion.getValue().getRejectionReason());
    }

    @Test
    void explicitRejectAdmissionWithBlockedDecisionMapsToCompletionRequest() {
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        when(admissions.findById(1L)).thenReturn(Optional.of(admission));
        when(careSchedules.findFirstByAdmissionIdAndStatusOrderByCreatedAtDesc(1L, CareScheduleStatus.IN_PROGRESS))
                .thenReturn(Optional.of(schedule));
        when(horses.findById(10L)).thenReturn(Optional.of(horse));
        when(stalls.findById(99L)).thenReturn(Optional.of(quarantine));

        when(careScheduleService.completeCareSchedule(eq(20L), any(), eq(5L))).thenAnswer(invocation -> {
            CompleteCareScheduleRequest comp = invocation.getArgument(1);
            admission.setStatus(AdmissionStatus.REJECTED);
            admission.setVetDecision(VetDecision.REJECTED);
            admission.setVetFeedback(comp.getRejectionReason());
            admission.setVetReviewedAt(LocalDateTime.now());
            horse.setCurrentStatus(HorseStatus.REJECTED);
            horse.setCurrentStallId(null);
            schedule.setStatus(CareScheduleStatus.COMPLETED);
            return CareScheduleResponse.from(schedule);
        });
        when(healthRecordRepository.findByCareScheduleId(20L)).thenReturn(Optional.empty());

        VetReviewRequest request = new VetReviewRequest();
        request.setPhysicalExamConfirmed(true);
        request.setFindings("Severe communicable infection detected");
        request.setTrainingDecision(TrainingDecision.BLOCKED);
        request.setRestrictionDetails("Permanent quarantine recommended");
        request.setRejectAdmission(true);
        request.setRejectionReason("Communicable disease outbreak prevention");

        var response = service.reviewByVet(1L, request, 5L);

        assertEquals(AdmissionStatus.REJECTED, response.status());
        assertEquals(VetDecision.REJECTED, response.decision());
        ArgumentCaptor<CompleteCareScheduleRequest> completion = ArgumentCaptor.forClass(CompleteCareScheduleRequest.class);
        verify(careScheduleService).completeCareSchedule(eq(20L), completion.capture(), eq(5L));
        assertTrue(completion.getValue().isRejectAdmission());
        assertEquals("Communicable disease outbreak prevention", completion.getValue().getRejectionReason());
        assertEquals(TrainingDecision.BLOCKED, completion.getValue().getTrainingDecision());
    }

    @Test
    void blockedTrainingDecisionWithoutRejectAdmissionAdvancesToTrainerReview() {
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        when(admissions.findById(1L)).thenReturn(Optional.of(admission));
        when(careSchedules.findFirstByAdmissionIdAndStatusOrderByCreatedAtDesc(1L, CareScheduleStatus.IN_PROGRESS))
                .thenReturn(Optional.of(schedule));
        when(horses.findById(10L)).thenReturn(Optional.of(horse));
        when(stalls.findById(99L)).thenReturn(Optional.of(quarantine));

        when(careScheduleService.completeCareSchedule(eq(20L), any(), eq(5L))).thenAnswer(invocation -> {
            CompleteCareScheduleRequest comp = invocation.getArgument(1);
            admission.setStatus(AdmissionStatus.TRAINER_REVIEW);
            admission.setVetDecision(VetDecision.APPROVED);
            admission.setVetFeedback(comp.getFindings());
            admission.setVetReviewedAt(LocalDateTime.now());
            schedule.setStatus(CareScheduleStatus.COMPLETED);
            return CareScheduleResponse.from(schedule);
        });
        when(healthRecordRepository.findByCareScheduleId(20L)).thenReturn(Optional.empty());

        VetReviewRequest request = new VetReviewRequest();
        request.setPhysicalExamConfirmed(true);
        request.setFindings("Splint inflammation; horse blocked from training but admitted");
        request.setTrainingDecision(TrainingDecision.BLOCKED);
        request.setRestrictionDetails("No track work for 30 days");
        request.setRejectAdmission(false);

        var response = service.reviewByVet(1L, request, 5L);

        assertEquals(AdmissionStatus.TRAINER_REVIEW, response.status());
        assertEquals(VetDecision.APPROVED, response.decision());
        ArgumentCaptor<CompleteCareScheduleRequest> completion = ArgumentCaptor.forClass(CompleteCareScheduleRequest.class);
        verify(careScheduleService).completeCareSchedule(eq(20L), completion.capture(), eq(5L));
        assertFalse(completion.getValue().isRejectAdmission());
        assertEquals(TrainingDecision.BLOCKED, completion.getValue().getTrainingDecision());
    }

    @Test
    void assignedVetFailureFromStateMachineIsPreserved() {
        when(admissions.findById(1L)).thenReturn(Optional.of(admission));
        // No IN_PROGRESS schedule found
        when(careSchedules.findFirstByAdmissionIdAndStatusOrderByCreatedAtDesc(1L, CareScheduleStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        // Falls back to INITIAL scheduled lookup
        when(careSchedules.findFirstByAdmissionIdAndCareTypeOrderByCreatedAtDesc(1L, CareType.INITIAL))
                .thenReturn(Optional.of(schedule));
        when(careScheduleService.startCareSchedule(20L, 6L)).thenThrow(new ApiException(
                HttpStatus.FORBIDDEN, "FORBIDDEN", "Only assigned veterinarian can start examination"));

        ApiException error = assertThrows(ApiException.class,
                () -> service.reviewByVet(1L, request(VetDecision.APPROVED), 6L));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
        assertEquals("FORBIDDEN", error.getErrorCode());
        verify(careScheduleService, never()).completeCareSchedule(anyLong(), any(), anyLong());
    }

    @Test
    void requestedExaminationCannotBypassOfferAcceptance() {
        assertPendingReviewRejected(CareScheduleStatus.REQUESTED);
    }

    @Test
    void awaitingConfirmationCannotBypassOfferAcceptance() {
        assertPendingReviewRejected(CareScheduleStatus.AWAITING_VET_CONFIRMATION);
    }

    private void assertPendingReviewRejected(CareScheduleStatus status) {
        schedule.setStatus(status);
        schedule.setVeterinarianId(null);
        when(admissions.findById(1L)).thenReturn(Optional.of(admission));
        when(careSchedules.findFirstByAdmissionIdAndCareTypeOrderByCreatedAtDesc(1L, CareType.INITIAL))
                .thenReturn(Optional.of(schedule));
        ApiException error = assertThrows(ApiException.class,
                () -> service.reviewByVet(1L, request(VetDecision.APPROVED), 5L));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        assertNull(schedule.getVeterinarianId());
        verify(careScheduleService, never()).startCareSchedule(anyLong(), anyLong());
        verify(careScheduleService, never()).completeCareSchedule(anyLong(), any(), anyLong());
        verify(careSchedules, never()).save(any());
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
            // followUpDate is now optional per R1
            assertTrue(validator.validate(recheck).isEmpty());
            // If provided in the past, it should be rejected
            recheck.setFollowUpDate(LocalDate.now().minusDays(1));
            assertFalse(validator.validate(recheck).isEmpty());
            // If provided in the future, it is valid
            recheck.setFollowUpDate(LocalDate.now().plusDays(1));
            assertTrue(validator.validate(recheck).isEmpty());

            // TrainingDecision.RESTRICTED requires rejectionReason, feedback, or restrictionDetails
            VetReviewRequest restricted = new VetReviewRequest();
            restricted.setPhysicalExamConfirmed(true);
            restricted.setFindings("Mild lameness observed");
            restricted.setTrainingDecision(TrainingDecision.RESTRICTED);
            assertFalse(validator.validate(restricted).isEmpty(),
                    "RESTRICTED trainingDecision without restriction details or feedback must fail validation");

            restricted.setRestrictionDetails("Light training only, avoid jumping");
            assertTrue(validator.validate(restricted).isEmpty(),
                    "RESTRICTED trainingDecision with restrictionDetails must pass validation");

            // TrainingDecision.BLOCKED requires rejectionReason, feedback, or restrictionDetails
            VetReviewRequest blocked = new VetReviewRequest();
            blocked.setPhysicalExamConfirmed(true);
            blocked.setFindings("Severe joint inflammation");
            blocked.setTrainingDecision(TrainingDecision.BLOCKED);
            assertFalse(validator.validate(blocked).isEmpty(),
                    "BLOCKED trainingDecision without restriction details or feedback must fail validation");

            blocked.setRestrictionDetails("No exercise for 4 weeks");
            assertTrue(validator.validate(blocked).isEmpty(),
                    "BLOCKED trainingDecision with restrictionDetails must pass validation");
        }
    }

    @Test
    void completeCareScheduleRequestValidationEnforcesRejectionReason() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();

            CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
            req.setFindings("Findings");
            req.setDiagnosis("Diagnosis");
            req.setTrainingDecision(TrainingDecision.BLOCKED);
            req.setRestrictionDetails("Details");
            req.setRejectAdmission(true);
            req.setRejectionReason(null);

            var violations = validator.validate(req);
            assertFalse(violations.isEmpty(), "rejectAdmission=true without rejectionReason must fail validation");

            req.setRejectionReason("Infectious condition");
            assertTrue(validator.validate(req).isEmpty(), "valid rejectAdmission request must pass");
        }
    }

    /**
     * Helper for tests where no IN_PROGRESS schedule exists but a SCHEDULED one does
     * (service should start it then complete).
     */
    private void mockScheduledLookup() {
        when(admissions.findById(1L)).thenReturn(Optional.of(admission));
        when(careSchedules.findFirstByAdmissionIdAndStatusOrderByCreatedAtDesc(1L, CareScheduleStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(careSchedules.findFirstByAdmissionIdAndCareTypeOrderByCreatedAtDesc(1L, CareType.INITIAL))
                .thenReturn(Optional.of(schedule));
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

    @Test
    void selectedScheduleFromAnotherAdmissionCannotBeReviewed() {
        when(admissions.findById(1L)).thenReturn(Optional.of(admission));
        schedule.setAdmissionId(2L);
        when(careSchedules.findById(20L)).thenReturn(Optional.of(schedule));
        VetReviewRequest review = request(VetDecision.APPROVED);
        review.setCareScheduleId(20L);
        assertEquals("INVALID_REVIEW_STATE", assertThrows(ApiException.class,
                () -> service.reviewByVet(1L, review, 5L)).getErrorCode());
        verifyNoInteractions(careScheduleService);
    }

    @Test
    void urgentExaminationCannotReplaceInitialAdmissionReview() {
        when(admissions.findById(1L)).thenReturn(Optional.of(admission));
        schedule.setCareType(CareType.URGENT);
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        when(careSchedules.findById(20L)).thenReturn(Optional.of(schedule));
        VetReviewRequest review = request(VetDecision.APPROVED);
        review.setCareScheduleId(20L);
        assertThrows(ApiException.class, () -> service.reviewByVet(1L, review, 5L));
        verifyNoInteractions(careScheduleService);
    }
}
