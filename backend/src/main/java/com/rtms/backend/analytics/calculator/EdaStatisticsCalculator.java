package com.rtms.backend.analytics.calculator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Pure mathematical utility for computing descriptive statistics (Descriptive Statistics)
 * and analyzing linear correlation (Bivariate Pearson Correlation & Regression) for EDA.
 */
public final class EdaStatisticsCalculator {

    private EdaStatisticsCalculator() {
        // static utility
    }

    public record DistributionStats(
            double min,
            double max,
            double mean,
            double median,
            double q1,
            double q3,
            double iqr,
            double stdDev,
            int sampleSize,
            List<Double> outliers
    ) {}

    public record CorrelationResult(
            double pearsonR,
            String strength,
            double slope,
            double intercept,
            int sampleSize
    ) {}

    /**
     * Computes descriptive statistics: Mean, Median, Q1, Q3, IQR, sample standard deviation, and Outliers according to Tukey's Fences.
     */
    public static DistributionStats computeDistribution(List<Double> rawValues) {
        if (rawValues == null || rawValues.isEmpty()) {
            return new DistributionStats(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, Collections.emptyList());
        }

        List<Double> values = rawValues.stream()
                .filter(Objects::nonNull)
                .filter(v -> !v.isNaN() && !v.isInfinite())
                .sorted()
                .toList();

        int n = values.size();
        if (n == 0) {
            return new DistributionStats(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, Collections.emptyList());
        }

        double min = values.get(0);
        double max = values.get(n - 1);

        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        double mean = sum / n;

        if (n == 1) {
            return new DistributionStats(min, max, mean, mean, mean, mean, 0.0, 0.0, 1, Collections.emptyList());
        }

        // Sample standard deviation with n - 1 degrees of freedom
        double sumSqDiff = 0.0;
        for (double v : values) {
            double diff = v - mean;
            sumSqDiff += diff * diff;
        }
        double stdDev = Math.sqrt(sumSqDiff / (n - 1));

        double median = computePercentile(values, 0.50);
        double q1 = computePercentile(values, 0.25);
        double q3 = computePercentile(values, 0.75);
        double iqr = Math.max(0.0, q3 - q1);

        // Tukey's fences: [Q1 - 1.5 * IQR, Q3 + 1.5 * IQR]
        double lowerBound = q1 - 1.5 * iqr;
        double upperBound = q3 + 1.5 * iqr;

        List<Double> outliers = new ArrayList<>();
        for (double v : values) {
            if (v < lowerBound || v > upperBound) {
                outliers.add(v);
            }
        }

        return new DistributionStats(min, max, mean, median, q1, q3, iqr, stdDev, n, outliers);
    }

    /**
     * Computes the Pearson correlation coefficient r and regression equation y = slope * x + intercept.
     */
    public static CorrelationResult computeCorrelation(List<Double> xRaw, List<Double> yRaw) {
        if (xRaw == null || yRaw == null || xRaw.size() != yRaw.size() || xRaw.size() < 2) {
            return new CorrelationResult(0.0, "INSUFFICIENT_DATA", 0.0, 0.0, xRaw != null ? xRaw.size() : 0);
        }

        List<Double> validX = new ArrayList<>();
        List<Double> validY = new ArrayList<>();

        for (int i = 0; i < xRaw.size(); i++) {
            Double xi = xRaw.get(i);
            Double yi = yRaw.get(i);
            if (xi != null && yi != null && !xi.isNaN() && !yi.isNaN() && !xi.isInfinite() && !yi.isInfinite()) {
                validX.add(xi);
                validY.add(yi);
            }
        }

        int n = validX.size();
        if (n < 2) {
            return new CorrelationResult(0.0, "INSUFFICIENT_DATA", 0.0, 0.0, n);
        }

        double sumX = 0.0;
        double sumY = 0.0;
        for (int i = 0; i < n; i++) {
            sumX += validX.get(i);
            sumY += validY.get(i);
        }
        double meanX = sumX / n;
        double meanY = sumY / n;

        double varX = 0.0;
        double varY = 0.0;
        double covXY = 0.0;

        for (int i = 0; i < n; i++) {
            double diffX = validX.get(i) - meanX;
            double diffY = validY.get(i) - meanY;
            varX += diffX * diffX;
            varY += diffY * diffY;
            covXY += diffX * diffY;
        }

        // Check for division by zero if either variable has zero variance
        if (varX <= 1e-12 || varY <= 1e-12) {
            return new CorrelationResult(0.0, "NO_VARIANCE", 0.0, meanY, n);
        }

        double r = covXY / Math.sqrt(varX * varY);
        // Clamp the value to the range [-1.0, 1.0] to eliminate floating-point error
        r = Math.max(-1.0, Math.min(1.0, r));

        double slope = covXY / varX;
        double intercept = meanY - slope * meanX;

        String strength = determineStrength(r);

        return new CorrelationResult(r, strength, slope, intercept, n);
    }

    private static String determineStrength(double r) {
        double absR = Math.abs(r);
        if (absR >= 0.7) {
            return r > 0 ? "STRONG_POSITIVE" : "STRONG_NEGATIVE";
        } else if (absR >= 0.4) {
            return r > 0 ? "MODERATE_POSITIVE" : "MODERATE_NEGATIVE";
        } else {
            return "WEAK";
        }
    }

    /**
     * Computes percentile p (from 0.0 to 1.0) using linear interpolation (Linear Interpolation).
     */
    private static double computePercentile(List<Double> sortedValues, double p) {
        int n = sortedValues.size();
        if (n == 0) return 0.0;
        if (n == 1) return sortedValues.get(0);

        double index = p * (n - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);

        if (lower == upper) {
            return sortedValues.get(lower);
        }

        double fraction = index - lower;
        return sortedValues.get(lower) + fraction * (sortedValues.get(upper) - sortedValues.get(lower));
    }
}
