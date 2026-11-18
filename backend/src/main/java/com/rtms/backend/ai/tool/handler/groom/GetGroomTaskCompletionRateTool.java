package com.rtms.backend.ai.tool.handler.groom;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.entity.GroomDailyTask;
import com.rtms.backend.stable.repository.GroomDailyTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Component
public class GetGroomTaskCompletionRateTool implements AiToolHandler {

    private final GroomDailyTaskRepository groomDailyTaskRepository;
    private final AiToolSupport support;

    @Autowired
    public GetGroomTaskCompletionRateTool(GroomDailyTaskRepository groomDailyTaskRepository,
                                         AiToolSupport support) {
        this.groomDailyTaskRepository = groomDailyTaskRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getGroomTaskCompletionRate";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        LocalDate targetDate = support.parseDate(arguments.get("date"), LocalDate.now());
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.atTime(LocalTime.MAX);

        List<GroomDailyTask> tasks = groomDailyTaskRepository.findByGroomIdAndScheduledTimeBetween(
                currentUser.getUserId(), start, end);

        int totalTasks = tasks.size();
        long completedTaskCount = tasks.stream().filter(t -> Boolean.TRUE.equals(t.getIsCompleted())).count();
        double rate = totalTasks > 0 ? ((double) completedTaskCount / totalTasks) * 100.0 : 100.0;
        String completionRateLabel = String.format(Locale.US, "%.1f%%", rate);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", targetDate.toString());
        result.put("totalTasksCount", totalTasks);
        result.put("completedTasksCount", completedTaskCount);
        result.put("completionRate", completionRateLabel);
        result.put("actionUrl", "/groom/tasks");
        return result;
    }
}
