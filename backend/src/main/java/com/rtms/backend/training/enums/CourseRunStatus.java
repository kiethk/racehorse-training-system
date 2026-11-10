package com.rtms.backend.training.enums;

import java.util.List;

public enum CourseRunStatus {
    UPCOMING,
    ACTIVE,
    COMPLETED,
    CANCELLED;

    /** Course Run chưa đóng: còn chiếm khoảng ngày của nhóm. (Not closed yet: still holds the group's date range.) */
    public static final List<CourseRunStatus> UNFINISHED = List.of(UPCOMING, ACTIVE);
}
