package com.rtms.backend.training.dto;

import com.rtms.backend.config.TrainingLimits;
import com.rtms.backend.training.enums.IntensityLevel;
import com.rtms.backend.training.enums.SubjectCategory;
import com.rtms.backend.training.enums.SurfaceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Tạo một bài tập. Chủ của bài luôn là người đang đăng nhập, nên request KHÔNG mang trainerId: cho phép client chọn chủ
 * sẽ cho một Trainer tạo bài thay cho Trainer khác.
 * (Creates a subject. The owner is always the signed-in user, so the request carries NO trainerId: letting the client pick the
 * owner would let one trainer create subjects for another.)
 */
public class CreateSubjectRequest {

    @NotNull(message = "Category is required")
    private SubjectCategory category;        // ENDURANCE, SPEED, GATE_START, RECOVERY

    @NotBlank(message = "Subject name is required")
    @Size(max = TrainingLimits.NAME_MAX_LENGTH, message = "Subject name cannot exceed 255 characters")
    private String name;

    @Size(max = TrainingLimits.DESCRIPTION_MAX_LENGTH, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Surface type is required")
    private SurfaceType surfaceType;        // TURF, DIRT, SYNTHETIC

    @NotNull(message = "Target distance is required")
    @Min(value = TrainingLimits.MIN_SUBJECT_DISTANCE_METERS, message = "Target distance must be at least 100 meters")
    @Max(value = TrainingLimits.MAX_SUBJECT_DISTANCE_METERS, message = "Target distance cannot exceed 10000 meters")
    private BigDecimal targetDistanceMeters;

    @NotNull(message = "Intensity level is required")
    private IntensityLevel intensityLevel;     // LOW, MEDIUM, HIGH

    @Min(value = TrainingLimits.MIN_SUBJECT_DURATION_MINUTES, message = "Subject duration must be between 15 and 240 minutes")
    @Max(value = TrainingLimits.MAX_SUBJECT_DURATION_MINUTES, message = "Subject duration must be between 15 and 240 minutes")
    private Integer durationMinutes;              // mặc định 60 nếu null (defaults to 60 when null)

    public SubjectCategory getCategory() { return category; }
    public void setCategory(SubjectCategory category) { this.category = category; }

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
}
