package com.rtms.backend.admission.dto;

import java.util.List;

public record GroomAdmissionQueueResponse(
        List<AdmissionSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
