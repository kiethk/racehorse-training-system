package com.rtms.backend.training.repository.projection;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Buổi tập đã hoàn thành gần đây — chỉ gồm các cột mà bộ luật cảnh báo cần.
 * (A recently completed workout — only the columns the alert rules need.)
 */
public record RecentWorkoutRow(Long horseId,
                               LocalDate sessionDate,
                               LocalTime sessionEndTime,
                               Integer maxHeartRate,
                               Integer averageHeartRate,
                               Integer recoveryHeartRate,
                               Integer performanceRating) {
}
