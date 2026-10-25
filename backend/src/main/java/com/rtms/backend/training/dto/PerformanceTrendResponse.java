package com.rtms.backend.training.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Biểu đồ phong độ chung trên dashboard. Khoảng ngày đi kèm để giao diện ghi đúng phạm vi và vẽ cả những tuần không có
 * buổi nào.
 * (The performance chart on the dashboard. The date range comes with it so the UI states the exact period and also
 * draws the weeks without any session.)
 *
 * @param fromDate thứ Hai của tuần đầu tiên (the Monday of the first week)
 * @param weeks    chỉ những tuần có buổi đã hoàn thành, tuần tăng dần (only weeks with completed sessions, ascending)
 */
public record PerformanceTrendResponse(LocalDate fromDate,
                                       LocalDate toDate,
                                       List<PerformanceWeekResponse> weeks) {
}
