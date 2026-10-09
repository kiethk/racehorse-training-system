package com.rtms.backend.medical.dto;

import jakarta.validation.constraints.NotBlank;

public class CancelCareScheduleRequest {

    @NotBlank(message = "Cancel reason is required")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}