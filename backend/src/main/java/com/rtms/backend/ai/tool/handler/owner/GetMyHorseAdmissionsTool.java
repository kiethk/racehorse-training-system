package com.rtms.backend.ai.tool.handler.owner;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.entity.CandidateHorseProfile;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.repository.CandidateHorseProfileRepository;
import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetMyHorseAdmissionsTool implements AiToolHandler {

    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final CandidateHorseProfileRepository candidateHorseProfileRepository;
    private final HorseRepository horseRepository;
    private final AiToolSupport support;

    @Autowired
    public GetMyHorseAdmissionsTool(AdmissionApplicationRepository admissionApplicationRepository,
                                    @Autowired(required = false) CandidateHorseProfileRepository candidateHorseProfileRepository,
                                    HorseRepository horseRepository,
                                    AiToolSupport support) {
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.candidateHorseProfileRepository = candidateHorseProfileRepository;
        this.horseRepository = horseRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getMyHorseAdmissions";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Long ownerId = currentUser.getUserId();
        List<AdmissionApplication> apps = admissionApplicationRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (AdmissionApplication a : apps) {
            Map<String, Object> item = new LinkedHashMap<>();
            CandidateHorseProfile cand = (candidateHorseProfileRepository != null && a.getId() != null)
                    ? candidateHorseProfileRepository.findByAdmissionId(a.getId()).orElse(null) : null;
            String horseName = cand != null && cand.getName() != null ? cand.getName() : "Horse in admission application";
            if (a.getHorseId() != null) {
                horseName = horseRepository.findById(a.getHorseId()).map(Horse::getName).orElse(horseName);
            }
            item.put("horseName", horseName);
            item.put("candidateName", cand != null && cand.getName() != null ? cand.getName() : horseName);
            item.put("breed", cand != null && cand.getBreed() != null ? cand.getBreed() : "N/A");
            item.put("sex", cand != null && cand.getSex() != null ? cand.getSex().name() : "N/A");
            item.put("dateOfBirth", cand != null && cand.getDateOfBirth() != null ? cand.getDateOfBirth().toString() : "N/A");
            item.put("registrationNumber", cand != null && cand.getRegistrationNumber() != null ? cand.getRegistrationNumber() : "N/A");
            item.put("applicationStatus", a.getStatus() != null ? a.getStatus().name() : "PENDING");
            item.put("arrivalDeadlineAt", a.getArrivalDeadlineAt() != null ? a.getArrivalDeadlineAt().toString() : "N/A");
            item.put("arrivedAt", a.getArrivedAt() != null ? a.getArrivedAt().toString() : "N/A");
            item.put("quarantineStallCode", a.getQuarantineStallId() != null ? support.resolveStallName(a.getQuarantineStallId()) : "Stall not yet assigned");
            item.put("groomCheckResult", a.getGroomDecision() != null ? a.getGroomDecision().name() : "Awaiting assessment");
            item.put("groomFeedback", a.getGroomFeedback() != null ? a.getGroomFeedback() : "");
            item.put("vetExamResult", a.getVetTrainingDecision() != null ? a.getVetTrainingDecision().name() : "Awaiting assessment");
            item.put("vetFeedback", a.getVetFeedback() != null ? a.getVetFeedback() : "");
            item.put("trainerFeedback", a.getTrainerFeedback() != null ? a.getTrainerFeedback() : "");
            item.put("managerApprovalResult", a.getManagerDecision() != null ? a.getManagerDecision().name() : "Awaiting approval");
            item.put("managerFeedback", a.getManagerFeedback() != null ? a.getManagerFeedback() : "");
            item.put("submittedDate", a.getSubmittedAt() != null ? a.getSubmittedAt().toString() : "N/A");
            item.put("actionUrl", a.getId() != null ? "/owner/admissions/" + a.getId() : "/owner/admissions");
            result.add(item);
        }
        int daDuyet = 0;
        int inProgressCount = 0;
        for (AdmissionApplication a : apps) {
            if (a.getStatus() == AdmissionStatus.APPROVED) {
                daDuyet++;
            } else if (a.getStatus() != AdmissionStatus.REJECTED && a.getStatus() != AdmissionStatus.ARRIVAL_EXPIRED) {
                inProgressCount++;
            }
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalSubmittedApplications", result.size());
        response.put("approvedApplicationCount", daDuyet);
        response.put("pendingApplicationCount", inProgressCount);
        response.put("applicationList", result);
        response.put("actionUrl", "/owner/admissions");
        return response;
    }
}
