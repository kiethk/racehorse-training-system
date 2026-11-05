package com.rtms.backend.training.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkoutPerformanceCalculatorTest {

    @Test
    void testExcellentPerformanceScenario() {
        int rating = WorkoutPerformanceCalculator.calculate(
                BigDecimal.valueOf(1200),
                BigDecimal.valueOf(1.6),
                BigDecimal.valueOf(52.0),
                BigDecimal.valueOf(45.0),
                158,
                192,
                82);
        assertTrue(rating >= 8 && rating <= 10, "Expected excellent rating (8-10), got: " + rating);
    }

    @Test
    void testStandardOnTargetPerformanceScenario() {
        int rating = WorkoutPerformanceCalculator.calculate(
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(2.0),
                BigDecimal.valueOf(38.0),
                BigDecimal.valueOf(30.0),
                155,
                180,
                105);
        assertTrue(rating >= 6 && rating <= 8, "Expected on-target rating (6-8), got: " + rating);
    }

    @Test
    void testPoorPerformanceScenario() {
        int rating = WorkoutPerformanceCalculator.calculate(
                BigDecimal.valueOf(800),
                BigDecimal.valueOf(2.5),
                BigDecimal.valueOf(32.0),
                BigDecimal.valueOf(20.0),
                170,
                190,
                145);
        assertTrue(rating <= 5, "Expected poor rating (<=5), got: " + rating);
    }

    @Test
    void testNullSafeFallback() {
        int rating = WorkoutPerformanceCalculator.calculate(
                null, null, null, null, null, null, null);
        assertEquals(8, rating);
    }
}
