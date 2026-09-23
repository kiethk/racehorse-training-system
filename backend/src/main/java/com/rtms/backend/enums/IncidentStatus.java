package com.rtms.backend.enums;

public enum IncidentStatus {
    /** Groom vừa gửi, chưa ai xem. */
    REPORTED,
    /** Thú y đã tiếp nhận, đang xử lý. Groom nhìn thấy được là đã có người lo. */
    IN_REVIEW,
    /** Đã khám và xử lý xong (thường kèm bệnh án). */
    RESOLVED,
    /** Báo động giả — kiểm tra thực tế không có vấn đề. */
    DISMISSED
}
