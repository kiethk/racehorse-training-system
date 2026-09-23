package com.rtms.backend.enums;

/**
 * Mức độ sẵn sàng THI ĐẤU (không phải sẵn sàng huấn luyện).
 * Dùng chung cho đánh giá lúc nhập học và đánh giá định kỳ.
 */
public enum RacingReadinessStatus {

    /** Đủ điều kiện đăng ký giải đua ngay. */
    READY,

    /** Chưa đủ, nhưng cứ theo lộ trình tập là sẽ đạt. */
    NEEDS_MORE_TRAINING,

    /**
     * Không phù hợp với đua — tập thêm cũng không giải quyết được
     * (dáng vóc lệch, tính nết không hợp, quá tuổi...).
     *
     * Đổi tên từ NOT_READY vì tên cũ không phân biệt được với
     * NEEDS_MORE_TRAINING. Đây là kênh DUY NHẤT để Trainer báo hiệu
     * "không nên nhận con này" — vì Trainer không có quyền reject đơn.
     */
    UNSUITABLE
}
