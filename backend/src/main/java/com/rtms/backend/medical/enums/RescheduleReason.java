package com.rtms.backend.medical.enums;

public enum RescheduleReason {
    RACE_CONFLICT,
    UNDER_TREATMENT,
    OWNER_REQUEST,
    WEATHER,
    /** D1/WF-01 §3.2: ca INITIAL/ROUTINE chưa xong bị job 17:00 dời sang ngày làm việc kế. */
    EOD_ROLLOVER,
    OTHER
}
