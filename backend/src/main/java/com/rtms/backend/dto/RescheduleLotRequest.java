package com.rtms.backend.dto;

import java.time.LocalTime;

public class RescheduleLotRequest {

    /** Giờ bắt đầu mới. Giờ kết thúc hệ thống tự tính = start + subject.durationMinutes. */
    private LocalTime newStartTime;

    private String reason;

    public LocalTime getNewStartTime() { return newStartTime; }
    public void setNewStartTime(LocalTime newStartTime) { this.newStartTime = newStartTime; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
