package com.rtms.backend.ai.tool.handler.owner;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.repository.TrainingSessionRepository;
import com.rtms.backend.training.repository.projection.SessionHorseNameRow;
import com.rtms.backend.training.repository.projection.SessionRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class GetMyHorseUpcomingScheduleTool implements AiToolHandler {

    private final TrainingSessionRepository trainingSessionRepository;
    private final AiToolSupport support;

    @Autowired
    public GetMyHorseUpcomingScheduleTool(TrainingSessionRepository trainingSessionRepository, AiToolSupport support) {
        this.trainingSessionRepository = trainingSessionRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getMyHorseUpcomingSchedule";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Long ownerId = currentUser.getUserId();
        LocalDate from = support.parseDate(arguments.get("fromDate"), LocalDate.now());
        LocalDate to = support.parseDate(arguments.get("toDate"), LocalDate.now().plusDays(14));

        List<SessionRow> sessionRows = trainingSessionRepository.findOwnerSessionRows(ownerId, from, to);
        if (sessionRows.isEmpty()) {
            Map<String, Object> emptyResp = new LinkedHashMap<>();
            emptyResp.put("totalUpcomingWorkouts", 0);
            emptyResp.put("workoutScheduleList", Collections.emptyList());
            return emptyResp;
        }

        List<Long> sessionIds = sessionRows.stream().map(SessionRow::sessionId).toList();
        List<SessionHorseNameRow> horseNames = trainingSessionRepository.findActiveHorseNamesOfOwner(sessionIds, ownerId);
        Map<Long, List<String>> namesBySession = horseNames.stream()
                .collect(Collectors.groupingBy(SessionHorseNameRow::sessionId,
                        Collectors.mapping(SessionHorseNameRow::horseName, Collectors.toList())));

        List<Map<String, Object>> result = new ArrayList<>();
        for (SessionRow session : sessionRows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sessionTime", support.formatSessionWindow(session.startTime(), session.endTime()));
            item.put("workoutDate", session.sessionDate() != null ? session.sessionDate().toString() : "N/A");
            item.put("startTime", session.startTime() != null ? session.startTime().toString() : "N/A");
            item.put("endTime", session.endTime() != null ? session.endTime().toString() : "N/A");
            item.put("exerciseName", session.exerciseName());
            item.put("myHorseList", namesBySession.getOrDefault(session.sessionId(), Collections.emptyList()));
            result.add(item);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalUpcomingWorkouts", result.size());
        response.put("workoutScheduleList", result);
        return response;
    }
}
