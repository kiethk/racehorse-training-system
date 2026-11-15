package com.rtms.backend.training.repository.projection;

import com.rtms.backend.training.enums.CourseRunStatus;

import java.time.LocalDate;

/**
 * Một Course Run đang chạy hoặc sắp chạy (ACTIVE / UPCOMING) của một nhóm, kèm tên khóa. Dùng để tìm ngày rảnh sớm nhất và
 * để hiển thị "nhóm đang bận vì khóa nào".
 * (A running or upcoming (ACTIVE / UPCOMING) Course Run of a group, with the course name. Used to find the earliest free date and to
 * show "which course keeps the group busy".)
 */
public record RunningCourseRunRow(Long courseRunId,
                                  String courseName,
                                  LocalDate startDate,
                                  LocalDate endDate,
                                  CourseRunStatus status) {

    public boolean overlaps(LocalDate otherStart, LocalDate otherEnd) {
        return !startDate.isAfter(otherEnd) && !endDate.isBefore(otherStart);
    }
}
