package com.rtms.backend.training.dto;

/**
 * Một chiến mã trong chi tiết buổi: Workout của nó và Groom dắt. result là kết quả đã lưu (chỉ có khi RAN) để form sửa điền sẵn.
 * (A horse in a Session detail: its Workout and its Groom. result is the stored result, present only when RAN, so the edit form can be pre-filled.)
 */
public record SessionHorseResponse(Long workoutId,
                                   Long horseId,
                                   String horseName,
                                   String groomName,
                                   String status,
                                   Integer performanceRating,
                                   SessionWorkoutResponse result) {
}
