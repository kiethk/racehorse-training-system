package com.rtms.backend.ai.tool.handler.vet;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetUrgentCareAndIncidentAlertsTool implements AiToolHandler {

    private final GetUrgentCareAlertsTool urgentCareTool;
    private final GetRecentIncidentReportsTool incidentReportsTool;

    @Autowired
    public GetUrgentCareAndIncidentAlertsTool(GetUrgentCareAlertsTool urgentCareTool,
                                             GetRecentIncidentReportsTool incidentReportsTool) {
        this.urgentCareTool = urgentCareTool;
        this.incidentReportsTool = incidentReportsTool;
    }

    @Override
    public String getToolName() {
        return "getUrgentCareAndIncidentAlerts";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Object urgentCare = urgentCareTool.execute(arguments, currentUser);
        Object recentIncidents = incidentReportsTool.execute(arguments, currentUser);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("caTrucCapCuu", urgentCare);
        result.put("suCoChuongTrai48h", recentIncidents);
        result.put("actionUrl", "/veterinarian/urgent");
        return result;
    }
}
