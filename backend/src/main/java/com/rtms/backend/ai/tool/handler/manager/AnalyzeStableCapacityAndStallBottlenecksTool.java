package com.rtms.backend.ai.tool.handler.manager;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.repository.StableStallRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AnalyzeStableCapacityAndStallBottlenecksTool implements AiToolHandler {

    private final StableStallRepository stableStallRepository;
    private final AdmissionApplicationRepository admissionApplicationRepository;

    @Autowired
    public AnalyzeStableCapacityAndStallBottlenecksTool(
            StableStallRepository stableStallRepository,
            @Autowired(required = false) AdmissionApplicationRepository admissionApplicationRepository) {
        this.stableStallRepository = stableStallRepository;
        this.admissionApplicationRepository = admissionApplicationRepository;
    }

    @Override
    public String getToolName() {
        return "analyzeStableCapacityAndStallBottlenecks";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        long totalStalls = stableStallRepository != null ? stableStallRepository.count() : 0;
        long availableRegular = stableStallRepository != null ? stableStallRepository.countAvailableRegularStalls() : 0;
        long availableQuarantine = stableStallRepository != null ? stableStallRepository.countAvailableQuarantineStalls() : 0;
        long occupiedQuarantine = stableStallRepository != null ? stableStallRepository.countOccupiedQuarantineStalls() : 0;
        long availableStalls = availableRegular + availableQuarantine;
        long occupiedStalls = totalStalls > availableStalls ? (totalStalls - availableStalls) : 0;

        List<AdmissionApplication> pendingAppsList = admissionApplicationRepository != null
                ? admissionApplicationRepository.findAll().stream()
                        .filter(a -> a.getStatus() != AdmissionStatus.APPROVED && a.getStatus() != AdmissionStatus.REJECTED)
                        .toList()
                : Collections.emptyList();
        int pendingApplicationCount = pendingAppsList.size();

        double occupancyRate = totalStalls > 0 ? ((double) occupiedStalls / totalStalls) * 100.0 : 0.0;
        String bottleneckForecast;
        String managementRecommendation;

        if (occupancyRate >= 80.0 || pendingApplicationCount > availableStalls) {
            bottleneckForecast = String.format(Locale.US, "Capacity bottleneck warning for the next 30-60 days: occupancy is %.1f%%, with %d admission applications waiting and only %d available stalls remaining.",
                    occupancyRate, pendingApplicationCount, availableStalls);
            managementRecommendation = "Prioritize completing discharge or stall-transfer procedures for horses that have completed quarantine, and temporarily slow acceptance of new admission applications to avoid overcapacity.";
        } else {
            bottleneckForecast = String.format(Locale.US, "Stable capacity is expected to remain steady over the next 30-60 days: occupancy is %.1f%%, with %d vacant stalls (%d regular, %d quarantine) ready to accommodate new horses.",
                    occupancyRate, availableStalls, availableRegular, availableQuarantine);
            managementRecommendation = "Continue maintaining strict quarantine procedures and schedule rotating cleaning and disinfection of vacant stalls.";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalStalls", totalStalls);
        result.put("occupiedStalls", occupiedStalls);
        result.put("vacantStalls", availableStalls);
        result.put("quarantineCasesCount", occupiedQuarantine);
        result.put("waitingAdmissionsCount", pendingApplicationCount);
        result.put("occupancyRate", String.format(Locale.US, "%.1f%%", occupancyRate));
        result.put("bottleneckForecast", bottleneckForecast);
        result.put("managementRecommendations", managementRecommendation);
        result.put("actionUrl", "/manager/stable");
        return result;
    }
}
