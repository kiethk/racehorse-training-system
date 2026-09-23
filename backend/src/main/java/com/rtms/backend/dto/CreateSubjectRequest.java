package com.rtms.backend.dto;

import com.rtms.backend.enums.IntensityLevel;
import com.rtms.backend.enums.SurfaceType;

import java.math.BigDecimal;

public class CreateSubjectRequest {
    private Long categoryId;
    private String name;
    private String description;
    private SurfaceType surfaceType;        // TURF, DIRT, SYNTHETIC
    private BigDecimal targetDistanceMeters;
    private IntensityLevel intensityLevel;     // LOW, MEDIUM, HIGH
    private Integer durationMinutes;              // mặc định 60 nếu null
    private com.rtms.backend.enums.WorkoutType workoutType;  // mặc định REGULAR nếu null

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public SurfaceType getSurfaceType() { return surfaceType; }
    public void setSurfaceType(SurfaceType surfaceType) { this.surfaceType = surfaceType; }

    public BigDecimal getTargetDistanceMeters() { return targetDistanceMeters; }
    public void setTargetDistanceMeters(BigDecimal targetDistanceMeters) { this.targetDistanceMeters = targetDistanceMeters; }

    public IntensityLevel getIntensityLevel() { return intensityLevel; }
    public void setIntensityLevel(IntensityLevel intensityLevel) { this.intensityLevel = intensityLevel; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public com.rtms.backend.enums.WorkoutType getWorkoutType() { return workoutType; }
    public void setWorkoutType(com.rtms.backend.enums.WorkoutType workoutType) { this.workoutType = workoutType; }
}