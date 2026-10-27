package com.rtms.backend.training.dto;

import com.rtms.backend.training.enums.LotStatus;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Một lot còn hiệu lực kèm số ngựa đang có trong lot, đủ để tính xem một ngày còn chỗ hay không mà không tải từng workout.
 * (An active lot with the number of horses in it, enough to tell whether a day still has room without loading each workout.)
 */
public record LotLoadRow(LocalDate lotDate,
                         Long subjectId,
                         LotStatus status,
                         int maxCapacity,
                         long occupied,
                         LocalTime startTime,
                         LocalTime endTime) {
}
