package com.rtms.backend.training.service;

import java.math.BigDecimal;

/**
 * Calculates a 1-10 performance rating based on IoT equine telemetry metrics.
 *
 * Grounded in Equine Exercise Physiology:
 * 1. Speed & Consistency (35%): Measures sustained speed relative to race benchmark (55 km/h)
 *    and speed preservation ratio (average speed / top speed).
 * 2. Heart Rate Recovery (40%): Evaluates cardiovascular fitness via the recovery drop ratio
 *    ((maxHeartRate - recoveryHeartRate) / maxHeartRate).
 * 3. Work Efficiency & Pacing (25%): Measures cardiac economy (average speed / average heart rate)
 *    and continuity density (calculated pace / average speed).
 */
public final class WorkoutPerformanceCalculator {

    private WorkoutPerformanceCalculator() {
    }

    public static int calculate(
            BigDecimal actualDistanceMeters,
            BigDecimal actualDurationMinutes,
            BigDecimal topSpeedKmh,
            BigDecimal averageSpeedKmh,
            Integer averageHeartRate,
            Integer maxHeartRate,
            Integer recoveryHeartRate) {

        if (actualDistanceMeters == null || actualDurationMinutes == null
                || topSpeedKmh == null || averageSpeedKmh == null
                || averageHeartRate == null || maxHeartRate == null || recoveryHeartRate == null) {
            return 8;
        }

        double d = actualDistanceMeters.doubleValue();
        double t = actualDurationMinutes.doubleValue();
        double vTop = Math.max(0.1, topSpeedKmh.doubleValue());
        double vAvg = Math.max(0.1, averageSpeedKmh.doubleValue());
        double hrAvg = Math.max(30.0, averageHeartRate.doubleValue());
        double hrMax = Math.max(hrAvg, maxHeartRate.doubleValue());
        double hrRec = Math.max(30.0, recoveryHeartRate.doubleValue());

        // --- 1. Speed & Consistency (Weight: 35%) ---
        // sSpeed: Average speed normalized against standard 55 km/h race-training benchmark (max 10)
        double sSpeed = Math.min(10.0, (vAvg / 55.0) * 10.0);
        // sConsistency: Ratio of average speed to peak speed (max 1.0 -> 10)
        double sConsistency = Math.min(10.0, (vAvg / vTop) * 10.0);
        double pSpeed = 0.60 * sSpeed + 0.40 * sConsistency;

        // --- 2. Heart Rate Recovery (Weight: 40%) ---
        // rRec: Recovery drop ratio. Benchmark: >= 55% recovery = 10 pts, <= 20% recovery = 1 pt.
        double rRec = (hrMax - hrRec) / hrMax;
        double pCardio = 1.0 + ((rRec - 0.20) / 0.35) * 9.0;
        pCardio = Math.max(1.0, Math.min(10.0, pCardio));

        // --- 3. Work Efficiency & Pacing (Weight: 25%) ---
        // Cardiac economy: (vAvg / hrAvg) * 100. Benchmark 28.0 = 10 pts
        double economy = (vAvg / hrAvg) * 100.0;
        double sEconomy = Math.max(1.0, Math.min(10.0, (economy / 28.0) * 10.0));

        // Pace density: vCalc = (d / 1000) / (t / 60)
        double vCalc = t > 0 ? (d / 1000.0) / (t / 60.0) : vAvg;
        double sDensity = Math.min(10.0, Math.max(1.0, (vCalc / vAvg) * 10.0));
        double pEfficiency = 0.60 * sEconomy + 0.40 * sDensity;

        // Composite Performance Score
        double composite = 0.35 * pSpeed + 0.40 * pCardio + 0.25 * pEfficiency;
        long rounded = Math.round(composite);
        return (int) Math.max(1, Math.min(10, rounded));
    }
}
