package com.rtms.backend.analytics.dto;

import java.util.List;

public record FleetEdaResponse(
        EdaDistributionStats speedStats,
        EdaDistributionStats recoveryHeartRateStats,
        EdaCorrelationSummary correlation,
        List<FleetHorseEdaPoint> points
) {}
