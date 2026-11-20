package com.rtms.backend.analytics;

import com.rtms.backend.analytics.calculator.*;
import com.rtms.backend.analytics.dto.common.*;
import com.rtms.backend.analytics.dto.horse.*;
import com.rtms.backend.analytics.dto.medical.*;
import com.rtms.backend.analytics.dto.manager.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Analytics Architecture & Package Alignment Regression Test")
class AnalyticsArchitectureRegressionTest {

    @Test
    @DisplayName("Verify Common DTOs integrity in analytics.dto.common")
    void testCommonDtos() {
        var dist = EdaStatisticsCalculator.computeDistribution(List.of(10.0, 20.0, 30.0, 40.0, 50.0));
        EdaDistributionStats stats = EdaDistributionStats.from(dist);
        assertEquals(5, stats.sampleSize());
        assertEquals(30.0, stats.mean());

        var corrResult = EdaStatisticsCalculator.computeCorrelation(
                List.of(10.0, 20.0, 30.0),
                List.of(20.0, 40.0, 60.0)
        );
        EdaCorrelationSummary corr = EdaCorrelationSummary.from("speed", "heartRate", corrResult);
        assertEquals("speed", corr.xVariable());
        assertEquals("heartRate", corr.yVariable());
        assertEquals(1.0, corr.pearsonR(), 1e-4);
    }

    @Test
    @DisplayName("Verify Horse DTOs integrity in analytics.dto.horse")
    void testHorseDtos() {
        FleetHorseEdaPoint pt = new FleetHorseEdaPoint(
                1L, "Thunder", "Thoroughbred", "ST-01", "Trainer John",
                55.0, 120.0, 180.0, false, null
        );
        assertEquals(1L, pt.horseId());
        assertEquals("Thunder", pt.horseName());

        FleetWorkoutSummaryRow row = new FleetWorkoutSummaryRow(1L, 52.0, 155.0, 180);
        assertEquals(1L, row.horseId());
        assertEquals(52.0, row.avgSpeedKmh());

        IndividualWorkoutEdaPoint indPt = new IndividualWorkoutEdaPoint(
                101L, "2026-11-20", "Sprint Session", 55.5, 120.0, 180.0, 1200.0, false, null
        );
        assertEquals(101L, indPt.workoutId());
        assertEquals("Sprint Session", indPt.sessionName());
    }

    @Test
    @DisplayName("Verify Medical DTOs integrity in analytics.dto.medical")
    void testMedicalDtos() {
        BioSpcSeriesResponse.BioSpcPointDto pt = new BioSpcSeriesResponse.BioSpcPointDto(
                10L, LocalDateTime.now(), 38.0, false, null
        );
        BioSpcSeriesResponse spc = new BioSpcSeriesResponse(
                1L, "Thunder", "TEMPERATURE", 38.0, 0.5, 10, true,
                39.5, 36.5, 39.0, 37.0, 36.0, 40.0, List.of(pt)
        );
        assertEquals(1L, spc.horseId());
        assertTrue(spc.isBaselineActive());

        SurvivalEdaResponse.SurvivalStratumDto stratum = new SurvivalEdaResponse.SurvivalStratumDto(
                1L, "3-5yo", 10, 8, 2, 14.5, List.of()
        );
        SurvivalEdaResponse survival = new SurvivalEdaResponse(
                "AGE_GROUP", List.of(stratum)
        );
        assertEquals("AGE_GROUP", survival.stratifyBy());
    }

    @Test
    @DisplayName("Verify Calculator engines integration in analytics.calculator package")
    void testCalculatorEngines() {
        // EdaStatisticsCalculator
        List<Double> values = List.of(10.0, 20.0, 30.0, 40.0, 50.0);
        EdaStatisticsCalculator.DistributionStats dist = EdaStatisticsCalculator.computeDistribution(values);
        assertEquals(5, dist.sampleSize());
        assertEquals(30.0, dist.mean());

        // BioSpcCalculator
        LocalDateTime now = LocalDateTime.now();
        List<BioSpcCalculator.RawPoint> rawPoints = List.of(
                new BioSpcCalculator.RawPoint(1L, now.minusDays(1), 37.8),
                new BioSpcCalculator.RawPoint(2L, now, 38.0)
        );
        BioSpcCalculator.SpcAnalysisResult spcResult = BioSpcCalculator.analyzeSeries(rawPoints, "TEMPERATURE");
        assertNotNull(spcResult);
        assertEquals(2, spcResult.analyzedPoints().size());

        // KaplanMeierCalculator
        List<KaplanMeierCalculator.SurvivalObservation> obs = List.of(
                new KaplanMeierCalculator.SurvivalObservation(10, true),
                new KaplanMeierCalculator.SurvivalObservation(20, false)
        );
        KaplanMeierCalculator.KmAnalysisResult kmResult = KaplanMeierCalculator.calculate(obs);
        assertNotNull(kmResult);
        assertEquals(2, kmResult.totalCases());
        assertEquals(1, kmResult.recoveredCount());
        assertEquals(1, kmResult.censoredCount());
    }
}
