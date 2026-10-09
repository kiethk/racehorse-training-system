package com.rtms.backend.admission.service;
import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.entity.CandidateHorseProfile;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.enums.ReviewDecision;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.repository.CandidateHorseProfileRepository;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.entity.HorsePedigree;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorsePedigreeRepository;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.service.CareScheduleService;
import com.rtms.backend.notification.entity.Notification;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.notification.service.NotificationTypes;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.enums.StallStatus;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.enums.TrainingDecision;


import com.rtms.backend.admission.dto.GroomAdmissionReviewRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdmissionGroomReviewService {
    private static final int ARRIVAL_WINDOW_DAYS = 14;

    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final CandidateHorseProfileRepository candidateHorseProfileRepository;
    private final StableStallRepository stableStallRepository;
    private final HorseRepository horseRepository;
    private final HorsePedigreeRepository horsePedigreeRepository;
    private final CareScheduleRepository careScheduleRepository;
    private final CareScheduleService careScheduleService;
    private final NotificationService notificationService;

    public AdmissionGroomReviewService(
            AdmissionApplicationRepository admissionApplicationRepository,
            CandidateHorseProfileRepository candidateHorseProfileRepository,
            StableStallRepository stableStallRepository,
            HorseRepository horseRepository,
            HorsePedigreeRepository horsePedigreeRepository,
            CareScheduleRepository careScheduleRepository,
            CareScheduleService careScheduleService,
            NotificationService notificationService) {
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.candidateHorseProfileRepository = candidateHorseProfileRepository;
        this.stableStallRepository = stableStallRepository;
        this.horseRepository = horseRepository;
        this.horsePedigreeRepository = horsePedigreeRepository;
        this.careScheduleRepository = careScheduleRepository;
        this.careScheduleService = careScheduleService;
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
        return reserveQuarantineStallIfCapacityAvailable(admission);
    }

    @Transactional
    public AdmissionApplication processWaitingForStall(Long admissionId) {
        return processWaitingForStall(admissionId, null);
    }

    @Transactional
    public AdmissionApplication processWaitingForStall(Long admissionId, Long groomId) {
        AdmissionApplication admission = admissionApplicationRepository.findByIdForUpdate(admissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission not found"));

        if (admission.getStatus() != AdmissionStatus.WAITING_FOR_STALL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admission is not waiting for stall capacity");
        }
        if (groomId != null && !java.util.Objects.equals(admission.getGroomId(), groomId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the Groom handling this admission can retry allocation");
        }
        return reserveQuarantineStallIfCapacityAvailable(admission);
    }

    @Transactional
    public AdmissionApplication confirmArrival(Long admissionId, Long groomId) {
        AdmissionApplication admission = admissionApplicationRepository.findByIdForUpdate(admissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission not found"));
        if (!java.util.Objects.equals(admission.getGroomId(), groomId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the Groom handling this admission can confirm arrival");
        }
        if (admission.getStatus() != AdmissionStatus.WAITING_FOR_ARRIVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admission is not waiting for horse arrival");
        }

        LocalDateTime now = LocalDateTime.now();
        if (admission.getArrivalDeadlineAt() == null || !admission.getArrivalDeadlineAt().isAfter(now)) {
            expireArrivalReservation(admission);
            return admissionApplicationRepository.save(admission);
        }

        StableStall quarantineStall = stableStallRepository
                .findQuarantineStallByIdForUpdate(admission.getQuarantineStallId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Reserved quarantine stall no longer exists"));
        if (quarantineStall.getStatus() != StallStatus.RESERVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reserved quarantine stall is no longer available");
        }

        CandidateHorseProfile candidate = candidateHorseProfileRepository
                .findByAdmissionId(admissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidate horse profile not found"));

        quarantineStall.setStatus(StallStatus.OCCUPIED);
        stableStallRepository.save(quarantineStall);

        Horse horse = findReusableHorseOrCreateNew(admission, candidate);
        applyCandidateSnapshot(horse, admission, candidate, quarantineStall);
        horse = horseRepository.save(horse);
        updatePedigree(horse.getId(), candidate);

        admission.setHorseId(horse.getId());
        admission.setArrivedAt(now);
        admission.setStatus(AdmissionStatus.VET_REVIEW);
        CareSchedule initialSchedule = ensureInitialCareSchedule(admissionId, horse.getId());
        admission = admissionApplicationRepository.save(admission);
        dispatchAssignmentNotifications(admission, horse, initialSchedule);
        return admission;
    }

    @Transactional
    public AdmissionApplication reopenExpiredArrival(Long admissionId) {
        AdmissionApplication admission = admissionApplicationRepository.findByIdForUpdate(admissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission not found"));
        if (admission.getStatus() != AdmissionStatus.ARRIVAL_EXPIRED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admission arrival reservation has not expired");
        }
        admission.setStatus(AdmissionStatus.WAITING_FOR_STALL);
        AdmissionApplication saved = admissionApplicationRepository.save(admission);
        return reserveQuarantineStallIfCapacityAvailable(saved);
    }

    @Transactional
    public void expireOverdueArrivalReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<AdmissionApplication> due = admissionApplicationRepository
                .findByStatusAndArrivalDeadlineAtLessThanEqualOrderByArrivalDeadlineAtAscIdAsc(
                        AdmissionStatus.WAITING_FOR_ARRIVAL, now);
        for (AdmissionApplication candidate : due) {
            AdmissionApplication admission = admissionApplicationRepository.findByIdForUpdate(candidate.getId())
                    .orElse(null);
            if (admission == null || admission.getStatus() != AdmissionStatus.WAITING_FOR_ARRIVAL
                    || admission.getArrivalDeadlineAt() == null || admission.getArrivalDeadlineAt().isAfter(now)) {
                continue;
            }
            expireArrivalReservation(admission);
            admissionApplicationRepository.save(admission);
        }
    }

    private AdmissionApplication reject(AdmissionApplication admission, Long groomId, String feedback) {
        stampGroomReview(admission, groomId, ReviewDecision.REJECTED, feedback);
        admission.setStatus(AdmissionStatus.REJECTED);
        return admissionApplicationRepository.save(admission);
    }

    private AdmissionApplication reserveQuarantineStallIfCapacityAvailable(AdmissionApplication admission) {
        stableStallRepository.lockAdmissionCapacityStallsForUpdate();
        if (!hasAdmissionCapacity()) {
            admission.setStatus(AdmissionStatus.WAITING_FOR_STALL);
            return admissionApplicationRepository.save(admission);
        }
        StableStall quarantineStall = stableStallRepository
                .findFirstAvailableQuarantineStallForUpdate()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No quarantine stall available"));
        quarantineStall.setStatus(StallStatus.RESERVED);
        stableStallRepository.save(quarantineStall);
        admission.setQuarantineStallId(quarantineStall.getId());
        admission.setArrivalDeadlineAt(LocalDateTime.now().plusDays(ARRIVAL_WINDOW_DAYS));
        admission.setArrivedAt(null);
        admission.setStatus(AdmissionStatus.WAITING_FOR_ARRIVAL);
        return admissionApplicationRepository.save(admission);
    }

    private void expireArrivalReservation(AdmissionApplication admission) {
        if (admission.getQuarantineStallId() != null) {
            StableStall stall = stableStallRepository.findQuarantineStallByIdForUpdate(admission.getQuarantineStallId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Reserved quarantine stall no longer exists"));
            if (stall.getStatus() == StallStatus.RESERVED) {
                stall.setStatus(StallStatus.AVAILABLE);
                stableStallRepository.save(stall);
            }
        }
        admission.setQuarantineStallId(null);
        admission.setStatus(AdmissionStatus.ARRIVAL_EXPIRED);
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
        // Không khóa tập ở đây: CANDIDATE vốn không tập được (Horse.canTrain()).
        // Ngựa từng bị từ chối nộp lại thì xóa kết luận cũ — lần khám nhập học mới sẽ quyết định.
        horse.setTrainingDecision(TrainingDecision.ALLOWED);
        horse.setTrainingDecisionReason(null);
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
