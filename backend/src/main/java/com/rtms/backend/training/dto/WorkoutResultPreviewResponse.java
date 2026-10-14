package com.rtms.backend.training.dto;

import java.math.BigDecimal;

/**
 * Simulated measurements shown before a trainer completes a workout.
 * This response is generated in memory and is not persisted by the preview endpoint.
 */
public record WorkoutResultPreviewResponse(
        BigDecimal actualDistanceMeters,
        BigDecimal actualDurationMinutes,
        BigDecimal topSpeedKmh,
        BigDecimal averageSpeedKmh,
        Integer averageHeartRate,
        Integer maxHeartRate,
        Integer recoveryHeartRate) {
}
