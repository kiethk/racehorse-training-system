package com.rtms.backend.training.dto;

/** Một chiến mã trong chi tiết buổi: Workout của nó, Groom dắt và đã có kết quả chưa. (A horse in a Session detail: its Workout, its Groom and whether it has a result.) */
public record SessionHorseResponse(Long workoutId,
                                   Long horseId,
                                   String horseName,
                                   String groomName,
                                   String status,
                                   boolean hasResult,
                                   Integer performanceRating) {
}
