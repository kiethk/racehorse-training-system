package com.rtms.backend.training.dto;

/** Số Workout còn hiệu lực của một buổi. (The number of non-cancelled Workouts of a Session.) */
public record SessionCountRow(Long sessionId, Long count) {
}
