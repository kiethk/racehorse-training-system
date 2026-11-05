package com.rtms.backend.identity.dto;

import java.util.List;

public class AccountSummaryResponse extends StaffSummaryResponse {
    private Long ownedHorsesCount;
    private String assignmentStatus;
    private String assignedAreaCode;
    private String assignedAreaType;
    private List<String> assignedStallCodes;
    private String trainerName;
    private List<String> managedAreaCodes;

    public Long getOwnedHorsesCount() {
        return ownedHorsesCount;
    }

    public void setOwnedHorsesCount(Long ownedHorsesCount) {
        this.ownedHorsesCount = ownedHorsesCount;
    }

    public String getAssignmentStatus() {
        return assignmentStatus;
    }

    public void setAssignmentStatus(String assignmentStatus) {
        this.assignmentStatus = assignmentStatus;
    }

    public String getAssignedAreaCode() {
        return assignedAreaCode;
    }

    public void setAssignedAreaCode(String assignedAreaCode) {
        this.assignedAreaCode = assignedAreaCode;
    }

    public String getAssignedAreaType() {
        return assignedAreaType;
    }

    public void setAssignedAreaType(String assignedAreaType) {
        this.assignedAreaType = assignedAreaType;
    }

    public List<String> getAssignedStallCodes() {
        return assignedStallCodes;
    }

    public void setAssignedStallCodes(List<String> assignedStallCodes) {
        this.assignedStallCodes = assignedStallCodes;
    }

    public String getTrainerName() {
        return trainerName;
    }

    public void setTrainerName(String trainerName) {
        this.trainerName = trainerName;
    }

    public List<String> getManagedAreaCodes() {
        return managedAreaCodes;
    }

    public void setManagedAreaCodes(List<String> managedAreaCodes) {
        this.managedAreaCodes = managedAreaCodes;
    }
}

