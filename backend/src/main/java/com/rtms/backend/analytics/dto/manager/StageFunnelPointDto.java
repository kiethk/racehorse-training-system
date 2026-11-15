package com.rtms.backend.analytics.dto.manager;

public record StageFunnelPointDto(
        String stage,
        long count,
        double conversionPercentage
) {}
