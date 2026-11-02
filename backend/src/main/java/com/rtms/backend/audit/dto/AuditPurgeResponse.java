package com.rtms.backend.audit.dto;

import java.time.LocalDateTime;

public record AuditPurgeResponse(
        int purgedCount,
        LocalDateTime purgedAt,
        String message
) {
}
