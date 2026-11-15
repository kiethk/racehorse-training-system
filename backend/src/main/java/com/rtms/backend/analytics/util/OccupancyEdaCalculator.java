package com.rtms.backend.analytics.util;

import com.rtms.backend.analytics.dto.manager.BarnOccupancyBreakdownDto;
import com.rtms.backend.analytics.dto.manager.StallOccupancyEdaDto;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.enums.AreaType;
import com.rtms.backend.stable.enums.StallStatus;
import com.rtms.backend.stable.repository.StableStallRepository.AreaStallCountProjection;

import java.util.*;

public final class OccupancyEdaCalculator {

    private OccupancyEdaCalculator() {}

    public static StallOccupancyEdaDto calculate(
            List<Area> areas,
            List<AreaStallCountProjection> projections,
            long awaitingArrivalAdmissions
    ) {
        Map<Long, Map<StallStatus, Long>> areaStatusMap = new HashMap<>();
        if (projections != null) {
            for (AreaStallCountProjection row : projections) {
                if (row.getAreaId() != null && row.getStatus() != null) {
                    areaStatusMap
                            .computeIfAbsent(row.getAreaId(), k -> new EnumMap<>(StallStatus.class))
                            .put(row.getStatus(), row.getCount());
                }
            }
        }

        long facilityTotal = 0;
        long facilityOccupied = 0;
        long facilityAvailable = 0;

        long quarantineTotal = 0;
        long quarantineOccupied = 0;
        long quarantineAvailable = 0;

        long regularTotal = 0;
        long regularOccupied = 0;
        long regularAvailable = 0;

        List<BarnOccupancyBreakdownDto> barnBreakdown = new ArrayList<>();

        if (areas != null) {
            for (Area area : areas) {
                Map<StallStatus, Long> counts = areaStatusMap.getOrDefault(area.getId(), Collections.emptyMap());
                long occupied = counts.getOrDefault(StallStatus.OCCUPIED, 0L);
                long reserved = counts.getOrDefault(StallStatus.RESERVED, 0L);
                long available = counts.getOrDefault(StallStatus.AVAILABLE, 0L);
                long total = occupied + reserved + available;

                long effectiveOccupied = occupied + reserved;
                double rate = total > 0 ? Math.round(((double) effectiveOccupied / total * 100.0) * 10.0) / 10.0 : 0.0;

                boolean isQuarantine = area.getType() == AreaType.QUARANTINE;
                if (isQuarantine) {
                    quarantineTotal += total;
                    quarantineOccupied += effectiveOccupied;
                    quarantineAvailable += available;
                } else {
                    regularTotal += total;
                    regularOccupied += effectiveOccupied;
                    regularAvailable += available;
                }

                facilityTotal += total;
                facilityOccupied += effectiveOccupied;
                facilityAvailable += available;

                barnBreakdown.add(new BarnOccupancyBreakdownDto(
                        area.getId(),
                        area.getCode(),
                        area.getName(),
                        area.getType() != null ? area.getType().name() : "REGULAR",
                        total,
                        effectiveOccupied,
                        available,
                        rate
                ));
            }
        }

        double overallRate = facilityTotal > 0
                ? Math.round(((double) facilityOccupied / facilityTotal * 100.0) * 10.0) / 10.0
                : 0.0;
        double quarantineRate = quarantineTotal > 0
                ? Math.round(((double) quarantineOccupied / quarantineTotal * 100.0) * 10.0) / 10.0
                : 0.0;
        double regularRate = regularTotal > 0
                ? Math.round(((double) regularOccupied / regularTotal * 100.0) * 10.0) / 10.0
                : 0.0;

        long inflowBufferMargin = quarantineAvailable - awaitingArrivalAdmissions;
        boolean quarantineStressAlert = quarantineRate >= 85.0 || inflowBufferMargin <= 0;

        return new StallOccupancyEdaDto(
                overallRate,
                facilityTotal,
                facilityOccupied,
                facilityAvailable,
                quarantineTotal,
                quarantineOccupied,
                quarantineAvailable,
                quarantineRate,
                regularTotal,
                regularOccupied,
                regularAvailable,
                regularRate,
                awaitingArrivalAdmissions,
                inflowBufferMargin,
                quarantineStressAlert,
                barnBreakdown
        );
    }
}
