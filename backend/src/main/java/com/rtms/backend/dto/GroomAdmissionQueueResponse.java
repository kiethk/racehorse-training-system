package com.rtms.backend.dto;

import java.util.List;

public record GroomAdmissionQueueResponse(
        List<AdmissionSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
