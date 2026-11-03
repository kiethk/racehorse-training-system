package com.rtms.backend.analytics.dto;

import com.rtms.backend.analytics.util.EdaStatisticsCalculator.DistributionStats;

public record EdaDistributionStats(
        double min,
        double max,
        double mean,
        double median,
        double q1,
        double q3,
        double iqr,
        double stdDev,
        int sampleSize,
        int outlierCount
) {
    public static EdaDistributionStats from(DistributionStats stats) {
        return new EdaDistributionStats(
                stats.min(),
                stats.max(),
                stats.mean(),
                stats.median(),
                stats.q1(),
                stats.q3(),
                stats.iqr(),
                stats.stdDev(),
                stats.sampleSize(),
                stats.outliers() != null ? stats.outliers().size() : 0
        );
    }
}
