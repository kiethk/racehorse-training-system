package com.rtms.backend.training.service;

import com.rtms.backend.training.dto.PerformanceSessionRow;
import com.rtms.backend.training.dto.PerformanceWeekResponse;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Gom các buổi tập đã hoàn thành thành từng tuần cho biểu đồ phong độ chung. Hàm thuần, không đụng DB.
 *
 * VÌ SAO MỖI CON MỘT LẦN MỖI TUẦN: mỗi ngày tập những con khác nhau, nên trung bình theo ngày phản ánh "hôm đó con nào
 * tập" nhiều hơn là phong độ của đàn. Trong một tuần hầu như con nào có kế hoạch cũng tập, và lấy trung bình của từng con
 * trước rồi mới lấy trung bình giữa các con thì con tập nhiều buổi không có trọng số lớn hơn.
 * (Pure functions, no DB. WHY ONE VALUE PER HORSE PER WEEK: each day trains different horses, so a daily average says
 * more about "which horses trained that day" than about the herd form. Within a week almost every horse with a plan
 * trains, and averaging each horse first and then the horses means a horse with more sessions does not weigh more.)
 */
final class PerformanceTrendCalculator {

    private PerformanceTrendCalculator() {
    }

    /** Thứ Hai của tuần chứa ngày này. (The Monday of the week containing the date.) */
    static LocalDate weekStartOf(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    static List<PerformanceWeekResponse> byWeek(List<PerformanceSessionRow> sessions) {
        Map<LocalDate, List<PerformanceSessionRow>> sessionsByWeek = new TreeMap<>(
                sessions.stream().collect(Collectors.groupingBy(session -> weekStartOf(session.date()))));

        List<PerformanceWeekResponse> weeks = new ArrayList<>();
        sessionsByWeek.forEach((weekStart, weekSessions) -> {
            Map<Long, List<PerformanceSessionRow>> byHorse =
                    weekSessions.stream().collect(Collectors.groupingBy(PerformanceSessionRow::horseId));
            weeks.add(new PerformanceWeekResponse(
                    weekStart,
                    byHorse.size(),
                    weekSessions.size(),
                    teamAverage(byHorse, session -> session.rating() == null ? null : session.rating().doubleValue()),
                    teamAverage(byHorse, session -> session.averageSpeedKmh() == null ? null : session.averageSpeedKmh().doubleValue()),
                    teamAverage(byHorse, session -> session.maxHeartRate() == null ? null : session.maxHeartRate().doubleValue())));
        });
        return weeks;
    }

    /**
     * Trung bình giữa các con của trung bình từng con. Con không có số đo nào cho chỉ số này thì bị bỏ qua; không con nào có
     * thì null.
     * (The average over the horses of each horse average. A horse with no measurement of this value is skipped; null
     * when no horse has one.)
     */
    private static Double teamAverage(Map<Long, List<PerformanceSessionRow>> byHorse,
                                      Function<PerformanceSessionRow, Double> value) {
        List<Double> horseAverages = new ArrayList<>();
        for (List<PerformanceSessionRow> horseSessions : byHorse.values()) {
            OptionalDouble average = horseSessions.stream()
                    .map(value)
                    .filter(v -> v != null)
                    .mapToDouble(Double::doubleValue)
                    .average();
            average.ifPresent(horseAverages::add);
        }
        return horseAverages.isEmpty() ? null : horseAverages.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
    }
}
