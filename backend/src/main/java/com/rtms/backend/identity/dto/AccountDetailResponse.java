package com.rtms.backend.identity.dto;

import java.util.List;

public class AccountDetailResponse extends StaffDetailResponse {

    private List<OwnedHorseSummary> ownedHorses;
    private String assignmentStatus;
    private Long assignedAreaId;
    private String assignedAreaCode;
    private String assignedAreaName;
    private String assignedAreaType;
    private List<String> assignedStallCodes;
    private Long trainerId;
    private String trainerName;
    private List<String> managedAreaCodes;

    public static class OwnedHorseSummary {
        private Long id;
        private String name;
        private String registrationNumber;
        private String currentStatus;
        private String stableLocation;

        public OwnedHorseSummary() {}

        public OwnedHorseSummary(Long id, String name, String registrationNumber, String currentStatus, String stableLocation) {
            this.id = id;
            this.name = name;
            this.registrationNumber = registrationNumber;
            this.currentStatus = currentStatus;
            this.stableLocation = stableLocation;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getRegistrationNumber() { return registrationNumber; }
        public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

        public String getCurrentStatus() { return currentStatus; }
        public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }

        public String getStableLocation() { return stableLocation; }
        public void setStableLocation(String stableLocation) { this.stableLocation = stableLocation; }
    }

    public List<OwnedHorseSummary> getOwnedHorses() {
        return ownedHorses;
    }

    public void setOwnedHorses(List<OwnedHorseSummary> ownedHorses) {
        this.ownedHorses = ownedHorses;
    }

    public String getAssignmentStatus() {
        return assignmentStatus;
    }

    public void setAssignmentStatus(String assignmentStatus) {
        this.assignmentStatus = assignmentStatus;
    }

    public Long getAssignedAreaId() {
        return assignedAreaId;
    }

    public void setAssignedAreaId(Long assignedAreaId) {
        this.assignedAreaId = assignedAreaId;
    }

    public String getAssignedAreaCode() {
        return assignedAreaCode;
    }

    public void setAssignedAreaCode(String assignedAreaCode) {
        this.assignedAreaCode = assignedAreaCode;
    }

    public String getAssignedAreaName() {
        return assignedAreaName;
    }

    public void setAssignedAreaName(String assignedAreaName) {
        this.assignedAreaName = assignedAreaName;
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

    public Long getTrainerId() {
        return trainerId;
    }

    public void setTrainerId(Long trainerId) {
        this.trainerId = trainerId;
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
