package com.rtms.backend.training.enums;

/**
 * Một chiến mã có ghi danh được vào kế hoạch đang xem trước không.
 * (Whether a horse can be enrolled in the plan being previewed.)
 */
public enum PlanAvailability {
    /** Rảnh trọn khóa. (Free for the whole course.) */
    AVAILABLE,
    /** Trùng một kế hoạch đang chạy hoặc sắp chạy; có ngày rảnh sớm nhất. (Overlaps a running or upcoming plan; has an earliest free date.) */
    BUSY,
    /** Thú y đang chặn tập. (Blocked from training by the vet.) */
    BLOCKED,
    /** Chuồng chưa có Groom, không ai dắt ngựa ra sân. (The stall has no groom, so nobody can lead the horse out.) */
    NO_GROOM
}
