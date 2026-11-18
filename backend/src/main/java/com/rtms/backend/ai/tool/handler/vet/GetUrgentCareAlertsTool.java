package com.rtms.backend.ai.tool.handler.vet;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class GetUrgentCareAlertsTool implements AiToolHandler {

    private final CareScheduleRepository careScheduleRepository;
    private final HorseRepository horseRepository;
    private final AiToolSupport support;

    @Autowired
    public GetUrgentCareAlertsTool(CareScheduleRepository careScheduleRepository,
                                  HorseRepository horseRepository,
                                  AiToolSupport support) {
        this.careScheduleRepository = careScheduleRepository;
        this.horseRepository = horseRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getUrgentCareAlerts";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        List<CareSchedule> all = careScheduleRepository.findByVeterinarianId(currentUser.getUserId());
        List<CareSchedule> urgent = all.stream()
                .filter(cs -> cs.getCareType() == CareType.URGENT
                        && cs.getStatus() != CareScheduleStatus.COMPLETED)
                .toList();

        Map<Long, Horse> horses = horseRepository.findAll().stream()
                .collect(Collectors.toMap(Horse::getId, h -> h, (a, b) -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        for (CareSchedule cs : urgent) {
            Horse h = horses.get(cs.getHorseId());
            String horseName = h != null ? h.getName() : "Horse";
            String stallName = h != null ? support.resolveStallName(h.getCurrentStallId()) : "Stall unknown";

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseName", horseName);
            item.put("stall", stallName);
            item.put("severity", "Urgent");
            item.put("symptoms", cs.getDescription() != null ? cs.getDescription() : "Urgent medical intervention required");
            item.put("timestamp", cs.getScheduledAt() != null ? cs.getScheduledAt().toString() : "N/A");
            item.put("actionUrl", cs.getId() != null ? "/veterinarian/urgent/" + cs.getId() : "/veterinarian/urgent");
            result.add(item);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalUrgentAlerts", result.size());
        response.put("alertList", result);
        response.put("actionUrl", "/veterinarian/urgent");
        return response;
    }
}
