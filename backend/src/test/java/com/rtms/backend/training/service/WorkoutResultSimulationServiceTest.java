package com.rtms.backend.training.service;

import com.rtms.backend.training.dto.WorkoutResultPreviewResponse;
import com.rtms.backend.training.entity.Subject;
import com.rtms.backend.training.enums.IntensityLevel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkoutResultSimulationServiceTest {

    private final WorkoutResultSimulationService service = new WorkoutResultSimulationService();

    @Test
    void generatedHighIntensityMetricsStayPlausibleAndInternallyConsistent() {
        Subject subject = new Subject();
        subject.setTargetDistanceMeters(BigDecimal.valueOf(1_200));
        subject.setDurationMinutes(45);
        subject.setIntensityLevel(IntensityLevel.HIGH);

        for (int i = 0; i < 100; i++) {
            WorkoutResultPreviewResponse result = service.generate(subject);

            assertBetween(result.actualDistanceMeters(), 1_140.0, 1_261.2);
            assertBetween(result.actualDurationMinutes(), 41.4, 46.4);
            assertBetween(result.averageSpeedKmh(), 36.0, 50.1);
            assertTrue(result.topSpeedKmh().compareTo(result.averageSpeedKmh()) > 0);
            assertTrue(result.averageHeartRate() >= 170 && result.averageHeartRate() <= 195);
            assertTrue(result.maxHeartRate() > result.averageHeartRate());
            assertTrue(result.maxHeartRate() <= 220);
            assertTrue(result.recoveryHeartRate() < result.averageHeartRate());
        }
    }

    private void assertBetween(BigDecimal actual, double minimum, double maximum) {
        assertTrue(actual.compareTo(BigDecimal.valueOf(minimum)) >= 0,
                () -> actual + " should be at least " + minimum);
        assertTrue(actual.compareTo(BigDecimal.valueOf(maximum)) <= 0,
                () -> actual + " should be at most " + maximum);
    }
}
