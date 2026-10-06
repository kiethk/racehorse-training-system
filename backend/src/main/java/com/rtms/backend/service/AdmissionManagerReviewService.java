package com.rtms.backend.service;
import com.rtms.backend.dto.ManagerReviewRequest;
import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.enums.ReviewDecision;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.entity.CareSchedule;
import com.rtms.backend.enums.CareScheduleStatus;
import com.rtms.backend.repository.CareScheduleRepository;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.enums.HorseStatus;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.entity.StableStall;
import com.rtms.backend.enums.StallStatus;
import com.rtms.backend.repository.StableStallRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdmissionManagerReviewService {

    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final StableStallRepository stableStallRepository;
    private final HorseRepository horseRepository;
    private final CareScheduleRepository careScheduleRepository;
    private final AdmissionGroomReviewService admissionGroomReviewService;

    public AdmissionManagerReviewService(
            AdmissionApplicationRepository admissionApplicationRepository,
            StableStallRepository stableStallRepository,
            HorseRepository horseRepository,
            CareScheduleRepository careScheduleRepository,
            AdmissionGroomReviewService admissionGroomReviewService) {
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.stableStallRepository = stableStallRepository;
        this.horseRepository = horseRepository;
        this.careScheduleRepository = careScheduleRepository;
        this.admissionGroomReviewService = admissionGroomReviewService;
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

        // 1. Lock REGULAR stall
        StableStall regularStall;

        if (request.getStallId() != null) {
            regularStall = stableStallRepository
                    .findAvailableRegularStallByIdForUpdate(request.getStallId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Selected regular stall is no longer available"));
        } else {
            regularStall = stableStallRepository
                    .findFirstAvailableRegularStallForUpdate()
                    .orElseThrow(() -> new IllegalStateException(
                            "No available regular stall"));
        }

        // 2. Update existing Horse
        horse.setCurrentStatus(HorseStatus.ELIGIBLE);
        horse.setCurrentStallId(regularStall.getId());

        // Protect Vet medical locks: only clear training lock if it was purely administrative
        if ("Admission pending trainer and manager review".equals(horse.getTrainingLockReason())
                && horse.getTrainingLockVetId() == null) {
            horse.setTrainingLocked(false);
            horse.setTrainingLockReason(null);
            horse.setTrainingLockReviewDate(null);
            horse.setTrainingLockVetId(null);
            horse.setTrainingLockUpdatedAt(LocalDateTime.now());
        }
        horseRepository.save(horse);

        // 3. Mark regular stall occupied
        regularStall.setStatus(StallStatus.OCCUPIED);
        stableStallRepository.save(regularStall);

        // 4. Update Admission
        admission.setManagerId(managerId);
        admission.setManagerDecision(ReviewDecision.APPROVED);
        admission.setManagerFeedback(request.getFeedback());
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
        processNextWaitingAdmission();
        return admission;
    }

    private AdmissionApplication reject(
            AdmissionApplication admission,
            Horse horse,
            Long managerId,
            ManagerReviewRequest request) {

        if (request.getFeedback() == null
                || request.getFeedback().isBlank()) {
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

        // Cancel active CareSchedule records for this horse
        if (careScheduleRepository != null) {
            List<CareSchedule> careSchedulesToCancel =
                    careScheduleRepository.findByHorseIdAndStatusIn(
                            horse.getId(),
                            List.of(CareScheduleStatus.REQUESTED,
                                    CareScheduleStatus.AWAITING_VET_CONFIRMATION,
                                    CareScheduleStatus.SCHEDULED,
                                    CareScheduleStatus.IN_PROGRESS,
                                    CareScheduleStatus.OVERDUE));
            for (CareSchedule cs : careSchedulesToCancel) {
                cs.setStatus(CareScheduleStatus.CANCELLED);
                cs.setCancelReason("Admission rejected by manager");
            }
            careScheduleRepository.saveAll(careSchedulesToCancel);
        }

        admission.setManagerId(managerId);
        admission.setManagerDecision(ReviewDecision.REJECTED);
        admission.setManagerFeedback(request.getFeedback());
        admission.setManagerReviewedAt(LocalDateTime.now());
        admission.setStatus(AdmissionStatus.REJECTED);

        admission = admissionApplicationRepository.save(admission);
        processNextWaitingAdmission();
        return admission;
    }

    private void processNextWaitingAdmission() {
        admissionApplicationRepository.findFirstByStatusOrderBySubmittedAtAscIdAsc(AdmissionStatus.WAITING_FOR_STALL)
                .ifPresent(waitingAdmission -> {
                    admissionGroomReviewService.processWaitingForStall(waitingAdmission.getId());
                });
    }
}
