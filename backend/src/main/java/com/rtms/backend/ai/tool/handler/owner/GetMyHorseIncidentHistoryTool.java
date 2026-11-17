package com.rtms.backend.ai.tool.handler.owner;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.incident.entity.HorseIncidentReport;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetMyHorseIncidentHistoryTool implements AiToolHandler {

    private final HorseIncidentReportRepository horseIncidentReportRepository;
    private final UserRepository userRepository;
    private final AiToolSupport support;

    @Autowired
    public GetMyHorseIncidentHistoryTool(@Autowired(required = false) HorseIncidentReportRepository horseIncidentReportRepository,
                                         @Autowired(required = false) UserRepository userRepository,
                                         AiToolSupport support) {
        this.horseIncidentReportRepository = horseIncidentReportRepository;
        this.userRepository = userRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getMyHorseIncidentHistory";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Horse horse = support.resolveHorse(arguments);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Please provide a horse ID or name");
        }
        support.verifyHorseOwnershipIfOwner(horse.getId(), currentUser);

        List<Map<String, Object>> result = new ArrayList<>();
        if (horseIncidentReportRepository != null) {
            List<HorseIncidentReport> reports = horseIncidentReportRepository.findByHorseId(horse.getId());
            for (HorseIncidentReport r : reports) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("horseName", horse.getName());
                item.put("title", r.getTitle());
                item.put("description", r.getDescription() != null ? r.getDescription() : "");
                item.put("imageUrl", r.getImageUrl() != null ? r.getImageUrl() : "");
                String handlerName = "Not yet acknowledged";
                if (r.getHandledById() != null && userRepository != null) {
                    handlerName = userRepository.findById(r.getHandledById()).map(User::getFullName).orElse("Staff member #" + r.getHandledById());
                }
                item.put("handlerName", handlerName);
                item.put("handledAt", r.getHandledAt() != null ? r.getHandledAt().toString() : "N/A");
                item.put("handlerNote", r.getHandlerNote() != null ? r.getHandlerNote() : "");
                item.put("careScheduleId", r.getCareScheduleId());
                item.put("timestamp", r.getReportedAt() != null ? r.getReportedAt().toString() : "N/A");
                item.put("status", r.getStatus() != null ? r.getStatus().name() : "REPORTED");
                item.put("actionUrl", horse.getId() != null ? "/owner/horses/" + horse.getId() : "/owner/horses");
                result.add(item);
            }
        }
        return result;
    }
}
