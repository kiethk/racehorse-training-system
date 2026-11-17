package com.rtms.backend.ai.tool.handler.vet;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.entity.CandidateHorseProfile;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.repository.CandidateHorseProfileRepository;
import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class GetPendingAdmissionExamsTool implements AiToolHandler {

    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final CareScheduleRepository careScheduleRepository;
    private final CandidateHorseProfileRepository candidateHorseProfileRepository;
    private final UserRepository userRepository;

    @Autowired
    public GetPendingAdmissionExamsTool(AdmissionApplicationRepository admissionApplicationRepository,
                                        CareScheduleRepository careScheduleRepository,
                                        @Autowired(required = false) CandidateHorseProfileRepository candidateHorseProfileRepository,
                                        @Autowired(required = false) UserRepository userRepository) {
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.careScheduleRepository = careScheduleRepository;
        this.candidateHorseProfileRepository = candidateHorseProfileRepository;
        this.userRepository = userRepository;
    }

    @Override
    public String getToolName() {
        return "getPendingAdmissionExams";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        List<AdmissionApplication> apps = admissionApplicationRepository.findAllByOrderBySubmittedAtDesc();
        Set<Long> vetAdmissionIds = careScheduleRepository.findByVeterinarianId(currentUser.getUserId()).stream()
                .filter(cs -> cs.getCareType() == CareType.INITIAL && cs.getAdmissionId() != null)
                .map(CareSchedule::getAdmissionId)
                .collect(Collectors.toSet());
        List<AdmissionApplication> pendingVetApps = apps.stream()
                .filter(a -> a.getStatus() == AdmissionStatus.VET_REVIEW)
                .filter(a -> vetAdmissionIds.contains(a.getId()))
                .toList();

        List<Long> appIds = pendingVetApps.stream().map(AdmissionApplication::getId).toList();
        Map<Long, CandidateHorseProfile> candidates = (candidateHorseProfileRepository != null && !appIds.isEmpty())
                ? candidateHorseProfileRepository.findByAdmissionIdIn(appIds).stream()
                        .collect(Collectors.toMap(CandidateHorseProfile::getAdmissionId, c -> c, (a, b) -> a))
                : Collections.emptyMap();

        List<Map<String, Object>> result = new ArrayList<>();
        for (AdmissionApplication a : pendingVetApps) {
            CandidateHorseProfile candidate = candidates.get(a.getId());
            String horseName = candidate != null ? candidate.getName() : "Horse with pending application";
            String ownerName = "Owner";
            if (a.getOwnerId() != null && userRepository != null) {
                ownerName = userRepository.findById(a.getOwnerId()).map(User::getFullName).orElse(ownerName);
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseName", horseName);
            item.put("breed", candidate != null && candidate.getBreed() != null ? candidate.getBreed() : "N/A");
            item.put("sex", candidate != null && candidate.getSex() != null ? candidate.getSex().name() : "N/A");
            item.put("dateOfBirth", candidate != null && candidate.getDateOfBirth() != null ? candidate.getDateOfBirth().toString() : "N/A");
            item.put("registrationNumber", candidate != null && candidate.getRegistrationNumber() != null ? candidate.getRegistrationNumber() : "N/A");
            item.put("ownerName", ownerName);
            item.put("submittedDate", a.getSubmittedAt() != null ? a.getSubmittedAt().toString() : (a.getCreatedAt() != null ? a.getCreatedAt().toString() : "N/A"));
            item.put("status", "Awaiting veterinary screening examination");
            item.put("actionUrl", a.getId() != null ? "/veterinarian/admissions?id=" + a.getId() : "/veterinarian/admissions");
            result.add(item);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("pendingExamCount", result.size());
        response.put("examList", result);
        response.put("actionUrl", "/veterinarian/admissions");
        return response;
    }
}
