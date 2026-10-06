package com.rtms.backend.service;
import com.rtms.backend.dto.VetReviewRequest;
import com.rtms.backend.dto.VetReviewResponse;
import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.enums.VetDecision;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.dto.CareScheduleResponse;
import com.rtms.backend.dto.CompleteCareScheduleRequest;
import com.rtms.backend.entity.CareSchedule;
import com.rtms.backend.entity.HealthRecord;
import com.rtms.backend.enums.CareScheduleStatus;
import com.rtms.backend.enums.CareType;
import com.rtms.backend.repository.CareScheduleRepository;
import com.rtms.backend.repository.HealthRecordRepository;
import com.rtms.backend.service.CareScheduleService;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.entity.StableStall;
import com.rtms.backend.repository.StableStallRepository;
import com.rtms.backend.enums.TrainingDecision;
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
        AdmissionApplication before = admissions.findById(admissionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Admission not found"));
        if (before.getStatus() != AdmissionStatus.VET_REVIEW && before.getStatus() != AdmissionStatus.PENDING_RECHECK) {
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
                    "Accept the examination offer and start it before submitting a review");
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

        // Map the trainingDecision back to a VetDecision for the response
        VetDecision responseDecision = admission.getVetDecision();

        return new VetReviewResponse(admission.getId(), admission.getStatus(), actorId,
                responseDecision, admission.getVetFeedback(), admission.getVetReviewedAt(),
                horse.getId(), horse.getCurrentStatus(), admission.getQuarantineStallId(),
                qStallCode, CareScheduleStatus.COMPLETED, hr != null ? hr.getId() : null, completed.id());
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

        if (Boolean.TRUE.equals(request.getRejectAdmission())
                || (request.getDecision() == VetDecision.REJECTED && !Boolean.FALSE.equals(request.getRejectAdmission()))) {
            compReq.setRejectAdmission(true);
            compReq.setRejectionReason(request.getRejectionReason() != null ? request.getRejectionReason()
                    : (request.getFeedback() != null ? request.getFeedback() : "Admission rejected by veterinarian"));
        } else {
            compReq.setRejectAdmission(false);
        }

        if (request.getTrainingDecision() != null) {
            compReq.setTrainingDecision(request.getTrainingDecision());
            compReq.setRestrictionDetails(
                    request.getRestrictionDetails() != null ? request.getRestrictionDetails() : request.getFeedback());
        } else if (request.getDecision() == VetDecision.APPROVED) {
            compReq.setTrainingDecision(TrainingDecision.ALLOWED);
        } else if (request.getDecision() == VetDecision.RECHECK_REQUIRED) {
            compReq.setTrainingDecision(TrainingDecision.RESTRICTED);
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
