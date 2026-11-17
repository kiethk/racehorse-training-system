package com.rtms.backend.ai.tool.handler.trainer;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.notification.dto.HorseAlertResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.service.HorsePerformanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetTrainerRiskAlertsTool implements AiToolHandler {

    private final HorseRepository horseRepository;
    private final HorsePerformanceService horsePerformanceService;
    private final AiToolSupport support;

    @Autowired
    public GetTrainerRiskAlertsTool(HorseRepository horseRepository,
                                   HorsePerformanceService horsePerformanceService,
                                   AiToolSupport support) {
        this.horseRepository = horseRepository;
        this.horsePerformanceService = horsePerformanceService;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getTrainerRiskAlerts";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Horse targetHorse = support.resolveHorse(arguments);
        Long horseId = targetHorse != null ? targetHorse.getId() : support.parseLong(arguments.get("horseId"));

        if (horseId != null) {
            Horse h = targetHorse != null ? targetHorse : horseRepository.findById(horseId).orElse(null);
            String hName = h != null ? h.getName() : "Horse #" + horseId;
            List<HorseAlertResponse> alerts = horsePerformanceService.getHorseAlerts(horseId, currentUser);
            return Map.of(
                    "horseName", hName,
                    "alertCount", alerts.size(),
                    "alertList", alerts
            );
        }

        List<Horse> managedHorses = horseRepository.findManagedHorsesByTrainerIdAndStatusIn(
                currentUser.getUserId(),
                List.of(HorseStatus.ELIGIBLE, HorseStatus.CANDIDATE)
        );

        List<Map<String, Object>> aggregated = new ArrayList<>();
        for (Horse h : managedHorses) {
            List<HorseAlertResponse> alerts = horsePerformanceService.getHorseAlerts(h.getId(), currentUser);
            if (!alerts.isEmpty()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("horseName", h.getName());
                item.put("alertCount", alerts.size());
                item.put("alertList", alerts);
                aggregated.add(item);
            }
        }
        return Map.of(
                "trainerName", support.resolveUserName(currentUser, "Trainer"),
                "horsesAtRiskCount", aggregated.size(),
                "alertDetails", aggregated
        );
    }
}
