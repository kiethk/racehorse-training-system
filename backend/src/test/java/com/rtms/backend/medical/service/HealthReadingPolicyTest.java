package com.rtms.backend.medical.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthReadingPolicyTest {

    private static BigDecimal n(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static List<String> issues(String temperature, String heartRate, String bodyCondition) {
        return HealthReadingPolicy.issuesOf(n(temperature), n(heartRate), n(bodyCondition));
    }

    @Test
    @DisplayName("A reading inside every range has no issue, and the limits themselves are normal")
    void limitsAreNormal() {
        assertTrue(issues("38.0", "40", "6.0").isEmpty());
        assertTrue(issues("37.2", "28", "4.0").isEmpty(), "Lower limits");
        assertTrue(issues("38.5", "44", "7.0").isEmpty(), "Upper limits");
    }

    @Test
    @DisplayName("Each value outside its range gives one ready-to-show sentence")
    void outsideTheRangeIsReported() {
        assertEquals(List.of("Temperature 38.6 °C"), issues("38.6", "40", "6.0"));
        assertEquals(List.of("Temperature 37.0 °C"), issues("37.0", "40", "6.0"));
        assertEquals(List.of("Heart rate 46 bpm"), issues("38.0", "46", "6.0"));
        assertEquals(List.of("Body condition 3.5"), issues("38.0", "40", "3.5"));
        assertEquals(List.of("Temperature 39.2 °C", "Heart rate 60 bpm", "Body condition 8.0"), issues("39.2", "60", "8.0"));
    }

    @Test
    @DisplayName("A value that was not measured is skipped, not reported")
    void missingValuesAreSkipped() {
        assertTrue(issues(null, null, null).isEmpty());
        assertEquals(List.of("Heart rate 50 bpm"), issues(null, "50", null));
    }
}
