package com.rtms.backend.ai.tool.handler.trainer;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.config.TrainingLimits;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.TrainingGroupResponse;
import com.rtms.backend.training.service.TrainingGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetTrainerGroupsTool implements AiToolHandler {

    private final TrainingGroupService trainingGroupService;
    private final AiToolSupport support;

    @Autowired
    public GetTrainerGroupsTool(TrainingGroupService trainingGroupService, AiToolSupport support) {
        this.trainingGroupService = trainingGroupService;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getTrainerGroups";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        List<Map<String, Object>> live = trainingGroupService.getGroups(currentUser).stream().map(this::groupItem).toList();
        List<Map<String, Object>> disbanded = trainingGroupService.getDisbandedGroups(currentUser).stream().map(this::groupItem).toList();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalGroups", live.size());
        response.put("groupList", live);
        response.put("disbandedGroupCount", disbanded.size());
        response.put("disbandedGroupList", disbanded);
        response.put("actionUrl", "/trainer/groups");
        return response;
    }

    private Map<String, Object> groupItem(TrainingGroupResponse group) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("groupName", support.groupLabel(group.name(), group.disbanded()));
        item.put("horseCount", group.members().size() + "/" + TrainingLimits.MAX_HORSES_PER_GROUP);
        item.put("horseList", group.members().stream()
                .map(member -> member.horseName() + " (Groom: " + (member.groomName() != null ? member.groomName() : "unassigned") + ")")
                .toList());
        if (group.currentCourseRunStatus() != null) {
            item.put("currentCourse", group.currentCourseName());
            item.put("currentCourseRunStatus", support.courseRunStatusLabel(group.currentCourseRunStatus()));
            item.put("currentCourseRunPeriod", group.currentCourseRunStartDate() + " → " + group.currentCourseRunEndDate());
        } else {
            item.put("currentCourse", group.disbanded() ? "Disbanded" : "Available (no course assigned)");
        }
        return item;
    }
}
