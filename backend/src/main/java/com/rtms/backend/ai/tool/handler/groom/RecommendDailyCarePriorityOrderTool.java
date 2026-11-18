package com.rtms.backend.ai.tool.handler.groom;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.entity.GroomDailyTask;
import com.rtms.backend.stable.repository.GroomDailyTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RecommendDailyCarePriorityOrderTool implements AiToolHandler {

    private final GroomDailyTaskRepository groomDailyTaskRepository;
    private final HorseRepository horseRepository;
    private final AiToolSupport support;

    @Autowired
    public RecommendDailyCarePriorityOrderTool(@Autowired(required = false) GroomDailyTaskRepository groomDailyTaskRepository,
                                               @Autowired(required = false) HorseRepository horseRepository,
                                               AiToolSupport support) {
        this.groomDailyTaskRepository = groomDailyTaskRepository;
        this.horseRepository = horseRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "recommendDailyCarePriorityOrder";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<GroomDailyTask> tasks = (groomDailyTaskRepository != null && currentUser.getUserId() != null)
                ? groomDailyTaskRepository.findByGroomIdAndScheduledTimeBetween(currentUser.getUserId(), startOfDay, endOfDay)
                : Collections.emptyList();

        Map<Long, String> horseNames = (horseRepository != null)
                ? horseRepository.findAll().stream().collect(Collectors.toMap(Horse::getId, Horse::getName, (a, b) -> a))
                : Collections.emptyMap();

        List<Map<String, Object>> priorityList = new ArrayList<>();
        if (!tasks.isEmpty()) {
            List<GroomDailyTask> sorted = tasks.stream()
                    .sorted((t1, t2) -> {
                        int p1 = support.getGroomTaskPriority(t1);
                        int p2 = support.getGroomTaskPriority(t2);
                        if (p1 != p2) return Integer.compare(p1, p2);
                        if (t1.getScheduledTime() == null && t2.getScheduledTime() == null) return 0;
                        if (t1.getScheduledTime() == null) return 1;
                        if (t2.getScheduledTime() == null) return -1;
                        return t1.getScheduledTime().compareTo(t2.getScheduledTime());
                    })
                    .toList();

            int seq = 1;
            for (GroomDailyTask t : sorted) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("priorityOrder", seq++);
                item.put("taskType", support.formatGroomTaskType(t.getTaskType()));
                item.put("horseName", horseNames.getOrDefault(t.getHorseId(), "Horse"));
                item.put("estimatedTime", t.getScheduledTime() != null ? t.getScheduledTime().toLocalTime().toString() : "Throughout the day");
                item.put("status", Boolean.TRUE.equals(t.getIsCompleted()) ? "Completed" : "Not completed");
                item.put("notes", t.getNotes() != null ? t.getNotes() : "");
                priorityList.add(item);
            }
        } else {
            priorityList.add(Map.of(
                    "priorityOrder", 1,
                    "taskType", "Health check & Urgent veterinary care (VET_ASSIST / SPECIAL_CARE)",
                    "horseName", "Horse with a health alert or follow-up examination",
                    "estimatedTime", "06:00 - 07:00",
                    "notes", "Priority 1: Help the veterinarian take vital signs and dispense prescribed medication"
            ));
            priorityList.add(Map.of(
                    "priorityOrder", 2,
                    "taskType", "Lead horses to the morning training area (WORKOUT_ASSIST)",
                    "horseName", "Horse scheduled for a morning training session (Session)",
                    "estimatedTime", "07:00 - 09:30",
                    "notes", "Priority 2: Be on time for the trainer's scheduled session"
            ));
            priorityList.add(Map.of(
                    "priorityOrder", 3,
                    "taskType", "Routine feeding & stall cleaning (FEEDING / MUCKING_OUT)",
                    "horseName", "Healthy horses in the stable area",
                    "estimatedTime", "09:30 - 11:30",
                    "notes", "Priority 3: Replace straw bedding and replenish clean water and forage"
            ));
            priorityList.add(Map.of(
                    "priorityOrder", 4,
                    "taskType", "Quarantine care & stall cleaning (QUARANTINE_CARE)",
                    "horseName", "Quarantine area",
                    "estimatedTime", "14:00 - 16:00",
                    "notes", "Priority 4: Perform LAST to maintain biosecurity"
            ));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", today.toString());
        result.put("groomName", support.resolveUserName(currentUser, "Care staff"));
        result.put("totalWorkItemsCount", priorityList.size());
        result.put("priorityOrderList", priorityList);
        result.put("biosecurityNotes", "Biosecurity rule: Always wear protective equipment when entering the quarantine area, and clean and feed at the quarantine stalls LAST to eliminate the risk of cross-contamination.");
        result.put("actionUrl", "/groom/tasks");
        return result;
    }
}
