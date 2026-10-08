package com.rtms.backend.dto;
import com.rtms.backend.entity.AuditLog;

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static AuditLogResponse from(com.rtms.backend.entity.AuditLog log) {
        AuditLogResponse dto = new AuditLogResponse();
        dto.setId(log.getId());
        dto.setHttpMethod(log.getHttpMethod());
        dto.setRequestPath(log.getRequestPath());
        dto.setStatusCode(log.getStatusCode());
        dto.setAction(log.getAction());
        dto.setEntityName(log.getEntityName());
        dto.setEntityId(log.getEntityId());
        dto.setCreatedAt(log.getCreatedAt());

        if (log.getUser() != null) {
            dto.setActorUserId(log.getUser().getId());
            dto.setActorName(log.getUser().getFullName());
            dto.setActorEmail(log.getUser().getEmail());
            dto.setActorRole(log.getUser().getRole() != null ? log.getUser().getRole().getName() : null);
        }

        return dto;
    }
}
