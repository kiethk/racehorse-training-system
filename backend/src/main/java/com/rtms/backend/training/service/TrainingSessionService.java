package com.rtms.backend.training.service;

import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CourseRunSessionResponse;
import com.rtms.backend.training.dto.SessionWorkoutResponse;

import java.time.LocalTime;

/**
 * Các thao tác của Trainer trên một buổi: dời giờ, hủy buổi, hủy Workout của một chiến mã. Cả ba theo MỘT luật: chỉ làm được khi buổi
 * CHƯA bắt đầu. Buổi đã bắt đầu thì chạy đến hết, sau đó chỉ còn việc ghi kết quả.
 * (A Trainer's operations on a Session: move it, cancel it, cancel one horse's Workout. All three follow ONE rule: only before the
 * Session has STARTED. A Session that has started runs to its end, after which only recording results remains.)
 */
public interface TrainingSessionService {

    /** Dời giờ bắt đầu; giờ mới phải đúng mốc 15 phút, trong Golden Window và không chồng buổi nào khác của Trainer. (Moves the start; the new time must be on the 15-minute step, inside the Golden Window and overlap no other Session of the Trainer.) */
    CourseRunSessionResponse reschedule(Long sessionId, LocalTime newStartTime, AuthenticatedUser currentUser);

    /** Hủy cả buổi (mưa, sân đóng) cùng mọi Workout còn SCHEDULED của nó; không dạy bù. (Cancels the whole Session and its SCHEDULED Workouts; no make-up.) */
    CourseRunSessionResponse cancel(Long sessionId, AuthenticatedUser currentUser);

    /** Hủy Workout của riêng một chiến mã; con vẫn ở trong nhóm. Buổi không còn Workout nào thì tự hủy. (Cancels one horse's Workout; the horse stays in the group. A Session left with none cancels itself.) */
    SessionWorkoutResponse cancelWorkout(Long workoutId, AuthenticatedUser currentUser);
}
