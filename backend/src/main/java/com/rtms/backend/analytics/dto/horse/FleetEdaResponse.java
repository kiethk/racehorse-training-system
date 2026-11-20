package com.rtms.backend.analytics.dto.horse;

import com.rtms.backend.analytics.dto.common.EdaCorrelationSummary;

import com.rtms.backend.analytics.dto.common.EdaDistributionStats;

import java.util.List;

public record FleetEdaResponse(
        EdaDistributionStats speedStats,
        EdaDistributionStats recoveryHeartRateStats,
        EdaCorrelationSummary correlation,
        List<FleetHorseEdaPoint> points
) {}
