package com.rtms.backend.analytics.dto.manager;

public record VeterinarianDelayDto(
        Long veterinarianId,
        String veterinarianName,
        int totalSchedules,
        int totalRollovers,
        double rolloverRate,
        double zScore,
        boolean individualDelinquencyAlert
) {}
