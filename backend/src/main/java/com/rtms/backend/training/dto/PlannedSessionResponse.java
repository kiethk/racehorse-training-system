package com.rtms.backend.training.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** Một buổi sẽ được xếp; startTime rỗng khi ngày đó hết khe. (A Session about to be placed; startTime is null when the day has no slot.) */
public record PlannedSessionResponse(LocalDate date,
                                     String exerciseName,
                                     int durationMinutes,
                                     LocalTime startTime,
                                     LocalTime endTime) {
}
