package com.rtms.backend.ai.tool.handler.manager;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.entity.CandidateHorseProfile;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.repository.CandidateHorseProfileRepository;
import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Component
public class GetAdmissionPipelineDetailsTool implements AiToolHandler {

    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final CandidateHorseProfileRepository candidateHorseProfileRepository;
    private final HorseRepository horseRepository;
    private final UserRepository userRepository;
    private final AiToolSupport support;

    @Autowired
    public GetAdmissionPipelineDetailsTool(AdmissionApplicationRepository admissionApplicationRepository,
                                           @Autowired(required = false) CandidateHorseProfileRepository candidateHorseProfileRepository,
                                           HorseRepository horseRepository,
                                           @Autowired(required = false) UserRepository userRepository,
                                           AiToolSupport support) {
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.candidateHorseProfileRepository = candidateHorseProfileRepository;
        this.horseRepository = horseRepository;
        this.userRepository = userRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getAdmissionPipelineDetails";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        List<AdmissionApplication> apps = admissionApplicationRepository.findAll();
        String statusFilter = arguments.get("status") != null ? arguments.get("status").toString().trim().toUpperCase() : null;

        List<AdmissionApplication> sortedApps = apps.stream()
                .sorted((a1, a2) -> {
                    LocalDateTime t1 = a1.getSubmittedAt() != null ? a1.getSubmittedAt() : a1.getCreatedAt();
                    LocalDateTime t2 = a2.getSubmittedAt() != null ? a2.getSubmittedAt() : a2.getCreatedAt();
                    if (t1 == null && t2 == null) return 0;
                    if (t1 == null) return 1;
                    if (t2 == null) return -1;
                    return t2.compareTo(t1);
                })
                .toList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (AdmissionApplication a : sortedApps) {
            if (statusFilter != null && !statusFilter.isBlank()) {
                if ("APPROVED".equals(statusFilter) && a.getStatus() != AdmissionStatus.APPROVED) {
                    continue;
                } else if ("REJECTED".equals(statusFilter) && a.getStatus() != AdmissionStatus.REJECTED) {
                    continue;
                } else if ("PENDING".equals(statusFilter) && (a.getStatus() == AdmissionStatus.APPROVED || a.getStatus() == AdmissionStatus.REJECTED)) {
                    continue;
                } else if (!"APPROVED".equals(statusFilter) && !"REJECTED".equals(statusFilter) && !"PENDING".equals(statusFilter)) {
                    if (a.getStatus() == null || !a.getStatus().name().equalsIgnoreCase(statusFilter)) {
                        continue;
                    }
                }
            }

            String horseName = "Registered horse";
            if (a.getHorseId() != null && horseRepository != null) {
                horseName = horseRepository.findById(a.getHorseId()).map(Horse::getName).orElse("Registered horse");
            }
            if ("Registered horse".equals(horseName) && candidateHorseProfileRepository != null) {
                horseName = candidateHorseProfileRepository.findByAdmissionId(a.getId())
                        .map(CandidateHorseProfile::getName)
                        .orElse("Registered horse");
            }

            String ownerName = "Unknown";
            if (a.getOwnerId() != null && userRepository != null) {
                ownerName = userRepository.findById(a.getOwnerId()).map(User::getFullName).orElse("Owner #" + a.getOwnerId());
            }

            String submittedDate = a.getSubmittedAt() != null ? a.getSubmittedAt().toString() : (a.getCreatedAt() != null ? a.getCreatedAt().toString() : "N/A");

            String currentStage = a.getStatus() != null ? switch (a.getStatus()) {
                case GROOM_REVIEW -> "Awaiting preliminary groom review (GROOM_REVIEW)";
                case WAITING_FOR_STALL -> "Awaiting quarantine stall assignment (WAITING_FOR_STALL)";
                case WAITING_FOR_ARRIVAL -> "Awaiting horse arrival at the stable (WAITING_FOR_ARRIVAL)";
                case ARRIVAL_EXPIRED -> "Arrival deadline has passed (ARRIVAL_EXPIRED)";
                case VET_REVIEW -> "Awaiting initial veterinarian examination (VET_REVIEW)";
                case TRAINER_REVIEW -> "Awaiting trainer assignment (TRAINER_REVIEW)";
                case MANAGER_REVIEW -> "Awaiting club manager approval (MANAGER_REVIEW)";
                case APPROVED -> "Admission application approved (APPROVED)";
                case REJECTED -> "Admission application rejected (REJECTED)";
            } : "Unknown";

            String vetName = "Not assigned";
            if (a.getVeterinarianId() != null && userRepository != null) {
                vetName = userRepository.findById(a.getVeterinarianId()).map(User::getFullName).orElse("Veterinarian #" + a.getVeterinarianId());
            }

            String trainerName = "Not assigned";
            if (a.getTrainerId() != null && userRepository != null) {
                trainerName = userRepository.findById(a.getTrainerId()).map(User::getFullName).orElse("Trainer #" + a.getTrainerId());
            }

            String navigationPath = a.getId() != null ? "/manager/admissions/" + a.getId() : "/manager/admissions";

            CandidateHorseProfile cand = (candidateHorseProfileRepository != null && a.getId() != null)
                    ? candidateHorseProfileRepository.findByAdmissionId(a.getId()).orElse(null) : null;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseName", horseName);
            item.put("candidateName", cand != null && cand.getName() != null ? cand.getName() : horseName);
            item.put("breed", cand != null && cand.getBreed() != null ? cand.getBreed() : "N/A");
            item.put("sex", cand != null && cand.getSex() != null ? cand.getSex().name() : "N/A");
            item.put("dateOfBirth", cand != null && cand.getDateOfBirth() != null ? cand.getDateOfBirth().toString() : "N/A");
            item.put("registrationNumber", cand != null && cand.getRegistrationNumber() != null ? cand.getRegistrationNumber() : "N/A");
            item.put("ownerName", ownerName);
            item.put("submittedDate", submittedDate);
            item.put("currentStage", currentStage);
            item.put("status", a.getStatus() != null ? a.getStatus().name() : "N/A");
            item.put("arrivalDeadlineAt", a.getArrivalDeadlineAt() != null ? a.getArrivalDeadlineAt().toString() : "N/A");
            item.put("arrivedAt", a.getArrivedAt() != null ? a.getArrivedAt().toString() : "N/A");
            item.put("quarantineStallCode", a.getQuarantineStallId() != null ? support.resolveStallName(a.getQuarantineStallId()) : "Stall not assigned");
            item.put("groomFeedback", a.getGroomFeedback() != null ? a.getGroomFeedback() : "");
            item.put("vetFeedback", a.getVetFeedback() != null ? a.getVetFeedback() : "");
            item.put("trainerFeedback", a.getTrainerFeedback() != null ? a.getTrainerFeedback() : "");
            item.put("managerFeedback", a.getManagerFeedback() != null ? a.getManagerFeedback() : "");
            item.put("assignedVet", vetName);
            item.put("designatedTrainer", trainerName);
            item.put("navigationUrl", navigationPath);
            item.put("actionUrl", navigationPath);

            result.add(item);
        }
        return result;
    }
}
