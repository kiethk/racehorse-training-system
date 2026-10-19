package com.rtms.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CorrectInjuryRecordRequest(
        @NotBlank(message = "Lý do hiệu chỉnh là bắt buộc")
        String correctionReason,
        String injuryType,
        String severity,
        String description,
        String status
) {}
