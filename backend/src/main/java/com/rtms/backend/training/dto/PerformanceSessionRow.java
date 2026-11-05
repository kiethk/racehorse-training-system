package com.rtms.backend.training.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Một buổi tập đã hoàn thành, chỉ gồm các cột mà biểu đồ phong độ cần. Các chỉ số có thể null khi buổi chưa ghi đủ số đo.
 * (A completed session, only the columns the performance chart needs. The values can be null when the session has not
 * recorded every measurement.)
 */
public record PerformanceSessionRow(Long horseId,
                                    LocalDate date,
                                    Integer rating,
                                    BigDecimal averageSpeedKmh,
                                    Integer maxHeartRate) {
}
