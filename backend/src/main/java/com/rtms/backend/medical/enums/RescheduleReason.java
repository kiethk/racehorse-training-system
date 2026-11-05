package com.rtms.backend.medical.enums;

public enum RescheduleReason {
    RACE_CONFLICT,
    UNDER_TREATMENT,
    OWNER_REQUEST,
    WEATHER,
    /** D1/WF-01 §3.2: ca INITIAL/ROUTINE chưa xong bị job 17:00 dời sang ngày làm việc kế. */
    EOD_ROLLOVER,
    /** V2: ca Routine bị dời ngày do bác sĩ phải ưu tiên xử lý ca cấp cứu khẩn cấp (không phạt KPI). */
    URGENT_PREEMPTION,
    /** V2: ca Routine/Admission bị dời sang ngày hôm sau do hết khung giờ khám mà chưa kịp hoàn tất. */
    TIME_EXCEEDED,
    /** V2: ca Routine được san sẻ sang bác sĩ thú y đồng nghiệp cùng ca. */
    PEER_OFFLOAD,
    OTHER
}
