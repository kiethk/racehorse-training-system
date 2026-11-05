package com.rtms.backend.training.service;

import com.rtms.backend.training.dto.WorkoutResultPreviewResponse;
import com.rtms.backend.training.entity.Subject;
import com.rtms.backend.training.enums.IntensityLevel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

/** Generates a plausible one-time measurement preview for the course demonstration. */
@Service
public class WorkoutResultSimulationService {

    private static final BigDecimal DEFAULT_DISTANCE_METERS = BigDecimal.valueOf(1_000);
    private static final int DEFAULT_DURATION_MINUTES = 60;
    private static final double POOR_RECOVERY_CHANCE = 0.1;

    public WorkoutResultPreviewResponse generate(Subject subject) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        IntensityLevel intensity = subject.getIntensityLevel() == null
                ? IntensityLevel.MEDIUM
                : subject.getIntensityLevel();

        BigDecimal targetDistance = subject.getTargetDistanceMeters() == null
                ? DEFAULT_DISTANCE_METERS
                : subject.getTargetDistanceMeters();
        int targetDuration = subject.getDurationMinutes() == null
                ? DEFAULT_DURATION_MINUTES
                : subject.getDurationMinutes();

        BigDecimal actualDistance = scaled(targetDistance.doubleValue()
                * random.nextDouble(0.95, 1.051));
        BigDecimal actualDuration = scaled(targetDuration
                * random.nextDouble(0.92, 1.031));

        double averageSpeedValue = switch (intensity) {
            case LOW -> random.nextDouble(15.0, 24.1);
            case MEDIUM -> random.nextDouble(24.0, 36.1);
            case HIGH -> random.nextDouble(36.0, 50.1);
        };
        BigDecimal averageSpeed = scaled(averageSpeedValue);
        BigDecimal topSpeed = scaled(averageSpeedValue * random.nextDouble(1.15, 1.31));

        int averageHeartRate = switch (intensity) {
            case LOW -> random.nextInt(115, 146);
            case MEDIUM -> random.nextInt(145, 176);
            case HIGH -> random.nextInt(170, 196);
        };
        int maxHeartRate = Math.min(220, averageHeartRate + random.nextInt(15, 31));
        int recoveryHeartRate = recoveryHeartRate(random, averageHeartRate);

        return new WorkoutResultPreviewResponse(
                actualDistance,
                actualDuration,
                topSpeed,
                averageSpeed,
                averageHeartRate,
                maxHeartRate,
                recoveryHeartRate);
    }

    /**
     * Most horses recover under the safe limit; about one session in ten recovers poorly and raises the alert.
     * A poor recovery stays at least 9 bpm under the average heart rate (always above the limit, since the
     * lowest average is 115).
     */
    private int recoveryHeartRate(ThreadLocalRandom random, int averageHeartRate) {
        int limit = HorsePerformanceService.RECOVERY_HEART_RATE_LIMIT;
        if (random.nextDouble() < POOR_RECOVERY_CHANCE) {
            return random.nextInt(limit + 1, Math.min(limit + 21, averageHeartRate - 8));
        }
        return random.nextInt(60, limit - 1);
    }

    private BigDecimal scaled(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }
}
