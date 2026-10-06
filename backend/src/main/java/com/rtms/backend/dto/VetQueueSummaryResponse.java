package com.rtms.backend.dto;

public record VetQueueSummaryResponse(
        long total,
        long awaiting,
        long inProgress
) {}
