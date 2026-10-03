package com.rtms.backend.service;

import com.rtms.backend.config.ApiException;
import jakarta.persistence.EntityManager;
import com.rtms.backend.dto.*;
import com.rtms.backend.entity.*;
import com.rtms.backend.enums.*;
import com.rtms.backend.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CareScheduleService {

    private static final int DEFAULT_DURATION_MINUTES = 30;
    private static final int OFFER_EXPIRATION_MINUTES = 15;

    private final CareScheduleRepository careScheduleRepository;
    private final VetOfferRepository vetOfferRepository;
    private final HorseRepository horseRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final HorseHealthMetricRepository metricRepository;
    private final AdmissionApplicationRepository admissionRepository;
    private final StableStallRepository stallRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    public CareScheduleService(
            CareScheduleRepository careScheduleRepository,
            VetOfferRepository vetOfferRepository,
            HorseRepository horseRepository,
            HealthRecordRepository healthRecordRepository,
            HorseHealthMetricRepository metricRepository,
            AdmissionApplicationRepository admissionRepository,
            StableStallRepository stallRepository,
            UserRepository userRepository, EntityManager entityManager) {
        this.careScheduleRepository = careScheduleRepository;
        this.vetOfferRepository = vetOfferRepository;
        this.horseRepository = horseRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.metricRepository = metricRepository;
        this.admissionRepository = admissionRepository;
        this.stallRepository = stallRepository;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public CareScheduleResponse createInitialSchedule(Long admissionId, Long horseId) {
        if (admissionId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "admissionId is required");
        }
        AdmissionApplication adm = admissionRepository.findByIdForUpdate(admissionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Admission not found"));
        if (adm.getStatus() != AdmissionStatus.VET_REVIEW && adm.getStatus() != AdmissionStatus.PENDING_RECHECK) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_REVIEW_STATE", "Admission is not awaiting a vet examination");
        }
        if (horseId != null && !Objects.equals(horseId, adm.getHorseId())) {
            throw new ApiException(HttpStatus.CONFLICT, "HORSE_MISMATCH", "Horse does not belong to this admission");
        }
        horseId = adm.getHorseId();
        if (horseId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "HORSE_NOT_ASSIGNED", "Admission does not have a horse assigned yet");
        }

        Horse initialHorse = horseRepository.findByIdForUpdate(horseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found"));
        boolean activeExists = careScheduleRepository.existsByHorseIdAndCareTypeAndStatusIn(
                horseId, CareType.INITIAL,
                List.of(CareScheduleStatus.REQUESTED, CareScheduleStatus.AWAITING_VET_CONFIRMATION,
                        CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS));
        if (activeExists) {
            CareSchedule existing = careScheduleRepository
                    .findFirstByAdmissionIdAndCareTypeOrderByCreatedAtDesc(admissionId, CareType.INITIAL)
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "SCHEDULE_EXISTS", "Active schedule already exists"));
            return CareScheduleResponse.from(existing);
        }

        CareSchedule schedule = new CareSchedule();
        schedule.setHorseId(horseId);
        schedule.setAdmissionId(admissionId);
        schedule.setCareType(CareType.INITIAL);
        schedule.setStatus(CareScheduleStatus.REQUESTED);
        schedule.setDurationMinutes(DEFAULT_DURATION_MINUTES);
        schedule.setDescription("Initial admission physical examination in quarantine area");
        CareSchedule saved = careScheduleRepository.save(schedule);

        Horse horse = initialHorse;
        if (horse != null) {
            horse.setTrainingStatus(TrainingStatus.BLOCKED);
            horse.setTrainingLocked(true);
            horse.setTrainingLockReason("Initial admission examination is pending");
            horseRepository.save(horse);
        }

        dispatchOffersForSchedule(saved);
        return CareScheduleResponse.from(saved);
    }

    @Transactional
    public CareScheduleResponse createSchedule(Long horseId, CareType careType, String description, LocalDateTime requestedAt, Long sourceIncidentId) {
        Horse horse = horseRepository.findByIdForUpdate(horseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found"));

        CareSchedule schedule = new CareSchedule();
        schedule.setHorseId(horseId);
        schedule.setCareType(careType);
        schedule.setStatus(CareScheduleStatus.REQUESTED);
        schedule.setScheduledAt(requestedAt);
        schedule.setDurationMinutes(DEFAULT_DURATION_MINUTES);
        schedule.setDescription(description != null ? description.trim() : null);
        schedule.setSourceIncidentId(sourceIncidentId);

        if (careType == CareType.URGENT) {
            horse.setTrainingStatus(TrainingStatus.BLOCKED);
            horse.setTrainingLocked(true);
            horse.setTrainingLockReason("Urgent veterinary care pending: " + description);
            horseRepository.save(horse);
        }

        CareSchedule saved = careScheduleRepository.save(schedule);
        dispatchOffersForSchedule(saved);
        return CareScheduleResponse.from(saved);
    }

    @Transactional
    public VetOfferResponse acceptOffer(Long offerId, Long vetId) {
        VetOffer offer = vetOfferRepository.findByIdForUpdate(offerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "OFFER_NOT_FOUND", "Offer not found"));

        if (!offer.getVeterinarianId().equals(vetId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only assigned veterinarian can accept this offer");
        }
        if (offer.getStatus() != VetOfferStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_OFFER_STATUS", "Offer is no longer pending");
        }
        if (offer.getExpiresAt() != null && offer.getExpiresAt().isBefore(LocalDateTime.now())) {
            offer.setStatus(VetOfferStatus.EXPIRED);
            vetOfferRepository.save(offer);
            throw new ApiException(HttpStatus.CONFLICT, "OFFER_EXPIRED", "Offer has expired");
        }

        // Serialize acceptance across all offers for the same veterinarian.
        userRepository.findByIdForUpdate(vetId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VET_NOT_FOUND", "Veterinarian not found"));
        CareSchedule schedule = lockClinicalSchedule(offer.getCareScheduleId());

        if (schedule.getStatus() != CareScheduleStatus.REQUESTED && schedule.getStatus() != CareScheduleStatus.AWAITING_VET_CONFIRMATION) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_SCHEDULE_STATUS", "Schedule is not awaiting acceptance");
        }

        LocalDateTime scheduledTime = offer.getProposedScheduledAt() != null ? offer.getProposedScheduledAt() : LocalDateTime.now();

        horseRepository.findByIdForUpdate(schedule.getHorseId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found"));
        ensureAvailableForAcceptance(schedule, vetId, scheduledTime);

        offer.setStatus(VetOfferStatus.ACCEPTED);
        offer.setRespondedAt(LocalDateTime.now());
        vetOfferRepository.save(offer);

        schedule.setVeterinarianId(vetId);
        schedule.setScheduledAt(scheduledTime);
        schedule.setStatus(CareScheduleStatus.SCHEDULED);
        careScheduleRepository.save(schedule);

        // BR-CARE-13 Preemption rule: when Vet accepts URGENT, conflicting INITIAL or ROUTINE SCHEDULED for that Vet are yielded
        if (schedule.getCareType() == CareType.URGENT) {
            LocalDateTime urgentStart = scheduledTime;
            LocalDateTime urgentEnd = urgentStart.plusMinutes(schedule.getDurationMinutes() > 0 ? schedule.getDurationMinutes() : 30);

            List<CareSchedule> scheduledVets = careScheduleRepository.findScheduledForVetForUpdate(vetId, CareScheduleStatus.SCHEDULED);
            if (scheduledVets == null || scheduledVets.isEmpty()) {
                scheduledVets = careScheduleRepository.findScheduledForVet(vetId, CareScheduleStatus.SCHEDULED);
            }
            if (scheduledVets != null) {
                for (CareSchedule existing : scheduledVets) {
                    if (Objects.equals(existing.getId(), schedule.getId())) {
                        continue;
                    }
                    if ((existing.getCareType() == CareType.INITIAL || existing.getCareType() == CareType.ROUTINE) && existing.getScheduledAt() != null) {
                        LocalDateTime exStart = existing.getScheduledAt();
                        LocalDateTime exEnd = exStart.plusMinutes(existing.getDurationMinutes() > 0 ? existing.getDurationMinutes() : 30);
                        if (urgentStart.isBefore(exEnd) && urgentEnd.isAfter(exStart)) {
                            // Conflict detected -> yield existing INITIAL/ROUTINE schedule
                            vetOfferRepository.findFirstByCareScheduleIdAndStatus(existing.getId(), VetOfferStatus.ACCEPTED)
                                    .ifPresent(acceptedOffer -> {
                                        acceptedOffer.setStatus(VetOfferStatus.RELEASED);
                                        acceptedOffer.setRespondedAt(LocalDateTime.now());
                                        vetOfferRepository.save(acceptedOffer);
                                    });
                            existing.setStatus(CareScheduleStatus.REQUESTED);
                            existing.setVeterinarianId(null);
                            existing.setScheduledAt(null);
                            careScheduleRepository.save(existing);
                            dispatchOffersForSchedule(existing);
                        }
                    }
                }
            }
        }

        Horse horse = horseRepository.findById(schedule.getHorseId()).orElse(null);
        return VetOfferResponse.from(offer, schedule, horse);
    }

    private CareSchedule lockClinicalSchedule(Long scheduleId) {
        CareSchedule snapshot = careScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND", "Care schedule not found"));
        // Match Manager review's admission -> horse -> schedule lock order.
        AdmissionApplication admission = snapshot.getAdmissionId() == null ? null
                : admissionRepository.findByIdForUpdate(snapshot.getAdmissionId())
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Admission not found"));
        Horse horse = horseRepository.findByIdForUpdate(snapshot.getHorseId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found"));
        CareSchedule locked = careScheduleRepository.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND", "Care schedule not found"));
        // The compatibility adapter may already have these entities in its persistence context.
        // Preserve this transaction's writes, then read the state committed before we acquired the locks.
        entityManager.flush();
        if (admission != null) entityManager.refresh(admission);
        entityManager.refresh(horse);
        entityManager.refresh(locked);
        return locked;
    }

    private void ensureAvailableForAcceptance(CareSchedule schedule, Long vetId, LocalDateTime start) {
        if (careScheduleRepository.existsByVeterinarianIdAndStatus(vetId, CareScheduleStatus.IN_PROGRESS)
                || careScheduleRepository.existsByHorseIdAndStatus(schedule.getHorseId(), CareScheduleStatus.IN_PROGRESS)) {
            throw new ApiException(HttpStatus.CONFLICT, "SLOT_UNAVAILABLE", "Veterinarian or horse has an examination in progress");
        }
        List<CareSchedule> booked = new ArrayList<>(careScheduleRepository.findScheduledForVet(vetId, CareScheduleStatus.SCHEDULED));
        booked.addAll(careScheduleRepository.findScheduledForHorse(schedule.getHorseId(), CareScheduleStatus.SCHEDULED));
        for (CareSchedule other : booked) {
            if (Objects.equals(other.getId(), schedule.getId()) || !overlaps(schedule, start, other)) continue;
            boolean canYield = schedule.getCareType() == CareType.URGENT
                    && Objects.equals(other.getVeterinarianId(), vetId)
                    && (other.getCareType() == CareType.INITIAL || other.getCareType() == CareType.ROUTINE);
            if (!canYield) {
                throw new ApiException(HttpStatus.CONFLICT, "SLOT_UNAVAILABLE", "Veterinarian or horse is already scheduled in this slot");
            }
        }
    }

    private boolean overlaps(CareSchedule schedule, LocalDateTime start, CareSchedule other) {
        if (other.getScheduledAt() == null) return false;
        LocalDateTime end = start.plusMinutes(schedule.getDurationMinutes() > 0 ? schedule.getDurationMinutes() : 30);
        LocalDateTime otherEnd = other.getScheduledAt().plusMinutes(other.getDurationMinutes() > 0 ? other.getDurationMinutes() : 30);
        return start.isBefore(otherEnd) && end.isAfter(other.getScheduledAt());
    }

    @Transactional
    public VetOfferResponse declineOffer(Long offerId, Long vetId) {
        VetOffer offer = vetOfferRepository.findByIdForUpdate(offerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "OFFER_NOT_FOUND", "Offer not found"));

        if (!offer.getVeterinarianId().equals(vetId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only assigned veterinarian can decline this offer");
        }
        if (offer.getStatus() != VetOfferStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_OFFER_STATUS", "Offer is no longer pending");
        }

        offer.setStatus(VetOfferStatus.DECLINED);
        offer.setRespondedAt(LocalDateTime.now());
        vetOfferRepository.save(offer);

        CareSchedule schedule = careScheduleRepository.findByIdForUpdate(offer.getCareScheduleId()).orElse(null);
        if (schedule != null) {
            Optional<VetOffer> pendingRemaining = vetOfferRepository.findFirstByCareScheduleIdAndStatus(schedule.getId(), VetOfferStatus.PENDING);
            if (pendingRemaining.isEmpty()) {
                schedule.setStatus(CareScheduleStatus.REQUESTED);
                careScheduleRepository.save(schedule);
                dispatchOffersForSchedule(schedule);
            }
        }

        Horse horse = schedule != null ? horseRepository.findById(schedule.getHorseId()).orElse(null) : null;
        return VetOfferResponse.from(offer, schedule, horse);
    }

    @Transactional
    public CareScheduleResponse startCareSchedule(Long scheduleId, Long vetId) {
        userRepository.findByIdForUpdate(vetId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VET_NOT_FOUND", "Veterinarian not found"));
        CareSchedule schedule = lockClinicalSchedule(scheduleId);

        if (schedule.getStatus() != CareScheduleStatus.SCHEDULED &&
                !(schedule.getStatus() == CareScheduleStatus.REQUESTED && schedule.getCareType() == CareType.URGENT)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS", "Only SCHEDULED or URGENT REQUESTED care schedules can be started");
        }

        if (schedule.getVeterinarianId() != null && !schedule.getVeterinarianId().equals(vetId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the assigned veterinarian can start this schedule");
        }

        horseRepository.findByIdForUpdate(schedule.getHorseId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found"));
        ensureAvailableForAcceptance(schedule, vetId,
                schedule.getScheduledAt() != null ? schedule.getScheduledAt() : LocalDateTime.now());
        schedule.setVeterinarianId(vetId);
        if (schedule.getScheduledAt() == null) {
            schedule.setScheduledAt(LocalDateTime.now());
        }
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        return CareScheduleResponse.from(careScheduleRepository.save(schedule));
    }

    @Transactional
    public CareScheduleResponse completeCareSchedule(Long scheduleId, CompleteCareScheduleRequest request, Long vetId) {
        if (request.getFindings() == null || request.getFindings().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Findings are required");
        }
        if (request.getDiagnosis() == null || request.getDiagnosis().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Diagnosis is required");
        }
        if (Boolean.TRUE.equals(request.getRejectAdmission())) {
            request.setTrainingDecision(TrainingDecision.BLOCKED);
            if (request.getRestrictionDetails() == null || request.getRestrictionDetails().isBlank()) {
                request.setRestrictionDetails(request.getRejectionReason());
            }
        }
        if (request.getTrainingDecision() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Training decision is required");
        }
        if ((request.getTrainingDecision() == TrainingDecision.RESTRICTED || request.getTrainingDecision() == TrainingDecision.BLOCKED)
                && (request.getRestrictionDetails() == null || request.getRestrictionDetails().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Restriction details required for RESTRICTED or BLOCKED decision");
        }
        if (Boolean.TRUE.equals(request.getRejectAdmission())
                && (request.getRejectionReason() == null || request.getRejectionReason().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Rejection reason is required when rejecting admission");
        }

        CareSchedule schedule = lockClinicalSchedule(scheduleId);

        if (schedule.getStatus() != CareScheduleStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS", "Care schedule must be IN_PROGRESS to complete");
        }
        if (!Objects.equals(schedule.getVeterinarianId(), vetId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the assigned veterinarian can complete this schedule");
        }

        AdmissionApplication linkedAdmission = schedule.getAdmissionId() == null ? null
                : admissionRepository.findByIdForUpdate(schedule.getAdmissionId())
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Admission not found"));
        if (schedule.getCareType() == CareType.INITIAL && (linkedAdmission == null
                || (linkedAdmission.getStatus() != AdmissionStatus.VET_REVIEW
                    && linkedAdmission.getStatus() != AdmissionStatus.PENDING_RECHECK))) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_REVIEW_STATE", "Admission is not awaiting a vet examination");
        }
        Horse horse = horseRepository.findByIdForUpdate(schedule.getHorseId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found"));

        // BR-TRN-05: If horse has active URGENT care, cannot set trainingStatus to ALLOWED
        if (request.getTrainingDecision() == TrainingDecision.ALLOWED) {
            boolean activeUrgent;
            List<CareScheduleStatus> activeStatuses = List.of(
                    CareScheduleStatus.REQUESTED,
                    CareScheduleStatus.AWAITING_VET_CONFIRMATION,
                    CareScheduleStatus.SCHEDULED,
                    CareScheduleStatus.IN_PROGRESS);
            if (schedule.getCareType() == CareType.URGENT) {
                activeUrgent = careScheduleRepository.existsByHorseIdAndCareTypeAndStatusInAndIdNot(
                        horse.getId(), CareType.URGENT, activeStatuses, schedule.getId());
            } else {
                activeUrgent = careScheduleRepository.existsByHorseIdAndCareTypeAndStatusIn(
                        horse.getId(), CareType.URGENT, activeStatuses);
            }
            if (activeUrgent) {
                throw new ApiException(HttpStatus.CONFLICT, "BR_TRN_05", "Cannot set training status to ALLOWED while horse has active URGENT care");
            }
        }

        // Atomic update Horse.trainingStatus and trainingLocked
        TrainingStatus newStatus = request.getTrainingDecision().toTrainingStatus();
        if (newStatus == TrainingStatus.ALLOWED) {
            horse.setTrainingStatus(TrainingStatus.ALLOWED);
            horse.setTrainingLocked(horse.getCurrentStatus() == HorseStatus.CANDIDATE);
            horse.setTrainingLockReason(horse.isTrainingLocked() ? "Admission pending trainer and manager review" : null);
            horse.setTrainingLockReviewDate(null);
            horse.setTrainingLockVetId(null);
        } else {
            horse.setTrainingLocked(true);
            horse.setTrainingStatus(newStatus);
            horse.setTrainingLockReason(request.getRestrictionDetails());
            horse.setTrainingLockVetId(vetId);
        }
        horse.setTrainingLockUpdatedAt(LocalDateTime.now());
        horseRepository.save(horse);

        LocalDateTime now = LocalDateTime.now();
        HealthRecord record = new HealthRecord();
        record.setHorseId(horse.getId());
        record.setVeterinarianId(vetId);
        record.setCareScheduleId(schedule.getId());
        record.setRecordType(schedule.getCareType().name());
        record.setExaminedAt(now);
        record.setFindings(request.getFindings().trim());
        record.setDiagnosis(request.getDiagnosis().trim());
        record.setTreatment(request.getTreatment() != null ? request.getTreatment().trim() : null);
        record.setTrainingDecision(request.getTrainingDecision());
        record.setRestrictionDetails(request.getRestrictionDetails() != null ? request.getRestrictionDetails().trim() : null);
        record.setSymptoms(request.getSymptoms() != null ? request.getSymptoms().trim() : null);
        record.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);
        record.setFollowUpDate(request.getFollowUpDate());
        horse.setTrainingLockReviewDate(request.getFollowUpDate());

        if (Boolean.TRUE.equals(request.getRejectAdmission())) {
            record.setVetDecision(VetDecision.REJECTED);
            record.setRejectionReason(request.getRejectionReason());
        } else if (request.getTrainingDecision() != TrainingDecision.ALLOWED) {
            record.setVetDecision(VetDecision.RECHECK_REQUIRED);
            record.setRejectionReason(null);
        } else {
            record.setVetDecision(VetDecision.APPROVED);
            record.setRejectionReason(null);
        }
        HealthRecord savedRecord = healthRecordRepository.save(record);

        // Save metrics if present
        if (request.getMetrics() != null) {
            for (HorseHealthMetricRequest metricReq : request.getMetrics()) {
                HorseHealthMetric m = new HorseHealthMetric();
                m.setHorseId(horse.getId());
                m.setHealthRecordId(savedRecord.getId());
                m.setRecordedAt(now);
                m.setHeartRate(metricReq.getHeartRate());
                m.setTemperature(metricReq.getTemperature());
                m.setWeight(metricReq.getWeight());
                m.setRespiratoryRate(metricReq.getRespiratoryRate());
                m.setHydrationStatus(metricReq.getHydrationStatus());
                m.setBodyConditionScore(metricReq.getBodyConditionScore());
                m.setNotes(metricReq.getNotes());
                metricRepository.save(m);
            }
        }

        // If exam linked to AdmissionApplication, update admission status
        if (schedule.getAdmissionId() != null) {
            AdmissionApplication admission = linkedAdmission;
            if (admission != null) {
                boolean isInitial = schedule.getCareType() == CareType.INITIAL;
                boolean isPendingRecheck = admission.getStatus() == AdmissionStatus.PENDING_RECHECK;

                // Only update admission if it's in a state we should transition from
                if (isInitial || isPendingRecheck) {
                    admission.setVeterinarianId(vetId);
                    admission.setVetReviewedAt(now);
                    admission.setVetFeedback(request.getFindings());
                    if (Boolean.TRUE.equals(request.getRejectAdmission())) {
                        admission.setStatus(AdmissionStatus.REJECTED);
                        admission.setVetDecision(VetDecision.REJECTED);
                        if (request.getRejectionReason() != null && !request.getRejectionReason().isBlank()) {
                            admission.setVetFeedback(request.getRejectionReason());
                        }
                        horse.setCurrentStatus(HorseStatus.REJECTED);
                        if (admission.getQuarantineStallId() != null) {
                            stallRepository.findById(admission.getQuarantineStallId()).ifPresent(stall -> {
                                stall.setStatus(StallStatus.AVAILABLE);
                                stallRepository.save(stall);
                            });
                            horse.setCurrentStallId(null);
                        }
                        horseRepository.save(horse);
                    } else {
                        // ALLOWED, RESTRICTED, or BLOCKED without rejectAdmission:
                        // Advances to TRAINER_REVIEW, horse remains CANDIDATE in quarantine stall
                        admission.setStatus(AdmissionStatus.TRAINER_REVIEW);
                        admission.setVetDecision(record.getVetDecision());
                        if (request.getTrainingDecision() == TrainingDecision.ALLOWED) {
                            // Medical clearance does not bypass final admission approval.
                            horse.setTrainingLocked(true);
                            horse.setTrainingLockReason("Admission pending trainer and manager review");
                            horse.setTrainingLockVetId(null);
                        }
                    }
                    admissionRepository.save(admission);
                }
            }
        }

        schedule.setStatus(CareScheduleStatus.COMPLETED);
        schedule.setCompletedAt(now);
        return CareScheduleResponse.from(careScheduleRepository.save(schedule));
    }

    @Transactional
    public CareScheduleResponse cancelCareSchedule(Long scheduleId, CancelCareScheduleRequest request, Long userId) {
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Cancel reason is required");
        }

        CareSchedule schedule = careScheduleRepository.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND", "Care schedule not found"));

        if (schedule.getStatus() == CareScheduleStatus.IN_PROGRESS || schedule.getStatus() == CareScheduleStatus.COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS", "Cannot cancel care schedule that is in progress or completed");
        }
        if (schedule.getStatus() == CareScheduleStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS", "Cannot cancel completed or already cancelled schedule");
        }

        User actor = userRepository.findById(userId).orElse(null);
        boolean isManager = actor != null && actor.getRole() != null && "CLUB_MANAGER".equals(actor.getRole().getName());
        boolean isAssignedVet = schedule.getVeterinarianId() != null && schedule.getVeterinarianId().equals(userId);

        if (!isManager && !isAssignedVet) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only assigned veterinarian or club manager can cancel schedule");
        }

        schedule.setStatus(CareScheduleStatus.CANCELLED);
        schedule.setCancelReason(request.getReason().trim());
        careScheduleRepository.save(schedule);

        List<VetOffer> offers = vetOfferRepository.findByCareScheduleId(schedule.getId());
        for (VetOffer offer : offers) {
            if (offer.getStatus() == VetOfferStatus.PENDING) {
                offer.setStatus(VetOfferStatus.EXPIRED);
                vetOfferRepository.save(offer);
            }
        }

        return CareScheduleResponse.from(schedule);
    }

    @Transactional(readOnly = true)
    public CareScheduleDetailResponse getScheduleDetail(Long scheduleId) {
        CareSchedule schedule = careScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND", "Care schedule not found"));
        Horse horse = horseRepository.findById(schedule.getHorseId()).orElse(null);
        User vet = schedule.getVeterinarianId() != null ? userRepository.findById(schedule.getVeterinarianId()).orElse(null) : null;
        HealthRecord hr = healthRecordRepository.findByCareScheduleId(scheduleId).orElse(null);

        List<VetOfferResponse> offerResponses = vetOfferRepository.findByCareScheduleId(scheduleId).stream()
                .map(vo -> VetOfferResponse.from(vo, schedule, horse))
                .toList();

        return CareScheduleDetailResponse.of(schedule, horse, vet, hr, offerResponses);
    }

    @Transactional(readOnly = true)
    public Page<CareScheduleResponse> listSchedules(CareScheduleStatus status, CareType careType,
            Long horseId, Long vetId, Long admissionId, Pageable pageable) {
        return careScheduleRepository.findFiltered(status, careType, horseId, vetId, admissionId, pageable)
                .map(CareScheduleResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<VetOfferResponse> listOffersForVet(Long vetId, VetOfferStatus status, Pageable pageable) {
        Page<VetOffer> page = status != null ?
                vetOfferRepository.findByVeterinarianIdAndStatus(vetId, status, pageable) :
                vetOfferRepository.findByVeterinarianId(vetId, pageable);
        return page.map(vo -> {
            CareSchedule cs = careScheduleRepository.findById(vo.getCareScheduleId()).orElse(null);
            Horse h = cs != null ? horseRepository.findById(cs.getHorseId()).orElse(null) : null;
            return VetOfferResponse.from(vo, cs, h);
        });
    }

    @Transactional(readOnly = true)
    public List<VetOfferResponse> listPendingOffersForVet(Long vetId) {
        List<VetOffer> offers = vetOfferRepository.findByVeterinarianIdAndStatus(vetId, VetOfferStatus.PENDING);
        return offers.stream().map(vo -> {
            CareSchedule cs = careScheduleRepository.findById(vo.getCareScheduleId()).orElse(null);
            Horse h = cs != null ? horseRepository.findById(cs.getHorseId()).orElse(null) : null;
            return VetOfferResponse.from(vo, cs, h);
        }).toList();
    }

    @Transactional
    public CareScheduleResponse createNextSchedule(CreateNextScheduleRequest request, Long userId) {
        if (request.getHorseId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "horseId is required");
        }
        Horse horse = horseRepository.findById(request.getHorseId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found"));

        CareType careType = request.getCareType() != null ? request.getCareType() : CareType.ROUTINE;
        if (careType == CareType.INITIAL) {
            return createInitialSchedule(request.getAdmissionId(), request.getHorseId());
        }

        // Idempotency check: if active routine care already exists for this horse, return existing
        List<CareScheduleStatus> activeStatuses = List.of(
                CareScheduleStatus.REQUESTED,
                CareScheduleStatus.AWAITING_VET_CONFIRMATION,
                CareScheduleStatus.SCHEDULED,
                CareScheduleStatus.IN_PROGRESS
        );
        Optional<CareSchedule> existing = careScheduleRepository
                .findFirstByHorseIdAndCareTypeAndStatusInOrderByCreatedAtDesc(horse.getId(), careType, activeStatuses);
        if (existing.isPresent()) {
            return CareScheduleResponse.from(existing.get());
        }

        // Determine requested proposed time for VetOffer without setting schedule.scheduledAt prematurely
        LocalDateTime proposedTime = null;
        if (request.getScheduledAt() != null) {
            proposedTime = request.getScheduledAt();
        } else if (request.getScheduledDate() != null && !request.getScheduledDate().isBlank()) {
            try {
                proposedTime = java.time.LocalDate.parse(request.getScheduledDate()).atTime(14, 0);
            } catch (Exception e) {
                proposedTime = LocalDateTime.now().plusDays(3).withHour(14).withMinute(0);
            }
        }

        CareSchedule schedule = new CareSchedule();
        schedule.setHorseId(horse.getId());
        schedule.setAdmissionId(request.getAdmissionId());
        schedule.setCareType(careType);
        schedule.setStatus(CareScheduleStatus.REQUESTED);
        schedule.setDurationMinutes(DEFAULT_DURATION_MINUTES);
        schedule.setDescription(request.getDescription() != null ? request.getDescription().trim() : "Follow-up care schedule");
        schedule.setRequestedAt(proposedTime);
        // scheduledAt remains NULL until a veterinarian accepts the offer

        CareSchedule saved = careScheduleRepository.save(schedule);
        dispatchOffersForSchedule(saved, proposedTime);
        return CareScheduleResponse.from(saved);
    }

    @Transactional
    public void dispatchOffersForSchedule(CareSchedule schedule) {
        dispatchOffersForSchedule(schedule, null);
    }

    @Transactional
    public void dispatchOffersForSchedule(CareSchedule schedule, LocalDateTime proposedTimeOverride) {
        if (schedule.getStatus() != CareScheduleStatus.REQUESTED) return;

        Optional<VetOffer> existingPending = vetOfferRepository.findFirstByCareScheduleIdAndStatus(schedule.getId(), VetOfferStatus.PENDING);
        if (existingPending.isPresent()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime resolvedProposedTime = proposedTimeOverride;
        if (resolvedProposedTime == null) {
            if (schedule.getRequestedAt() != null) {
                resolvedProposedTime = schedule.getRequestedAt();
            } else if (schedule.getScheduledAt() != null) {
                resolvedProposedTime = schedule.getScheduledAt();
            } else {
                List<VetOffer> pastOffers = vetOfferRepository.findByCareScheduleId(schedule.getId());
                resolvedProposedTime = pastOffers.stream()
                        .map(VetOffer::getProposedScheduledAt)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(now.plusHours(2));
            }
        }
        final LocalDateTime proposedScheduledAt = resolvedProposedTime;
        int duration = schedule.getDurationMinutes() > 0 ? schedule.getDurationMinutes() : 30;
        LocalDateTime proposedEnd = proposedScheduledAt.plusMinutes(duration);

        // Check horse availability: verify the horse has no conflicting schedule with status IN_PROGRESS or SCHEDULED at that time
        if (schedule.getHorseId() != null) {
            if (careScheduleRepository.existsByHorseIdAndStatus(schedule.getHorseId(), CareScheduleStatus.IN_PROGRESS)) {
                return;
            }
            List<CareSchedule> horseSchedules = careScheduleRepository.findScheduledForHorse(schedule.getHorseId(), CareScheduleStatus.SCHEDULED);
            if (horseSchedules != null) {
                boolean horseConflict = horseSchedules.stream().anyMatch(hs -> {
                    if (Objects.equals(hs.getId(), schedule.getId())) return false;
                    LocalDateTime hsStart = hs.getScheduledAt();
                    if (hsStart == null) return false;
                    LocalDateTime hsEnd = hsStart.plusMinutes(hs.getDurationMinutes() > 0 ? hs.getDurationMinutes() : 30);
                    return proposedScheduledAt.isBefore(hsEnd) && proposedEnd.isAfter(hsStart);
                });
                if (horseConflict) {
                    return;
                }
            }
        }

        List<User> vets = userRepository.findActiveVeterinarians();
        if (vets == null || vets.isEmpty()) return;

        // Filter veterinarian candidates:
        // 1. Exclude any vet who currently has a care schedule with status IN_PROGRESS
        // 2. Check slot availability: exclude any vet who has a schedule with status SCHEDULED in the time window [proposedScheduledAt, proposedScheduledAt + durationMinutes]
        List<User> eligibleVets = vets.stream()
                .filter(v -> !careScheduleRepository.existsByVeterinarianIdAndStatus(v.getId(), CareScheduleStatus.IN_PROGRESS))
                .filter(v -> {
                    List<CareSchedule> scheduled = careScheduleRepository.findScheduledForVet(v.getId(), CareScheduleStatus.SCHEDULED);
                    if (scheduled == null || scheduled.isEmpty()) {
                        scheduled = careScheduleRepository.findConflictingSchedulesForVet(v.getId());
                    }
                    if (scheduled == null || scheduled.isEmpty()) {
                        return true;
                    }
                    return scheduled.stream().noneMatch(vs -> {
                        if (Objects.equals(vs.getId(), schedule.getId())) return false;
                        LocalDateTime vsStart = vs.getScheduledAt();
                        if (vsStart == null) return false;
                        LocalDateTime vsEnd = vsStart.plusMinutes(vs.getDurationMinutes() > 0 ? vs.getDurationMinutes() : 30);
                        return proposedScheduledAt.isBefore(vsEnd) && proposedEnd.isAfter(vsStart);
                    });
                })
                .toList();

        if (eligibleVets.isEmpty()) {
            return;
        }

        List<VetOffer> pastOffers = vetOfferRepository.findByCareScheduleId(schedule.getId());
        int round = pastOffers.isEmpty() ? 1 : pastOffers.stream().mapToInt(VetOffer::getRound).max().orElse(1);

        final int targetRound = round;
        Set<Long> offeredVetIdsInRound = pastOffers.stream()
                .filter(vo -> vo.getRound() == targetRound)
                .map(VetOffer::getVeterinarianId)
                .collect(java.util.stream.Collectors.toSet());

        List<User> candidates = eligibleVets.stream()
                .filter(v -> !offeredVetIdsInRound.contains(v.getId()))
                .toList();

        int finalRound = targetRound;
        if (candidates.isEmpty()) {
            finalRound = targetRound + 1;
            candidates = eligibleVets;
        }

        User selectedVet = candidates.get(0);
        LocalDateTime expiresAt = now.plusMinutes(schedule.getCareType() == CareType.URGENT ? 3 :
                (schedule.getCareType() == CareType.INITIAL ? 30 : OFFER_EXPIRATION_MINUTES));

        VetOffer offer = new VetOffer();
        offer.setCareScheduleId(schedule.getId());
        offer.setVeterinarianId(selectedVet.getId());
        offer.setRound(finalRound);
        offer.setStatus(VetOfferStatus.PENDING);
        offer.setOfferedAt(now);
        offer.setExpiresAt(expiresAt);
        offer.setProposedScheduledAt(proposedScheduledAt);
        vetOfferRepository.save(offer);

        schedule.setStatus(CareScheduleStatus.AWAITING_VET_CONFIRMATION);
        careScheduleRepository.save(schedule);
    }

    @Transactional
    public void expirePendingOffers() {
        LocalDateTime now = LocalDateTime.now();
        List<VetOffer> expired = vetOfferRepository.findByStatusAndExpiresAtBefore(VetOfferStatus.PENDING, now);
        Set<Long> affectedScheduleIds = new HashSet<>();

        for (VetOffer offer : expired) {
            offer.setStatus(VetOfferStatus.EXPIRED);
            vetOfferRepository.save(offer);
            affectedScheduleIds.add(offer.getCareScheduleId());
        }

        for (Long scheduleId : affectedScheduleIds) {
            CareSchedule schedule = careScheduleRepository.findByIdForUpdate(scheduleId).orElse(null);
            if (schedule != null && schedule.getStatus() == CareScheduleStatus.AWAITING_VET_CONFIRMATION) {
                Optional<VetOffer> pending = vetOfferRepository.findFirstByCareScheduleIdAndStatus(scheduleId, VetOfferStatus.PENDING);
                if (pending.isEmpty()) {
                    schedule.setStatus(CareScheduleStatus.REQUESTED);
                    careScheduleRepository.save(schedule);
                    dispatchOffersForSchedule(schedule);
                }
            }
        }
    }

    @Transactional
    public void dispatchRequestedSchedules() {
        List<CareSchedule> requested = careScheduleRepository.findByStatus(CareScheduleStatus.REQUESTED);
        for (CareSchedule schedule : requested) {
            dispatchOffersForSchedule(schedule);
        }
    }
}