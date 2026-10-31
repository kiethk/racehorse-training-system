package com.rtms.backend.training.service;

import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.GroupPlanDetailResponse;
import com.rtms.backend.training.dto.GroupPlanPreviewResponse;
import com.rtms.backend.training.dto.GroupPlanRequest;
import com.rtms.backend.training.dto.GroupPlanSummaryResponse;

import java.util.List;

/**
 * Giao một khóa cho một Training Group và theo dõi các Group Plan. Xem trước và tạo dùng CHUNG một phép tính lịch, nên giờ
 * Trainer thấy trên màn hình đúng là giờ được lưu.
 * (Assigns a course to a Training Group and tracks Group Plans. Preview and create share ONE schedule calculation, so the times
 * the Trainer saw on screen are the times stored.)
 */
public interface GroupPlanService {

    /** Cho Trainer thấy hậu quả trước khi tạo; không ghi gì. (Shows the outcome before creating; writes nothing.) */
    GroupPlanPreviewResponse previewPlan(GroupPlanRequest request, AuthenticatedUser currentUser);

    /** Tạo kế hoạch, các buổi và Workout của mọi thành viên trong MỘT giao dịch. (Creates the plan, its Sessions and every member's Workouts in ONE transaction.) */
    GroupPlanDetailResponse createPlan(GroupPlanRequest request, AuthenticatedUser currentUser);

    /** Danh sách kế hoạch; Trainer thấy của mình, vai khác được xem thấy tất cả. (Plan list: a Trainer sees own, other viewing roles all.) */
    List<GroupPlanSummaryResponse> getPlans(Long groupId, AuthenticatedUser currentUser);

    GroupPlanDetailResponse getPlan(Long planId, AuthenticatedUser currentUser);

    /**
     * Bỏ khóa: mọi buổi CHƯA bắt đầu bị hủy và kế hoạch thành CANCELLED ngay, kể cả khi một buổi đang chạy. Buổi đang chạy vẫn chạy
     * hết, tự thành HELD và ghi kết quả được. Khoảng ngày của kế hoạch được giải phóng.
     * (Drops the course: every Session that has NOT started is cancelled and the plan becomes CANCELLED at once, even while a Session
     * is running. The running Session finishes, becomes HELD by itself and can have results recorded. The plan dates are released.)
     */
    GroupPlanDetailResponse cancelPlan(Long planId, AuthenticatedUser currentUser);
}
