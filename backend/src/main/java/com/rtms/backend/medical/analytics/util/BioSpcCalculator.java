package com.rtms.backend.medical.analytics.util;

import com.rtms.backend.medical.analytics.dto.BioSpcSeriesResponse.BioSpcPointDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure algorithmic utility for Bio-Process Control (Shewhart Bio-SPC).
 * Computes sample mean, Bessel-corrected sample standard deviation,
 * control/warning limits, and applies Western Electric anomaly rules.
 */
public final class BioSpcCalculator {

    public static final double SPECIES_TEMP_MIN = 37.2;
    public static final double SPECIES_TEMP_MAX = 38.5;
    public static final double SPECIES_HR_MIN = 28.0;
    public static final double SPECIES_HR_MAX = 44.0;
    public static final int MIN_BASELINE_SAMPLES = 5;

    private BioSpcCalculator() {
    }

    public record RawPoint(Long id, LocalDateTime recordedAt, double value) {}

    public record SpcAnalysisResult(
            Double mean,
            Double stdDev,
            Double ucl,
            Double lcl,
            Double uwl,
            Double lwl,
            boolean baselineActive,
            int baselineSampleCount,
            List<BioSpcPointDto> analyzedPoints
    ) {}

    /**
     * Calculates sample mean \bar{X} = (1/n) \sum x_i.
     */
    public static double calculateMean(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.size();
    }

    /**
     * Calculates Bessel-corrected sample standard deviation:
     * s = sqrt( (1 / (n - 1)) * \sum (x_i - mean)^2 ).
     * Returns 0.0 if n < 2.
     */
    public static double calculateSampleStdDev(List<Double> values, double mean) {
        if (values == null || values.size() < 2) {
            return 0.0;
        }
        double sumSq = 0.0;
        for (double v : values) {
            double diff = v - mean;
            sumSq += diff * diff;
        }
        return Math.sqrt(sumSq / (values.size() - 1));
    }

    /**
     * Analyzes a time-series of raw biometric points for a specific metric.
     */
    public static SpcAnalysisResult analyzeSeries(
            List<RawPoint> rawPoints,
            String metricType
    ) {
        if (rawPoints == null || rawPoints.isEmpty()) {
            double specMin = isTemperature(metricType) ? SPECIES_TEMP_MIN : SPECIES_HR_MIN;
            double specMax = isTemperature(metricType) ? SPECIES_TEMP_MAX : SPECIES_HR_MAX;
            return new SpcAnalysisResult(null, null, specMax, specMin, null, null, false, 0, List.of());
        }

        double specMin = isTemperature(metricType) ? SPECIES_TEMP_MIN : SPECIES_HR_MIN;
        double specMax = isTemperature(metricType) ? SPECIES_TEMP_MAX : SPECIES_HR_MAX;
        // Filter healthy baseline readings within species physiological range
        List<RawPoint> healthyPoints = new ArrayList<>();
        for (RawPoint rp : rawPoints) {
            if (rp.value() >= specMin && rp.value() <= specMax) {
                healthyPoints.add(rp);
            }
        }

        int healthyCount = healthyPoints.size();

        if (healthyCount < MIN_BASELINE_SAMPLES) {
            // Baseline is still accumulating (healthy K < 5) -> Fallback to species safe limits
            List<BioSpcPointDto> points = new ArrayList<>();
            for (RawPoint rp : rawPoints) {
                boolean special = false;
                String desc = null;
                if (rp.value() > specMax) {
                    special = true;
                    desc = String.format("Exceeds species upper limit (%.1f)", specMax);
                } else if (rp.value() < specMin) {
                    special = true;
                    desc = String.format("Below species lower limit (%.1f)", specMin);
                }
                points.add(new BioSpcPointDto(rp.id(), rp.recordedAt(), rp.value(), special, desc));
            }

            List<Double> vals = rawPoints.stream().map(RawPoint::value).toList();
            double mean = calculateMean(vals);
            Double stdDev = rawPoints.size() >= 2 ? calculateSampleStdDev(vals, mean) : null;

            return new SpcAnalysisResult(
                    mean,
                    stdDev,
                    specMax,
                    specMin,
                    null,
                    null,
                    false,
                    healthyCount,
                    points
            );
        }

        // Baseline Active (healthy K >= 5) -> Compute personalized Shewhart limits from initial healthy baseline calibration
        List<Double> baselineValues = healthyPoints.subList(0, MIN_BASELINE_SAMPLES).stream()
                .map(RawPoint::value)
                .toList();
        double mean = calculateMean(baselineValues);
        double stdDev = calculateSampleStdDev(baselineValues, mean);

        // Avoid degenerate control limits if measurements have zero or near-zero variance
        // Temperature uses 0.05°C floor; Resting Heart Rate uses 1.5 bpm floor
        double minStdDev = isTemperature(metricType) ? 0.05 : 1.5;
        double effectiveStdDev = Math.max(stdDev, minStdDev);

        // Guard personalized limits against exceeding physiological species boundaries
        double ucl = Math.min(mean + 3.0 * effectiveStdDev, specMax);
        double lcl = Math.max(mean - 3.0 * effectiveStdDev, specMin);
        double uwl = Math.min(mean + 2.0 * effectiveStdDev, specMax);
        double lwl = Math.max(mean - 2.0 * effectiveStdDev, specMin);

        List<BioSpcPointDto> analyzedPoints = applyWesternElectricRules(rawPoints, mean, ucl, lcl, uwl, lwl);

        return new SpcAnalysisResult(
                round(mean, 2),
                round(stdDev, 2),
                round(ucl, 2),
                round(lcl, 2),
                round(uwl, 2),
                round(lwl, 2),
                true,
                healthyCount,
                analyzedPoints
        );
    }

