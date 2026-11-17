package com.rtms.backend.ai.tool.handler.manager;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.repository.StableStallRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetStableOccupancyMetricsTool implements AiToolHandler {

    private final StableStallRepository stableStallRepository;

    @Autowired
    public GetStableOccupancyMetricsTool(StableStallRepository stableStallRepository) {
        this.stableStallRepository = stableStallRepository;
    }

    @Override
    public String getToolName() {
        return "getStableOccupancyMetrics";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        long totalStalls = stableStallRepository.count();
        long availableQuarantine = stableStallRepository.countAvailableQuarantineStalls();
        long occupiedQuarantine = stableStallRepository.countOccupiedQuarantineStalls();
        long availableRegular = stableStallRepository.countAvailableRegularStalls();
        long occupiedRegular = Math.max(0, totalStalls - availableRegular - availableQuarantine - occupiedQuarantine);
        String occupancyRate = totalStalls > 0 ? String.format(Locale.US, "%.1f%%", ((double)(totalStalls - availableRegular - availableQuarantine) / totalStalls * 100)) : "0.0%";

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalStalls", totalStalls);
        metrics.put("vacantQuarantineStalls", availableQuarantine);
        metrics.put("occupiedQuarantineStalls", occupiedQuarantine);
        metrics.put("vacantRegularStalls", availableRegular);
        metrics.put("occupiedRegularStalls", occupiedRegular);
        metrics.put("occupancyRate", occupancyRate);
        return metrics;
    }
}
