package com.rtms.backend.medical.dto;

import jakarta.validation.constraints.NotBlank;

public record CorrectInjuryRecordRequest(
        @NotBlank(message = "Ly do hieu chinh la bat buoc")
        String correctionReason,
        String injuryType,
        String severity,
        String description,
        String status,
        MarkerPlacementRequest marker
) {
    /** Existing callers (and tests) without a marker keep their old shape. */
    public CorrectInjuryRecordRequest(
            String correctionReason,
            String injuryType,
            String severity,
            String description,
            String status
    ) {
        this(correctionReason, injuryType, severity, description, status, null);
    }
}
