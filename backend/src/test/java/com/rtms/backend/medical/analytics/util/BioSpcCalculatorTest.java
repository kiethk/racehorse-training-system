package com.rtms.backend.medical.analytics.util;

import com.rtms.backend.medical.analytics.dto.BioSpcSeriesResponse.BioSpcPointDto;
import com.rtms.backend.medical.analytics.util.BioSpcCalculator.RawPoint;
import com.rtms.backend.medical.analytics.util.BioSpcCalculator.SpcAnalysisResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BioSpcCalculatorTest {

    @Test
    @DisplayName("Sample mean and Bessel-corrected sample standard deviation")
    void testMeanAndSampleStdDev() {
        List<Double> values = List.of(37.5, 37.6, 37.4, 37.5, 37.8);
        double mean = BioSpcCalculator.calculateMean(values);
        // (37.5 + 37.6 + 37.4 + 37.5 + 37.8) / 5 = 187.8 / 5 = 37.56
        assertEquals(37.56, mean, 1e-4);

        double stdDev = BioSpcCalculator.calculateSampleStdDev(values, mean);
        // diffs: -0.06, 0.04, -0.16, -0.06, 0.24
        // squares: 0.0036 + 0.0016 + 0.0256 + 0.0036 + 0.0576 = 0.092
        // s = sqrt(0.092 / 4) = sqrt(0.023) ~= 0.1516575
        assertEquals(0.1517, stdDev, 1e-3);
    }

    @Test
    @DisplayName("When K < 5 samples, baseline is not active and uses species safe limits")
    void testAccumulatingBaselineUnder5Samples() {
        LocalDateTime now = LocalDateTime.now();
        List<RawPoint> raw = List.of(
                new RawPoint(1L, now.minusDays(4), 37.5),
                new RawPoint(2L, now.minusDays(3), 37.7),
                new RawPoint(3L, now.minusDays(2), 37.6),
                new RawPoint(4L, now.minusDays(1), 38.9) // > 38.5 species max
        );

        SpcAnalysisResult res = BioSpcCalculator.analyzeSeries(raw, "TEMPERATURE");

        assertFalse(res.baselineActive());
        assertEquals(BioSpcCalculator.SPECIES_TEMP_MAX, res.ucl());
        assertEquals(BioSpcCalculator.SPECIES_TEMP_MIN, res.lcl());
        assertNull(res.uwl());
        assertNull(res.lwl());

        assertEquals(4, res.analyzedPoints().size());
        assertFalse(res.analyzedPoints().get(0).isSpecialCause());
        assertFalse(res.analyzedPoints().get(1).isSpecialCause());
        assertFalse(res.analyzedPoints().get(2).isSpecialCause());
        // Point 4 is > 38.5 species max -> special cause
        assertTrue(res.analyzedPoints().get(3).isSpecialCause());
        assertTrue(res.analyzedPoints().get(3).anomalyDescription().contains("species"));
    }

    @Test
    @DisplayName("When K >= 5 samples, personalized baseline is activated with 3-sigma and 2-sigma limits")
    void testActiveBaselineShewhartLimits() {
        LocalDateTime now = LocalDateTime.now();
        List<RawPoint> raw = new ArrayList<>();
        // 5 baseline readings around 37.5 with s ~= 0.1
        raw.add(new RawPoint(1L, now.minusDays(6), 37.4));
        raw.add(new RawPoint(2L, now.minusDays(5), 37.5));
        raw.add(new RawPoint(3L, now.minusDays(4), 37.6));
        raw.add(new RawPoint(4L, now.minusDays(3), 37.5));
        raw.add(new RawPoint(5L, now.minusDays(2), 37.5));

        SpcAnalysisResult res = BioSpcCalculator.analyzeSeries(raw, "TEMPERATURE");

        assertTrue(res.baselineActive());
        assertNotNull(res.mean());
        assertNotNull(res.stdDev());
        assertNotNull(res.ucl());
        assertNotNull(res.lcl());
        assertNotNull(res.uwl());
        assertNotNull(res.lwl());

        assertEquals(37.5, res.mean(), 0.05);
        assertTrue(res.ucl() > res.uwl());
        assertTrue(res.uwl() > res.mean());
        assertTrue(res.mean() > res.lwl());
        assertTrue(res.lwl() > res.lcl());
    }

    @Test
    @DisplayName("Western Electric Rule 1: Point beyond 3-sigma is flagged as critical anomaly")
    void testWesternElectricRule1() {
        LocalDateTime now = LocalDateTime.now();
        List<RawPoint> raw = new ArrayList<>();
        raw.add(new RawPoint(1L, now.minusDays(5), 37.5));
        raw.add(new RawPoint(2L, now.minusDays(4), 37.5));
        raw.add(new RawPoint(3L, now.minusDays(3), 37.5));
        raw.add(new RawPoint(4L, now.minusDays(2), 37.5));
        raw.add(new RawPoint(5L, now.minusDays(1), 37.5));
        raw.add(new RawPoint(6L, now, 38.8)); // Far beyond UCL

        SpcAnalysisResult res = BioSpcCalculator.analyzeSeries(raw, "TEMPERATURE");
        BioSpcPointDto anomalyPoint = res.analyzedPoints().get(5);

        assertTrue(anomalyPoint.isSpecialCause());
        assertNotNull(anomalyPoint.anomalyDescription());
        assertTrue(anomalyPoint.anomalyDescription().contains("exceeds upper safe threshold"));
    }

    @Test
    @DisplayName("Western Electric Rule 2: 2 of 3 consecutive points beyond 2-sigma trigger warning")
    void testWesternElectricRule2() {
        LocalDateTime now = LocalDateTime.now();
        List<RawPoint> raw = new ArrayList<>();
        // Baseline established around 37.5, std dev ~ 0.1, uwl ~ 37.7, ucl ~ 37.8
        raw.add(new RawPoint(1L, now.minusDays(7), 37.4));
        raw.add(new RawPoint(2L, now.minusDays(6), 37.6));
        raw.add(new RawPoint(3L, now.minusDays(5), 37.5));
        raw.add(new RawPoint(4L, now.minusDays(4), 37.4));
        raw.add(new RawPoint(5L, now.minusDays(3), 37.6));
        // Point 6 is at 37.75 (> uwl, < ucl)
        raw.add(new RawPoint(6L, now.minusDays(2), 37.75));
        // Point 7 normal
        raw.add(new RawPoint(7L, now.minusDays(1), 37.5));
        // Point 8 is at 37.76 (> uwl, < ucl) -> 2 of last 3 are > uwl!
        raw.add(new RawPoint(8L, now, 37.76));

        SpcAnalysisResult res = BioSpcCalculator.analyzeSeries(raw, "TEMPERATURE");
        BioSpcPointDto pt8 = res.analyzedPoints().get(7);

        assertTrue(pt8.isSpecialCause());
        assertTrue(pt8.anomalyDescription().contains("2 of 3"));
    }

    @Test
    @DisplayName("Western Electric Rule 2: Normal point following 2 high points is NOT flagged")
    void testWesternElectricRule2_NormalPointNotFlagged() {
        LocalDateTime now = LocalDateTime.now();
        List<RawPoint> raw = new ArrayList<>();
        // 5 baseline readings around 37.5
        raw.add(new RawPoint(1L, now.minusDays(7), 37.4));
        raw.add(new RawPoint(2L, now.minusDays(6), 37.6));
        raw.add(new RawPoint(3L, now.minusDays(5), 37.5));
        raw.add(new RawPoint(4L, now.minusDays(4), 37.4));
        raw.add(new RawPoint(5L, now.minusDays(3), 37.6));
        // Point 6 and 7 beyond uwl (~37.7)
        raw.add(new RawPoint(6L, now.minusDays(2), 37.75));
        raw.add(new RawPoint(7L, now.minusDays(1), 37.75));
        // Point 8 returns to 37.51 (Normal, within 2-sigma)
        raw.add(new RawPoint(8L, now, 37.51));

        SpcAnalysisResult res = BioSpcCalculator.analyzeSeries(raw, "TEMPERATURE");
        BioSpcPointDto pt8 = res.analyzedPoints().get(7);

        assertFalse(pt8.isSpecialCause(), "Normal point must NOT be flagged as special cause");
        assertNull(pt8.anomalyDescription());
    }

    @Test
    @DisplayName("Resting Heart Rate uses 1.5 bpm floor so zero-variance baseline does not false-alarm normal readings")
    void testRestingHeartRateVarianceFloor() {
        LocalDateTime now = LocalDateTime.now();
        List<RawPoint> raw = new ArrayList<>();
        // 5 baseline readings identical at 32 bpm (sample std dev = 0)
        for (int i = 5; i >= 1; i--) {
            raw.add(new RawPoint((long) i, now.minusDays(i), 32.0));
        }
        // Reading 6 at 33 bpm (normal resting pulse)
        raw.add(new RawPoint(6L, now, 33.0));

        SpcAnalysisResult res = BioSpcCalculator.analyzeSeries(raw, "RESTING_HEART_RATE");

        assertTrue(res.baselineActive());
        assertEquals(32.0, res.mean());
        // Floor of 1.5 gives UCL = 32 + 3(1.5) = 36.5
        assertEquals(36.5, res.ucl());
        assertEquals(28.0, res.lcl()); // capped at SPECIES_HR_MIN

        BioSpcPointDto pt6 = res.analyzedPoints().get(5);
        assertFalse(pt6.isSpecialCause(), "33 bpm should not be an anomaly against a 32 bpm baseline");
    }

    @Test
    @DisplayName("Personalized control limits are safely capped by species boundaries")
    void testPersonalizedLimitsCappedAtSpeciesBounds() {
        LocalDateTime now = LocalDateTime.now();
        List<RawPoint> raw = new ArrayList<>();
        // Baseline readings around 38.3 -> mean ~ 38.32, 3*sigma ~ 0.25 -> 38.57 > 38.5
        raw.add(new RawPoint(1L, now.minusDays(5), 38.2));
        raw.add(new RawPoint(2L, now.minusDays(4), 38.4));
        raw.add(new RawPoint(3L, now.minusDays(3), 38.3));
        raw.add(new RawPoint(4L, now.minusDays(2), 38.4));
        raw.add(new RawPoint(5L, now.minusDays(1), 38.3));

        SpcAnalysisResult res = BioSpcCalculator.analyzeSeries(raw, "TEMPERATURE");

        assertTrue(res.baselineActive());
        // UCL must be capped at 38.5 (species max)
        assertEquals(BioSpcCalculator.SPECIES_TEMP_MAX, res.ucl());
    }

    @Test
    @DisplayName("Empty raw points handled safely")
    void testEmptyPoints() {
        SpcAnalysisResult res = BioSpcCalculator.analyzeSeries(List.of(), "TEMPERATURE");
        assertFalse(res.baselineActive());
        assertNull(res.mean());
        assertEquals(BioSpcCalculator.SPECIES_TEMP_MAX, res.ucl());
        assertEquals(BioSpcCalculator.SPECIES_TEMP_MIN, res.lcl());
        assertTrue(res.analyzedPoints().isEmpty());
    }
}
