package com.rtms.backend.training.dto;

import com.rtms.backend.training.enums.SessionStatus;
import com.rtms.backend.training.enums.WorkoutStatus;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Một buổi tập của một con ngựa trong lịch: buổi nào, giờ nào, và trạng thái của cả buổi lẫn Workout riêng của con ngựa đó.
 * (One Session of a horse in its schedule: which Session, when, and the status of the Session and of this horse's own Workout.)
 */
public record HorseScheduleItemResponse(Long sessionId, LocalDate sessionDate, LocalTime startTime, LocalTime endTime,
                                        String exerciseName, SessionStatus sessionStatus, WorkoutStatus workoutStatus) {
}
