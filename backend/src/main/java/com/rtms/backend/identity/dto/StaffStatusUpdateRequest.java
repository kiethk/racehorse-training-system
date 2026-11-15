package com.rtms.backend.identity.dto;

public class StaffStatusUpdateRequest {
    private boolean active;
    private Long replacementTrainerId;

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Long getReplacementTrainerId() { return replacementTrainerId; }
    public void setReplacementTrainerId(Long replacementTrainerId) { this.replacementTrainerId = replacementTrainerId; }
}
