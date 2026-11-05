package com.rtms.backend.analytics.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EdaStatisticsCalculatorTest {

    @Test
    @DisplayName("computeDistribution handles empty or null lists safely")
    void testComputeDistribution_EmptyOrNull() {
        var emptyStats = EdaStatisticsCalculator.computeDistribution(Collections.emptyList());
        assertEquals(0, emptyStats.sampleSize());
        assertEquals(0.0, emptyStats.mean(), 1e-6);
        assertEquals(0.0, emptyStats.stdDev(), 1e-6);
        assertTrue(emptyStats.outliers().isEmpty());

        var nullStats = EdaStatisticsCalculator.computeDistribution(null);
        assertEquals(0, nullStats.sampleSize());
    }

    @Test
    @DisplayName("computeDistribution handles single element")
    void testComputeDistribution_SingleElement() {
        var stats = EdaStatisticsCalculator.computeDistribution(List.of(45.5));
        assertEquals(1, stats.sampleSize());
        assertEquals(45.5, stats.min(), 1e-6);
        assertEquals(45.5, stats.max(), 1e-6);
        assertEquals(45.5, stats.mean(), 1e-6);
        assertEquals(45.5, stats.median(), 1e-6);
        assertEquals(0.0, stats.stdDev(), 1e-6);
        assertEquals(0.0, stats.iqr(), 1e-6);
        assertTrue(stats.outliers().isEmpty());
    }

    @Test
    @DisplayName("computeDistribution accurately calculates mean, median, IQR and Tukey outliers")
    void testComputeDistribution_StandardDataset() {
        // Dataset: 10, 12, 14, 15, 16, 18, 20, 22, 100 (100 is an obvious upper outlier)
        List<Double> values = Arrays.asList(10.0, 12.0, 14.0, 15.0, 16.0, 18.0, 20.0, 22.0, 100.0);
        var stats = EdaStatisticsCalculator.computeDistribution(values);

        assertEquals(9, stats.sampleSize());
        assertEquals(10.0, stats.min(), 1e-6);
        assertEquals(100.0, stats.max(), 1e-6);
        assertEquals(25.2222, stats.mean(), 1e-3);
        assertEquals(16.0, stats.median(), 1e-6);
        assertTrue(stats.iqr() > 0);
        assertEquals(1, stats.outliers().size());
        assertEquals(100.0, stats.outliers().get(0), 1e-6);
    }

    @Test
    @DisplayName("computeCorrelation handles empty, null, or mismatched lists")
    void testComputeCorrelation_EmptyOrMismatched() {
        var res1 = EdaStatisticsCalculator.computeCorrelation(null, List.of(1.0, 2.0));
        assertEquals("INSUFFICIENT_DATA", res1.strength());
        assertEquals(0.0, res1.pearsonR(), 1e-6);

        var res2 = EdaStatisticsCalculator.computeCorrelation(List.of(1.0), List.of(2.0));
        assertEquals("INSUFFICIENT_DATA", res2.strength());

        var res3 = EdaStatisticsCalculator.computeCorrelation(List.of(1.0, 2.0), List.of(3.0));
        assertEquals("INSUFFICIENT_DATA", res3.strength());
    }

    @Test
    @DisplayName("computeCorrelation handles zero variance without division by zero")
    void testComputeCorrelation_ZeroVariance() {
        List<Double> x = List.of(5.0, 5.0, 5.0, 5.0);
        List<Double> y = List.of(10.0, 12.0, 14.0, 16.0);
        var res = EdaStatisticsCalculator.computeCorrelation(x, y);

        assertEquals(0.0, res.pearsonR(), 1e-6);
        assertEquals("NO_VARIANCE", res.strength());
        assertEquals(0.0, res.slope(), 1e-6);
    }

    @Test
    @DisplayName("computeCorrelation accurately calculates positive Pearson r and regression line")
    void testComputeCorrelation_StrongPositive() {
        // Perfect linear relationship: y = 2x + 10
        List<Double> x = List.of(10.0, 20.0, 30.0, 40.0, 50.0);
        List<Double> y = List.of(30.0, 50.0, 70.0, 90.0, 110.0);
        var res = EdaStatisticsCalculator.computeCorrelation(x, y);

        assertEquals(1.0, res.pearsonR(), 1e-6);
        assertEquals("STRONG_POSITIVE", res.strength());
        assertEquals(2.0, res.slope(), 1e-6);
        assertEquals(10.0, res.intercept(), 1e-6);
        assertEquals(5, res.sampleSize());
    }

    @Test
    @DisplayName("computeCorrelation accurately identifies moderate negative correlation")
    void testComputeCorrelation_ModerateNegative() {
        List<Double> x = List.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0);
        List<Double> y = List.of(10.0, 8.5, 7.0, 6.0, 4.0, 4.5);
        var res = EdaStatisticsCalculator.computeCorrelation(x, y);

        assertTrue(res.pearsonR() < -0.6 && res.pearsonR() > -1.0);
        assertEquals("STRONG_NEGATIVE", res.strength());
        assertTrue(res.slope() < 0);
    }
}
