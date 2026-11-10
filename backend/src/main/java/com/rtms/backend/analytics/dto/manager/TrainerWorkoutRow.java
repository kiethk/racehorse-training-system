package com.rtms.backend.analytics.dto.manager;

import com.rtms.backend.training.enums.WorkoutStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TrainerWorkoutRow(
        Long trainerId,
        WorkoutStatus status,
        Integer performanceRating,
        BigDecimal averageSpeedKmhBigDecimal,
        Integer recoveryHeartRate,
        LocalDate sessionDate,
        Long horseId
) {
    public Double averageSpeedKmh() {
        return averageSpeedKmhBigDecimal != null ? averageSpeedKmhBigDecimal.doubleValue() : null;
    }
}


