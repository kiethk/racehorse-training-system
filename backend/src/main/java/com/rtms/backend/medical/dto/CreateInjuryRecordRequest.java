package com.rtms.backend.medical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateInjuryRecordRequest(
        @NotNull Long healthRecordId,
        @NotBlank String injuryType,
        @NotNull Long bodyRegionId,
        @NotBlank String severity,
        String description,
        @NotNull LocalDateTime diagnosedAt,
        BigDecimal positionX,
        BigDecimal positionY,
        BigDecimal positionZ,
        String zoneId,
        BigDecimal localX,
        BigDecimal localY,
        BigDecimal localZ,
        BigDecimal normalX,
        BigDecimal normalY,
        BigDecimal normalZ,
        String meshName
) {}