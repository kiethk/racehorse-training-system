package com.rtms.backend.ai.tool.handler.groom;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.service.GroomDailyTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
public class GetTodayAggregatedTasksTool implements AiToolHandler {

    private static final Logger log = LoggerFactory.getLogger(GetTodayAggregatedTasksTool.class);

    private final GroomDailyTaskService groomDailyTaskService;

    @Autowired
    public GetTodayAggregatedTasksTool(GroomDailyTaskService groomDailyTaskService) {
        this.groomDailyTaskService = groomDailyTaskService;
    }

    @Override
    public String getToolName() {
        return "getTodayAggregatedTasks";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        LocalDate targetDate = LocalDate.now();
        if (arguments.containsKey("date") && arguments.get("date") != null) {
            try {
                targetDate = LocalDate.parse(arguments.get("date").toString().trim());
            } catch (Exception e) {
                log.warn("Invalid date [{}]; using the current date", arguments.get("date"));
            }
        }
        return groomDailyTaskService.getTodayAggregatedTasks(currentUser.getUserId(), targetDate, currentUser);
    }
}
