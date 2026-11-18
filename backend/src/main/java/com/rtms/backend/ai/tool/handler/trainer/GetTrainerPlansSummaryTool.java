package com.rtms.backend.ai.tool.handler.trainer;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.HorseAttendanceResponse;
import com.rtms.backend.training.dto.HorseCourseRunResponse;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.CourseRun;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.CourseRunRepository;
import com.rtms.backend.training.service.HorseAttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class GetTrainerPlansSummaryTool implements AiToolHandler {

    private final CourseRunRepository courseRunRepository;
    private final HorseAttendanceService horseAttendanceService;
    private final CourseRepository courseRepository;
    private final AiToolSupport support;

    @Autowired
    public GetTrainerPlansSummaryTool(CourseRunRepository courseRunRepository,
                                      HorseAttendanceService horseAttendanceService,
                                      @Autowired(required = false) CourseRepository courseRepository,
                                      AiToolSupport support) {
        this.courseRunRepository = courseRunRepository;
        this.horseAttendanceService = horseAttendanceService;
        this.courseRepository = courseRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getTrainerPlansSummary";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Long trainerId = currentUser.getUserId();
        Horse targetHorse = support.resolveHorse(arguments);
        Long horseId = targetHorse != null ? targetHorse.getId() : support.parseLong(arguments.get("horseId"));
        String status = arguments.get("status") != null ? arguments.get("status").toString().trim() : null;

        List<HorseCourseRunResponse> summaries = courseRunRepository.findHorseCourseRuns(trainerId, horseId);
        Map<Long, Map<Long, HorseAttendanceResponse>> historyByHorse = new HashMap<>();
        for (Long id : summaries.stream().map(HorseCourseRunResponse::getHorseId).distinct().toList()) {
            historyByHorse.put(id, horseAttendanceService.getHistory(id, currentUser).stream()
                    .collect(Collectors.toMap(HorseAttendanceResponse::courseRunId, Function.identity(), (a, b) -> a)));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (HorseCourseRunResponse p : summaries) {
            HorseAttendanceResponse line = historyByHorse.getOrDefault(p.getHorseId(), Map.of()).get(p.getCourseRunId());
            if (line == null || (status != null && !status.isBlank() && !status.equalsIgnoreCase(line.courseRunStatus()))) {
                continue;
            }
            String targetGoal = "Improve fitness, endurance, and racing skills";
            if (courseRepository != null && p.getCourseId() != null) {
                targetGoal = courseRepository.findById(p.getCourseId())
                        .map(Course::getTargetGoal)
                        .filter(g -> g != null && !g.isBlank())
                        .orElse(targetGoal);
            }
            Map<String, Object> item = attendanceItem(p.getHorseName() != null ? p.getHorseName() : "Racehorse", line);
            item.put("targetGoal", targetGoal);
            if (courseRunRepository != null && p.getCourseRunId() != null) {
                CourseRun gp = courseRunRepository.findById(p.getCourseRunId()).orElse(null);
                if (gp != null && gp.getTrainingDays() != null && !gp.getTrainingDays().isEmpty()) {
                    String daysStr = gp.getTrainingDays().stream().map(Enum::name).collect(Collectors.joining(", "));
                    item.put("trainingDays", daysStr);
                } else {
                    item.put("trainingDays", "N/A");
                }
            } else {
                item.put("trainingDays", "N/A");
            }
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> attendanceItem(String horseName, HorseAttendanceResponse line) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("planName", line.courseName() != null ? line.courseName() : "Training course");
        item.put("horseName", horseName);
        item.put("groupName", support.groupLabel(line.groupName(), line.groupDisbanded()));
        item.put("status", support.courseRunStatusLabel(line.courseRunStatus()));
        item.put("progress", line.attended() + "/" + line.total() + " sessions attended");
        item.put("completedSessions", line.attended());
        item.put("totalSessions", line.total());
        item.put("joinedOn", line.joinedOn() != null ? line.joinedOn().toString() : "N/A");
        item.put("startDate", line.startDate() != null ? line.startDate().toString() : "N/A");
        item.put("endDate", line.endDate() != null ? line.endDate().toString() : "N/A");
        return item;
    }
}
