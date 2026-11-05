package com.rtms.backend.training.service;

import com.rtms.backend.training.dto.PerformanceSessionRow;
import com.rtms.backend.training.dto.PerformanceWeekResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceTrendCalculatorTest {

    // Monday 05/10/2026 and the Monday after it
    private static final LocalDate WEEK_1 = LocalDate.of(2026, 10, 5);
    private static final LocalDate WEEK_2 = LocalDate.of(2026, 10, 12);

    private PerformanceSessionRow session(long horseId, LocalDate date, Integer rating, String speed, Integer maxHeartRate) {
        return new PerformanceSessionRow(horseId, date, rating, speed == null ? null : new BigDecimal(speed), maxHeartRate);
    }

    @Test
    @DisplayName("The week starts on Monday: Sunday belongs to the week before it")
    void weekStartIsMonday() {
        assertEquals(WEEK_1, PerformanceTrendCalculator.weekStartOf(LocalDate.of(2026, 10, 5)));
        assertEquals(WEEK_1, PerformanceTrendCalculator.weekStartOf(LocalDate.of(2026, 10, 11)), "Sunday");
        assertEquals(WEEK_2, PerformanceTrendCalculator.weekStartOf(LocalDate.of(2026, 10, 12)));
    }

    @Test
    @DisplayName("Each horse counts once per week: a horse with more sessions does not weigh more")
    void eachHorseCountsOncePerWeek() {
        // Horse 1 trains three times (9, 9, 9), horse 2 once (5). Per session the average would be 8.0, per horse it is 7.0.
        List<PerformanceWeekResponse> weeks = PerformanceTrendCalculator.byWeek(List.of(
                session(1, WEEK_1, 9, null, null),
                session(1, WEEK_1.plusDays(2), 9, null, null),
                session(1, WEEK_1.plusDays(4), 9, null, null),
                session(2, WEEK_1.plusDays(1), 5, null, null)));

        assertEquals(1, weeks.size());
        PerformanceWeekResponse week = weeks.get(0);
        assertEquals(WEEK_1, week.weekStart());
        assertEquals(2, week.horses());
        assertEquals(4, week.sessions());
        assertEquals(7.0, week.averageRating(), 0.001);
    }

    @Test
    @DisplayName("Weeks come in ascending order and only weeks with sessions are listed")
    void weeksAreAscendingAndSparse() {
        List<PerformanceWeekResponse> weeks = PerformanceTrendCalculator.byWeek(List.of(
                session(1, WEEK_2.plusDays(1), 8, null, null),
                session(1, WEEK_1.plusDays(3), 6, null, null),
                session(1, WEEK_2.plusDays(3), 10, null, null)));

        assertEquals(List.of(WEEK_1, WEEK_2), weeks.stream().map(PerformanceWeekResponse::weekStart).toList());
        assertEquals(6.0, weeks.get(0).averageRating(), 0.001);
        assertEquals(9.0, weeks.get(1).averageRating(), 0.001, "(8 + 10) / 2 for the one horse");
    }

    @Test
    @DisplayName("A session without a value is skipped for that value only")
    void missingValuesAreSkipped() {
        List<PerformanceWeekResponse> weeks = PerformanceTrendCalculator.byWeek(List.of(
                session(1, WEEK_1, 8, "50", 180),
                session(1, WEEK_1.plusDays(2), 8, null, null),     // no speed, no heart rate recorded
                session(2, WEEK_1.plusDays(1), 6, "40", null)));   // no heart rate recorded

        PerformanceWeekResponse week = weeks.get(0);
        assertEquals(45.0, week.averageSpeedKmh(), 0.001, "(50 + 40) / 2 horses");
        assertEquals(180.0, week.averageMaxHeartRate(), 0.001, "Only horse 1 has one");
        assertEquals(3, week.sessions());
    }

    @Test
    @DisplayName("A week where nobody recorded a value has null for it, and no session means no week")
    void nothingRecordedIsNull() {
        assertNull(PerformanceTrendCalculator.byWeek(List.of(session(1, WEEK_1, null, null, null))).get(0).averageRating());
        assertTrue(PerformanceTrendCalculator.byWeek(List.of()).isEmpty());
    }
}
