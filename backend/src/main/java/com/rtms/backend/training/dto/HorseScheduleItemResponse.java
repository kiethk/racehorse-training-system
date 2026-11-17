package com.rtms.backend.training.dto;

import com.rtms.backend.training.enums.WorkoutStatus;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Một buổi tập của một con ngựa trong lịch: buổi nào, giờ nào, và trạng thái Workout riêng của con ngựa đó (không bao giờ là CANCELLED).
 * (One Session of a horse in its schedule: which Session, when, and the status of this horse's own Workout, never CANCELLED.)
 */
public record HorseScheduleItemResponse(Long sessionId, LocalDate sessionDate, LocalTime startTime, LocalTime endTime,
                                        String exerciseName, WorkoutStatus workoutStatus) {
}
