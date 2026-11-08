package com.rtms.backend.medical.analytics.util;

import com.rtms.backend.medical.analytics.dto.SurvivalEdaResponse.KaplanMeierStepDto;
import com.rtms.backend.medical.analytics.util.KaplanMeierCalculator.KmAnalysisResult;
import com.rtms.backend.medical.analytics.util.KaplanMeierCalculator.SurvivalObservation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KaplanMeierCalculatorTest {

    @Test
    @DisplayName("Kaplan-Meier product-limit estimator with right-censoring and median interpolation")
    void testStandardKaplanMeierCalculation() {
        // 5 subjects:
        // Day 6: recovered
        // Day 7: censored (still in treatment)
        // Day 10: recovered
        // Day 15: recovered
        // Day 19: censored
        List<SurvivalObservation> obs = List.of(
                new SurvivalObservation(6, true),
                new SurvivalObservation(7, false),
                new SurvivalObservation(10, true),
                new SurvivalObservation(15, true),
                new SurvivalObservation(19, false)
        );

        KmAnalysisResult result = KaplanMeierCalculator.calculate(obs);

        assertEquals(5, result.totalCases());
        assertEquals(3, result.recoveredCount());
        assertEquals(2, result.censoredCount());

        List<KaplanMeierStepDto> steps = result.steps();
        // Steps: 0, 6, 7, 10, 15, 19
        assertEquals(6, steps.size());

        // Step 0: t=0, S(0)=1.0, atRisk=5
        assertEquals(0, steps.get(0).days());
        assertEquals(1.0, steps.get(0).survivalProbability());
        assertEquals(5, steps.get(0).atRiskCount());

        // Step 1: t=6, d=1, n=5 -> S=0.8
        assertEquals(6, steps.get(1).days());
        assertEquals(0.8, steps.get(1).survivalProbability());
        assertEquals(5, steps.get(1).atRiskCount());
        assertEquals(1, steps.get(1).recoveredEventsCount());

        // Step 2: t=7, d=0 (censored), n=4 -> S=0.8
        assertEquals(7, steps.get(2).days());
        assertEquals(0.8, steps.get(2).survivalProbability());
        assertEquals(4, steps.get(2).atRiskCount());
        assertEquals(0, steps.get(2).recoveredEventsCount());

        // Step 3: t=10, d=1, n=3 -> S = 0.8 * (2/3) = 0.533
        assertEquals(10, steps.get(3).days());
        assertEquals(0.533, steps.get(3).survivalProbability());
        assertEquals(3, steps.get(3).atRiskCount());

        // Step 4: t=15, d=1, n=2 -> S = 0.533 * (1/2) = 0.267 <= 0.5
        assertEquals(15, steps.get(4).days());
        assertEquals(0.267, steps.get(4).survivalProbability());

        // Median crosses between 10 and 15
        assertNotNull(result.medianRecoveryDays());
        assertTrue(result.medianRecoveryDays() >= 10.0 && result.medianRecoveryDays() <= 15.0);
    }

    @Test
    @DisplayName("When all cases are right-censored, survival is 1.0 and median is null")
    void testAllCensored() {
        List<SurvivalObservation> obs = List.of(
                new SurvivalObservation(10, false),
                new SurvivalObservation(20, false),
                new SurvivalObservation(30, false)
        );

        KmAnalysisResult result = KaplanMeierCalculator.calculate(obs);

        assertEquals(3, result.totalCases());
        assertEquals(0, result.recoveredCount());
        assertEquals(3, result.censoredCount());
        assertNull(result.medianRecoveryDays());

        for (KaplanMeierStepDto s : result.steps()) {
            assertEquals(1.0, s.survivalProbability());
        }
    }

    @Test
    @DisplayName("Empty observations returns empty result safely")
    void testEmptyObservations() {
        KmAnalysisResult result = KaplanMeierCalculator.calculate(List.of());

        assertEquals(0, result.totalCases());
        assertEquals(0, result.recoveredCount());
        assertEquals(0, result.censoredCount());
        assertNull(result.medianRecoveryDays());
        assertTrue(result.steps().isEmpty());
    }
}
