package com.rtms.backend.audit.dto;

import java.time.LocalDateTime;

public record AuditArchiveStatusResponse(
        long totalCount,
        long agedCount,
        LocalDateTime oldestLogDate,
        boolean requiresMaintenance
) {
}
