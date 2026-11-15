package com.rtms.backend.analytics.dto.manager;

public record TrainerPerformanceDto(
        Long trainerId,
        String trainerName,
        int scheduledWorkouts,
        int completedWorkouts,
        int heldWorkouts,
        int cancelledWorkouts,
        double adherenceRate,
        double averageRating,
        double averageSpeedKmh,
        double averageRecoveryHeartRate,
        double speedGainKmh,
        double hrRecoveryImprovementBpm
) {}
