package com.rtms.backend.analytics.dto.manager;

public record BarnOccupancyBreakdownDto(
        Long areaId,
        String areaCode,
        String areaName,
        String areaType,
        long totalStalls,
        long occupiedStalls,
        long availableStalls,
        double occupancyRate
) {}
