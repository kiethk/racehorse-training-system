package com.rtms.backend.identity.dto;

public class EligibleReplacementTrainerResponse {
    private Long id;
    private String fullName;
    private String email;
    private long managedAreasCount;
    private long remainingCapacity;
    private boolean canAccept;

    public EligibleReplacementTrainerResponse() {}

    public EligibleReplacementTrainerResponse(Long id, String fullName, String email, long managedAreasCount, long remainingCapacity, boolean canAccept) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.managedAreasCount = managedAreasCount;
        this.remainingCapacity = remainingCapacity;
        this.canAccept = canAccept;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTrainerId() { return id; }
    public void setTrainerId(Long trainerId) { this.id = trainerId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public long getManagedAreasCount() { return managedAreasCount; }
    public void setManagedAreasCount(long managedAreasCount) { this.managedAreasCount = managedAreasCount; }

    public long getRemainingCapacity() { return remainingCapacity; }
    public void setRemainingCapacity(long remainingCapacity) { this.remainingCapacity = remainingCapacity; }

    public boolean isCanAccept() { return canAccept; }
    public void setCanAccept(boolean canAccept) { this.canAccept = canAccept; }
}
