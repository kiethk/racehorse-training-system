package com.rtms.backend.training.service;

import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CompleteWorkoutRequest;
import com.rtms.backend.training.dto.SessionWorkoutResponse;
import com.rtms.backend.training.dto.WorkoutResultPreviewResponse;

/**
 * Xác nhận Workout của buổi đã diễn ra: ghi kết quả chuyển HELD thành RAN (bằng chứng con ngựa đã chạy), "Did not run" chuyển HELD
 * thành CANCELLED. Session và kế hoạch vẫn theo đồng hồ.
 * (Confirming the Workouts of a Session that took place: recording results turns HELD into RAN (the proof the horse took part),
 * "Did not run" turns HELD into CANCELLED. Sessions and course runs still follow the clock.)
 */
public interface SessionWorkoutService {

    /** Số đo mô phỏng mới, không lưu. Chỉ sau khi buổi đã hết giờ. (A fresh simulated measurement, not saved. Only after the Session ended.) */
    WorkoutResultPreviewResponse previewResult(Long workoutId, AuthenticatedUser currentUser);

    /** Ghi kết quả lần đầu cho một Workout HELD, chuyển nó thành RAN; không có hạn chót. (First recording on a HELD Workout, which becomes RAN; no deadline.) */
    SessionWorkoutResponse recordResult(Long workoutId, CompleteWorkoutRequest request, AuthenticatedUser currentUser);

    /** Sửa kết quả của Workout RAN. (Edits the results of a Workout that Ran.) */
    SessionWorkoutResponse updateResult(Long workoutId, CompleteWorkoutRequest request, AuthenticatedUser currentUser);

    /**
     * Đánh dấu "Không chạy": chiến mã thực ra không tham gia dù buổi đã diễn ra. Workout thành CANCELLED và không còn tính vào
     * Attendance. Chỉ khi còn HELD.
     * (Marks "Did not run": the horse did not actually take part although the Session took place. The Workout becomes CANCELLED and
     * no longer counts in Attendance. Only while still HELD.)
     */
    SessionWorkoutResponse markDidNotRun(Long workoutId, AuthenticatedUser currentUser);
}
