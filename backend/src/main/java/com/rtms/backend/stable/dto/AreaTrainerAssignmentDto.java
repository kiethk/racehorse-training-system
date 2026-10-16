package com.rtms.backend.stable.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;

public class AreaTrainerAssignmentDto {

    @NotNull(message = "areaId must not be null")
    private Long areaId;

    /** null means unassign trainer */
    private Long trainerId;

    /** Expected trainer ID at the time data was loaded (for conflict detection) */
    private Long expectedTrainerId;

    private boolean expectedTrainerIdSet = false;

    public AreaTrainerAssignmentDto() {
    }

    public AreaTrainerAssignmentDto(Long areaId, Long trainerId) {
        this.areaId = areaId;
        this.trainerId = trainerId;
    }

    public AreaTrainerAssignmentDto(Long areaId, Long trainerId, Long expectedTrainerId) {
        this.areaId = areaId;
        this.trainerId = trainerId;
        this.expectedTrainerId = expectedTrainerId;
        this.expectedTrainerIdSet = true;
    }

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public Long getTrainerId() {
        return trainerId;
    }

    public void setTrainerId(Long trainerId) {
        this.trainerId = trainerId;
    }

    public Long getExpectedTrainerId() {
        return expectedTrainerId;
    }

    public void setExpectedTrainerId(Long expectedTrainerId) {
        this.expectedTrainerId = expectedTrainerId;
        this.expectedTrainerIdSet = true;
    }

    @JsonIgnore
    public boolean isExpectedTrainerIdSet() {
        return expectedTrainerIdSet;
    }
}
