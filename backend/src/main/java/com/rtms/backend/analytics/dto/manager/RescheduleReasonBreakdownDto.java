package com.rtms.backend.analytics.dto.manager;

public record RescheduleReasonBreakdownDto(
        String reason,
        long count,
        double percentage
) {}
