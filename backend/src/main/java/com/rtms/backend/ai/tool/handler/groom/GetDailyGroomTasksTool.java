package com.rtms.backend.ai.tool.handler.groom;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.repository.GroomDailyTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class GetDailyGroomTasksTool implements AiToolHandler {

    private final GroomDailyTaskRepository groomDailyTaskRepository;

    @Autowired
    public GetDailyGroomTasksTool(GroomDailyTaskRepository groomDailyTaskRepository) {
        this.groomDailyTaskRepository = groomDailyTaskRepository;
    }

    @Override
    public String getToolName() {
        return "getDailyGroomTasks";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
        return groomDailyTaskRepository.findByScheduledTimeBetween(startOfDay, endOfDay);
    }
}
