package com.rtms.backend.entity;

import com.rtms.backend.enums.IntensityLevel;
import com.rtms.backend.enums.SurfaceType;
import com.rtms.backend.enums.WorkoutType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subjects")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "surface_type", nullable = false, length = 30)
    private SurfaceType surfaceType; // TURF, DIRT, SYNTHETIC

    @Column(name = "target_distance_meters")
    private BigDecimal targetDistanceMeters;

    @Enumerated(EnumType.STRING)
    @Column(name = "intensity_level", nullable = false, length = 20)
    private IntensityLevel intensityLevel; // LOW, MEDIUM, HIGH

    // ===== THÊM MỚI (V43) =====

    /** Thời lượng buổi tập (phút) — quyết định độ dài của lot chạy bài này. */
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes = 60;

    /**
     * Thay cho cách suy WorkoutType cũ bằng so chuỗi tên bài:
     *   name.toUpperCase().contains("TRIAL RUN") ? TRIAL_RUN : REGULAR
     * Cách cũ hỏng im lặng ngay khi ai đó đổi tên bài sang tiếng Việt.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "workout_type", nullable = false, length = 20)
    private WorkoutType workoutType = WorkoutType.REGULAR;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public WorkoutType getWorkoutType() { return workoutType; }
    public void setWorkoutType(WorkoutType workoutType) { this.workoutType = workoutType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}