package com.rtms.backend.training.service;

import com.rtms.backend.training.dto.RunningPlanRow;
import com.rtms.backend.training.enums.TrainingDay;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Phép tính lịch của một khóa: buổi đầu rơi vào ngày nào, buổi cuối rơi vào ngày nào, và ngày sớm nhất một khóa
 * xếp vừa mà không chồng lên các kế hoạch đang có. Toàn hàm thuần, không đụng DB, nên lập kế hoạch và xem trước
 * kế hoạch dùng CHUNG một cách tính, và viết unit test được mà không cần Spring.
 * (Calendar arithmetic of a course: the date of the first session, the date of the last one, and the earliest date a
 * course fits without overlapping existing plans. All pure functions with no DB access, so creating a plan and
 * previewing one share ONE calculation, unit-testable without Spring.)
 */
public final class TrainingCalendar {

    private static final int DAYS_IN_WEEK = 7;

    private TrainingCalendar() {
    }

    public static boolean isTrainingDay(LocalDate date, Set<TrainingDay> trainingDays) {
        return trainingDays.contains(TrainingDay.valueOf(date.getDayOfWeek().name()));
    }

    /**
     * Mọi hàm bên dưới lặp tới khi gặp một ngày tập, nên tập thứ phải chứa ít nhất một thứ hợp lệ; nếu không (rỗng, hoặc chỉ
     * có null) vòng lặp không bao giờ dừng.
     * (Every function below loops until it meets a training day, so the set must hold at least one real weekday; otherwise
     * (empty, or only null) the loop never ends.)
     */
    public static Set<TrainingDay> requireWeekdays(Set<TrainingDay> trainingDays) {
        Set<TrainingDay> days = new java.util.LinkedHashSet<>();
        if (trainingDays != null) {
            days.addAll(trainingDays);
        }
        days.remove(null);
        if (days.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least 1 weekday must be selected for training (for example MONDAY, WEDNESDAY, FRIDAY)!");
        }
        return days;
    }

    /**
     * Ngày tập đầu tiên từ {@code date} trở đi. Ngày Trainer chọn có thể không phải ngày tập (chọn Thứ Ba cho khóa
     * T2-T4-T6), khi đó kế hoạch thật sự bắt đầu ở Thứ Tư; lưu đúng ngày đó để khoảng ngày của kế hoạch không bị
     * phình ra bằng những ngày không tập.
     * (The first training day on or after {@code date}. The chosen date may not be a training day (a Tuesday for a
     * Mon-Wed-Fri course); the plan then really starts on Wednesday, and that date is stored so the plan range is not
     * stretched by days without training.)
     *
     * @param trainingDays không rỗng, nên vòng lặp chạy tối đa 6 lần (not empty, so the loop runs at most 6 times)
     */
    public static LocalDate firstSessionOnOrAfter(LocalDate date, Set<TrainingDay> trainingDays) {
        LocalDate cursor = date;
        for (int attempt = 0; attempt < DAYS_IN_WEEK; attempt++) {
            if (isTrainingDay(cursor, trainingDays)) {
                return cursor;
            }
            cursor = cursor.plusDays(1);
        }
        throw new IllegalArgumentException("No weekday is selected for training!");
    }

    /**
     * Ngày diễn ra buổi tập cuối cùng khi khóa bắt đầu ở {@code startDate}.
     * (The date of the last workout when the course starts on {@code startDate}.)
     */
    public static LocalDate endDate(LocalDate startDate, Set<TrainingDay> trainingDays, int totalSessions) {
        LocalDate cursor = startDate;
        int count = 0;
        while (count < totalSessions) {
            if (isTrainingDay(cursor, trainingDays)) {
                count++;
            }
            if (count < totalSessions) {
                cursor = cursor.plusDays(1);
            }
        }
        return cursor;
    }

    /**
     * Ngày bắt đầu sớm nhất (từ {@code from} trở đi) để TRỌN khóa không chồng lên kế hoạch bận nào.
     * (The earliest start date, on or after {@code from}, at which the WHOLE course overlaps no busy plan.)
     *
     * VÌ SAO PHẢI LẶP: ngày kết thúc phụ thuộc ngày bắt đầu. Dời ra sau kế hoạch A thì khóa có thể vừa đủ dài để chạm
     * kế hoạch B, nên mỗi lần dời phải tính lại ngày kết thúc. Mỗi vòng nhảy qua ít nhất một khoảng bận, nên số vòng
     * không vượt quá số khoảng bận. Khoảng trống nào ngắn hơn khóa sẽ tự bị bỏ qua.
     * (WHY A LOOP: the end date depends on the start date. Moving past plan A can make the course just long enough to
     * reach plan B, so the end is recomputed after every move. Each round skips at least one busy range, so the number
     * of rounds never exceeds the number of busy ranges. Gaps shorter than the course are skipped by themselves.)
     */
    public static LocalDate earliestFreeStart(LocalDate from, Set<TrainingDay> trainingDays, int totalSessions,
                                              List<RunningPlanRow> busy) {
        LocalDate start = firstSessionOnOrAfter(from, trainingDays);
        while (true) {
            LocalDate end = endDate(start, trainingDays, totalSessions);
            LocalDate blockedUntil = null;
            for (RunningPlanRow plan : busy) {
                if (plan.overlaps(start, end) && (blockedUntil == null || plan.endDate().isAfter(blockedUntil))) {
                    blockedUntil = plan.endDate();
                }
            }
            if (blockedUntil == null) {
                return start;
            }
            start = firstSessionOnOrAfter(blockedUntil.plusDays(1), trainingDays);
        }
    }
}
