package com.rtms.backend.admission.service;

import com.rtms.backend.admission.dto.ManagerReviewRequest;
import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.audit.dto.DomainAuditEvent;
import com.rtms.backend.audit.service.AuditLogService;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.admission.enums.ReviewDecision;
import com.rtms.backend.stable.enums.StallStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.service.PreventiveStatusService;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.identity.repository.GroomProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AdmissionManagerReviewService {

    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final StableStallRepository stableStallRepository;
    private final HorseRepository horseRepository;
    private final CareScheduleRepository careScheduleRepository;
    private final AdmissionGroomReviewService admissionGroomReviewService;
    private final PreventiveStatusService preventiveStatusService;
    private final GroomProfileRepository groomProfileRepository;
    private final AdmissionNotificationService admissionNotificationService;
    private final AuditLogService auditLogService;

    @Autowired
    public AdmissionManagerReviewService(
            AdmissionApplicationRepository admissionApplicationRepository,
            StableStallRepository stableStallRepository,
            HorseRepository horseRepository,
            CareScheduleRepository careScheduleRepository,
            AdmissionGroomReviewService admissionGroomReviewService,
            PreventiveStatusService preventiveStatusService,
            GroomProfileRepository groomProfileRepository,
            AdmissionNotificationService admissionNotificationService,
            AuditLogService auditLogService) {
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.stableStallRepository = stableStallRepository;
        this.horseRepository = horseRepository;
        this.careScheduleRepository = careScheduleRepository;
        this.admissionGroomReviewService = admissionGroomReviewService;
        this.preventiveStatusService = preventiveStatusService;
        this.groomProfileRepository = groomProfileRepository;
        this.admissionNotificationService = admissionNotificationService;
        this.auditLogService = auditLogService;
    }

    public AdmissionManagerReviewService(
            AdmissionApplicationRepository admissionApplicationRepository,
            StableStallRepository stableStallRepository,
            HorseRepository horseRepository,
            CareScheduleRepository careScheduleRepository,
            AdmissionGroomReviewService admissionGroomReviewService,
            PreventiveStatusService preventiveStatusService,
            GroomProfileRepository groomProfileRepository,
            AdmissionNotificationService admissionNotificationService) {
        this(admissionApplicationRepository, stableStallRepository, horseRepository,
             careScheduleRepository, admissionGroomReviewService, preventiveStatusService,
             groomProfileRepository, admissionNotificationService, null);
    }

    @Transactional
    public AdmissionApplication review(
            Long admissionId,
            Long managerId,
            ManagerReviewRequest request) {

        AdmissionApplication admission = admissionApplicationRepository
                .findByIdForUpdate(admissionId)
                .orElseThrow(() -> new IllegalArgumentException("Admission not found"));

        if (admission.getStatus() != AdmissionStatus.MANAGER_REVIEW) {
            throw new IllegalStateException(
                    "Admission is not ready for manager review");
        }

        if (admission.getHorseId() == null) {
            throw new IllegalStateException("Admission is missing horseId");
        }

        Horse horse = horseRepository.findByIdForUpdate(admission.getHorseId())
                .orElseThrow(() -> new IllegalStateException("Horse not found"));

        if (horse.getCurrentStatus() != HorseStatus.CANDIDATE) {
            throw new IllegalStateException("Horse must be in CANDIDATE status");
        }

        if (request.getDecision() == null) {
            throw new IllegalArgumentException("Decision is required");
        }

        if (request.getDecision() == ReviewDecision.REJECTED) {
            return reject(admission, horse, managerId, request);
        }

        if (request.getDecision() == ReviewDecision.APPROVED) {
            return approve(admission, horse, managerId, request);
        }

        throw new IllegalArgumentException("Unsupported review decision");
    }

    private AdmissionApplication approve(
            AdmissionApplication admission,
            Horse horse,
            Long managerId,
            ManagerReviewRequest request) {

        // 1. Lock REGULAR stall. Every allocation first takes the same stall-row locks, so two
        //    approvals cannot both read the same groom loads and pick the same "least loaded" groom.
        stableStallRepository.lockAdmissionCapacityStallsForUpdate();
        StableStall regularStall;

        if (request.getStallId() != null) {
            regularStall = stableStallRepository
                    .findAvailableRegularStallByIdForUpdate(request.getStallId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "Selected regular stall is no longer available or has no assigned groom"));
        } else {
            regularStall = stableStallRepository
                    .findFairAvailableStallForUpdate("REGULAR")
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "No available regular stall with an assigned groom"));
        }

        // 2. Update existing Horse
        // Thành ELIGIBLE là đủ để ngựa tập được nếu Thú y đã cho phép. Kết luận
        // chặn tập của Thú y (nếu có) giữ nguyên, không cần mở khóa gì ở đây.
        horse.setCurrentStatus(HorseStatus.ELIGIBLE);
        horse.setCurrentStallId(regularStall.getId());
        horseRepository.save(horse);

        // 3. Mark regular stall occupied
        regularStall.setStatus(StallStatus.OCCUPIED);
        stableStallRepository.save(regularStall);
        groomProfileRepository.touchLastHorseAssignedAt(regularStall.getGroomId(), LocalDateTime.now());

        // 4. Update Admission
        admission.setManagerId(managerId);
        admission.setManagerDecision(ReviewDecision.APPROVED);
        admission.setManagerFeedback(AdmissionGroomReviewService.normalizeFeedback(request.getFeedback()));
        admission.setManagerReviewedAt(LocalDateTime.now());
        admission.setStatus(AdmissionStatus.APPROVED);

        // 5. Release quarantine stall
        Long quarantineStallId = admission.getQuarantineStallId();

        if (quarantineStallId != null) {
            StableStall quarantineStall = stableStallRepository
                    .findById(quarantineStallId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Quarantine stall not found"));

            quarantineStall.setStatus(StallStatus.AVAILABLE);
            stableStallRepository.save(quarantineStall);
        }

        admission = admissionApplicationRepository.save(admission);

        // 6. Phác đồ bắt buộc mà khám nhập trại chưa tiêm, cũng chưa có sổ cũ: hẹn tiêm ngay hôm sau.
        preventiveStatusService.upsertMilestone(horse.getId(), "preventive:approval:" + admission.getId(),
                managerId, LocalDate.now().plusDays(1));

        admissionGroomReviewService.notifyGroomAssigned(admission, regularStall,
                horse.getName() + " was approved and moved to stall " + regularStall.getStallCode() + ", which you look after.");
        admissionNotificationService.notifyOwnerApproved(admission, horse, regularStall);
        admissionGroomReviewService.allocateWaitingAdmissions();

        if (auditLogService != null) {
            auditLogService.recordDomainEvent(DomainAuditEvent.builder()
                    .userId(managerId)
                    .action("MANAGER_ADMISSION_APPROVAL")
                    .entityName("AdmissionApplication")
                    .entityId(admission.getId())
                    .module("ADMISSIONS")
                    .summary("Club Manager approved admission application and allocated stall " + regularStall.getStallCode())
                    .reason(admission.getManagerFeedback())
                    .targetUrl("/manager/admissions/" + admission.getId())
                    .before(Map.of(
                            "admissionStatus", "MANAGER_REVIEW",
                            "horseStatus", "CANDIDATE",
                            "regularStall", "None"
                    ))
                    .after(Map.of(
                            "admissionStatus", "APPROVED",
                            "horseStatus", "ELIGIBLE",
                            "regularStall", regularStall.getStallCode()
                    ))
                    .build());
        }

        return admission;
    }

    private AdmissionApplication reject(
            AdmissionApplication admission,
            Horse horse,
            Long managerId,
            ManagerReviewRequest request) {

        String feedback = AdmissionGroomReviewService.normalizeFeedback(request.getFeedback());
        if (feedback == null) {
            throw new IllegalArgumentException(
                    "Feedback is required when rejecting an admission");
        }

        Long quarantineStallId = admission.getQuarantineStallId();

        if (quarantineStallId != null) {
            StableStall quarantineStall = stableStallRepository
                    .findById(quarantineStallId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Quarantine stall not found"));

            quarantineStall.setStatus(StallStatus.AVAILABLE);
            stableStallRepository.save(quarantineStall);
        }

        // Update existing Horse
        horse.setCurrentStatus(HorseStatus.REJECTED);
        horse.setCurrentStallId(null);
        horseRepository.save(horse);

        // Tự động chuyển CANCELLED các lịch chưa diễn ra của ngựa khi manager từ chối nhập viện.
        if (careScheduleRepository != null) {
            List<CareSchedule> nonCancellableSchedules =
                    careScheduleRepository.findByHorseIdAndStatusIn(horse.getId(),
                            CareScheduleStatus.ACTIVE_EXAM);
            if (!nonCancellableSchedules.isEmpty()) {
                throw new IllegalStateException(
                        "Admission cannot be rejected while veterinary care is IN_PROGRESS");
            }
            careScheduleRepository.cancelOpenSchedulesIfUnreferenced(
                    horse.getId(), List.of(CareScheduleStatus.SCHEDULED),
                    "Admission application rejected by Club Manager: " + feedback,
                    LocalDateTime.now());
        }

        admission.setManagerId(managerId);
        admission.setManagerDecision(ReviewDecision.REJECTED);
        admission.setManagerFeedback(feedback);
        admission.setManagerReviewedAt(LocalDateTime.now());
        admission.setStatus(AdmissionStatus.REJECTED);

        admission = admissionApplicationRepository.save(admission);
        admissionNotificationService.notifyOwnerRejected(
                admission, horse.getName(), "Club Manager Final Review", feedback);
        admissionGroomReviewService.allocateWaitingAdmissions();

        if (auditLogService != null) {
            auditLogService.recordDomainEvent(DomainAuditEvent.builder()
                    .userId(managerId)
                    .action("MANAGER_ADMISSION_REJECTION")
                    .entityName("AdmissionApplication")
                    .entityId(admission.getId())
                    .module("ADMISSIONS")
                    .summary("Club Manager rejected admission application #" + admission.getId())
                    .reason(feedback)
                    .targetUrl("/manager/admissions/" + admission.getId())
                    .before(Map.of(
                            "admissionStatus", "MANAGER_REVIEW",
                            "horseStatus", "CANDIDATE"
                    ))
                    .after(Map.of(
                            "admissionStatus", "REJECTED",
                            "horseStatus", "REJECTED"
                    ))
                    .build());
        }

        return admission;
    }
}
