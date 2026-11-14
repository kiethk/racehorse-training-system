package com.rtms.backend.training.dto;

import com.rtms.backend.training.enums.IntensityLevel;
import com.rtms.backend.training.enums.SurfaceType;

import java.math.BigDecimal;

public class CourseExerciseResponse {
    private Long id; // ID bản ghi liên kết course_exercises (ID of the course_exercises link row)
    private Long exerciseId;
    private String exerciseName;
    private String description;
    private SurfaceType surfaceType;
    private BigDecimal targetDistanceMeters;
    private IntensityLevel intensityLevel;
    private Integer orderIndex;

    /**
     * Thời lượng bài tập (phút) — con số QUYẾT ĐỊNH ĐỘ DÀI LOT khi hệ thống
     * xếp lịch: session.endTime = session.startTime + durationMinutes.
     *
     * Không có trường này thì màn hình chọn khoá không cho Trainer thấy khoá
     * sẽ ngốn bao nhiêu khung giờ vàng — đúng thứ họ cần để quyết định.
     * (Exercise duration in minutes: the number that DECIDES THE LOT LENGTH when the system schedules
     *  (session.endTime = session.startTime + durationMinutes). Without it the course picker cannot show the trainer how much
     *  golden window a course will consume, which is exactly what they need to decide.)
     */
    private Integer durationMinutes;

    /** Giữ chữ ký 8 tham số cho các lời gọi đã có. (Keeps the 8-parameter signature for existing callers.) */
    public CourseExerciseResponse(Long id, Long exerciseId, String exerciseName, String description,
                                 SurfaceType surfaceType, BigDecimal targetDistanceMeters,
                                 IntensityLevel intensityLevel, Integer orderIndex) {
        this(id, exerciseId, exerciseName, description, surfaceType, targetDistanceMeters,
                intensityLevel, orderIndex, null);
    }

    public CourseExerciseResponse(Long id, Long exerciseId, String exerciseName, String description,
                                 SurfaceType surfaceType, BigDecimal targetDistanceMeters,
                                 IntensityLevel intensityLevel, Integer orderIndex,
                                 Integer durationMinutes) {
        this.id = id;
        this.exerciseId = exerciseId;
        this.exerciseName = exerciseName;
        this.description = description;
        this.surfaceType = surfaceType;
        this.targetDistanceMeters = targetDistanceMeters;
        this.intensityLevel = intensityLevel;
        this.orderIndex = orderIndex;
        this.durationMinutes = durationMinutes;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getExerciseId() { return exerciseId; }
    public void setExerciseId(Long exerciseId) { this.exerciseId = exerciseId; }

    public String getExerciseName() { return exerciseName; }
    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public SurfaceType getSurfaceType() { return surfaceType; }
    public void setSurfaceType(SurfaceType surfaceType) { this.surfaceType = surfaceType; }

    public BigDecimal getTargetDistanceMeters() { return targetDistanceMeters; }
    public void setTargetDistanceMeters(BigDecimal targetDistanceMeters) { this.targetDistanceMeters = targetDistanceMeters; }

    public IntensityLevel getIntensityLevel() { return intensityLevel; }
    public void setIntensityLevel(IntensityLevel intensityLevel) { this.intensityLevel = intensityLevel; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
}