package com.rtms.backend.training.enums;

public enum WorkoutStatus {
    SCHEDULED,
    /** Buổi của nhóm đã diễn ra (theo đồng hồ) nhưng chưa xác nhận con ngựa có chạy. (The group's Session took place, by the clock; not yet confirmed.) */
    HELD,
    /** Đã ghi kết quả: bằng chứng con ngựa đã chạy. (Results recorded: the proof the horse took part.) */
    RAN,
    CANCELLED
}
