package com.rtms.backend.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.dto.CareScheduleResponse;
import com.rtms.backend.dto.CompleteCareScheduleRequest;
import com.rtms.backend.dto.CreateNextScheduleRequest;
import com.rtms.backend.dto.VetReviewRequest;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    void allowedExamStartsScheduledExamAndReturnsTrainerReviewState() {
        mockScheduledLookup();
        HealthRecord record = new HealthRecord();
        record.setId(30L);
        record.setCareScheduleId(20L);
        when(healthRecordRepository.findByCareScheduleId(20L)).thenReturn(Optional.of(record));

        when(careScheduleService.completeCareSchedule(eq(20L), any(), eq(5L))).thenAnswer(invocation -> {
            admission.setStatus(AdmissionStatus.TRAINER_REVIEW);
            admission.setVetReviewedAt(LocalDateTime.now());
            schedule.setStatus(CareScheduleStatus.COMPLETED);
            return CareScheduleResponse.from(schedule);
        });

        var response = service.reviewByVet(1L, request(TrainingDecision.ALLOWED), 5L);

        assertEquals(AdmissionStatus.TRAINER_REVIEW, response.status());
        assertEquals(HorseStatus.CANDIDATE, response.horseStatus());
        assertEquals(99L, response.quarantineStallId());
        assertEquals("Q-1", response.quarantineStallCode());
        assertEquals(CareScheduleStatus.COMPLETED, response.initialExamStatus());
        assertEquals(30L, response.healthRecordId());
        assertEquals(20L, response.vetExamId());
        assertEquals(20L, response.careScheduleId());
        assertEquals(TrainingDecision.ALLOWED, response.trainingDecision());
        verify(careScheduleService).startCareSchedule(20L, 5L);
    }

    @Test
    void missingTrainingDecisionIsRejectedBeforeAnyLookup() {
        VetReviewRequest request = request(null);

        ApiException error = assertThrows(ApiException.class, () -> service.reviewByVet(1L, request, 5L));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verifyNoInteractions(admissions, careScheduleService);
    }

    @Test
    void blockedDecisionAdvancesToTrainerReviewAndForwardsFollowUp() {
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        when(admissions.findById(1L)).thenReturn(Optional.of(admission));
        when(careSchedules.findFirstByAdmissionIdAndStatusOrderByCreatedAtDesc(1L, CareScheduleStatus.IN_PROGRESS))
                .thenReturn(Optional.of(schedule));
        when(horses.findById(10L)).thenReturn(Optional.of(horse));
        when(stalls.findById(99L)).thenReturn(Optional.of(quarantine));

        when(careScheduleService.completeCareSchedule(eq(20L), any(), eq(5L))).thenAnswer(invocation -> {
            CompleteCareScheduleRequest comp = invocation.getArgument(1);
            admission.setStatus(AdmissionStatus.TRAINER_REVIEW);
            admission.setVetFeedback(comp.getFindings());
            admission.setVetReviewedAt(LocalDateTime.now());
            schedule.setStatus(CareScheduleStatus.COMPLETED);
            horse.setTrainingDecision(TrainingDecision.BLOCKED);
            return CareScheduleResponse.from(schedule);
        });
        when(healthRecordRepository.findByCareScheduleId(20L)).thenReturn(Optional.empty());

        CreateNextScheduleRequest next = followUp();
        VetReviewRequest request = request(TrainingDecision.BLOCKED);
        request.setFindings("Splint inflammation; horse blocked from training but admitted");
        request.setRestrictionDetails("No track work for 30 days");
        request.setNextSchedule(next);

        var response = service.reviewByVet(1L, request, 5L);

        // Thú y không từ chối đơn: kết luận BLOCKED vẫn chuyển đơn sang Trainer.
        assertEquals(AdmissionStatus.TRAINER_REVIEW, response.status());
        assertEquals(TrainingDecision.BLOCKED, response.trainingDecision());
        assertEquals("No track work for 30 days", response.restrictionDetails());
        assertEquals(20L, response.careScheduleId());

        ArgumentCaptor<CompleteCareScheduleRequest> completion = ArgumentCaptor.forClass(CompleteCareScheduleRequest.class);
        verify(careScheduleService).completeCareSchedule(eq(20L), completion.capture(), eq(5L));
        assertEquals(TrainingDecision.BLOCKED, completion.getValue().getTrainingDecision());
        assertEquals("No track work for 30 days", completion.getValue().getRestrictionDetails());
        assertSame(next, completion.getValue().getNextSchedule());
    }

    @Test
    void blockedDecisionFallsBackToFeedbackAsRestrictionDetails() {
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        when(admissions.findById(1L)).thenReturn(Optional.of(admission));
        when(careSchedules.findFirstByAdmissionIdAndStatusOrderByCreatedAtDesc(1L, CareScheduleStatus.IN_PROGRESS))
                .thenReturn(Optional.of(schedule));
        when(horses.findById(10L)).thenReturn(Optional.of(horse));
        when(stalls.findById(99L)).thenReturn(Optional.of(quarantine));
        when(careScheduleService.completeCareSchedule(eq(20L), any(), eq(5L)))
                .thenReturn(CareScheduleResponse.from(schedule));

        HealthRecord hr = new HealthRecord();
        hr.setId(31L);
        hr.setCareScheduleId(20L);
        hr.setTrainingDecision(TrainingDecision.BLOCKED);
        hr.setRestrictionDetails("Light walking only");
        when(healthRecordRepository.findByCareScheduleId(20L)).thenReturn(Optional.of(hr));

        VetReviewRequest request = request(TrainingDecision.BLOCKED);
        request.setFeedback("Light walking only");
        request.setNextSchedule(followUp());

        var response = service.reviewByVet(1L, request, 5L);

        ArgumentCaptor<CompleteCareScheduleRequest> completion = ArgumentCaptor.forClass(CompleteCareScheduleRequest.class);
        verify(careScheduleService).completeCareSchedule(eq(20L), completion.capture(), eq(5L));
        assertEquals("Light walking only", completion.getValue().getRestrictionDetails());
        // Phản hồi lấy theo bệnh án đã lưu.
        assertEquals(TrainingDecision.BLOCKED, response.trainingDecision());
        assertEquals("Light walking only", response.restrictionDetails());
        assertEquals(31L, response.healthRecordId());
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
                () -> service.reviewByVet(1L, request(TrainingDecision.ALLOWED), 6L));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
        assertEquals("FORBIDDEN", error.getErrorCode());
        verify(careScheduleService, never()).completeCareSchedule(anyLong(), any(), anyLong());
    }

    @Test
    void requestedExaminationCannotBypassAutomaticAssignment() {
        schedule.setStatus(CareScheduleStatus.REQUESTED);
        schedule.setVeterinarianId(null);
        when(admissions.findById(1L)).thenReturn(Optional.of(admission));
        when(careSchedules.findFirstByAdmissionIdAndCareTypeOrderByCreatedAtDesc(1L, CareType.INITIAL))
                .thenReturn(Optional.of(schedule));

        ApiException error = assertThrows(ApiException.class,
                () -> service.reviewByVet(1L, request(TrainingDecision.ALLOWED), 5L));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        assertNull(schedule.getVeterinarianId());
        verify(careScheduleService, never()).startCareSchedule(anyLong(), anyLong());
        verify(careScheduleService, never()).completeCareSchedule(anyLong(), any(), anyLong());
        verify(careSchedules, never()).save(any());
    }

    @Test
    void vetReviewRequestValidation() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();

            VetReviewRequest allowed = request(TrainingDecision.ALLOWED);
            assertTrue(validator.validate(allowed).isEmpty());
            allowed.setFindings(" ");
            assertFalse(validator.validate(allowed).isEmpty(), "Findings are required");

            assertFalse(validator.validate(request(null)).isEmpty(), "Training decision is required");

            VetReviewRequest blocked = request(TrainingDecision.BLOCKED);
            assertFalse(validator.validate(blocked).isEmpty(),
                    "BLOCKED without restriction details or feedback must fail validation");
            blocked.setRestrictionDetails("No exercise for 4 weeks");
            assertTrue(validator.validate(blocked).isEmpty(),
                    "BLOCKED with restrictionDetails passes; the follow-up rule is enforced by CareScheduleService");
        }
    }

    @Test
    void completeCareScheduleRequestValidationRequiresFollowUpWhenBlocked() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();

            CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
            req.setFindings("Findings");
            req.setDiagnosis("Diagnosis");
            req.setTrainingDecision(TrainingDecision.BLOCKED);
            req.setRestrictionDetails("Box rest");
            assertFalse(validator.validate(req).isEmpty(), "BLOCKED without nextSchedule must fail validation");

            req.setNextSchedule(followUp());
            assertTrue(validator.validate(req).isEmpty(), "BLOCKED with restriction and nextSchedule is valid");

            req.setRestrictionDetails(" ");
            assertFalse(validator.validate(req).isEmpty(), "BLOCKED without restriction details must fail validation");

            CompleteCareScheduleRequest allowed = new CompleteCareScheduleRequest();
            allowed.setFindings("Findings");
            allowed.setDiagnosis("Diagnosis");
            allowed.setTrainingDecision(TrainingDecision.ALLOWED);
            assertTrue(validator.validate(allowed).isEmpty(), "ALLOWED needs no follow-up");
        }
    }

    @Test
    void selectedScheduleFromAnotherAdmissionCannotBeReviewed() {
        when(admissions.findById(1L)).thenReturn(Optional.of(admission));
        schedule.setAdmissionId(2L);
        when(careSchedules.findById(20L)).thenReturn(Optional.of(schedule));
        VetReviewRequest review = request(TrainingDecision.ALLOWED);
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
        VetReviewRequest review = request(TrainingDecision.ALLOWED);
        review.setCareScheduleId(20L);
        assertThrows(ApiException.class, () -> service.reviewByVet(1L, review, 5L));
        verifyNoInteractions(careScheduleService);
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

    private VetReviewRequest request(TrainingDecision decision) {
        VetReviewRequest request = new VetReviewRequest();
        request.setTrainingDecision(decision);
        request.setPhysicalExamConfirmed(true);
        request.setFindings("Clinical findings");
        return request;
    }

    private CreateNextScheduleRequest followUp() {
        CreateNextScheduleRequest next = new CreateNextScheduleRequest();
        next.setHorseId(10L);
        next.setCareType(CareType.ROUTINE);
        next.setDescription("Recheck");
        next.setScheduledDate(LocalDate.now().plusDays(7).toString());
        return next;
    }
}
