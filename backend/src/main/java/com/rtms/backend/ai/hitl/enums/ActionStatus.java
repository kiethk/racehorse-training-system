package com.rtms.backend.ai.hitl.enums;

/**
 * Trạng thái của một hành động AI chờ xác nhận (HITL).
 */
public enum ActionStatus {
    PENDING,    // Đang chờ con người xác nhận
    COMMITTED,  // Đã được xác nhận và thực thi vào database
    EXPIRED,    // Quá thời hạn 5 phút (hết hiệu lực)
    REJECTED    // Người dùng từ chối thực hiện đề xuất
}
