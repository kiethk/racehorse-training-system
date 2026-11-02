package com.rtms.backend.training.service;

import com.rtms.backend.training.service.SessionSlotFinder.Busy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Quét khe giờ vàng của một ngày: first-fit, tất định, mốc 15 phút. Hàm thuần nên không cần Spring hay DB.
 * (Scans the Golden Window of a day: first-fit, deterministic, 15-minute steps. A pure function, so no Spring or DB.)
 */
class SessionSlotFinderTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 4);

    private static Busy busy(int fromHour, int fromMinute, int toHour, int toMinute) {
        return new Busy(LocalTime.of(fromHour, fromMinute), LocalTime.of(toHour, toMinute));
    }

    @Test
    void anEmptyDayStartsAtTheOpeningOfTheWindow() {
        assertEquals(LocalTime.of(6, 0), SessionSlotFinder.firstFreeSlot(List.of(), 60, LocalTime.of(6, 0)));
    }

    @Test
    void aSessionIsPlacedRightAfterTheOneBeforeIt() {
        assertEquals(LocalTime.of(7, 0),
                SessionSlotFinder.firstFreeSlot(List.of(busy(6, 0, 7, 0)), 45, LocalTime.of(6, 0)));
    }

    @Test
    void aSessionNeverStartsOffTheQuarterHour() {
        // A 40-minute session ending 06:40 puts the next one at 06:45.
        assertEquals(LocalTime.of(6, 45),
                SessionSlotFinder.firstFreeSlot(List.of(busy(6, 0, 6, 40)), 30, LocalTime.of(6, 0)));
    }

    @Test
    void aGapInTheMiddleIsUsedWhenItIsWideEnough() {
        List<Busy> day = List.of(busy(8, 0, 9, 0), busy(6, 0, 6, 30));

        assertEquals(LocalTime.of(6, 30), SessionSlotFinder.firstFreeSlot(day, 60, LocalTime.of(6, 0)));
    }

    @Test
    void aGapThatIsTooNarrowIsSkipped() {
        List<Busy> day = List.of(busy(6, 0, 6, 30), busy(7, 0, 8, 0));

        assertEquals(LocalTime.of(8, 0), SessionSlotFinder.firstFreeSlot(day, 45, LocalTime.of(6, 0)));
    }

    @Test
    void aDayWithNoRoomLeftReturnsNull() {
        // 150 busy minutes leave 90, which cannot hold 105.
        List<Busy> day = List.of(busy(6, 0, 7, 0), busy(7, 0, 8, 30));

        assertNull(SessionSlotFinder.firstFreeSlot(day, 105, LocalTime.of(6, 0)));
    }

    @Test
    void aSessionMayEndExactlyAtTheClosingOfTheWindow() {
        assertEquals(LocalTime.of(8, 0), SessionSlotFinder.firstFreeSlot(List.of(busy(6, 0, 8, 0)), 120, LocalTime.of(6, 0)));
    }

    @Test
    void theSearchNeverStartsBeforeTheGivenTime() {
        assertEquals(LocalTime.of(7, 15), SessionSlotFinder.firstFreeSlot(List.of(), 30, LocalTime.of(7, 15)));
    }

    @Test
    void anotherDayOpensAtSixInTheMorning() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 3, 9, 41);

        assertEquals(LocalTime.of(6, 0), SessionSlotFinder.earliestStart(DAY, now));
    }

    @Test
    void todayOpensAtTheNextQuarterHourFromNow() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 4, 7, 10);

        assertEquals(LocalTime.of(7, 15), SessionSlotFinder.earliestStart(DAY, now));
    }

    @Test
    void todayBeforeTheWindowOpensAtSix() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 4, 4, 30);

        assertEquals(LocalTime.of(6, 0), SessionSlotFinder.earliestStart(DAY, now));
    }

    @Test
    void todayAfterTheWindowHasNoRoom() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 4, 10, 5);
        LocalTime start = SessionSlotFinder.earliestStart(DAY, now);

        assertNull(SessionSlotFinder.firstFreeSlot(List.of(), 30, start));
    }
}
