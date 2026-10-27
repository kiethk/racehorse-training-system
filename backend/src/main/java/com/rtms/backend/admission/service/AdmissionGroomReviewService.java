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
import com.rtms.backend.horse.service.PedigreeBirthDateValidator;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.service.CareScheduleService;
import com.rtms.backend.notification.entity.Notification;
import com.rtms.backend.identity.repository.GroomProfileRepository;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.notification.service.NotificationTypes;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.enums.StallStatus;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.enums.TrainingDecision;


import com.rtms.backend.admission.dto.GroomAdmissionReviewRequest;
import com.rtms.backend.config.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdmissionGroomReviewService {
    private static final int ARRIVAL_WINDOW_DAYS = 14;
    public static final int MAX_FEEDBACK_LENGTH = 2000;

    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final CandidateHorseProfileRepository candidateHorseProfileRepository;
    private final StableStallRepository stableStallRepository;
    private final HorseRepository horseRepository;
    private final HorsePedigreeRepository horsePedigreeRepository;
    private final CareScheduleRepository careScheduleRepository;
    private final CareScheduleService careScheduleService;
    private final NotificationService notificationService;
    private final PedigreeBirthDateValidator birthDates;
    private final GroomProfileRepository groomProfileRepository;
    private final AdmissionGroomPoolService groomPool;
    private final AdmissionGroomPoolNotifier poolNotifier;

    public AdmissionGroomReviewService(
            AdmissionApplicationRepository admissionApplicationRepository,
            CandidateHorseProfileRepository candidateHorseProfileRepository,
            StableStallRepository stableStallRepository,
            HorseRepository horseRepository,
            HorsePedigreeRepository horsePedigreeRepository,
            CareScheduleRepository careScheduleRepository,
            CareScheduleService careScheduleService,
            NotificationService notificationService,
            PedigreeBirthDateValidator birthDates,
            GroomProfileRepository groomProfileRepository,
            AdmissionGroomPoolService groomPool,
            AdmissionGroomPoolNotifier poolNotifier) {
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.candidateHorseProfileRepository = candidateHorseProfileRepository;
        this.stableStallRepository = stableStallRepository;
        this.horseRepository = horseRepository;
        this.horsePedigreeRepository = horsePedigreeRepository;
        this.careScheduleRepository = careScheduleRepository;
        this.careScheduleService = careScheduleService;
        this.notificationService = notificationService;
        this.birthDates = birthDates;
        this.groomProfileRepository = groomProfileRepository;
        this.groomPool = groomPool;
        this.poolNotifier = poolNotifier;
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
        if (request.getDecision() == ReviewDecision.REJECTED) {
            // A rejection frees nothing and takes nothing, so the pool needs no stall lock here.
            assertInPool(groomPool.viewFor(groomId), admission);
            String feedback = normalizeFeedback(request.getFeedback());
            if (feedback == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Feedback is required to reject an admission");
            }
            return reject(admission, groomId, feedback);
        }
        if (request.getDecision() != ReviewDecision.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported Groom decision");
        }
        return approve(admission, groomId, normalizeFeedback(request.getFeedback()));
    }

    /**
     * Approving is claiming: the reviewer's own first free quarantine stall is reserved for the horse.
     * Lock order matches the Manager review (admission, then stalls). The stall lock comes before the
     * pool is read, so two grooms approving at once cannot both count themselves least loaded.
     */
    private AdmissionApplication approve(AdmissionApplication admission, Long groomId, String feedback) {
        stableStallRepository.lockAdmissionCapacityStallsForUpdate();
        AdmissionGroomPoolService.PoolView pool = groomPool.viewFor(groomId);
        assertInPool(pool, admission);

        List<Long> oldest = admissionApplicationRepository.findPoolIdsInApprovalOrder(
                pool.leastLoaded(), pool.eligible(), pool.fallbackCutoff(), AdmissionStatus.GROOM_REVIEW,
                PageRequest.of(0, 1));
        if (!oldest.isEmpty() && !oldest.getFirst().equals(admission.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "OLDER_ADMISSION_FIRST",
                    "Review older admissions first. Approvals follow submission order.");
        }

        String blockingReason = AdmissionCapacityPolicy.blockingReason(
                stableStallRepository.countAvailableQuarantineStalls(),
                stableStallRepository.countAvailableRegularStalls(),
                stableStallRepository.countOccupiedQuarantineStalls());
        if (blockingReason != null) {
            throw new ApiException(HttpStatus.CONFLICT, blockingReason, blockingReason.equals("NO_QUARANTINE_STALL")
                    ? "No quarantine stall is available."
                    : "Regular-stall reserve is below the required quarantine turnover reserve.");
        }

        StableStall stall = stableStallRepository.findFirstAvailableQuarantineStallOfGroomForUpdate(groomId)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "NO_QUARANTINE_STALL",
                        "You have no free quarantine stall."));

        stampGroomReview(admission, groomId, ReviewDecision.APPROVED, feedback);
        reserve(admission, stall);
        return admission;
    }

    private static void assertInPool(AdmissionGroomPoolService.PoolView pool, AdmissionApplication admission) {
        if (!pool.canSee(admission)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_IN_GROOM_POOL",
                    "This admission is not in your review pool.");
        }
    }

    @Transactional
    public AdmissionApplication confirmArrival(Long admissionId, Long groomId) {
        AdmissionApplication admission = admissionApplicationRepository.findByIdForUpdate(admissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission not found"));
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
        // The stall's current groom, not the reviewer: a block can change hands while the horse travels.
        if (!java.util.Objects.equals(quarantineStall.getGroomId(), groomId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the groom of the reserved quarantine stall can confirm arrival");
        }
        if (quarantineStall.getStatus() != StallStatus.RESERVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reserved quarantine stall is no longer available");
        }

        CandidateHorseProfile candidate = candidateHorseProfileRepository
                .findByAdmissionId(admissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidate horse profile not found"));

        birthDates.validateByUeln(candidate.getDateOfBirth(),
                candidate.getSireRegistrationNumber(), candidate.getDamRegistrationNumber());

        Horse horse = findReusableHorseOrCreateNew(admission, candidate);
        assertArrivalDoesNotCollide(admission, horse, quarantineStall);

        quarantineStall.setStatus(StallStatus.OCCUPIED);
        stableStallRepository.save(quarantineStall);

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
        // Back to the groom pool as a fresh review: the old reviewer no longer holds a stall for it,
        // and a new start time puts it behind admissions that waited without missing a deadline.
        admission.setStatus(AdmissionStatus.GROOM_REVIEW);
        admission.setGroomId(null);
        admission.setGroomDecision(null);
        admission.setGroomFeedback(null);
        admission.setGroomReviewedAt(null);
        admission.setGroomReviewStartedAt(LocalDateTime.now());
        admission.setGroomPoolWidenedAt(null);
        admission = admissionApplicationRepository.save(admission);
        poolNotifier.notifyReopened(admission);
        return admission;
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

    private void reserve(AdmissionApplication admission, StableStall quarantineStall) {
        LocalDateTime now = LocalDateTime.now();
        quarantineStall.setStatus(StallStatus.RESERVED);
        stableStallRepository.save(quarantineStall);
        admission.setQuarantineStallId(quarantineStall.getId());
        admission.setArrivalDeadlineAt(now.plusDays(ARRIVAL_WINDOW_DAYS));
        admission.setArrivedAt(null);
        admission.setStatus(AdmissionStatus.WAITING_FOR_ARRIVAL);
        admissionApplicationRepository.save(admission);
        groomProfileRepository.touchLastHorseAssignedAt(quarantineStall.getGroomId(), now);
        notifyGroomAssigned(admission, quarantineStall,
                "Quarantine stall " + quarantineStall.getStallCode() + " is reserved for admission #" + admission.getId()
                        + ". Confirm the horse's arrival before " + admission.getArrivalDeadlineAt().toLocalDate() + ".");
    }

    /**
     * Stalls whose groom just changed: a horse on its way to one of them now belongs to the new groom
     * (the horse follows the stall), so that groom hears about it. The key includes the groom, so the
     * previous groom's notification does not suppress this one.
     */
    public void notifyReservedStallsChangedHands(java.util.Collection<StableStall> changedStalls) {
        java.util.Map<Long, StableStall> reserved = new java.util.HashMap<>();
        for (StableStall stall : changedStalls) {
            if (stall.getStatus() == StallStatus.RESERVED && stall.getGroomId() != null) reserved.put(stall.getId(), stall);
        }
        if (reserved.isEmpty()) return;
        for (AdmissionApplication admission : admissionApplicationRepository
                .findByQuarantineStallIdInAndStatus(reserved.keySet(), AdmissionStatus.WAITING_FOR_ARRIVAL)) {
            StableStall stall = reserved.get(admission.getQuarantineStallId());
            notifyGroomAssigned(admission, stall, "You now look after quarantine stall " + stall.getStallCode()
                    + ", reserved for admission #" + admission.getId() + ". Confirm the horse's arrival before "
                    + admission.getArrivalDeadlineAt().toLocalDate() + ".");
        }
    }

    /** Scenario B: the groom now responsible for the admission's horse. Keyed per stall so a new stall notifies again. */
    void notifyGroomAssigned(AdmissionApplication admission, StableStall stall, String message) {
        notificationService.createNotification(
                stall.getGroomId(),
                "Admission assigned to you",
                message,
                NotificationTypes.ADMISSION_GROOM_ASSIGNED,
                NotificationTypes.REFERENCE_ADMISSION,
                admission.getId(),
                Notification.buildDeduplicationKey(stall.getGroomId(), NotificationTypes.REFERENCE_ADMISSION,
                        admission.getId(), NotificationTypes.ADMISSION_GROOM_ASSIGNED) + ":stall:" + stall.getId());
    }

    /** Trimmed feedback, or null when blank; rejects text longer than reviewers are allowed to write. */
    public static String normalizeFeedback(String raw) {
        String feedback = raw == null ? null : raw.trim();
        if (feedback == null || feedback.isEmpty()) return null;
        if (feedback.length() > MAX_FEEDBACK_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Feedback must be at most " + MAX_FEEDBACK_LENGTH + " characters");
        }
        return feedback;
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

    private Horse findReusableHorseOrCreateNew(AdmissionApplication admission, CandidateHorseProfile candidate) {
        String ueln = candidate.getRegistrationNumber();
        if (ueln == null || ueln.isBlank()) {
            return new Horse();
        }

        List<Horse> matches = horseRepository.findByRegistrationNumberForUpdate(ueln);
        if (matches.isEmpty()) {
            return new Horse();
        }

        Horse existing = matches.get(0);
        if (existing.getCurrentStatus() == HorseStatus.REJECTED) {
            return existing;
        }

        throw new ResponseStatusException(HttpStatus.CONFLICT, "Horse with this UELN already exists");
    }

    /**
     * Both cases would otherwise only surface as a unique-constraint failure at flush time
     * (uq_horses_current_stall, uq_admission_active_horse), which the client sees as HTTP 500.
     */
    private void assertArrivalDoesNotCollide(AdmissionApplication admission, Horse horse, StableStall quarantineStall) {
        horseRepository.findByCurrentStallId(quarantineStall.getId())
                .filter(other -> !java.util.Objects.equals(other.getId(), horse.getId()))
                .ifPresent(other -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Quarantine stall " + quarantineStall.getStallCode() + " is still linked to another horse");
                });
        if (horse.getId() != null && admissionApplicationRepository.existsByHorseIdAndIdNotAndStatusNotIn(
                horse.getId(), admission.getId(), List.of(AdmissionStatus.APPROVED, AdmissionStatus.REJECTED))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This horse already has another active admission");
        }
    }

    private void applyCandidateSnapshot(Horse horse, AdmissionApplication admission, CandidateHorseProfile candidate, StableStall quarantineStall) {
        horse.setName(candidate.getName());
        horse.setBreed(candidate.getBreed());
        // Legacy snapshots may have no sex; do not erase a known value on readmission.
        if (candidate.getSex() != null) {
            horse.setSex(candidate.getSex());
        }
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

        String sireUeln = com.rtms.backend.horse.util.UelnUtils.normalize(candidate.getSireRegistrationNumber());
        String sireName = candidate.getSireName() != null && !candidate.getSireName().isBlank() ? candidate.getSireName().trim() : null;

        if (sireUeln != null) {
            List<Horse> matches = horseRepository.findByRegistrationNumber(sireUeln);
            if (!matches.isEmpty()) {
                Horse matchedSire = matches.get(0);
                pedigree.setSireId(matchedSire.getId());
                pedigree.setSireName(matchedSire.getName());
                pedigree.setSireRegistrationNumber(matchedSire.getRegistrationNumber());
            } else {
                pedigree.setSireId(null);
                pedigree.setSireName(sireName);
                pedigree.setSireRegistrationNumber(sireUeln);
            }
        } else {
            pedigree.setSireId(null);
            pedigree.setSireName(sireName);
            pedigree.setSireRegistrationNumber(null);
        }

        String damUeln = com.rtms.backend.horse.util.UelnUtils.normalize(candidate.getDamRegistrationNumber());
        String damName = candidate.getDamName() != null && !candidate.getDamName().isBlank() ? candidate.getDamName().trim() : null;

        if (damUeln != null) {
            List<Horse> matches = horseRepository.findByRegistrationNumber(damUeln);
            if (!matches.isEmpty()) {
                Horse matchedDam = matches.get(0);
                pedigree.setDamId(matchedDam.getId());
                pedigree.setDamName(matchedDam.getName());
                pedigree.setDamRegistrationNumber(matchedDam.getRegistrationNumber());
            } else {
                pedigree.setDamId(null);
                pedigree.setDamName(damName);
                pedigree.setDamRegistrationNumber(damUeln);
            }
        } else {
            pedigree.setDamId(null);
            pedigree.setDamName(damName);
            pedigree.setDamRegistrationNumber(null);
        }

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

        // Only this admission's exam: the batch variant also processes every other horse's
        // REQUESTED schedule, so any failure there would roll back the arrival itself.
        // The rest stays with CareScheduleScheduler.
        careScheduleService.assignRequestedSchedule(schedule);

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
