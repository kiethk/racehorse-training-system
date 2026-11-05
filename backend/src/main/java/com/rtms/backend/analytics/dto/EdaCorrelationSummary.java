package com.rtms.backend.analytics.dto;

import com.rtms.backend.analytics.util.EdaStatisticsCalculator.CorrelationResult;

public record EdaCorrelationSummary(
        String xVariable,
        String yVariable,
        double pearsonR,
        String strength,
        double slope,
        double intercept,
        int sampleSize
) {
    public static EdaCorrelationSummary from(String xVar, String yVar, CorrelationResult res) {
        return new EdaCorrelationSummary(
                xVar,
                yVar,
                res.pearsonR(),
                res.strength(),
                res.slope(),
                res.intercept(),
                res.sampleSize()
        );
    }
}
