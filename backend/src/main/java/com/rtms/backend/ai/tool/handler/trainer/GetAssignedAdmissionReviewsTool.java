package com.rtms.backend.ai.tool.handler.trainer;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.entity.CandidateHorseProfile;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.repository.CandidateHorseProfileRepository;
import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class GetAssignedAdmissionReviewsTool implements AiToolHandler {

    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final CandidateHorseProfileRepository candidateHorseProfileRepository;
    private final UserRepository userRepository;
    private final AiToolSupport support;

    @Autowired
    public GetAssignedAdmissionReviewsTool(AdmissionApplicationRepository admissionApplicationRepository,
                                           @Autowired(required = false) CandidateHorseProfileRepository candidateHorseProfileRepository,
                                           @Autowired(required = false) UserRepository userRepository,
                                           AiToolSupport support) {
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.candidateHorseProfileRepository = candidateHorseProfileRepository;
        this.userRepository = userRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getAssignedAdmissionReviews";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        List<AdmissionApplication> apps = admissionApplicationRepository.findAllByOrderBySubmittedAtDesc();
        Long trainerId = currentUser.getUserId();

        List<AdmissionApplication> pendingTrainerApps = apps.stream()
                .filter(a -> a.getStatus() == AdmissionStatus.TRAINER_REVIEW
                        && (a.getTrainerId() == null || Objects.equals(a.getTrainerId(), trainerId)))
                .toList();

        List<Long> appIds = pendingTrainerApps.stream().map(AdmissionApplication::getId).toList();
        Map<Long, CandidateHorseProfile> candidates = (candidateHorseProfileRepository != null && !appIds.isEmpty())
                ? candidateHorseProfileRepository.findByAdmissionIdIn(appIds).stream()
                        .collect(Collectors.toMap(CandidateHorseProfile::getAdmissionId, c -> c, (a, b) -> a))
                : Collections.emptyMap();

        List<Map<String, Object>> result = new ArrayList<>();
        for (AdmissionApplication a : pendingTrainerApps) {
            CandidateHorseProfile candidate = candidates.get(a.getId());
            String horseName = candidate != null ? candidate.getName() : "Applicant horse";
            String breed = candidate != null && candidate.getBreed() != null ? candidate.getBreed() : "Unknown breed";
            String ownerName = "Owner";
            if (a.getOwnerId() != null && userRepository != null) {
                ownerName = userRepository.findById(a.getOwnerId()).map(User::getFullName).orElse(ownerName);
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseName", horseName);
            item.put("breed", breed);
            item.put("sex", candidate != null && candidate.getSex() != null ? candidate.getSex().name() : "N/A");
            item.put("dateOfBirth", candidate != null && candidate.getDateOfBirth() != null ? candidate.getDateOfBirth().toString() : "N/A");
            item.put("registrationNumber", candidate != null && candidate.getRegistrationNumber() != null ? candidate.getRegistrationNumber() : "N/A");
            item.put("sireName", candidate != null && candidate.getSireName() != null ? candidate.getSireName() : "N/A");
            item.put("damName", candidate != null && candidate.getDamName() != null ? candidate.getDamName() : "N/A");
            item.put("groomDecision", a.getGroomDecision() != null ? a.getGroomDecision().name() : "NOT_ASSESSED");
            item.put("groomFeedback", a.getGroomFeedback() != null ? a.getGroomFeedback() : "");
            item.put("vetTrainingDecision", a.getVetTrainingDecision() != null ? a.getVetTrainingDecision().name() : "NOT_ASSESSED");
            item.put("vetFeedback", a.getVetFeedback() != null ? a.getVetFeedback() : "");
            item.put("ownerName", ownerName);
            item.put("submittedDate", a.getSubmittedAt() != null ? a.getSubmittedAt().toString() : (a.getCreatedAt() != null ? a.getCreatedAt().toString() : "N/A"));
            item.put("status", "Awaiting trainer review");
            item.put("actionUrl", a.getId() != null ? "/trainer/admissions/" + a.getId() : "/trainer/admissions");
            result.add(item);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("pendingReviewCount", result.size());
        response.put("applicationList", result);
        response.put("actionUrl", "/trainer/admissions");
        return response;
    }
}
