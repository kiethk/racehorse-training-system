package com.rtms.backend.analytics.dto;

import java.util.List;

public record IndividualHorseEdaResponse(
        Long horseId,
        String horseName,
        String breed,
        EdaDistributionStats speedStats,
        EdaDistributionStats recoveryHeartRateStats,
        EdaCorrelationSummary correlation,
        List<IndividualWorkoutEdaPoint> points
) {}
