package com.rtms.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response returned after creating a staff member.
 * Extends StaffSummaryResponse with auto-assignment result fields
 * so the frontend can display what was assigned on creation.
 */
public class StaffCreationResponse {

    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String role;
    private boolean active;
    private LocalDateTime createdAt;
    private String profileSummary;

    // HEAD_TRAINER auto-assignment result
    private List<Long>   assignedAreaIds;
    private List<String> assignedAreaCodes;

    // GROOM auto-assignment result
    private Long   trainerId;
    private String trainerName;
    private Long   assignedAreaId;
    private String assignedAreaCode;
    private List<Long>   assignedStallIds;
    private List<String> assignedStallCodes;

    /**
     * true  = no stall block was available, Groom still created successfully.
     * null  = not applicable (not a Groom creation, or block was found).
     */
    private Boolean noStallBlockAvailable;

    // ── Accessors ────────────────────────────────────────────────────────────

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getProfileSummary() { return profileSummary; }
    public void setProfileSummary(String profileSummary) { this.profileSummary = profileSummary; }

    public List<Long> getAssignedAreaIds() { return assignedAreaIds; }
    public void setAssignedAreaIds(List<Long> assignedAreaIds) { this.assignedAreaIds = assignedAreaIds; }

    public List<String> getAssignedAreaCodes() { return assignedAreaCodes; }
    public void setAssignedAreaCodes(List<String> assignedAreaCodes) { this.assignedAreaCodes = assignedAreaCodes; }

    public Long getTrainerId() { return trainerId; }
    public void setTrainerId(Long trainerId) { this.trainerId = trainerId; }

    public String getTrainerName() { return trainerName; }
    public void setTrainerName(String trainerName) { this.trainerName = trainerName; }

    public Long getAssignedAreaId() { return assignedAreaId; }
    public void setAssignedAreaId(Long assignedAreaId) { this.assignedAreaId = assignedAreaId; }

    public String getAssignedAreaCode() { return assignedAreaCode; }
    public void setAssignedAreaCode(String assignedAreaCode) { this.assignedAreaCode = assignedAreaCode; }

    public List<Long> getAssignedStallIds() { return assignedStallIds; }
    public void setAssignedStallIds(List<Long> assignedStallIds) { this.assignedStallIds = assignedStallIds; }

    public List<String> getAssignedStallCodes() { return assignedStallCodes; }
    public void setAssignedStallCodes(List<String> assignedStallCodes) { this.assignedStallCodes = assignedStallCodes; }

    public Boolean getNoStallBlockAvailable() { return noStallBlockAvailable; }
    public void setNoStallBlockAvailable(Boolean noStallBlockAvailable) { this.noStallBlockAvailable = noStallBlockAvailable; }
}
