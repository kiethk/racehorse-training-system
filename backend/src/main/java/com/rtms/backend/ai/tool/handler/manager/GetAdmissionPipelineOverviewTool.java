package com.rtms.backend.ai.tool.handler.manager;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class GetAdmissionPipelineOverviewTool implements AiToolHandler {

    private final GetAdmissionRequestsOverviewTool overviewTool;
    private final GetAdmissionPipelineDetailsTool detailsTool;

    @Autowired
    public GetAdmissionPipelineOverviewTool(
            GetAdmissionRequestsOverviewTool overviewTool,
            GetAdmissionPipelineDetailsTool detailsTool) {
        this.overviewTool = overviewTool;
        this.detailsTool = detailsTool;
    }

    @Override
    public String getToolName() {
        return "getAdmissionPipelineOverview";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Object overview = overviewTool.execute(arguments, currentUser);
        Object details = detailsTool.execute(arguments, currentUser);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tongQuanSoLuong", overview);
        result.put("chiTietCacDon", details);
        result.put("actionUrl", "/manager/admissions");
        return result;
    }
}
