package com.rtms.backend.analytics.dto;

public record FleetWorkoutSummaryRow(
        Long horseId,
        Double avgSpeedKmh,
        Double avgRecoveryHeartRate,
        Integer maxHeartRate
) {}
