package com.rtms.backend.training.enums;

/**
 * Trạng thái một buổi ra sân của nhóm. HELD đến từ đồng hồ (hết giờ kết thúc), không phụ thuộc việc ghi kết quả.
 * (State of one outing of a group. HELD comes from the clock (the end time passed), never from results being recorded.)
 */
public enum SessionStatus {
    SCHEDULED,
    HELD,
    CANCELLED
}
