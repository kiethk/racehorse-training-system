package com.rtms.backend.stable.dto;

import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.List;

public class BatchStableAssignmentRequest {

    @Valid
    private List<AreaTrainerAssignmentDto> trainerAssignments = new ArrayList<>();

    @Valid
    private List<BlockGroomAssignmentDto> groomAssignments = new ArrayList<>();

    public BatchStableAssignmentRequest() {
    }

    public BatchStableAssignmentRequest(List<AreaTrainerAssignmentDto> trainerAssignments,
                                        List<BlockGroomAssignmentDto> groomAssignments) {
        this.trainerAssignments = trainerAssignments != null ? trainerAssignments : new ArrayList<>();
        this.groomAssignments = groomAssignments != null ? groomAssignments : new ArrayList<>();
    }

    public List<AreaTrainerAssignmentDto> getTrainerAssignments() {
        return trainerAssignments;
    }

    public void setTrainerAssignments(List<AreaTrainerAssignmentDto> trainerAssignments) {
        this.trainerAssignments = trainerAssignments != null ? trainerAssignments : new ArrayList<>();
    }

    public List<BlockGroomAssignmentDto> getGroomAssignments() {
        return groomAssignments;
    }

    public void setGroomAssignments(List<BlockGroomAssignmentDto> groomAssignments) {
        this.groomAssignments = groomAssignments != null ? groomAssignments : new ArrayList<>();
    }
}
