package com.rtms.backend.analytics.dto.manager;

import java.util.List;

public record StallOccupancyEdaDto(
        double overallRate,
        long totalStalls,
        long occupiedStalls,
        long availableStalls,
        long quarantineTotal,
        long quarantineOccupied,
        long quarantineAvailable,
        double quarantineRate,
        long regularTotal,
        long regularOccupied,
        long regularAvailable,
        double regularRate,
        long awaitingArrivalAdmissions,
        long inflowBufferMargin,
        boolean quarantineStressAlert,
        List<BarnOccupancyBreakdownDto> barnBreakdown
) {}
