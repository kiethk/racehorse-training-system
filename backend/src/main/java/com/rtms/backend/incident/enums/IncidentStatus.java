package com.rtms.backend.incident.enums;

public enum IncidentStatus {
    /** Người báo cáo (Groom / Trainer) vừa gửi, chưa ai xem. */
    REPORTED,
    /** Thú y đã tiếp nhận, đang xử lý. */
    IN_REVIEW,
    /** Đã khám và xử lý xong (thường kèm bệnh án). */
    RESOLVED,
    /** Báo động giả — kiểm tra thực tế không có vấn đề. */
    DISMISSED
}