    /**
     * Western Electric Special Cause Rules:
     * Rule 1: Point falls beyond 3-sigma limits (> UCL or < LCL) -> Severe Anomaly.
     * Rule 2: 2 out of 3 consecutive points fall beyond 2-sigma on the same side (> UWL or < LWL) -> Early Warning.
     */
    public static List<BioSpcPointDto> applyWesternElectricRules(
            List<RawPoint> points,
            double mean,
            double ucl,
            double lcl,
            double uwl,
            double lwl
    ) {
        List<BioSpcPointDto> result = new ArrayList<>(points.size());

        for (int i = 0; i < points.size(); i++) {
            RawPoint current = points.get(i);
            double v = current.value();

            // Rule 1: Beyond 3-sigma (UCL / LCL)
            if (v > ucl) {
                result.add(new BioSpcPointDto(
                        current.id(),
                        current.recordedAt(),
                        round(v, 2),
                        true,
                        String.format("Critical: Value (%.2f) exceeds upper safe threshold (%.2f)", v, ucl)
                ));
                continue;
            } else if (v < lcl) {
                result.add(new BioSpcPointDto(
                        current.id(),
                        current.recordedAt(),
                        round(v, 2),
                        true,
                        String.format("Critical: Value (%.2f) is below lower safe threshold (%.2f)", v, lcl)
                ));
                continue;
            }

            // Rule 2: 2 of 3 consecutive points beyond 2-sigma on the same side
            boolean rule2Upper = false;
            boolean rule2Lower = false;

            if (v > uwl) {
                // Current point is beyond upper 2-sigma limit.
                // Triggers Rule 2 if at least 1 of the prior 2 points was also beyond upper 2-sigma.
                boolean prev1Beyond = (i >= 1 && points.get(i - 1).value() > uwl);
                boolean prev2Beyond = (i >= 2 && points.get(i - 2).value() > uwl);
                if (prev1Beyond || prev2Beyond) {
                    rule2Upper = true;
                }
            } else if (v < lwl) {
                // Current point is beyond lower 2-sigma limit.
                // Triggers Rule 2 if at least 1 of the prior 2 points was also beyond lower 2-sigma.
                boolean prev1Beyond = (i >= 1 && points.get(i - 1).value() < lwl);
                boolean prev2Beyond = (i >= 2 && points.get(i - 2).value() < lwl);
                if (prev1Beyond || prev2Beyond) {
                    rule2Lower = true;
                }
            }

            if (rule2Upper) {
                result.add(new BioSpcPointDto(
                        current.id(),
                        current.recordedAt(),
                        round(v, 2),
                        true,
                        String.format("Warning: 2 of 3 consecutive readings above warning threshold (%.2f)", uwl)
                ));
            } else if (rule2Lower) {
                result.add(new BioSpcPointDto(
                        current.id(),
                        current.recordedAt(),
                        round(v, 2),
                        true,
                        String.format("Warning: 2 of 3 consecutive readings below warning threshold (%.2f)", lwl)
                ));
            } else {
                result.add(new BioSpcPointDto(
                        current.id(),
                        current.recordedAt(),
                        round(v, 2),
                        false,
                        null
                ));
            }
        }

        return result;
    }

    public static boolean isTemperature(String metricType) {
        return metricType == null || metricType.equalsIgnoreCase("TEMPERATURE") || metricType.equalsIgnoreCase("TEMP");
    }

    private static double round(double val, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(val * factor) / factor;
    }
}
