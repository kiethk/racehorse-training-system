package com.rtms.backend.admission.dto;

public record VetQueueSummaryResponse(
        long total,
        long active,
        long awaiting,
        long inProgress,
        long approved
) {
    public VetQueueSummaryResponse(long total, long awaiting, long inProgress) {
        this(total, total, awaiting, inProgress, 0);
    }
}
