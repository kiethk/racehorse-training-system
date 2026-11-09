package com.rtms.backend.training.repository.projection;

import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.SessionStatus;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Workout của một chiến mã trong một buổi đã diễn ra hoặc đã hủy, kèm ngày giờ, bài tập và nhóm của buổi đó.
 * (A horse's Workout in a Session that took place or was cancelled, with the Session's date, time, Exercise and group.)
 */
public record PastWorkoutRow(TrainingWorkout workout,
                             LocalDate sessionDate,
                             LocalTime startTime,
                             LocalTime endTime,
                             String exerciseName,
                             String groupName,
                             boolean groupDisbanded,
                             SessionStatus sessionStatus) {
}
