package com.rtms.backend.training.repository.projection;

import com.rtms.backend.training.enums.CourseRunStatus;

import java.time.LocalDate;

/**
 * Một kế hoạch của Trainer kèm tên khóa, số buổi đã hoàn thành, số buổi bị hủy và số Workout còn chờ tập của chính con ngựa
 * (tính bằng SQL).
 * (A trainer's course run with course name, completed-session count, cancelled-session count and the horse's own Workouts still
 * to run, computed in SQL.)
 */
public record DashboardCourseRunRow(Long courseRunId,
                               Long horseId,
                               CourseRunStatus status,
                               LocalDate startDate,
                               LocalDate endDate,
                               String courseName,
                               Integer totalSessions,
                               Long completedSessions,
                               Long cancelledSessions,
                               Long scheduledWorkouts) {

    /**
     * Trạng thái của khóa đối với riêng con ngựa này: khóa còn UPCOMING/ACTIVE nhưng ngựa không còn Workout nào chờ tập (đã tập
     * xong phần của mình hoặc đã rời nhóm) thì coi như COMPLETED. Cùng luật với tab khóa học trong hồ sơ ngựa.
     * (The course run's status for THIS horse: an UPCOMING/ACTIVE course run where the horse has no Workout left to run, because it
     * finished its part or left the group, counts as COMPLETED. Same rule as the course runs tab of the horse profile.)
     */
    public DashboardCourseRunRow forHorse() {
        boolean open = status == CourseRunStatus.UPCOMING || status == CourseRunStatus.ACTIVE;
        if (!open || scheduledWorkouts > 0) {
            return this;
        }
        return new DashboardCourseRunRow(courseRunId, horseId, CourseRunStatus.COMPLETED, startDate, endDate, courseName,
                totalSessions, completedSessions, cancelledSessions, scheduledWorkouts);
    }
}
