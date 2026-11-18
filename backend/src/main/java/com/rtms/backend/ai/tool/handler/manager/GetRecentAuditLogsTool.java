package com.rtms.backend.ai.tool.handler.manager;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.audit.entity.AuditLog;
import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetRecentAuditLogsTool implements AiToolHandler {

    private final AuditLogRepository auditLogRepository;
    private final AiToolSupport support;

    @Autowired
    public GetRecentAuditLogsTool(@Autowired(required = false) AuditLogRepository auditLogRepository,
                                  AiToolSupport support) {
        this.auditLogRepository = auditLogRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getRecentAuditLogs";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        if (auditLogRepository == null) {
            Map<String, Object> emptyResp = new LinkedHashMap<>();
            emptyResp.put("totalAuditLogs", 0);
            emptyResp.put("auditLogList", Collections.emptyList());
            emptyResp.put("actionUrl", "/manager/audit-log");
            return emptyResp;
        }
        int limit = support.parseInteger(arguments.get("limit"), 10);
        if (limit <= 0) limit = 10;
        if (limit > 50) limit = 50;

        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<AuditLog> logs = auditLogRepository.findAll(pageable).getContent();

        List<Map<String, Object>> result = new ArrayList<>();
        for (AuditLog log : logs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("timestamp", log.getCreatedAt() != null ? log.getCreatedAt().toString() : "N/A");

            String performedBy = "System";
            if (log.getUser() != null) {
                String roleStr = log.getUser().getRole() != null ? log.getUser().getRole().getName() : "";
                performedBy = log.getUser().getFullName() + (!roleStr.isBlank() ? " (" + roleStr + ")" : "");
            }
            item.put("performedBy", performedBy);

            String actionText = log.getAction() != null && !log.getAction().isBlank()
                    ? log.getAction()
                    : (log.getHttpMethod() != null ? log.getHttpMethod() + " " + (log.getRequestPath() != null ? log.getRequestPath() : "") : "System action");
            item.put("action", actionText);

            StringBuilder sb = new StringBuilder();
            if (log.getEntityName() != null && !log.getEntityName().isBlank()) {
                sb.append("Entity: ").append(log.getEntityName());
            }
            if (log.getRequestPath() != null && !log.getRequestPath().isBlank()) {
                if (!sb.isEmpty()) sb.append(" | ");
                sb.append("Path: ").append(log.getHttpMethod() != null ? log.getHttpMethod() + " " : "").append(log.getRequestPath());
            }
            if (log.getStatusCode() != null) {
                if (!sb.isEmpty()) sb.append(" | ");
                sb.append("HTTP status code: ").append(log.getStatusCode());
            }
            item.put("entityName", log.getEntityName() != null ? log.getEntityName() : "N/A");
            item.put("entityId", log.getEntityId());
            item.put("httpMethod", log.getHttpMethod() != null ? log.getHttpMethod() : "N/A");
            item.put("requestPath", log.getRequestPath() != null ? log.getRequestPath() : "N/A");
            item.put("statusCode", log.getStatusCode());
            item.put("details", log.getDetails() != null ? log.getDetails() : "");
            item.put("changeDetails", sb.isEmpty() ? "No additional details" : sb.toString());
            item.put("actionUrl", "/manager/audit-log");

            result.add(item);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalAuditLogs", result.size());
        response.put("auditLogList", result);
        response.put("actionUrl", "/manager/audit-log");
        return response;
    }
}
