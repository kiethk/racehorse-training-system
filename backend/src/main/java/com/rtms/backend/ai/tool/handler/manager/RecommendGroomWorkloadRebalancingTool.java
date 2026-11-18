package com.rtms.backend.ai.tool.handler.manager;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.repository.GroomDailyTaskRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Component
public class RecommendGroomWorkloadRebalancingTool implements AiToolHandler {

    private final UserRepository userRepository;
    private final StableStallRepository stableStallRepository;
    private final GroomDailyTaskRepository groomDailyTaskRepository;

    @Autowired
    public RecommendGroomWorkloadRebalancingTool(
            @Autowired(required = false) UserRepository userRepository,
            StableStallRepository stableStallRepository,
            @Autowired(required = false) GroomDailyTaskRepository groomDailyTaskRepository) {
        this.userRepository = userRepository;
        this.stableStallRepository = stableStallRepository;
        this.groomDailyTaskRepository = groomDailyTaskRepository;
    }

    @Override
    public String getToolName() {
        return "recommendGroomWorkloadRebalancing";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        List<User> grooms = (userRepository != null)
                ? userRepository.findAll().stream()
                        .filter(u -> u.getRole() != null && ("GROOM".equalsIgnoreCase(u.getRole().getName()) || "STABLE_STAFF".equalsIgnoreCase(u.getRole().getName())))
                        .filter(User::isActive)
                        .toList()
                : Collections.emptyList();

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<Map<String, Object>> lowLoadGroomDetails = new ArrayList<>();
        List<String> overloadedGrooms = new ArrayList<>();
        List<String> underloadedGrooms = new ArrayList<>();

        for (User g : grooms) {
            List<StableStall> stalls = (stableStallRepository != null)
                    ? stableStallRepository.findByGroomId(g.getId())
                    : Collections.emptyList();
            int taskCount = (groomDailyTaskRepository != null)
                    ? groomDailyTaskRepository.findByGroomIdAndScheduledTimeBetween(g.getId(), startOfDay, endOfDay).size()
                    : 0;

            String loadStatus;
            if (stalls.size() >= 5 || taskCount >= 10) {
                loadStatus = "Overloaded (Needs workload reduction)";
                overloadedGrooms.add(g.getFullName() != null ? g.getFullName() : g.getEmail());
            } else if (stalls.size() <= 2 && taskCount <= 3) {
                loadStatus = "Underloaded (Available for more work)";
                underloadedGrooms.add(g.getFullName() != null ? g.getFullName() : g.getEmail());
            } else {
                loadStatus = "Well balanced";
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("groomName", g.getFullName() != null ? g.getFullName() : g.getEmail());
            item.put("assignedStallCount", stalls.size());
            item.put("tasksTodayCount", taskCount);
            item.put("workloadStatus", loadStatus);
            lowLoadGroomDetails.add(item);
        }

        String stallRebalancingProposal;
        if (!overloadedGrooms.isEmpty() && !underloadedGrooms.isEmpty()) {
            stallRebalancingProposal = "Recommend transferring 1-2 stalls from the overloaded staff group (" + String.join(", ", overloadedGrooms)
                    + ") to staff with lighter workloads (" + String.join(", ", underloadedGrooms) + ") to optimize care productivity.";
        } else if (!overloadedGrooms.isEmpty()) {
            stallRebalancingProposal = "All grooms currently responsible for stalls have heavy workloads (" + String.join(", ", overloadedGrooms)
                    + "). Consider adding staff or temporarily assigning support personnel.";
        } else {
            stallRebalancingProposal = "Workloads and stall assignments are well balanced across the current grooms, so no reallocation is needed.";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalGrooms", grooms.size());
        result.put("groomWorkloadList", lowLoadGroomDetails);
        result.put("overloadedStaff", overloadedGrooms.isEmpty() ? List.of("No overloaded staff members") : overloadedGrooms);
        result.put("proposedStallTransfer", stallRebalancingProposal);
        result.put("actionUrl", "/manager/staff");
        return result;
    }
}
