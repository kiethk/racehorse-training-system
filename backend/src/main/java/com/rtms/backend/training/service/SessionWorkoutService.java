package com.rtms.backend.training.service;

import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CompleteWorkoutRequest;
import com.rtms.backend.training.dto.SessionWorkoutResponse;
import com.rtms.backend.training.dto.WorkoutResultPreviewResponse;

/**
 * Ghi kết quả của Workout trong buổi của nhóm. Ghi kết quả chỉ điền số liệu và KHÔNG đổi trạng thái của Workout, Session hay kế
 * hoạch: trạng thái do đồng hồ quyết định.
 * (Recording the results of a Workout in a group Session. Recording only fills in the numbers and does NOT change the status of
 * the Workout, the Session or the plan: statuses are decided by the clock.)
 */
public interface SessionWorkoutService {

    /** Số đo mô phỏng mới, không lưu. Chỉ sau khi buổi đã hết giờ. (A fresh simulated measurement, not saved. Only after the Session ended.) */
    WorkoutResultPreviewResponse previewResult(Long workoutId, AuthenticatedUser currentUser);

    /** Ghi kết quả lần đầu cho một Workout đã HELD; làm được bất cứ lúc nào sau đó, không có hạn chót. (First recording on a HELD Workout, at any time afterwards, no deadline.) */
    SessionWorkoutResponse recordResult(Long workoutId, CompleteWorkoutRequest request, AuthenticatedUser currentUser);

    /** Sửa kết quả đã ghi. (Edits results already recorded.) */
    SessionWorkoutResponse updateResult(Long workoutId, CompleteWorkoutRequest request, AuthenticatedUser currentUser);

    /**
     * Đánh dấu "Không chạy": chiến mã thực ra không tham gia dù buổi đã diễn ra. Workout thành CANCELLED và không còn tính vào
     * Attendance. Chỉ khi chưa có kết quả.
     * (Marks "Did not run": the horse did not actually take part although the Session took place. The Workout becomes CANCELLED and
     * no longer counts in Attendance. Only while no result exists.)
     */
    SessionWorkoutResponse markDidNotRun(Long workoutId, AuthenticatedUser currentUser);
}
