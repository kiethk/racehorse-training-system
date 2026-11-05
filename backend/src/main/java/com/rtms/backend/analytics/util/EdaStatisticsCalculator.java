package com.rtms.backend.analytics.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Tiện ích toán học thuần túy tính toán các đặc trưng thống kê mô tả (Descriptive Statistics)
 * và phân tích tương quan tuyến tính (Bivariate Pearson Correlation & Regression) cho EDA.
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
     * Tính toán thống kê mô tả: Mean, Median, Q1, Q3, IQR, Độ lệch chuẩn mẫu, và Outliers theo Tukey's Fences.
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

        // Độ lệch chuẩn mẫu (sample standard deviation) với n - 1 bậc tự do
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
     * Tính toán hệ số tương quan Pearson r và phương trình đường hồi quy y = slope * x + intercept.
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

        // Kiểm tra chống chia cho 0 nếu một trong hai biến có phương sai bằng 0
        if (varX <= 1e-12 || varY <= 1e-12) {
            return new CorrelationResult(0.0, "NO_VARIANCE", 0.0, meanY, n);
        }

        double r = covXY / Math.sqrt(varX * varY);
        // Kẹp giá trị trong khoảng [-1.0, 1.0] để khử sai số dấu phẩy động
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
     * Tính phân vị p (từ 0.0 đến 1.0) bằng phương pháp nội suy tuyến tính (Linear Interpolation).
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
