package com.rtms.backend.training.repository.projection;

import com.rtms.backend.training.enums.CourseRunStatus;

import java.time.LocalDate;

/**
 * Một kế hoạch của Trainer kèm tên khóa, số buổi đã hoàn thành và số buổi bị hủy (tính bằng SQL).
 * (A trainer's course run with course name, completed-session count and cancelled-session count, computed in SQL.)
 */
public record DashboardCourseRunRow(Long courseRunId,
                               Long horseId,
                               CourseRunStatus status,
                               LocalDate startDate,
                               LocalDate endDate,
                               String courseName,
                               Integer totalSessions,
                               Long completedSessions,
                               Long cancelledSessions) {
}
