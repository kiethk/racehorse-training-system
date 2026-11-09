package com.rtms.backend.training.service;

import com.rtms.backend.training.repository.projection.RunningCourseRunRow;
import com.rtms.backend.training.enums.TrainingDay;
import com.rtms.backend.training.enums.CourseRunStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrainingCalendarTest {

    private static final Set<TrainingDay> MON_WED_FRI =
            Set.of(TrainingDay.MONDAY, TrainingDay.WEDNESDAY, TrainingDay.FRIDAY);

    private RunningCourseRunRow courseRun(long id, LocalDate start, LocalDate end) {
        return new RunningCourseRunRow(id, "Course " + id, start, end, CourseRunStatus.ACTIVE);
    }

    @Test
    @DisplayName("endDate: 6 sessions on Mon/Wed/Fri from Monday 05/10/2026 end on Friday 16/10")
    void endDate_monWedFri() {
        // Buổi 1-3: T2 05/10, T4 07/10, T6 09/10; buổi 4-6: T2 12/10, T4 14/10, T6 16/10
        // (Sessions 1-3: Mon 05/10, Wed 07/10, Fri 09/10; sessions 4-6: Mon 12/10, Wed 14/10, Fri 16/10)
        assertEquals(LocalDate.of(2026, 10, 16),
                TrainingCalendar.endDate(LocalDate.of(2026, 10, 5), MON_WED_FRI, 6));
    }

    @Test
    @DisplayName("firstSessionOnOrAfter: a Tuesday moves to Wednesday, a training day stays")
    void firstSessionOnOrAfter_movesToTrainingDay() {
        assertEquals(LocalDate.of(2026, 10, 7),
                TrainingCalendar.firstSessionOnOrAfter(LocalDate.of(2026, 10, 6), MON_WED_FRI));
        assertEquals(LocalDate.of(2026, 10, 5),
                TrainingCalendar.firstSessionOnOrAfter(LocalDate.of(2026, 10, 5), MON_WED_FRI));
    }

    @Test
    @DisplayName("earliestFreeStart: no busy course run keeps the chosen date")
    void earliestFreeStart_noBusyCourseRun() {
        assertEquals(LocalDate.of(2026, 11, 2),
                TrainingCalendar.earliestFreeStart(LocalDate.of(2026, 11, 2), MON_WED_FRI, 12, List.of()));
    }

    @Test
    @DisplayName("earliestFreeStart: skips a gap between two course runs that is too short for the course")
    void earliestFreeStart_skipsShortGap() {
        // Course run A 26/10-13/11 and course run B 23/11-11/12. Starting right after A (Mon 16/11) the 12 sessions run to
        // 04/12 and hit B, so the earliest free start is the first training day after B: Mon 14/12.
        List<RunningCourseRunRow> busy = List.of(
                courseRun(1, LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 13)),
                courseRun(2, LocalDate.of(2026, 11, 23), LocalDate.of(2026, 12, 11)));

        assertEquals(LocalDate.of(2026, 12, 14),
                TrainingCalendar.earliestFreeStart(LocalDate.of(2026, 11, 2), MON_WED_FRI, 12, busy));
    }

    @Test
    @DisplayName("earliestFreeStart: fits into a gap that is long enough")
    void earliestFreeStart_fitsLongGap() {
        // 3 sessions after A fit before B: Mon 16/11, Wed 18/11, Fri 20/11
        List<RunningCourseRunRow> busy = List.of(
                courseRun(1, LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 13)),
                courseRun(2, LocalDate.of(2026, 11, 23), LocalDate.of(2026, 12, 11)));

        assertEquals(LocalDate.of(2026, 11, 16),
                TrainingCalendar.earliestFreeStart(LocalDate.of(2026, 11, 2), MON_WED_FRI, 3, busy));
    }

    @Test
    @DisplayName("earliestFreeStart: a course run ending on the first session day still counts as a conflict")
    void earliestFreeStart_sameDayIsConflict() {
        List<RunningCourseRunRow> busy = List.of(courseRun(1, LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 2)));

        assertEquals(LocalDate.of(2026, 11, 4),
                TrainingCalendar.earliestFreeStart(LocalDate.of(2026, 11, 2), MON_WED_FRI, 3, busy));
    }
}
