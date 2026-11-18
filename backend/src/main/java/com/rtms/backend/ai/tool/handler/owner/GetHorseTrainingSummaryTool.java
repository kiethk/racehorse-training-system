package com.rtms.backend.ai.tool.handler.owner;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.HorseAttendanceResponse;
import com.rtms.backend.training.service.HorseAttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetHorseTrainingSummaryTool implements AiToolHandler {

    private final HorseAttendanceService horseAttendanceService;
    private final AiToolSupport support;

    @Autowired
    public GetHorseTrainingSummaryTool(HorseAttendanceService horseAttendanceService, AiToolSupport support) {
        this.horseAttendanceService = horseAttendanceService;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getHorseTrainingSummary";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Horse horse = support.resolveHorse(arguments);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Please provide a horse ID or name");
        }
        support.verifyHorseOwnershipIfOwner(horse.getId(), currentUser);

        List<HorseAttendanceResponse> history = horseAttendanceService.getHistory(horse.getId(), currentUser);
        int totalSessions = history.stream().mapToInt(HorseAttendanceResponse::total).sum();
        int completedSessions = history.stream().mapToInt(HorseAttendanceResponse::attended).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horse.getName());
        result.put("registrationNumber", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("trainingDecision", horse.getTrainingDecision() != null ? horse.getTrainingDecision().name() : "N/A");
        result.put("decisionReason", horse.getTrainingDecisionReason() != null ? horse.getTrainingDecisionReason() : "Normal");
        result.put("totalSessions", totalSessions);
        result.put("completedSessions", completedSessions);
        return result;
    }
}
