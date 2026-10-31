package com.rtms.backend.training.enums;

public enum WorkoutStatus {
    SCHEDULED,
    /** Buổi của nhóm đã diễn ra (theo đồng hồ), dù đã ghi kết quả hay chưa. (The group's Session took place, by the clock.) */
    HELD,
    CANCELLED
}
