package com.rtms.backend.analytics.dto.horse;

public record FleetWorkoutSummaryRow(
        Long horseId,
        Double avgSpeedKmh,
        Double avgRecoveryHeartRate,
        Integer maxHeartRate
) {}
