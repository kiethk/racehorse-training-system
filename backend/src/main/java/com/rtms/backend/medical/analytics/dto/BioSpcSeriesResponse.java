package com.rtms.backend.medical.analytics.dto;

import java.time.LocalDateTime;
import java.util.List;

public record BioSpcSeriesResponse(
        Long horseId,
        String horseName,
        String metricType,
        Double baselineMean,
        Double baselineStdDev,
        Integer sampleCount,
        boolean isBaselineActive,
        Double upperControlLimit,
        Double lowerControlLimit,
        Double upperWarningLimit,
        Double lowerWarningLimit,
        Double speciesMinLimit,
        Double speciesMaxLimit,
        List<BioSpcPointDto> points
) {
    public record BioSpcPointDto(
            Long id,
            LocalDateTime recordedAt,
            Double value,
            boolean isSpecialCause,
            String anomalyDescription
    ) {}
}
