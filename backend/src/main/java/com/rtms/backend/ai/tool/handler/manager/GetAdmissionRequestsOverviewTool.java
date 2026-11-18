package com.rtms.backend.ai.tool.handler.manager;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class GetAdmissionRequestsOverviewTool implements AiToolHandler {

    private final AdmissionApplicationRepository admissionApplicationRepository;

    @Autowired
    public GetAdmissionRequestsOverviewTool(AdmissionApplicationRepository admissionApplicationRepository) {
        this.admissionApplicationRepository = admissionApplicationRepository;
    }

    @Override
    public String getToolName() {
        return "getAdmissionRequestsOverview";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        List<AdmissionApplication> allApps = admissionApplicationRepository != null
                ? admissionApplicationRepository.findAll()
                : Collections.emptyList();
        long totalApplications = allApps.size();

        Map<AdmissionStatus, Long> countsByStatus = allApps.stream()
                .filter(a -> a.getStatus() != null)
                .collect(Collectors.groupingBy(AdmissionApplication::getStatus, Collectors.counting()));

        long applicationsAwaitingManagerApproval = countsByStatus.getOrDefault(AdmissionStatus.MANAGER_REVIEW, 0L);
        long applicationsAwaitingGroomReview = countsByStatus.getOrDefault(AdmissionStatus.GROOM_REVIEW, 0L);
        long applicationsAwaitingArrival = countsByStatus.getOrDefault(AdmissionStatus.WAITING_FOR_ARRIVAL, 0L);
        long applicationsAwaitingVetReview = countsByStatus.getOrDefault(AdmissionStatus.VET_REVIEW, 0L);
        long applicationsAwaitingTrainerReview = countsByStatus.getOrDefault(AdmissionStatus.TRAINER_REVIEW, 0L);
        long approvedApplications = countsByStatus.getOrDefault(AdmissionStatus.APPROVED, 0L);
        long rejectedApplications = countsByStatus.getOrDefault(AdmissionStatus.REJECTED, 0L);
        long expiredArrivalApplications = countsByStatus.getOrDefault(AdmissionStatus.ARRIVAL_EXPIRED, 0L);
        long applicationsAwaitingQuarantineStall = countsByStatus.getOrDefault(AdmissionStatus.WAITING_FOR_STALL, 0L);

        long applicationsInProgress = applicationsAwaitingGroomReview + applicationsAwaitingQuarantineStall + applicationsAwaitingArrival
                + applicationsAwaitingVetReview + applicationsAwaitingTrainerReview + applicationsAwaitingManagerApproval;

        Map<String, Object> stageBreakdown = new LinkedHashMap<>();
        stageBreakdown.put("waitingGroomReview", applicationsAwaitingGroomReview);
        stageBreakdown.put("waitingQuarantineStall", applicationsAwaitingQuarantineStall);
        stageBreakdown.put("waitingArrival", applicationsAwaitingArrival);
        stageBreakdown.put("waitingVetReview", applicationsAwaitingVetReview);
        stageBreakdown.put("waitingTrainerReview", applicationsAwaitingTrainerReview);
        stageBreakdown.put("waitingManagerApproval", applicationsAwaitingManagerApproval);
        stageBreakdown.put("fullyApproved", approvedApplications);
        stageBreakdown.put("rejected", rejectedApplications);
        stageBreakdown.put("arrivalExpired", expiredArrivalApplications);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalApplications", totalApplications);
        response.put("pendingAdmissionsCount", applicationsAwaitingManagerApproval);
        response.put("pendingManagerReviewCount", applicationsAwaitingManagerApproval);
        response.put("inProgressApplications", applicationsInProgress);
        response.put("approvedApplications", approvedApplications);
        response.put("rejectedOrExpiredApplications", rejectedApplications + expiredArrivalApplications);
        response.put("stageBreakdown", stageBreakdown);
        response.put("systemStatus", "HOAT_DONG_BINH_THUONG");
        response.put("actionUrl", "/manager/admissions");

        return response;
    }
}

