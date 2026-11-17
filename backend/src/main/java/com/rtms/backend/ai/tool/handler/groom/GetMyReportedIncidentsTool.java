package com.rtms.backend.ai.tool.handler.groom;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.incident.entity.HorseIncidentReport;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class GetMyReportedIncidentsTool implements AiToolHandler {

    private final HorseIncidentReportRepository horseIncidentReportRepository;
    private final HorseRepository horseRepository;
    private final UserRepository userRepository;

    @Autowired
    public GetMyReportedIncidentsTool(@Autowired(required = false) HorseIncidentReportRepository horseIncidentReportRepository,
                                      HorseRepository horseRepository,
                                      @Autowired(required = false) UserRepository userRepository) {
        this.horseIncidentReportRepository = horseIncidentReportRepository;
        this.horseRepository = horseRepository;
        this.userRepository = userRepository;
    }

    @Override
    public String getToolName() {
        return "getMyReportedIncidents";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        Map<Long, String> horseNames = horseRepository.findAll().stream()
                .collect(Collectors.toMap(Horse::getId, Horse::getName, (a, b) -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        if (horseIncidentReportRepository != null) {
            List<HorseIncidentReport> reports = horseIncidentReportRepository.findByReportedById(currentUser.getUserId());
            for (HorseIncidentReport r : reports) {
                if (r.getReportedAt() != null && r.getReportedAt().isBefore(cutoff)) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("horseName", horseNames.getOrDefault(r.getHorseId(), "Horse"));
                item.put("title", r.getTitle());
                item.put("incidentType", r.getIncidentType() != null ? r.getIncidentType().name() : "PROBLEM");
                item.put("detailedDescription", r.getDescription());
                item.put("imageUrl", r.getImageUrl() != null ? r.getImageUrl() : "");
                String groomHandler = "Not yet assigned";
                if (r.getHandledById() != null && userRepository != null) {
                    groomHandler = userRepository.findById(r.getHandledById()).map(User::getFullName).orElse("Staff #" + r.getHandledById());
                }
                item.put("handlerName", groomHandler);
                item.put("handledAt", r.getHandledAt() != null ? r.getHandledAt().toString() : "N/A");
                item.put("handlerNote", r.getHandlerNote() != null ? r.getHandlerNote() : "");
                item.put("careScheduleId", r.getCareScheduleId());
                item.put("reportedAt", r.getReportedAt() != null ? r.getReportedAt().toString() : "N/A");
                item.put("incidentStatus", r.getStatus() != null ? r.getStatus().name() : "REPORTED");
                item.put("actionUrl", "/groom/incidents");
                result.add(item);
            }
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalReportedIncidents", result.size());
        response.put("incidentList", result);
        response.put("actionUrl", "/groom/incidents");
        return response;
    }
}
