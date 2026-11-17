package com.rtms.backend.ai.tool.handler.owner;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetHorseCareScheduleTool implements AiToolHandler {

    private final CareScheduleRepository careScheduleRepository;
    private final AiToolSupport support;

    @Autowired
    public GetHorseCareScheduleTool(CareScheduleRepository careScheduleRepository, AiToolSupport support) {
        this.careScheduleRepository = careScheduleRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getHorseCareSchedule";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Horse horse = support.resolveHorse(arguments);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Please provide a horse ID or name");
        }
        support.verifyHorseOwnershipIfOwner(horse.getId(), currentUser);

        List<CareSchedule> schedules = careScheduleRepository.findByHorseIdOrderByScheduledAtAsc(horse.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (CareSchedule cs : schedules) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseName", horse.getName());
            item.put("careType", cs.getCareType() != null ? cs.getCareType().name() : "Veterinary examination");
            item.put("scheduledAt", cs.getScheduledAt() != null ? cs.getScheduledAt().toString() : "N/A");
            item.put("status", cs.getStatus() != null ? cs.getStatus().name() : "N/A");
            item.put("purpose", cs.getPurpose() != null ? cs.getPurpose().name() : "N/A");
            result.add(item);
        }
        return result;
    }
}
