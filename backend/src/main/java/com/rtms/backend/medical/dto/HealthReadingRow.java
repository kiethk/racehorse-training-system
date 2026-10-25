package com.rtms.backend.medical.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Một lần đo sức khỏe kèm tên ngựa, chỉ gồm các cột mà biểu đồ sức khỏe cần.
 * (One health reading with the horse name, only the columns the health chart needs.)
 */
public record HealthReadingRow(LocalDate date,
                               Long horseId,
                               String horseName,
                               BigDecimal temperature,
                               BigDecimal heartRate,
                               BigDecimal bodyCondition) {
}
