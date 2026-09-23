package com.rtms.backend.enums;

public enum TaskSource {
    /** Từ bảng groom_daily_tasks — việc thường nhật do SOP Generator sinh. */
    SOP,
    /** Từ training_workouts JOIN training_lots — buổi tập Trainer gán cho Groom. */
    WORKOUT,
    /** Từ preventive_care_schedules — lịch thú y định kỳ của ngựa trong chuồng mình. */
    PREVENTIVE_CARE
}
