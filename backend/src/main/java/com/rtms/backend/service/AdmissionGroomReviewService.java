package com.rtms.backend.service;

import com.rtms.backend.dto.GroomAdmissionReviewRequest;
import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.entity.CandidateHorseProfile;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.HorsePedigree;
import com.rtms.backend.entity.PreventiveCareSchedule;
import com.rtms.backend.entity.StableStall;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.enums.HorseStatus;
import com.rtms.backend.enums.ReviewDecision;
import com.rtms.backend.enums.StallStatus;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.repository.CandidateHorseProfileRepository;
import com.rtms.backend.repository.HorsePedigreeRepository;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.PreventiveCareScheduleRepository;
import com.rtms.backend.repository.StableStallRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdmissionGroomReviewService {

    private static final String INITIAL_EXAM = "INITIAL_EXAM";
    private static final List<String> OPEN_INITIAL_EXAM_STATUSES = List.of("PENDING", "OVERDUE");

    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final CandidateHorseProfileRepository candidateHorseProfileRepository;
    private final StableStallRepository stableStallRepository;
    private final HorseRepository horseRepository;
    private final HorsePedigreeRepository horsePedigreeRepository;
    private final PreventiveCareScheduleRepository preventiveCareScheduleRepository;

    public AdmissionGroomReviewService(
            AdmissionApplicationRepository admissionApplicationRepository,
            CandidateHorseProfileRepository candidateHorseProfileRepository,
            StableStallRepository stableStallRepository,
            HorseRepository horseRepository,
            HorsePedigreeRepository horsePedigreeRepository,
            PreventiveCareScheduleRepository preventiveCareScheduleRepository) {
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.candidateHorseProfileRepository = candidateHorseProfileRepository;
        this.stableStallRepository = stableStallRepository;
        this.horseRepository = horseRepository;
        this.horsePedigreeRepository = horsePedigreeRepository;
        this.preventiveCareScheduleRepository = preventiveCareScheduleRepository;
    }

    @Transactional
    public AdmissionApplication review(
            Long admissionId,
            Long groomId,
            GroomAdmissionReviewRequest request) {

        AdmissionApplication admission = admissionApplicationRepository.findByIdForUpdate(admissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission not found"));

        if (admission.getStatus() != AdmissionStatus.GROOM_REVIEW) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Admission is not awaiting Groom review");
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
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Admission is not waiting for stall capacity");
        }

        if (admission.getGroomDecision() != ReviewDecision.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Admission has not been approved by Groom");
        }

        return moveForwardIfCapacityAvailable(admission);
    }

    private AdmissionApplication reject(
            AdmissionApplication admission,
            Long groomId,
            String feedback) {

        if (feedback == null || feedback.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Feedback is required when rejecting an admission");
        }

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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Candidate horse profile not found"));

        StableStall quarantineStall = stableStallRepository
                .findFirstAvailableQuarantineStallForUpdate()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "No available quarantine stall"));

        Horse horse = findReusableHorseOrCreateNew(admission, candidate);
        applyCandidateSnapshot(horse, admission, candidate, quarantineStall);
        horse = horseRepository.save(horse);

        updatePedigree(horse.getId(), candidate);
        ensureInitialExam(horse.getId());

        quarantineStall.setStatus(StallStatus.OCCUPIED);
        stableStallRepository.save(quarantineStall);

        admission.setHorseId(horse.getId());
        admission.setQuarantineStallId(quarantineStall.getId());
        admission.setStatus(AdmissionStatus.VET_REVIEW);

        return admissionApplicationRepository.save(admission);
    }

    private boolean hasAdmissionCapacity() {
        long availableQuarantineStalls = stableStallRepository.countAvailableQuarantineStalls();
        long availableRegularStalls = stableStallRepository.countAvailableRegularStalls();
        long occupiedQuarantineStalls = stableStallRepository.countOccupiedQuarantineStalls();

        return AdmissionCapacityPolicy.isAvailable(
                availableQuarantineStalls, availableRegularStalls, occupiedQuarantineStalls);
    }

    private Horse findReusableHorseOrCreateNew(
            AdmissionApplication admission,
            CandidateHorseProfile candidate) {

        String ueln = candidate.getRegistrationNumber();
        if (ueln == null || ueln.isBlank()) {
            return new Horse();
        }

        List<Horse> matches = horseRepository.findByRegistrationNumber(ueln);
        if (matches.isEmpty()) {
            return new Horse();
        }

        Horse existing = matches.get(0);
        if (existing.getCurrentStatus() == HorseStatus.REJECTED
                && admission.getOwnerId().equals(existing.getOwnerId())) {
            return existing;
        }

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Horse with this UELN already exists");
    }

    private void applyCandidateSnapshot(
            Horse horse,
            AdmissionApplication admission,
            CandidateHorseProfile candidate,
            StableStall quarantineStall) {

        horse.setName(candidate.getName());
        horse.setBreed(candidate.getBreed());
        horse.setDateOfBirth(candidate.getDateOfBirth());
        horse.setOwnerId(admission.getOwnerId());
        horse.setRegistryName(candidate.getRegistryName());
        horse.setRegistrationNumber(candidate.getRegistrationNumber());
        horse.setCurrentStatus(HorseStatus.CANDIDATE);
        horse.setCurrentStallId(quarantineStall.getId());
    }

    private void updatePedigree(Long horseId, CandidateHorseProfile candidate) {
        HorsePedigree pedigree = horsePedigreeRepository.findByHorseId(horseId)
                .orElseGet(HorsePedigree::new);

        pedigree.setHorseId(horseId);
        pedigree.setSireName(candidate.getSireName());
        pedigree.setSireRegistrationNumber(candidate.getSireRegistrationNumber());
        pedigree.setDamName(candidate.getDamName());
        pedigree.setDamRegistrationNumber(candidate.getDamRegistrationNumber());
        pedigree.setPedigreeNotes(candidate.getPedigreeNotes());

        horsePedigreeRepository.save(pedigree);
    }

    private void ensureInitialExam(Long horseId) {
        boolean alreadyOpen = preventiveCareScheduleRepository
                .existsByHorseIdAndCareTypeAndStatusIn(
                        horseId,
                        INITIAL_EXAM,
                        OPEN_INITIAL_EXAM_STATUSES);

        if (alreadyOpen) {
            return;
        }

        PreventiveCareSchedule schedule = new PreventiveCareSchedule();
        schedule.setHorseId(horseId);
        schedule.setCareType(INITIAL_EXAM);
        schedule.setStatus("PENDING");
        schedule.setScheduledDate(null);
        schedule.setDescription("Initial admission physical examination in quarantine area");

        preventiveCareScheduleRepository.save(schedule);
    }

    private void stampGroomReview(
            AdmissionApplication admission,
            Long groomId,
            ReviewDecision decision,
            String feedback) {

        admission.setGroomId(groomId);
        admission.setGroomDecision(decision);
        admission.setGroomFeedback(feedback);
        admission.setGroomReviewedAt(LocalDateTime.now());
    }
}
