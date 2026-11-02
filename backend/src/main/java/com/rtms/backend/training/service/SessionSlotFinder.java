package com.rtms.backend.training.service;

import com.rtms.backend.config.FarmSchedulePolicy;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/**
 * Xếp một buổi vào Golden Window của một ngày bằng first-fit. Toàn hàm thuần, không đụng DB, nên lập kế hoạch và xem trước dùng
 * CHUNG một cách tính, và viết unit test được mà không cần Spring.
 * (Places a Session into the Golden Window of one day with first-fit. All functions are pure with no DB access, so creating a plan
 * and previewing one share ONE calculation, unit-testable without Spring.)
 *
 * Vì sao quét khe ở giữa mà không "nối vào sau buổi cuối": lỗ trống ở giữa SẼ xuất hiện khi một buổi bị hủy; chỉ biết nối đuôi
 * thì hệ thống bỏ phí lỗ đó. First-fit không tối ưu nhưng TẤT ĐỊNH, O(n log n) và dễ giải thích.
 * (Why scan gaps instead of "append after the last Session": gaps WILL appear when a Session is cancelled; only appending would
 * waste them. First-fit is not optimal but is DETERMINISTIC, O(n log n) and easy to explain.)
 */
public final class SessionSlotFinder {

    /** Khoảng giờ đã bị một buổi khác của Trainer chiếm. (A time range already taken by another Session of the Trainer.) */
    public record Busy(LocalTime start, LocalTime end) {
    }

    private SessionSlotFinder() {
        // lớp tiện ích, không cho khởi tạo (utility class)
    }

    /**
     * Giờ sớm nhất một buổi của ngày này được bắt đầu: đầu Golden Window, còn hôm nay thì không sớm hơn lúc này (làm tròn LÊN mốc
     * 15 phút), vì không xếp buổi tập vào một giờ đã trôi qua. Sau khi Golden Window đóng thì trả về giờ đóng cửa, và không còn
     * khe nào.
     * (The earliest a Session of this day may start: the opening of the Golden Window, and for today no earlier than now (rounded UP
     * to the 15-minute step), since no workout is placed at a time that has passed. After the window closes it returns the closing
     * time, and no slot is left.)
     */
    public static LocalTime earliestStart(LocalDate date, LocalDateTime now) {
        if (!date.equals(now.toLocalDate())) {
            return FarmSchedulePolicy.GOLDEN_HOURS_START;
        }
        LocalTime time = now.toLocalTime();
        if (!time.isBefore(FarmSchedulePolicy.GOLDEN_HOURS_END)) {
            return FarmSchedulePolicy.GOLDEN_HOURS_END;
        }
        LocalTime rounded = FarmSchedulePolicy.roundUpToSessionStartStep(time);
        return rounded.isBefore(FarmSchedulePolicy.GOLDEN_HOURS_START) ? FarmSchedulePolicy.GOLDEN_HOURS_START : rounded;
    }

    /**
     * Giờ bắt đầu cho một buổi dài {@code durationMinutes} phút, không đè buổi nào, nằm trọn trong Golden Window và không sớm hơn
     * {@code notBefore}; null nếu không còn khe đủ rộng.
     * (A start time for a Session of {@code durationMinutes} minutes that overlaps nothing, lies fully inside the Golden Window and
     * is not earlier than {@code notBefore}; null if no slot is wide enough.)
     *
     * Vì sao phải sắp xếp trước: con trỏ chỉ tiến, không lùi; danh sách lộn xộn sẽ làm nó nhảy qua 08:30 rồi mới gặp buổi ở 07:00.
     * (Why sort first: the pointer only moves forward; an unsorted list would make it jump past 08:30 and only then meet a Session
     * at 07:00.)
     */
    public static LocalTime firstFreeSlot(List<Busy> busy, int durationMinutes, LocalTime notBefore) {
        LocalTime cursor = notBefore.isBefore(FarmSchedulePolicy.GOLDEN_HOURS_START)
                ? FarmSchedulePolicy.GOLDEN_HOURS_START : notBefore;

        List<Busy> sorted = busy.stream().sorted(Comparator.comparing(Busy::start)).toList();
        for (Busy taken : sorted) {
            long gap = Duration.between(cursor, taken.start()).toMinutes();
            if (gap >= durationMinutes) {
                return cursor;
            }
            if (taken.end().isAfter(cursor)) {
                // Nhảy qua buổi này, con trỏ KHÔNG lùi; một bài 40 phút kết thúc 06:40 thì buổi sau bắt đầu 06:45.
                // (Skip this Session, the pointer never moves back; a 40-minute Subject ending 06:40 puts the next at 06:45.)
                cursor = FarmSchedulePolicy.roundUpToSessionStartStep(taken.end());
            }
        }

        long tail = Duration.between(cursor, FarmSchedulePolicy.GOLDEN_HOURS_END).toMinutes();
        return tail >= durationMinutes ? cursor : null;
    }
}
