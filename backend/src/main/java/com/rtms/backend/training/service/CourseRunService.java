package com.rtms.backend.training.service;

import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CourseRunDetailResponse;
import com.rtms.backend.training.dto.CourseRunPreviewResponse;
import com.rtms.backend.training.dto.CourseRunRequest;
import com.rtms.backend.training.dto.CourseRunSummaryResponse;
import com.rtms.backend.training.dto.CourseRunHorseResponse;

import java.util.List;

/**
 * Giao một khóa cho một Training Group và theo dõi các Course Run. Xem trước và tạo dùng CHUNG một phép tính lịch, nên giờ
 * Trainer thấy trên màn hình đúng là giờ được lưu.
 * (Assigns a course to a Training Group and tracks Course Runs. Preview and create share ONE schedule calculation, so the times
 * the Trainer saw on screen are the times stored.)
 */
public interface CourseRunService {

    /** Cho Trainer thấy hậu quả trước khi tạo; không ghi gì. (Shows the outcome before creating; writes nothing.) */
    CourseRunPreviewResponse previewCourseRun(CourseRunRequest request, AuthenticatedUser currentUser);

    /** Tạo kế hoạch, các buổi và Workout của mọi thành viên trong MỘT giao dịch. (Creates the course run, its Sessions and every member's Workouts in ONE transaction.) */
    CourseRunDetailResponse createCourseRun(CourseRunRequest request, AuthenticatedUser currentUser);

    /** Danh sách kế hoạch; Trainer thấy của mình, vai khác được xem thấy tất cả. (Course run list: a Trainer sees own, other viewing roles all.) */
    List<CourseRunSummaryResponse> getCourseRuns(Long groupId, Long horseId, AuthenticatedUser currentUser);

    CourseRunDetailResponse getCourseRun(Long courseRunId, AuthenticatedUser currentUser);

    /**
     * Mọi chiến mã từng có Workout trong kế hoạch, kèm Attendance; không phụ thuộc thành viên hiện tại của nhóm.
     * (Every horse that ever had a Workout in the course run, with Attendance; independent of the group's current members.)
     */
    List<CourseRunHorseResponse> getCourseRunHorses(Long courseRunId, AuthenticatedUser currentUser);

    /**
     * Bỏ khóa: mọi buổi CHƯA bắt đầu bị hủy và kế hoạch thành CANCELLED ngay, kể cả khi một buổi đang chạy. Buổi đang chạy vẫn chạy
     * hết, tự thành HELD và ghi kết quả được. Khoảng ngày của kế hoạch được giải phóng.
     * (Drops the course: every Session that has NOT started is cancelled and the course run becomes CANCELLED at once, even while a Session
     * is running. The running Session finishes, becomes HELD by itself and can have results recorded. The course run dates are released.)
     */
    CourseRunDetailResponse cancelCourseRun(Long courseRunId, AuthenticatedUser currentUser);
}
