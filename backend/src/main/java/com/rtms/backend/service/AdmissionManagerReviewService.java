package com.rtms.backend.service;

import com.rtms.backend.dto.ManagerReviewRequest;
import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.PreventiveCareSchedule;
import com.rtms.backend.entity.StableStall;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.enums.HorseStatus;
import com.rtms.backend.enums.ReviewDecision;
import com.rtms.backend.enums.StallStatus;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.PreventiveCareScheduleRepository;
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
    private final PreventiveCareScheduleRepository preventiveCareScheduleRepository;

    public AdmissionManagerReviewService(
            AdmissionApplicationRepository admissionApplicationRepository,
            StableStallRepository stableStallRepository,
            HorseRepository horseRepository,
            PreventiveCareScheduleRepository preventiveCareScheduleRepository) {
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.stableStallRepository = stableStallRepository;
        this.horseRepository = horseRepository;
        this.preventiveCareScheduleRepository = preventiveCareScheduleRepository;
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

        Horse horse = horseRepository.findById(admission.getHorseId())
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

        return admissionApplicationRepository.save(admission);
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

        // Cancel all PENDING and OVERDUE preventive care schedules for this horse
        List<PreventiveCareSchedule> schedulesToCancel =
                preventiveCareScheduleRepository.findByHorseIdAndStatusIn(
                        horse.getId(), List.of("PENDING", "OVERDUE"));
        for (PreventiveCareSchedule schedule : schedulesToCancel) {
            schedule.setStatus("CANCELLED");
        }
        preventiveCareScheduleRepository.saveAll(schedulesToCancel);

        admission.setManagerId(managerId);
        admission.setManagerDecision(ReviewDecision.REJECTED);
        admission.setManagerFeedback(request.getFeedback());
        admission.setManagerReviewedAt(LocalDateTime.now());
        admission.setStatus(AdmissionStatus.REJECTED);

        return admissionApplicationRepository.save(admission);
    }
}