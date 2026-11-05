package com.rtms.backend.audit.dto;

import java.time.LocalDateTime;

public class AuditLogResponse {

    private Long id;
    private Long actorUserId;
    private String actorName;
    private String actorEmail;
    private String actorRole;
    private String httpMethod;
    private String requestPath;
    private Integer statusCode;
    private String action;
    private String entityName;
    private Long entityId;
    private String details;
    private String module;
    private boolean success;
    private LocalDateTime createdAt;

    public AuditLogResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public String getActorEmail() { return actorEmail; }
    public void setActorEmail(String actorEmail) { this.actorEmail = actorEmail; }

    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public String getRequestPath() { return requestPath; }
    public void setRequestPath(String requestPath) { this.requestPath = requestPath; }

    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static AuditLogResponse from(com.rtms.backend.audit.entity.AuditLog log) {
        AuditLogResponse dto = new AuditLogResponse();
        dto.setId(log.getId());
        dto.setHttpMethod(log.getHttpMethod());
        dto.setRequestPath(log.getRequestPath());
        dto.setStatusCode(log.getStatusCode());
        dto.setAction(log.getAction());
        dto.setEntityName(log.getEntityName());
        dto.setEntityId(log.getEntityId());
        dto.setDetails(log.getDetails());
        dto.setModule(resolveModule(log.getEntityName(), log.getDetails(), log.getRequestPath(), log.getAction()));
        dto.setSuccess(log.getStatusCode() == null || (log.getStatusCode() >= 200 && log.getStatusCode() < 300));
        dto.setCreatedAt(log.getCreatedAt());

        if (log.getUser() != null) {
            dto.setActorUserId(log.getUser().getId());
            dto.setActorName(log.getUser().getFullName());
            dto.setActorEmail(log.getUser().getEmail());
            dto.setActorRole(log.getUser().getRole() != null ? log.getUser().getRole().getName() : null);
        }

        return dto;
    }

    public static String resolveModule(String entityName, String details) {
        return resolveModule(entityName, details, null, null);
    }

    public static String resolveModule(String entityName, String details, String requestPath, String action) {
        // 1. JSON payload module (highest priority)
        if (details != null && details.startsWith("{") && details.contains("\"module\":")) {
            int idx = details.indexOf("\"module\":");
            int startQuote = details.indexOf("\"", idx + 9);
            if (startQuote != -1) {
                int endQuote = details.indexOf("\"", startQuote + 1);
                if (endQuote != -1) {
                    return details.substring(startQuote + 1, endQuote).toUpperCase();
                }
            }
        }

        // 2. Entity name mapping
        if (entityName != null && !entityName.equalsIgnoreCase("System")) {
            String upper = entityName.toUpperCase();
            if (upper.contains("ADMISSION")) return "ADMISSIONS";
            if (upper.contains("CARE") || upper.contains("HEALTH") || upper.contains("MEDICAL")
                    || upper.contains("TREATMENT") || upper.contains("INCIDENT") || upper.contains("PRESCRIPTION")) return "MEDICAL";
            if (upper.contains("STABLE") || upper.contains("STALL") || upper.contains("AREA") || upper.contains("GROOM")) return "STABLE";
            if (upper.contains("HORSE")) return "HORSES";
            if (upper.contains("USER") || upper.contains("STAFF") || upper.contains("ACCOUNT") || upper.contains("PROFILE")) return "STAFF";
            if (upper.contains("TRAINING") || upper.contains("COURSE") || upper.contains("SUBJECT")
                    || upper.contains("LOT") || upper.contains("WORKOUT") || upper.contains("RULE")) return "TRAINING";
            if (upper.contains("RACE") || upper.contains("RACING")) return "RACING";
        }

        // 3. Path heuristic
        if (requestPath != null && !requestPath.isBlank()) {
            String p = requestPath.toLowerCase();
            if (p.contains("/admissions")) return "ADMISSIONS";
            if (p.contains("/care-schedules") || p.contains("/health-records") || p.contains("/health-metrics")
                    || p.contains("/preventive") || p.contains("/treatment-plans") || p.contains("/incidents")
                    || p.contains("/vet/")) return "MEDICAL";
            if (p.contains("/stalls") || p.contains("/areas") || p.contains("/groom-daily-tasks")) return "STABLE";
            if (p.contains("/horses")) return "HORSES";
            if (p.contains("/staff") || p.contains("/users") || p.contains("/accounts")) return "STAFF";
            if (p.contains("/training-plans") || p.contains("/courses") || p.contains("/subjects")
                    || p.contains("/lots") || p.contains("/workouts") || p.contains("/training-rules")) return "TRAINING";
            if (p.contains("/race-registrations")) return "RACING";
        }

        // 4. Action heuristic
        if (action != null && !action.isBlank()) {
            String a = action.toUpperCase();
            if (a.contains("ADMISSION")) return "ADMISSIONS";
            if (a.contains("CARE") || a.contains("HEALTH") || a.contains("VET") || a.contains("TREATMENT")) return "MEDICAL";
            if (a.contains("STALL") || a.contains("AREA") || a.contains("STABLE")) return "STABLE";
            if (a.contains("HORSE")) return "HORSES";
            if (a.contains("STAFF") || a.contains("USER") || a.contains("ACCOUNT")) return "STAFF";
            if (a.contains("TRAINING") || a.contains("WORKOUT") || a.contains("LOT") || a.contains("COURSE")) return "TRAINING";
            if (a.contains("RACE")) return "RACING";
        }

        return "SYSTEM";
    }
}
