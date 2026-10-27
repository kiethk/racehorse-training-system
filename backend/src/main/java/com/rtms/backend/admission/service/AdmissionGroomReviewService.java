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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import com.rtms.backend.horse.util.UelnUtils;
import java.util.Objects;

@Service
public class AdmissionGroomReviewService {
    private static final int ARRIVAL_WINDOW_DAYS = 14;
    public static final int MAX_FEEDBACK_LENGTH = 2000;
    private static final String QUARANTINE = "QUARANTINE";

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
            GroomProfileRepository groomProfileRepository) {
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
    }

    @Transactional
    public AdmissionApplication review(Long admissionId, Long groomId, GroomAdmissionReviewRequest request) {
        AdmissionApplication admission = admissionApplicationRepository.findByIdForUpdate(admissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission not found"));

        if (admission.getStatus() != AdmissionStatus.GROOM_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admission is not awaiting Groom review");
        }
        if (!stableStallRepository.isQuarantineGroom(groomId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only grooms of a quarantine stall can review new admissions");
        }
        if (request.getDecision() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision is required");
        }

        String feedback = normalizeFeedback(request.getFeedback());
        if (request.getDecision() == ReviewDecision.REJECTED) {
            if (feedback == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Feedback is required to reject an admission");
            }
            return reject(admission, groomId, feedback);
        }
        if (request.getDecision() != ReviewDecision.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported Groom decision");
        }

        // Every approval joins the stall queue; allocation then serves it in submission order,
        // so a fresh approval never overtakes admissions that were already waiting.
        stampGroomReview(admission, groomId, ReviewDecision.APPROVED, feedback);
        admission.setStatus(AdmissionStatus.WAITING_FOR_STALL);
        admissionApplicationRepository.save(admission);
        allocateWaitingAdmissions();
        return admission;
    }

    /**
     * Gives free quarantine stalls to WAITING_FOR_STALL admissions, oldest first, until capacity
     * runs out. Never throws for lack of capacity: callers such as the Manager review must not be
     * rolled back because the queue could not move.
     * <p>
     * Lock order: all admission stalls first, then admissions with SKIP LOCKED. Other flows lock an
     * admission before stalls, but this loop never waits for an admission row, so no wait cycle forms;
     * a skipped admission is served by the next run (at the latest the scheduler).
     *
     * @return how many admissions received a stall
     */
    @Transactional
    public int allocateWaitingAdmissions() {
        // The capacity counts below are native SQL and must see changes made earlier in this transaction.
        admissionApplicationRepository.flush();
        stableStallRepository.lockAdmissionCapacityStallsForUpdate();
        int allocated = 0;
        while (hasAdmissionCapacity()) {
            AdmissionApplication next = admissionApplicationRepository.findNextWaitingForStallForUpdate().orElse(null);
            if (next == null) break;
            StableStall stall = stableStallRepository.findFairAvailableStallForUpdate(QUARANTINE).orElse(null);
            if (stall == null) break;
            reserve(next, stall);
            allocated++;
        }
        return allocated;
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
        if (!Objects.equals(quarantineStall.getGroomId(), groomId)) {
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
        admission.setStatus(AdmissionStatus.WAITING_FOR_STALL);
        admissionApplicationRepository.save(admission);
        allocateWaitingAdmissions();
        return admission;
    }

    @Transactional
    public void expireOverdueArrivalReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<AdmissionApplication> due = admissionApplicationRepository
                .findByStatusAndArrivalDeadlineAtLessThanEqualOrderByArrivalDeadlineAtAscIdAsc(
                        AdmissionStatus.WAITING_FOR_ARRIVAL, now);
        boolean released = false;
        for (AdmissionApplication candidate : due) {
            AdmissionApplication admission = admissionApplicationRepository.findByIdForUpdate(candidate.getId())
                    .orElse(null);
            if (admission == null || admission.getStatus() != AdmissionStatus.WAITING_FOR_ARRIVAL
                    || admission.getArrivalDeadlineAt() == null || admission.getArrivalDeadlineAt().isAfter(now)) {
                continue;
            }
            expireArrivalReservation(admission);
            admissionApplicationRepository.save(admission);
            released = true;
        }
        if (released) {
            allocateWaitingAdmissions();
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
        // Flush so the next loop iteration's native queries see this stall and admission as taken.
        admissionApplicationRepository.saveAndFlush(admission);
        groomProfileRepository.touchLastHorseAssignedAt(quarantineStall.getGroomId(), now);
        notifyGroomAssigned(admission, quarantineStall,
                "Quarantine stall " + quarantineStall.getStallCode() + " is reserved for admission #" + admission.getId()
                        + ". Confirm the horse's arrival before " + admission.getArrivalDeadlineAt().toLocalDate() + ".");
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
                .filter(other -> !Objects.equals(other.getId(), horse.getId()))
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

        String sireUeln = UelnUtils.normalize(candidate.getSireRegistrationNumber());
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

        String damUeln = UelnUtils.normalize(candidate.getDamRegistrationNumber());
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
