package com.rtms.backend.medical.dto;

import jakarta.validation.constraints.NotBlank;

public record CorrectInjuryRecordRequest(
        @NotBlank(message = "Ly do hieu chinh la bat buoc")
        String correctionReason,
        String injuryType,
        String severity,
        String description,
        String status
) {}
