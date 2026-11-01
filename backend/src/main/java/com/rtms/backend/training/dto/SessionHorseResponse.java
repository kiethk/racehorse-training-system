package com.rtms.backend.training.dto;

/**
 * Một chiến mã trong chi tiết buổi: Workout của nó, Groom dắt và đã có kết quả chưa. result là kết quả đã lưu (null khi chưa ghi) để form sửa điền sẵn.
 * (A horse in a Session detail: its Workout, its Groom and whether it has a result. result is the stored result, null when none, so the edit form can be pre-filled.)
 */
public record SessionHorseResponse(Long workoutId,
                                   Long horseId,
                                   String horseName,
                                   String groomName,
                                   String status,
                                   boolean hasResult,
                                   Integer performanceRating,
                                   SessionWorkoutResponse result) {
}
