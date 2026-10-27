package com.rtms.backend.admission.service;
import com.rtms.backend.admission.dto.VetReviewRequest;
import com.rtms.backend.admission.dto.VetReviewResponse;
import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.dto.CareScheduleResponse;
import com.rtms.backend.medical.dto.CompleteCareScheduleRequest;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.medical.service.CareScheduleService;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.enums.TrainingDecision;


import com.rtms.backend.config.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Objects;

/** Compatibility adapter for Admission Vet UI delegating to CareScheduleService */
@Service
public class AdmissionReviewService {
    private final AdmissionApplicationRepository admissions;
    private final CareScheduleRepository careSchedules;
    private final HorseRepository horses;
    private final StableStallRepository stalls;
    private final CareScheduleService careScheduleService;
    private final HealthRecordRepository healthRecordRepository;

    @Autowired
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
        if (request.getTrainingDecision() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Training decision is required");
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
        if (!Objects.equals(schedule.getAdmissionId(), admissionId)
                || !Objects.equals(schedule.getHorseId(), before.getHorseId())
                || (before.getStatus() == AdmissionStatus.VET_REVIEW && schedule.getCareType() != CareType.INITIAL)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_REVIEW_STATE", "Examination does not belong to this admission review");
        }
        if (schedule.getStatus() == CareScheduleStatus.REQUESTED) {
            Long assignedVetId = schedule.getVeterinarianId() != null
                    ? schedule.getVeterinarianId()
                    : before.getVeterinarianId();

            if (assignedVetId == null) {
                schedule.setVeterinarianId(actorId);
                schedule.setScheduledAt(LocalDateTime.now());
                schedule.setStatus(CareScheduleStatus.SCHEDULED);
                careSchedules.save(schedule);
                careScheduleService.startCareSchedule(schedule.getId(), actorId);
            } else if (Objects.equals(assignedVetId, actorId)) {
                schedule.setVeterinarianId(actorId);
                if (schedule.getScheduledAt() == null) {
                    schedule.setScheduledAt(LocalDateTime.now());
                }
                schedule.setStatus(CareScheduleStatus.SCHEDULED);
                careSchedules.save(schedule);
                careScheduleService.startCareSchedule(schedule.getId(), actorId);
            } else {
                throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                        "Only the assigned veterinarian can review this examination");
            }
        } else if (schedule.getStatus() == CareScheduleStatus.SCHEDULED) {
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
        compReq.setMetrics(request.getMetrics());
        compReq.setNextSchedule(request.getNextSchedule());
        compReq.setVaccinations(request.getVaccinations());
        compReq.setTrainingDecision(request.getTrainingDecision());
        compReq.setRestrictionDetails(
                request.getRestrictionDetails() != null && !request.getRestrictionDetails().isBlank()
                        ? request.getRestrictionDetails()
                        : request.getFeedback());
        return compReq;
    }

}
