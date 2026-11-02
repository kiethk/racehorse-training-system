package com.rtms.backend.audit.dto;

import java.util.Map;

public record DomainAuditEvent(
        Long userId,
        String action,
        String entityName,
        Long entityId,
        String module,
        String summary,
        String reason,
        String targetUrl,
        Map<String, Object> before,
        Map<String, Object> after
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private String action;
        private String entityName;
        private Long entityId;
        private String module;
        private String summary;
        private String reason;
        private String targetUrl;
        private Map<String, Object> before;
        private Map<String, Object> after;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder entityName(String entityName) {
            this.entityName = entityName;
            return this;
        }

        public Builder entityId(Long entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder module(String module) {
            this.module = module;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder targetUrl(String targetUrl) {
            this.targetUrl = targetUrl;
            return this;
        }

        public Builder before(Map<String, Object> before) {
            this.before = before;
            return this;
        }

        public Builder after(Map<String, Object> after) {
            this.after = after;
            return this;
        }

        public DomainAuditEvent build() {
            return new DomainAuditEvent(
                    userId,
                    action,
                    entityName,
                    entityId,
                    module,
                    summary,
                    reason,
                    targetUrl,
                    before,
                    after
            );
        }
    }
}

