package com.rtms.backend.ai.tool.handler;

import com.rtms.backend.training.enums.DistanceBand;

import java.math.BigDecimal;
import java.util.*;

/** Calculates readable specialty statistics from horse performance data. */
final class SpecialtyMetricsCalculator {
    private SpecialtyMetricsCalculator() {
    }

    static AiToolSupport.SpecialtyMetrics compute(AiToolSupport support, com.rtms.backend.training.dto.HorseSpecialtyResponse resp) {
            if (resp == null) {
                return new AiToolSupport.SpecialtyMetrics(
                        0,
                        Collections.emptyList(),
                        List.of("Undetermined"),
                        "Unknown",
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        Collections.emptyList()
                );
            }
    
            List<DistanceBand> strong = resp.strongDistances() != null ? resp.strongDistances() : Collections.emptyList();
            int specialtyCount = strong.size();
            List<String> strongBands = strong.stream().map(DistanceBand::name).toList();
            List<String> formattedDistances = !strong.isEmpty()
                    ? strong.stream().map(support::formatDistanceBand).toList()
                    : List.of("Undetermined");
    
            String surf = resp.preferredSurface() != null
                    ? (resp.preferredSurface() == com.rtms.backend.training.enums.SurfaceType.TURF ? "Turf (TURF)" : "Dirt (DIRT)")
                    : "Unknown";
    
            List<com.rtms.backend.training.dto.DistanceStat> stats = resp.distanceStats() != null ? resp.distanceStats() : Collections.emptyList();
    
            List<com.rtms.backend.training.dto.DistanceStat> qualifyingStats = stats.stream()
                    .filter(s -> strong.contains(s.band()) && s.averageRating() != null && s.workoutCount() > 0)
                    .toList();
    
            BigDecimal bestRating = BigDecimal.ZERO;
            BigDecimal avgRating = BigDecimal.ZERO;
    
            if (!qualifyingStats.isEmpty()) {
                bestRating = qualifyingStats.stream()
                        .map(com.rtms.backend.training.dto.DistanceStat::averageRating)
                        .max(Comparator.naturalOrder())
                        .orElse(BigDecimal.ZERO);
    
                int totalWorkouts = qualifyingStats.stream().mapToInt(com.rtms.backend.training.dto.DistanceStat::workoutCount).sum();
                if (totalWorkouts > 0) {
                    BigDecimal sum = BigDecimal.ZERO;
                    for (com.rtms.backend.training.dto.DistanceStat s : qualifyingStats) {
                        sum = sum.add(s.averageRating().multiply(BigDecimal.valueOf(s.workoutCount())));
                    }
                    avgRating = sum.divide(BigDecimal.valueOf(totalWorkouts), 1, java.math.RoundingMode.HALF_UP);
                }
            } else {
                List<com.rtms.backend.training.dto.DistanceStat> allValid = stats.stream()
                        .filter(s -> s.averageRating() != null && s.workoutCount() > 0)
                        .toList();
                if (!allValid.isEmpty()) {
                    bestRating = allValid.stream()
                            .map(com.rtms.backend.training.dto.DistanceStat::averageRating)
                            .max(Comparator.naturalOrder())
                            .orElse(BigDecimal.ZERO);
                    int totalWorkouts = allValid.stream().mapToInt(com.rtms.backend.training.dto.DistanceStat::workoutCount).sum();
                    if (totalWorkouts > 0) {
                        BigDecimal sum = BigDecimal.ZERO;
                        for (com.rtms.backend.training.dto.DistanceStat s : allValid) {
                            sum = sum.add(s.averageRating().multiply(BigDecimal.valueOf(s.workoutCount())));
                        }
                        avgRating = sum.divide(BigDecimal.valueOf(totalWorkouts), 1, java.math.RoundingMode.HALF_UP);
                    }
                }
            }
    
            List<String> breakdown = new ArrayList<>();
            for (com.rtms.backend.training.dto.DistanceStat s : stats) {
                if (s.workoutCount() > 0 && s.averageRating() != null) {
                    String badge = strong.contains(s.band()) ? "⭐ [SPECIALTY] " : "";
                    String speedStr = s.averageTopSpeedKmh() != null ? (", Top speed: " + s.averageTopSpeedKmh() + " km/h") : "";
                    breakdown.add(badge + support.formatDistanceBand(s.band()) + ": " + s.averageRating() + "/10 (" + s.workoutCount() + " workouts" + speedStr + ")");
                }
            }
    
            return new AiToolSupport.SpecialtyMetrics(
                    specialtyCount,
                    strongBands,
                    formattedDistances,
                    surf,
                    avgRating,
                    bestRating,
                    breakdown
            );
        }
}
