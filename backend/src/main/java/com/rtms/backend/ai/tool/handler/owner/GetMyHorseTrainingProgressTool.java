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
public class GetMyHorseTrainingProgressTool implements AiToolHandler {

    private final HorseAttendanceService horseAttendanceService;
    private final AiToolSupport support;

    @Autowired
    public GetMyHorseTrainingProgressTool(HorseAttendanceService horseAttendanceService, AiToolSupport support) {
        this.horseAttendanceService = horseAttendanceService;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getMyHorseTrainingProgress";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Horse horse = support.resolveHorse(arguments);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Please provide a horse ID or name");
        }
        support.verifyHorseOwnershipIfOwner(horse.getId(), currentUser);

        List<HorseAttendanceResponse> history = horseAttendanceService.getHistory(horse.getId(), currentUser);
        if (history.isEmpty()) {
            return Map.of("message", "Horse " + horse.getName() + " currently has no training plan.");
        }
        return history.stream().map(line -> attendanceItem(horse.getName(), line)).toList();
    }

    private Map<String, Object> attendanceItem(String horseName, HorseAttendanceResponse line) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("planName", line.courseName() != null ? line.courseName() : "Training course");
        item.put("horseName", horseName);
        item.put("groupName", support.groupLabel(line.groupName(), line.groupDisbanded()));
        item.put("status", support.courseRunStatusLabel(line.courseRunStatus()));
        item.put("progress", line.attended() + "/" + line.total() + " sessions completed");
        item.put("completedSessions", line.attended());
        item.put("totalSessions", line.total());
        item.put("joinedOn", line.joinedOn() != null ? line.joinedOn().toString() : "N/A");
        item.put("startDate", line.startDate() != null ? line.startDate().toString() : "N/A");
        item.put("endDate", line.endDate() != null ? line.endDate().toString() : "N/A");
        return item;
    }
}
