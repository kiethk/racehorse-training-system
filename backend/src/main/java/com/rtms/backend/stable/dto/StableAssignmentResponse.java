package com.rtms.backend.stable.dto;

import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;

import java.util.ArrayList;
import java.util.List;

public class StableAssignmentResponse {

    private List<Area> updatedAreas = new ArrayList<>();
    private List<StableStall> updatedStalls = new ArrayList<>();
    private int affectedWorkoutsCount;
    private String message;

    public StableAssignmentResponse() {
    }

    public StableAssignmentResponse(List<Area> updatedAreas,
                                    List<StableStall> updatedStalls,
                                    int affectedWorkoutsCount,
                                    String message) {
        this.updatedAreas = updatedAreas != null ? updatedAreas : new ArrayList<>();
        this.updatedStalls = updatedStalls != null ? updatedStalls : new ArrayList<>();
        this.affectedWorkoutsCount = affectedWorkoutsCount;
        this.message = message;
    }

    public List<Area> getUpdatedAreas() {
        return updatedAreas;
    }

    public void setUpdatedAreas(List<Area> updatedAreas) {
        this.updatedAreas = updatedAreas;
    }

    public List<StableStall> getUpdatedStalls() {
        return updatedStalls;
    }

    public void setUpdatedStalls(List<StableStall> updatedStalls) {
        this.updatedStalls = updatedStalls;
    }

    public int getAffectedWorkoutsCount() {
        return affectedWorkoutsCount;
    }

    public void setAffectedWorkoutsCount(int affectedWorkoutsCount) {
        this.affectedWorkoutsCount = affectedWorkoutsCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
