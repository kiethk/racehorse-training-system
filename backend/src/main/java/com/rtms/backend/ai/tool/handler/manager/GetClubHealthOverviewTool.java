package com.rtms.backend.ai.tool.handler.manager;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.incident.entity.HorseIncidentReport;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.enums.TrainingDecision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Component
public class GetClubHealthOverviewTool implements AiToolHandler {

    private final HorseRepository horseRepository;
    private final HorseIncidentReportRepository horseIncidentReportRepository;

    @Autowired
    public GetClubHealthOverviewTool(HorseRepository horseRepository,
                                     @Autowired(required = false) HorseIncidentReportRepository horseIncidentReportRepository) {
        this.horseRepository = horseRepository;
        this.horseIncidentReportRepository = horseIncidentReportRepository;
    }

    @Override
    public String getToolName() {
        return "getClubHealthOverview";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        List<Horse> horses = horseRepository.findAll();
        long allowedCount = horses.stream().filter(h -> h.getTrainingDecision() == TrainingDecision.ALLOWED).count();
        long blockedCount = horses.stream().filter(h -> h.getTrainingDecision() == TrainingDecision.BLOCKED).count();

        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<HorseIncidentReport> weekIncidents = (horseIncidentReportRepository != null)
                ? horseIncidentReportRepository.findByReportedAtAfterOrderByReportedAtDesc(weekAgo)
                : Collections.emptyList();

        return Map.of(
                "totalClubHorses", horses.size(),
                "allowedTrainingHorses", allowedCount,
                "medicallyBlockedHorsesCount", blockedCount,
                "incidentsLast7DaysCount", weekIncidents.size()
        );
    }
}

