package com.rtms.backend.admission.service;
import com.rtms.backend.admission.dto.VetReviewRequest;
import com.rtms.backend.admission.dto.VetReviewResponse;
import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.enums.ReviewDecision;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.audit.dto.DomainAuditEvent;
import com.rtms.backend.audit.service.AuditLogService;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
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
import com.rtms.backend.stable.enums.StallStatus;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.enums.TrainingDecision;

import com.rtms.backend.config.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final AdmissionNotificationService admissionNotificationService;
    private final AdmissionGroomReviewService admissionGroomReviewService;
    private final AuditLogService auditLogService;

    @Autowired
    public AdmissionReviewService(AdmissionApplicationRepository admissions,
            CareScheduleRepository careSchedules,
            HorseRepository horses, StableStallRepository stalls,
            CareScheduleService careScheduleService,
            HealthRecordRepository healthRecordRepository,
            AdmissionNotificationService admissionNotificationService,
            AdmissionGroomReviewService admissionGroomReviewService,
            AuditLogService auditLogService) {
        this.admissions = admissions;
        this.careSchedules = careSchedules;
        this.horses = horses;
        this.stalls = stalls;
        this.careScheduleService = careScheduleService;
        this.healthRecordRepository = healthRecordRepository;
        this.admissionNotificationService = admissionNotificationService;
        this.admissionGroomReviewService = admissionGroomReviewService;
        this.auditLogService = auditLogService;
    }

    public AdmissionReviewService(AdmissionApplicationRepository admissions,
            CareScheduleRepository careSchedules,
            HorseRepository horses, StableStallRepository stalls,
            CareScheduleService careScheduleService,
            HealthRecordRepository healthRecordRepository,
            AdmissionNotificationService admissionNotificationService,
            AdmissionGroomReviewService admissionGroomReviewService) {
        this(admissions, careSchedules, horses, stalls, careScheduleService, healthRecordRepository,
                admissionNotificationService, admissionGroomReviewService, null);
    }

    public AdmissionReviewService(AdmissionApplicationRepository admissions,
            CareScheduleRepository careSchedules,
            HorseRepository horses, StableStallRepository stalls,
            CareScheduleService careScheduleService,
            HealthRecordRepository healthRecordRepository) {
        this(admissions, careSchedules, horses, stalls, careScheduleService, healthRecordRepository, null, null, null);
    }


    @Transactional
    public VetReviewResponse reviewByVet(Long admissionId, VetReviewRequest request, Long actorId) {
        boolean isReject = request.getDecision() == ReviewDecision.REJECT
                || request.getDecision() == ReviewDecision.REJECTED;
        if (request.getTrainingDecision() == null && !isReject) {
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
        if (!java.util.Objects.equals(schedule.getAdmissionId(), admissionId)
                || !java.util.Objects.equals(schedule.getHorseId(), before.getHorseId())
                || (before.getStatus() == AdmissionStatus.VET_REVIEW && schedule.getCareType() != CareType.INITIAL)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_REVIEW_STATE", "Examination does not belong to this admission review");
        }
        if (schedule.getStatus() == CareScheduleStatus.SCHEDULED) {
            Long assignedVetId = schedule.getVeterinarianId() != null
                    ? schedule.getVeterinarianId()
                    : before.getVeterinarianId();

            if (assignedVetId == null || java.util.Objects.equals(assignedVetId, actorId)) {
                schedule.setVeterinarianId(actorId);
                if (schedule.getScheduledAt() == null) {
                    schedule.setScheduledAt(java.time.LocalDateTime.now());
                }
                careSchedules.save(schedule);
                careScheduleService.startCareSchedule(schedule.getId(), actorId);
            } else {
                throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                        "Only the assigned veterinarian can review this examination");
            }
        } else if (!CareScheduleStatus.ACTIVE_EXAM.contains(schedule.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_REVIEW_STATE",
                    "Start the examination before submitting a review");
        }

        if (isReject) {
            return rejectByVet(admissionId, schedule, request, actorId);
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

        if (auditLogService != null) {
            String noteOrReason = request.getNotes() != null && !request.getNotes().isBlank()
                    ? request.getNotes()
                    : request.getFeedback();
            auditLogService.recordDomainEvent(DomainAuditEvent.builder()
                    .userId(actorId)
                    .action("VET_INTAKE_EXAM")
                    .entityName("AdmissionApplication")
                    .entityId(admissionId)
                    .module("MEDICAL")
                    .summary("Veterinarian completed intake exam and evaluated training eligibility as " + trainingDecision)
                    .reason(noteOrReason)
                    .targetUrl("/manager/admissions/" + admissionId)
                    .before(Map.of(
                            "admissionStatus", "VET_REVIEW",
                            "horseStatus", horse.getCurrentStatus() != null ? horse.getCurrentStatus().name() : "PENDING"
                    ))
                    .after(Map.of(
                            "admissionStatus", admission.getStatus().name(),
                            "trainingDecision", trainingDecision.name(),
                            "horseStatus", horse.getCurrentStatus() != null ? horse.getCurrentStatus().name() : "PENDING"
                    ))
                    .build());
        }

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
        compReq.setNotes(request.getNotes() != null ? request.getNotes() : request.getFeedback());
        compReq.setMetrics(request.getMetrics());
        compReq.setInjuryMarkers(request.getInjuryMarkers());
        compReq.setNextSchedule(request.getNextSchedule());
        compReq.setVaccinations(request.getVaccinations());
        compReq.setTrainingDecision(request.getTrainingDecision());
        compReq.setRestrictionDetails(
                request.getRestrictionDetails() != null && !request.getRestrictionDetails().isBlank()
                        ? request.getRestrictionDetails()
                        : request.getFeedback());
        return compReq;
    }

    private VetReviewResponse rejectByVet(Long admissionId, CareSchedule schedule,
            VetReviewRequest request, Long actorId) {
        if (!Objects.equals(schedule.getVeterinarianId(), actorId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Only the assigned veterinarian can reject this admission");
        }

        String reason = request.getRejectReason();
        if (reason == null || reason.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "A rejection reason is required");
        }
        reason = reason.trim();

        AdmissionApplication admission = admissions.findById(admissionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Admission not found"));
        Long horseId = admission.getHorseId();
        Horse horse = horseId != null ? horses.findById(horseId).orElse(null) : null;
        LocalDateTime now = LocalDateTime.now();

        // AD-022/AD-025 rule 6: mở rộng như nghiệp vụ cũ (mọi lịch còn mở của ngựa),
        // nhưng xóa cứng thay vì CANCELLED. NOT EXISTS trong hàm xóa bảo vệ ca URGENT
        // còn horse_incident_reports / health_records tham chiếu — những ca đó ở lại.
        if (horseId != null) {
            careSchedules.deleteOpenSchedulesIfUnreferenced(horseId, CareScheduleStatus.OPEN);
        }

        // Release the quarantine stall if one is recorded for this admission.
        Long quarantineStallId = admission.getQuarantineStallId();
        if (quarantineStallId != null) {
            StableStall quarantineStall = stalls.findById(quarantineStallId).orElse(null);
            if (quarantineStall != null) {
                quarantineStall.setStatus(StallStatus.AVAILABLE);
                stalls.save(quarantineStall);
            }
        }

        if (horse != null) {
            horse.setCurrentStatus(HorseStatus.REJECTED);
            horse.setCurrentStallId(null);
            horses.save(horse);
        }

        admission.setVeterinarianId(actorId);
        admission.setVetReviewedAt(now);
        admission.setVetFeedback(reason);
        // vetTrainingDecision intentionally left null: the horse never entered training.
        admission.setStatus(AdmissionStatus.REJECTED);
        admissions.save(admission);

        String horseName = horse != null ? horse.getName() : null;
        admissionNotificationService.notifyOwnerRejected(admission, horseName,
                "Veterinary Clinical Review", reason);
        admissionNotificationService.notifyManagersOfAdmissionRejection(admission, horseName,
                "Veterinary Clinical Review", reason);

        admissionGroomReviewService.allocateWaitingAdmissions();

        if (auditLogService != null) {
            auditLogService.recordDomainEvent(DomainAuditEvent.builder()
                    .userId(actorId)
                    .action("VET_INTAKE_REJECT")
                    .entityName("AdmissionApplication")
                    .entityId(admissionId)
                    .module("MEDICAL")
                    .summary("Veterinarian rejected admission application")
                    .reason(reason)
                    .targetUrl("/manager/admissions/" + admissionId)
                    .before(Map.of(
                            "admissionStatus", "VET_REVIEW",
                            "horseStatus", horse != null && horse.getCurrentStatus() != null ? horse.getCurrentStatus().name() : "PENDING"
                    ))
                    .after(Map.of(
                            "admissionStatus", "REJECTED",
                            "horseStatus", "REJECTED"
                    ))
                    .build());
        }

        String qStallCode = quarantineStallId != null
                ? stalls.findById(quarantineStallId).map(StableStall::getStallCode).orElse(null)
                : null;

        return new VetReviewResponse(
                admission.getId(),
                admission.getStatus(),
                actorId,
                null,
                null,
                admission.getVetFeedback(),
                admission.getVetReviewedAt(),
                horseId,
                horse != null ? horse.getCurrentStatus() : null,
                quarantineStallId,
                qStallCode,
                schedule.getStatus(),
                null,
                schedule.getId(),
                schedule.getId()
        );
    }

}
