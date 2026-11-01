package com.rtms.backend.training.dto;

import com.rtms.backend.training.enums.LotStatus;
import com.rtms.backend.training.enums.WorkoutStatus;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Một buổi tập của một con ngựa trong lịch: lot nào, giờ nào, và trạng thái của cả lot lẫn buổi của riêng con ngựa đó.
 * (One session of a horse in its schedule: which lot, when, and the status of the lot and of this horse own workout.)
 */
public record HorseScheduleItemResponse(Long lotId, LocalDate lotDate, LocalTime startTime, LocalTime endTime,
                                        String subjectName, LotStatus lotStatus, WorkoutStatus workoutStatus) {
}
