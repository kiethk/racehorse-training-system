package com.rtms.backend.audit.dto;

import java.time.LocalDateTime;

public record AuditPurgeRequest(
        LocalDateTime beforeDate,
        LocalDateTime fromDate,
        String reason
) {
}
