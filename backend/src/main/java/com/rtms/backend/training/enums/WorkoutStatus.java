package com.rtms.backend.training.enums;

public enum WorkoutStatus {
    SCHEDULED,
    /** Mô hình cũ: đã ghi kết quả. Bỏ khi mô hình lot bị xóa. (Old model: result recorded. Removed with the lot model.) */
    COMPLETED,
    /** Buổi của nhóm đã diễn ra (theo đồng hồ), dù đã ghi kết quả hay chưa. (The group's Session took place, by the clock.) */
    HELD,
    CANCELLED
}
