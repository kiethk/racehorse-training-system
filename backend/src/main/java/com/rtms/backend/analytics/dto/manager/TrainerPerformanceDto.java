package com.rtms.backend.analytics.dto.manager;

public record TrainerPerformanceDto(
        Long trainerId,
        String trainerName,
        int scheduledWorkouts,
        int completedWorkouts,
        int heldWorkouts,
        int cancelledWorkouts,
        double adherenceRate,
        double cancellationRate,
        double averageRating,
        double averageSpeedKmh,
        double averageRecoveryHeartRate,
        double speedGainKmh,
        double hrRecoveryImprovementBpm
) {
    public TrainerPerformanceDto(
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
    ) {
        this(trainerId, trainerName, scheduledWorkouts, completedWorkouts, heldWorkouts, cancelledWorkouts,
                adherenceRate, 0.0, averageRating, averageSpeedKmh, averageRecoveryHeartRate, speedGainKmh, hrRecoveryImprovementBpm);
    }
}
