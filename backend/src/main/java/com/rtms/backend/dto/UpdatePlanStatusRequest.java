package com.rtms.backend.dto;

public class UpdatePlanStatusRequest {
    private String status; // ACTIVE, COMPLETED, PAUSED, CANCELLED

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}