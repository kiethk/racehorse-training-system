package com.rtms.backend.ai.tool.handler.vet;

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
public class GetRecentIncidentReportsTool implements AiToolHandler {

    private final HorseIncidentReportRepository horseIncidentReportRepository;
    private final HorseRepository horseRepository;
    private final UserRepository userRepository;

    @Autowired
    public GetRecentIncidentReportsTool(@Autowired(required = false) HorseIncidentReportRepository horseIncidentReportRepository,
                                        HorseRepository horseRepository,
                                        @Autowired(required = false) UserRepository userRepository) {
        this.horseIncidentReportRepository = horseIncidentReportRepository;
        this.horseRepository = horseRepository;
        this.userRepository = userRepository;
    }

    @Override
    public String getToolName() {
        return "getRecentIncidentReports";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(48);
        List<HorseIncidentReport> reports = (horseIncidentReportRepository != null)
                ? horseIncidentReportRepository.findByReportedAtAfterOrderByReportedAtDesc(cutoff)
                : Collections.emptyList();
        Map<Long, String> horseNames = horseRepository.findAll().stream()
                .collect(Collectors.toMap(Horse::getId, Horse::getName, (a, b) -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        for (HorseIncidentReport r : reports) {
            Map<String, Object> item = new LinkedHashMap<>();
            String horseName = horseNames.getOrDefault(r.getHorseId(), "Horse #" + r.getHorseId());
            String groomName = "Assigned groom";
            if (r.getGroomId() != null && userRepository != null) {
                groomName = userRepository.findById(r.getGroomId()).map(User::getFullName).orElse(groomName);
            }
            item.put("incidentTitle", r.getTitle());
            item.put("horseName", horseName);
            item.put("reporterName", groomName);
            item.put("incidentType", r.getIncidentType());
            item.put("detailedDescription", r.getDescription());
            item.put("imageUrl", r.getImageUrl() != null ? r.getImageUrl() : "");
            String repHandler = "Not yet acknowledged";
            if (r.getHandledById() != null && userRepository != null) {
                repHandler = userRepository.findById(r.getHandledById()).map(User::getFullName).orElse("Staff member #" + r.getHandledById());
            }
            item.put("handlerName", repHandler);
            item.put("handledAt", r.getHandledAt() != null ? r.getHandledAt().toString() : "N/A");
            item.put("handlerNote", r.getHandlerNote() != null ? r.getHandlerNote() : "");
            item.put("careScheduleId", r.getCareScheduleId());
            item.put("incidentStatus", r.getStatus());
            item.put("reportedAt", r.getReportedAt() != null ? r.getReportedAt().toString() : "N/A");
            result.add(item);
        }
        return Map.of(
                "totalReportsLast48Hours", result.size(),
                "incidentList", result
        );
    }
}
