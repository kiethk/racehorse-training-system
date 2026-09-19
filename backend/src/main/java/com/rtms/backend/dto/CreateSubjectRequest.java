package com.rtms.backend.dto;

import java.math.BigDecimal;

public class CreateSubjectRequest {
    private Long categoryId;
    private String name;
    private String description;
    private String surfaceType;        // TURF, DIRT, SYNTHETIC
    private BigDecimal targetDistanceMeters;
    private String intensityLevel;     // LOW, MEDIUM, HIGH

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSurfaceType() { return surfaceType; }
    public void setSurfaceType(String surfaceType) { this.surfaceType = surfaceType; }

    public BigDecimal getTargetDistanceMeters() { return targetDistanceMeters; }
    public void setTargetDistanceMeters(BigDecimal targetDistanceMeters) { this.targetDistanceMeters = targetDistanceMeters; }

    public String getIntensityLevel() { return intensityLevel; }
    public void setIntensityLevel(String intensityLevel) { this.intensityLevel = intensityLevel; }
}