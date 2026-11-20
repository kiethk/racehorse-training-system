package com.rtms.backend.analytics.dto.horse;

public record IndividualWorkoutEdaPoint(
        Long workoutId,
        String sessionDate,
        String sessionName,
        Double speedKmh,
        Double recoveryHeartRateBpm,
        Double maxHeartRateBpm,
        Double distanceMeters,
        boolean isOutlier,
        String outlierReason
) {}
