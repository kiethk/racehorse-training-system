package com.rtms.backend.ai.hitl.model;

import com.rtms.backend.ai.hitl.enums.ActionStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pending_agent_actions")
public class PendingAgentAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action_token", nullable = false, unique = true, length = 255)
    private String actionToken;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "target_role", nullable = false, length = 50)
    private String targetRole;

    @Column(name = "tool_name", nullable = false, length = 100)
    private String toolName;

    @Column(name = "target_entity_id", length = 100)
    private String targetEntityId;

    @Column(name = "diff_payload", nullable = false, columnDefinition = "jsonb")
    private String diffPayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ActionStatus status = ActionStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public PendingAgentAction() {
    }

    public PendingAgentAction(String actionToken, Long userId, String targetRole,
                              String toolName, String targetEntityId, String diffPayload,
                              LocalDateTime expiresAt) {
        this.actionToken = actionToken;
        this.userId = userId;
        this.targetRole = targetRole;
        this.toolName = toolName;
        this.targetEntityId = targetEntityId;
        this.diffPayload = diffPayload;
        this.expiresAt = expiresAt;
        this.status = ActionStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getActionToken() {
        return actionToken;
    }

    public void setActionToken(String actionToken) {
        this.actionToken = actionToken;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getTargetEntityId() {
        return targetEntityId;
    }

    public void setTargetEntityId(String targetEntityId) {
        this.targetEntityId = targetEntityId;
    }

    public String getDiffPayload() {
        return diffPayload;
    }

    public void setDiffPayload(String diffPayload) {
        this.diffPayload = diffPayload;
    }

    public ActionStatus getStatus() {
        return status;
    }

    public void setStatus(ActionStatus status) {
        this.status = status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
