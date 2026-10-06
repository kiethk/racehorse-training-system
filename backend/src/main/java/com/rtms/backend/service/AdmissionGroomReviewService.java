package com.rtms.backend.service;
import com.rtms.backend.dto.GroomAdmissionReviewRequest;
import com.rtms.backend.dto.GroomArrivalConfirmationRequest;
import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.entity.CandidateHorseProfile;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.enums.ArrivalStatus;
import com.rtms.backend.enums.ReviewDecision;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.repository.CandidateHorseProfileRepository;
import com.rtms.backend.repository.CareScheduleRepository;
import com.rtms.backend.service.CareScheduleService;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.HorsePedigree;
import com.rtms.backend.enums.HorseStatus;
import com.rtms.backend.repository.HorsePedigreeRepository;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.entity.StableStall;
import com.rtms.backend.enums.StallStatus;
import com.rtms.backend.repository.StableStallRepository;
import com.rtms.backend.enums.TrainingStatus;
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

    public AdmissionGroomReviewService(
            AdmissionApplicationRepository admissionApplicationRepository,
            CandidateHorseProfileRepository candidateHorseProfileRepository,
            StableStallRepository stableStallRepository,
            HorseRepository horseRepository,
            HorsePedigreeRepository horsePedigreeRepository,
            CareScheduleRepository careScheduleRepository,
            CareScheduleService careScheduleService) {
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.candidateHorseProfileRepository = candidateHorseProfileRepository;
        this.stableStallRepository = stableStallRepository;
        this.horseRepository = horseRepository;
        this.horsePedigreeRepository = horsePedigreeRepository;
        this.careScheduleRepository = careScheduleRepository;
        this.careScheduleService = careScheduleService;
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
    public AdmissionApplication confirmArrival(Long admissionId, Long groomId,
            GroomArrivalConfirmationRequest request) {
        AdmissionApplication admission = admissionApplicationRepository.findByIdForUpdate(admissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission not found"));

        if (admission.getStatus() != AdmissionStatus.WAITING_FOR_ARRIVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Admission is not waiting for horse arrival");
        }

        String feedback = request == null || request.feedback() == null
                ? null : request.feedback().trim();
        if (request == null || !request.confirmed()) {
            if (feedback == null || feedback.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Feedback is required when rejecting the arriving horse");
            }
            releaseReservedQuarantineStall(admission);
            admission.setQuarantineStallId(null);
            admission.setStatus(AdmissionStatus.REJECTED);
            admission.setGroomFeedback(feedback);
            return admissionApplicationRepository.save(admission);
        }

        CandidateHorseProfile candidate = candidateHorseProfileRepository
                .findByAdmissionId(admission.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Candidate horse profile not found"));

        StableStall quarantineStall = stableStallRepository
                .findQuarantineStallByIdForUpdate(admission.getQuarantineStallId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT, "Reserved quarantine stall not found"));
        if (quarantineStall.getStatus() != StallStatus.OCCUPIED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Reserved quarantine stall is no longer occupied");
        }

        Horse horse = findReusableHorseOrCreateNew(admission, candidate);
        applyCandidateSnapshot(horse, admission, candidate, quarantineStall);
        horse = horseRepository.save(horse);
        updatePedigree(horse.getId(), candidate);

        admission.setHorseId(horse.getId());
        admission.setArrivalStatus(ArrivalStatus.CONFIRMED);
        admission.setArrivalConfirmedAt(LocalDateTime.now());
        admission.setArrivalConfirmedBy(groomId);
        admission.setStatus(AdmissionStatus.VET_REVIEW);
        AdmissionApplication saved = admissionApplicationRepository.save(admission);

        careScheduleService.createInitialScheduleForGroom(saved.getId(), horse.getId());
        return saved;
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

        StableStall quarantineStall = stableStallRepository
                .findFirstAvailableQuarantineStallForUpdate()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No quarantine stall available"));

        quarantineStall.setStatus(StallStatus.OCCUPIED);
        stableStallRepository.save(quarantineStall);

        admission.setHorseId(null);
        admission.setQuarantineStallId(quarantineStall.getId());
        admission.setArrivalStatus(ArrivalStatus.PENDING);
        admission.setStatus(AdmissionStatus.WAITING_FOR_ARRIVAL);

        return admissionApplicationRepository.save(admission);
    }

    private void releaseReservedQuarantineStall(AdmissionApplication admission) {
        if (admission.getQuarantineStallId() == null) {
            return;
        }
        StableStall stall = stableStallRepository.findQuarantineStallByIdForUpdate(
                admission.getQuarantineStallId()).orElse(null);
        if (stall != null) {
            stall.setStatus(StallStatus.AVAILABLE);
            stableStallRepository.save(stall);
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
        horse.setTrainingStatus(TrainingStatus.BLOCKED);
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

    private void stampGroomReview(AdmissionApplication admission, Long groomId, ReviewDecision decision, String feedback) {
        admission.setGroomId(groomId);
        admission.setGroomDecision(decision);
        admission.setGroomFeedback(feedback);
        admission.setGroomReviewedAt(LocalDateTime.now());
    }
}
