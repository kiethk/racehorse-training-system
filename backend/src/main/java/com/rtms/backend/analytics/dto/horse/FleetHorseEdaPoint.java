package com.rtms.backend.analytics.dto.horse;

public record FleetHorseEdaPoint(
        Long horseId,
        String horseName,
        String breed,
        String stallCode,
        String trainerName,
        Double avgSpeedKmh,
        Double recoveryHeartRateBpm,
        Double maxHeartRateBpm,
        boolean isOutlier,
        String outlierReason
) {}
