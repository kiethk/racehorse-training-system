package com.rtms.backend.horse.service;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.horse.dto.HorseAdmissionHistoryResponse;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class HorseAdmissionService {

    private final HorseRepository horseRepository;
    private final AdmissionApplicationRepository admissionRepo;
    private final UserRepository userRepo;

    public HorseAdmissionService(HorseRepository horseRepository,
                                 AdmissionApplicationRepository admissionRepo,
                                 UserRepository userRepo) {
        this.horseRepository = horseRepository;
        this.admissionRepo = admissionRepo;
        this.userRepo = userRepo;
    }

    /**
     * Retrieves all admission applications linked to a specific horse ID.
     * Enforces horse access control: Owner can only view their own horse;
     * Club Manager and staff with HORSE_VIEW can view.
     */
    public List<HorseAdmissionHistoryResponse> getAdmissionsForHorse(Long horseId, AuthenticatedUser currentUser) {
        Horse horse = horseRepository.findById(horseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Horse not found with id: " + horseId));

        if (currentUser != null && "HORSE_OWNER".equalsIgnoreCase(currentUser.getRole())) {
            if (!Objects.equals(horse.getOwnerId(), currentUser.getUserId())) {
                throw new AccessDeniedException("Access denied: You do not own this horse.");
            }
        }

        List<AdmissionApplication> admissions = admissionRepo.findByHorseIdOrderBySubmittedAtDesc(horseId);
        if (admissions.isEmpty()) {
            return Collections.emptyList();
        }

        // Batch collect user IDs for name resolution (groom, vet, trainer, manager) without N+1
        Set<Long> userIds = new HashSet<>();
        for (AdmissionApplication a : admissions) {
            if (a.getGroomId() != null) userIds.add(a.getGroomId());
            if (a.getVeterinarianId() != null) userIds.add(a.getVeterinarianId());
            if (a.getTrainerId() != null) userIds.add(a.getTrainerId());
            if (a.getManagerId() != null) userIds.add(a.getManagerId());
        }

        Map<Long, String> userNameMap = userRepo.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName, (existing, replacement) -> existing));

        return admissions.stream().map(a -> new HorseAdmissionHistoryResponse(
                a.getId(),
                a.getHorseId(),
                a.getStatus(),
                a.getSubmittedAt(),
                a.getGroomId(),
                a.getGroomId() != null ? userNameMap.get(a.getGroomId()) : null,
                a.getGroomReviewedAt(),
                a.getGroomDecision(),
                a.getGroomFeedback(),
                a.getArrivedAt(),
                a.getVeterinarianId(),
                a.getVeterinarianId() != null ? userNameMap.get(a.getVeterinarianId()) : null,
                a.getVetReviewedAt(),
                a.getVetTrainingDecision(),
                a.getVetFeedback(),
                a.getTrainerId(),
                a.getTrainerId() != null ? userNameMap.get(a.getTrainerId()) : null,
                a.getTrainerReviewedAt(),
                a.getTrainerFeedback(),
                a.getManagerId(),
                a.getManagerId() != null ? userNameMap.get(a.getManagerId()) : null,
                a.getManagerReviewedAt(),
                a.getManagerDecision(),
                a.getManagerFeedback()
        )).toList();
    }
}
