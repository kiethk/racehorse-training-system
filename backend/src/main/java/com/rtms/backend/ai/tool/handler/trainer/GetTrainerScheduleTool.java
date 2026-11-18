package com.rtms.backend.ai.tool.handler.trainer;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.SessionHorseResponse;
import com.rtms.backend.training.dto.SessionSummaryResponse;
import com.rtms.backend.training.entity.Exercise;
import com.rtms.backend.training.repository.ExerciseRepository;
import com.rtms.backend.training.repository.TrainingSessionRepository;
import com.rtms.backend.training.repository.projection.SessionHorseNameRow;
import com.rtms.backend.training.repository.projection.SessionRow;
import com.rtms.backend.training.service.TrainingSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class GetTrainerScheduleTool implements AiToolHandler {

    private final TrainingSessionRepository trainingSessionRepository;
    private final TrainingSessionService trainingSessionService;
    private final ExerciseRepository exerciseRepository;
    private final AiToolSupport support;

    @Autowired
    public GetTrainerScheduleTool(TrainingSessionRepository trainingSessionRepository,
                                  TrainingSessionService trainingSessionService,
                                  @Autowired(required = false) ExerciseRepository exerciseRepository,
                                  AiToolSupport support) {
        this.trainingSessionRepository = trainingSessionRepository;
        this.trainingSessionService = trainingSessionService;
        this.exerciseRepository = exerciseRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getTrainerSchedule";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        if (Boolean.parseBoolean(String.valueOf(arguments.get("needsResultOnly")))) {
            return sessionsNeedingResult(currentUser);
        }
        Long trainerId = currentUser.getUserId();
        LocalDate from;
        LocalDate to;

        if (arguments.containsKey("date") && arguments.get("date") != null && !arguments.get("date").toString().isBlank()) {
            from = support.parseDate(arguments.get("date"), LocalDate.now());
            to = from;
        } else {
            from = support.parseDate(arguments.get("fromDate"), LocalDate.now());
            to = support.parseDate(arguments.get("toDate"), from);
        }

        List<SessionRow> sessionRows = trainingSessionRepository.findTrainerSessionRows(trainerId, from, to, false);
        if (sessionRows.isEmpty()) {
            Map<String, Object> emptyResp = new LinkedHashMap<>();
            emptyResp.put("totalSessions", 0);
            emptyResp.put("sessionList", Collections.emptyList());
            return emptyResp;
        }

        List<Long> sessionIds = sessionRows.stream().map(SessionRow::sessionId).toList();
        List<SessionHorseNameRow> horseNames = trainingSessionRepository.findActiveHorseNames(sessionIds);
        Map<Long, List<String>> namesBySession = horseNames.stream()
                .collect(Collectors.groupingBy(SessionHorseNameRow::sessionId,
                        Collectors.mapping(SessionHorseNameRow::horseName, Collectors.toList())));

        List<Map<String, Object>> result = new ArrayList<>();
        for (SessionRow session : sessionRows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sessionTime", support.formatSessionWindow(session.startTime(), session.endTime()));
            item.put("workoutDate", session.sessionDate() != null ? session.sessionDate().toString() : "Today");
            item.put("startTime", session.startTime() != null ? session.startTime().toString() : "N/A");
            item.put("endTime", session.endTime() != null ? session.endTime().toString() : "N/A");
            item.put("exerciseName", session.exerciseName() != null ? session.exerciseName() : "Conditioning exercise");
            Exercise subj = (session.exerciseId() != null && exerciseRepository != null) ? exerciseRepository.findById(session.exerciseId()).orElse(null) : null;
            item.put("surfaceType", subj != null && subj.getSurfaceType() != null ? subj.getSurfaceType().name() : "N/A");
            item.put("intensityLevel", subj != null && subj.getIntensityLevel() != null ? subj.getIntensityLevel().name() : "N/A");
            item.put("targetDistanceMeters", subj != null && subj.getTargetDistanceMeters() != null ? subj.getTargetDistanceMeters() : BigDecimal.ZERO);
            item.put("durationMinutes", session.durationMinutes() != null ? session.durationMinutes() : (subj != null ? subj.getDurationMinutes() : 0));
            item.put("horseCount", session.occupied());
            item.put("horseList", namesBySession.getOrDefault(session.sessionId(), Collections.emptyList()));
            result.add(item);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalSessions", result.size());
        response.put("sessionList", result);
        return response;
    }

    private Object sessionsNeedingResult(AuthenticatedUser currentUser) {
        LocalDate today = LocalDate.now();
        List<SessionSummaryResponse> sessions = trainingSessionService.getSessions(
                today.minusDays(AiToolSupport.NEEDS_RESULT_LOOKBACK_DAYS), today, null, true, currentUser);
        List<Map<String, Object>> list = new ArrayList<>();
        int waiting = 0;
        for (SessionSummaryResponse session : sessions) {
            List<String> horses = trainingSessionService.getSession(session.id(), currentUser).horses().stream()
                    .filter(horse -> "HELD".equals(horse.status()))
                    .map(SessionHorseResponse::horseName)
                    .toList();
            waiting += horses.size();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("workoutDate", session.date().toString());
            item.put("sessionTime", support.formatSessionWindow(session.startTime(), session.endTime()));
            item.put("groupName", support.groupLabel(session.groupName(), session.groupDisbanded()));
            item.put("exerciseName", session.exerciseName());
            item.put("needsResultCount", horses.size());
            item.put("horseList", horses);
            list.add(item);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalSessions", list.size());
        response.put("totalWorkoutsNeedingResult", waiting);
        response.put("sessionList", list);
        response.put("actionUrl", "/trainer/schedule");
        return response;
    }
}
