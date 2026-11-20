package com.rtms.backend.analytics.dto.common;

import com.rtms.backend.analytics.calculator.EdaStatisticsCalculator.CorrelationResult;

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
