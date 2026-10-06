package com.rtms.backend.service;

import com.rtms.backend.dto.GroomAdmissionReviewRequest;
import com.rtms.backend.entity.*;
import com.rtms.backend.enums.*;
import com.rtms.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdmissionGroomReviewService {

    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final CandidateHorseProfileRepository candidateHorseProfileRepository;
    private final StableStallRepository stableStallRepository;
    private final HorseRepository horseRepository;
    private final HorsePedigreeRepository horsePedigreeRepository;
    private final CareScheduleRepository careScheduleRepository;
    private final CareScheduleService careScheduleService;
    private final HeadTrainerWorkloadService headTrainerWorkloadService;
    private final NotificationService notificationService;

    public AdmissionGroomReviewService(
            AdmissionApplicationRepository admissionApplicationRepository,
            CandidateHorseProfileRepository candidateHorseProfileRepository,
            StableStallRepository stableStallRepository,
            HorseRepository horseRepository,
            HorsePedigreeRepository horsePedigreeRepository,
            CareScheduleRepository careScheduleRepository,
            CareScheduleService careScheduleService,
            HeadTrainerWorkloadService headTrainerWorkloadService,
            NotificationService notificationService) {
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.candidateHorseProfileRepository = candidateHorseProfileRepository;
        this.stableStallRepository = stableStallRepository;
        this.horseRepository = horseRepository;
        this.horsePedigreeRepository = horsePedigreeRepository;
        this.careScheduleRepository = careScheduleRepository;
        this.careScheduleService = careScheduleService;
        this.headTrainerWorkloadService = headTrainerWorkloadService;
        this.notificationService = notificationService;
    }

    @Transactional
    public AdmissionApplication review(Long admissionId, Long groomId, GroomAdmissionReviewRequest request) {
        AdmissionApplication admission = admissionApplicationRepository.findByIdForUpdate(admissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission not found"));

        if (admission.getStatus() != AdmissionStatus.GROOM_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admission is not awaiting Groom review");
        }
        if (request.getDecision() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision is required");
        }

        String feedback = request.getFeedback() == null ? null : request.getFeedback().trim();
        if (feedback == null || feedback.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Feedback is required for Groom review");
        }

        if (request.getDecision() == ReviewDecision.REJECTED) {
            return reject(admission, groomId, feedback);
        }
        if (request.getDecision() != ReviewDecision.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported Groom decision");
        }

        stampGroomReview(admission, groomId, ReviewDecision.APPROVED, feedback);
        return moveForwardIfCapacityAvailable(admission);
    }

    @Transactional
    public AdmissionApplication processWaitingForStall(Long admissionId) {
        AdmissionApplication admission = admissionApplicationRepository.findByIdForUpdate(admissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission not found"));

        if (admission.getStatus() != AdmissionStatus.WAITING_FOR_STALL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admission is not waiting for stall capacity");
        }
        return moveForwardIfCapacityAvailable(admission);
    }

    @Transactional
    public void assignPendingTrainers() {
        List<AdmissionApplication> pending = admissionApplicationRepository.findByStatusInAndTrainerIdIsNull(
                List.of(AdmissionStatus.VET_REVIEW, AdmissionStatus.TRAINER_REVIEW, AdmissionStatus.MANAGER_REVIEW),
                org.springframework.data.domain.PageRequest.of(0, 25)
        );

        for (AdmissionApplication item : pending) {
            admissionApplicationRepository.findByIdForUpdate(item.getId()).ifPresent(admission -> {
                if (admission.getTrainerId() == null) {
                    headTrainerWorkloadService.selectLeastLoadedHeadTrainerId().ifPresent(trainerId -> {
                        admission.setTrainerId(trainerId);
                        admissionApplicationRepository.save(admission);

                        if (admission.getHorseId() != null) {
                            horseRepository.findById(admission.getHorseId()).ifPresent(horse -> {
                                notificationService.sendAssignmentNotification(
                                        trainerId,
                                        admission.getId(),
                                        horse.getId(),
                                        NotificationTypes.ADMISSION_TRAINER_ASSIGNED,
                                        "New horse assignment",
                                        "You have been assigned to candidate horse " + horse.getName() + " for racing readiness assessment."
                                );
                            });
                        }
                    });
                }
            });
        }
    }

    private AdmissionApplication reject(AdmissionApplication admission, Long groomId, String feedback) {
        stampGroomReview(admission, groomId, ReviewDecision.REJECTED, feedback);
        admission.setStatus(AdmissionStatus.REJECTED);
        return admissionApplicationRepository.save(admission);
    }

    private AdmissionApplication moveForwardIfCapacityAvailable(AdmissionApplication admission) {
        stableStallRepository.lockAdmissionCapacityStallsForUpdate();
        if (!hasAdmissionCapacity()) {
            admission.setStatus(AdmissionStatus.WAITING_FOR_STALL);
            return admissionApplicationRepository.save(admission);
        }

        CandidateHorseProfile candidate = candidateHorseProfileRepository
                .findByAdmissionId(admission.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidate horse profile not found"));

        StableStall quarantineStall = stableStallRepository
                .findFirstAvailableQuarantineStallForUpdate()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No quarantine stall available"));

        // 1. Tạo hoặc tái sử dụng Horse CANDIDATE
        Horse horse = findReusableHorseOrCreateNew(admission, candidate);
        applyCandidateSnapshot(horse, admission, candidate, quarantineStall);
        horse = horseRepository.save(horse);

        updatePedigree(horse.getId(), candidate);

        // 2. Tạo hoặc tái sử dụng INITIAL CareSchedule và gán Vet
        CareSchedule initialSchedule = ensureInitialCareSchedule(admission.getId(), horse.getId());

        // 3. Gán chuồng cách ly
        quarantineStall.setStatus(StallStatus.OCCUPIED);
        stableStallRepository.save(quarantineStall);

        admission.setHorseId(horse.getId());
        admission.setQuarantineStallId(quarantineStall.getId());

        // 4. Chọn Head Trainer có workload thấp nhất và gán vào AdmissionApplication.trainerId
        // Nếu đã được gán trước đó thì giữ nguyên
        if (admission.getTrainerId() == null) {
            headTrainerWorkloadService.selectLeastLoadedHeadTrainerId()
                    .ifPresent(admission::setTrainerId);
        }

        admission.setStatus(AdmissionStatus.VET_REVIEW);
        admission = admissionApplicationRepository.save(admission);

        // 5. Tạo notification "new horse assignment" cho Vet và Trainer (có khóa chống trùng)
        dispatchAssignmentNotifications(admission, horse, initialSchedule);

        return admission;
    }

    private void dispatchAssignmentNotifications(AdmissionApplication admission, Horse horse, CareSchedule initialSchedule) {
        // Notification cho Vet
        if (initialSchedule != null && initialSchedule.getVeterinarianId() != null) {
            notificationService.sendAssignmentNotification(
                    initialSchedule.getVeterinarianId(),
                    admission.getId(),
                    horse.getId(),
                    NotificationTypes.ADMISSION_VET_ASSIGNED,
                    "New horse assignment",
                    "You have been assigned to candidate horse " + horse.getName() + " for initial admission examination in quarantine."
            );
        }

        // Notification cho Trainer
        if (admission.getTrainerId() != null) {
            notificationService.sendAssignmentNotification(
                    admission.getTrainerId(),
                    admission.getId(),
                    horse.getId(),
                    NotificationTypes.ADMISSION_TRAINER_ASSIGNED,
                    "New horse assignment",
                    "You have been assigned to candidate horse " + horse.getName() + " for racing readiness assessment."
            );
        }
    }

    private boolean hasAdmissionCapacity() {
        long availableQuarantineStalls = stableStallRepository.countAvailableQuarantineStalls();
        long availableRegularStalls = stableStallRepository.countAvailableRegularStalls();
        long occupiedQuarantineStalls = stableStallRepository.countOccupiedQuarantineStalls();

        return AdmissionCapacityPolicy.isAvailable(availableQuarantineStalls, availableRegularStalls, occupiedQuarantineStalls);
    }

    private Horse findReusableHorseOrCreateNew(AdmissionApplication admission, CandidateHorseProfile candidate) {
        String ueln = candidate.getRegistrationNumber();
        if (ueln == null || ueln.isBlank()) {
            return new Horse();
        }

        List<Horse> matches = horseRepository.findByRegistrationNumber(ueln);
        if (matches.isEmpty()) {
            return new Horse();
        }

        Horse existing = matches.get(0);
        if (existing.getCurrentStatus() == HorseStatus.REJECTED && admission.getOwnerId().equals(existing.getOwnerId())) {
            return existing;
        }

        throw new ResponseStatusException(HttpStatus.CONFLICT, "Horse with this UELN already exists");
    }

    private void applyCandidateSnapshot(Horse horse, AdmissionApplication admission, CandidateHorseProfile candidate, StableStall quarantineStall) {
        horse.setName(candidate.getName());
        horse.setBreed(candidate.getBreed());
        horse.setDateOfBirth(candidate.getDateOfBirth());
        horse.setOwnerId(admission.getOwnerId());
        horse.setRegistryName(candidate.getRegistryName());
        horse.setRegistrationNumber(candidate.getRegistrationNumber());
        horse.setCurrentStatus(HorseStatus.CANDIDATE);
        horse.setCurrentStallId(quarantineStall.getId());
        horse.setTrainingStatus(TrainingDecision.BLOCKED);
        horse.setTrainingLocked(true);
        horse.setTrainingLockReason("Initial admission examination is pending");
        horse.setTrainingLockVetId(null);
        horse.setTrainingLockReviewDate(null);
        horse.setTrainingLockUpdatedAt(LocalDateTime.now());
    }

    private void updatePedigree(Long horseId, CandidateHorseProfile candidate) {
        HorsePedigree pedigree = horsePedigreeRepository.findByHorseId(horseId).orElseGet(HorsePedigree::new);
        pedigree.setHorseId(horseId);
        pedigree.setSireName(candidate.getSireName());
        pedigree.setSireRegistrationNumber(candidate.getSireRegistrationNumber());
        pedigree.setDamName(candidate.getDamName());
        pedigree.setDamRegistrationNumber(candidate.getDamRegistrationNumber());
        pedigree.setPedigreeNotes(candidate.getPedigreeNotes());
        horsePedigreeRepository.save(pedigree);
    }

    private CareSchedule ensureInitialCareSchedule(Long admissionId, Long horseId) {
        CareSchedule schedule = careScheduleRepository
                .findFirstByAdmissionIdAndCareTypeOrderByCreatedAtDesc(admissionId, CareType.INITIAL)
                .orElse(null);

        if (schedule == null) {
            // Spec item 11: every admission reaching VET_REVIEW owns its own INITIAL
            // care_schedule row. The horse-level check previously skipped creation
            // when a different admission already had an open INITIAL, leaving this
            // admission without a schedule of its own.
            schedule = new CareSchedule();
            schedule.setHorseId(horseId);
            schedule.setAdmissionId(admissionId);
            schedule.setCareType(CareType.INITIAL);
            schedule.setStatus(CareScheduleStatus.REQUESTED);
            schedule.setDurationMinutes(30);
            schedule.setDescription("Initial admission physical examination in quarantine area");
            schedule = careScheduleRepository.save(schedule);
        }

        careScheduleService.assignRequestedSchedules();

        return careScheduleRepository
                .findFirstByAdmissionIdAndCareTypeOrderByCreatedAtDesc(admissionId, CareType.INITIAL)
                .orElse(schedule);
    }

    private void stampGroomReview(AdmissionApplication admission, Long groomId, ReviewDecision decision, String feedback) {
        admission.setGroomId(groomId);
        admission.setGroomDecision(decision);
        admission.setGroomFeedback(feedback);
        admission.setGroomReviewedAt(LocalDateTime.now());
    }

}
