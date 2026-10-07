package com.rtms.backend.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.dto.*;
import com.rtms.backend.entity.*;
import com.rtms.backend.enums.*;
import com.rtms.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/** Compatibility adapter for Admission Vet UI delegating to CareScheduleService */
@Service
public class AdmissionReviewService {
    private final AdmissionApplicationRepository admissions;
    private final CareScheduleRepository careSchedules;
    private final HorseRepository horses;
    private final StableStallRepository stalls;
    private final CareScheduleService careScheduleService;
    private final HealthRecordRepository healthRecordRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public AdmissionReviewService(AdmissionApplicationRepository admissions,
            CareScheduleRepository careSchedules,
            HorseRepository horses, StableStallRepository stalls,
            CareScheduleService careScheduleService,
            HealthRecordRepository healthRecordRepository) {
        this.admissions = admissions;
        this.careSchedules = careSchedules;
        this.horses = horses;
        this.stalls = stalls;
        this.careScheduleService = careScheduleService;
        this.healthRecordRepository = healthRecordRepository;
    }


    @Transactional
    public VetReviewResponse reviewByVet(Long admissionId, VetReviewRequest request, Long actorId) {
        if (Boolean.TRUE.equals(request.getRejectAdmission()) || request.getDecision() == VetDecision.REJECTED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "VET_CANNOT_REJECT_ADMISSION",
                    "Veterinarians cannot reject admissions; only medical training decisions (ALLOWED, BLOCKED) are permitted.");
        }
        if (request.getFollowUpDate() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LEGACY_FIELD_NOT_SUPPORTED",
                    "followUpDate is deprecated and not supported; schedule follow-up examinations via nextSchedule instead.");
        }

        AdmissionApplication before = admissions.findById(admissionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Admission not found"));
        if (before.getStatus() != AdmissionStatus.VET_REVIEW) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_REVIEW_STATE", "Admission is not awaiting a vet examination");
        }

        CareSchedule schedule;
        if (request.getCareScheduleId() != null) {
            schedule = careSchedules.findById(request.getCareScheduleId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND", "Care schedule not found"));
        } else {
            schedule = careSchedules.findFirstByAdmissionIdAndStatusOrderByCreatedAtDesc(
                    admissionId, CareScheduleStatus.IN_PROGRESS)
                    .orElseGet(() -> careSchedules.findFirstByAdmissionIdAndCareTypeOrderByCreatedAtDesc(
                            admissionId, CareType.INITIAL)
                            .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "INVALID_REVIEW_STATE", "No active examination found")));
        }
        if (!java.util.Objects.equals(schedule.getAdmissionId(), admissionId)
                || !java.util.Objects.equals(schedule.getHorseId(), before.getHorseId())
                || (before.getStatus() == AdmissionStatus.VET_REVIEW && schedule.getCareType() != CareType.INITIAL)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_REVIEW_STATE", "Examination does not belong to this admission review");
        }
        if (schedule.getStatus() == CareScheduleStatus.SCHEDULED) {
            careScheduleService.startCareSchedule(schedule.getId(), actorId);
        } else if (schedule.getStatus() != CareScheduleStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_REVIEW_STATE",
                    "Start the examination before submitting a review");
        }

        return completeCareScheduleReview(admissionId, schedule, request, actorId);
    }

    private VetReviewResponse completeCareScheduleReview(Long admissionId, CareSchedule schedule,
            VetReviewRequest request, Long actorId) {
        CompleteCareScheduleRequest compReq = buildCompleteCareScheduleRequest(request);
        CareScheduleResponse completed = careScheduleService.completeCareSchedule(schedule.getId(), compReq, actorId);

        AdmissionApplication admission = admissions.findById(admissionId).orElseThrow();
        Horse horse = horses.findById(admission.getHorseId()).orElseThrow();
        String qStallCode = Optional.ofNullable(admission.getQuarantineStallId())
                .flatMap(stalls::findById).map(StableStall::getStallCode).orElse(null);

        HealthRecord hr = healthRecordRepository.findByCareScheduleId(schedule.getId()).orElse(null);

        // Map the trainingDecision back to a VetDecision for the response (nullable per modern workflow)
        VetDecision responseDecision = admission.getVetDecision();
        TrainingDecision trainingDecision = hr != null && hr.getTrainingDecision() != null
                ? hr.getTrainingDecision()
                : compReq.getTrainingDecision();
        String restrictionDetails = hr != null && hr.getRestrictionDetails() != null
                ? hr.getRestrictionDetails()
                : compReq.getRestrictionDetails();
        Long careScheduleId = completed != null ? completed.id() : schedule.getId();
        Long vetExamId = careScheduleId;

        return new VetReviewResponse(
                admission.getId(),
                admission.getStatus(),
                actorId,
                responseDecision,
                trainingDecision,
                restrictionDetails,
                admission.getVetFeedback(),
                admission.getVetReviewedAt(),
                horse.getId(),
                horse.getCurrentStatus(),
                admission.getQuarantineStallId(),
                qStallCode,
                CareScheduleStatus.COMPLETED,
                hr != null ? hr.getId() : null,
                vetExamId,
                careScheduleId
        );
    }

    private CompleteCareScheduleRequest buildCompleteCareScheduleRequest(VetReviewRequest request) {
        CompleteCareScheduleRequest compReq = new CompleteCareScheduleRequest();
        compReq.setFindings(request.getFindings() != null ? request.getFindings() : "Exam completed");
        compReq.setDiagnosis(request.getDiagnosis() != null ? request.getDiagnosis() : "Quarantine physical exam");
        compReq.setTreatment(request.getTreatment());
        compReq.setSymptoms(request.getSymptoms());
        compReq.setNotes(request.getNotes() != null ? request.getNotes() : request.getFeedback());
        compReq.setFollowUpDate(request.getFollowUpDate());
        compReq.setMetrics(request.getMetrics());
        compReq.setNextSchedule(request.getNextSchedule());
        compReq.setRejectAdmission(false);

        if (request.getTrainingDecision() != null) {
            compReq.setTrainingDecision(request.getTrainingDecision());
            compReq.setRestrictionDetails(
                    request.getRestrictionDetails() != null ? request.getRestrictionDetails() : request.getFeedback());
        } else if (request.getDecision() == VetDecision.APPROVED) {
            compReq.setTrainingDecision(TrainingDecision.ALLOWED);
        } else if (request.getDecision() == VetDecision.RECHECK_REQUIRED) {
            compReq.setTrainingDecision(TrainingDecision.BLOCKED);
            compReq.setRestrictionDetails(request.getFeedback() != null ? request.getFeedback() : "Recheck required");
        } else {
            compReq.setTrainingDecision(TrainingDecision.BLOCKED);
            compReq.setRestrictionDetails(
                    request.getRejectionReason() != null ? request.getRejectionReason() : request.getFeedback());
        }

        // Ensure restriction details are present for non-ALLOWED decisions
        if (compReq.getTrainingDecision() != TrainingDecision.ALLOWED
                && (compReq.getRestrictionDetails() == null || compReq.getRestrictionDetails().isBlank())) {
            compReq.setRestrictionDetails(request.getRejectionReason() != null ? request.getRejectionReason()
                    : (request.getFeedback() != null ? request.getFeedback() : "Training restricted by veterinarian"));
        }

        return compReq;
    }

}
