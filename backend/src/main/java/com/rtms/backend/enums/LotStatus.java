package com.rtms.backend.enums;

public enum LotStatus {
    SCHEDULED,     // đã xếp lịch, chưa diễn ra
    COMPLETED,     // đã hoàn thành
    CANCELLED      // đã huỷ — KHÔNG còn chiếm khe giờ, thuật toán xếp khe bỏ qua
}
