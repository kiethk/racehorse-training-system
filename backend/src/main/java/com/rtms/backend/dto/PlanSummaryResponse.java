package com.rtms.backend.dto;
import com.rtms.backend.entity.HorseTrainingPlan;
import com.rtms.backend.enums.TrainingPlanStatus;

import java.time.LocalDate;

/**
 * Một dòng trong danh sách kế hoạch huấn luyện.
 *
 * VÌ SAO CẦN DTO RIÊNG thay vì trả thẳng entity HorseTrainingPlan:
 * dự án không dùng quan hệ JPA trên entity nghiệp vụ, nên entity chỉ có
 * horseId / courseId dạng số. Trả nguyên entity thì màn hình danh sách hiện
 * "Kế hoạch #27 · ngựa #15 · khoá #3" — không ai đọc được. Tên phải được
 * ghép ở tầng service, giống cách PlanWorkoutItemResponse đã mang subjectName.
 *
 * Ba con số tiến độ đi kèm luôn để danh sách khỏi phải gọi thêm API cho
 * từng dòng.
 */
public class PlanSummaryResponse {

    private Long planId;
    private Long horseId;
    private String horseName;
    private Long courseId;
    private String courseName;
    private LocalDate startDate;
    private LocalDate endDate;
    private TrainingPlanStatus status;

    /** Tổng số buổi đã sinh (bằng course.totalSessions lúc tạo). */
    private Integer totalSessions;
    private Integer completedSessions;

    /**
     * Buổi bị huỷ — bị TRỪ khỏi mẫu số khi tính tiến độ.
     * Khoá 12 buổi huỷ 1 sẽ đóng ở 11/11 (100%), không treo mãi ở 91%.
     */
    private Integer cancelledSessions;

    public PlanSummaryResponse(Long planId, Long horseId, String horseName,
                               Long courseId, String courseName,
                               LocalDate startDate, LocalDate endDate,
                               TrainingPlanStatus status,
                               Integer totalSessions, Integer completedSessions,
                               Integer cancelledSessions) {
        this.planId = planId;
        this.horseId = horseId;
        this.horseName = horseName;
        this.courseId = courseId;
        this.courseName = courseName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.totalSessions = totalSessions;
        this.completedSessions = completedSessions;
        this.cancelledSessions = cancelledSessions;
    }

    public Long getPlanId() { return planId; }
    public Long getHorseId() { return horseId; }
    public String getHorseName() { return horseName; }
    public Long getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public TrainingPlanStatus getStatus() { return status; }
    public Integer getTotalSessions() { return totalSessions; }
    public Integer getCompletedSessions() { return completedSessions; }
    public Integer getCancelledSessions() { return cancelledSessions; }
}
