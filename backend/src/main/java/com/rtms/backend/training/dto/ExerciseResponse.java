package com.rtms.backend.training.dto;

import com.rtms.backend.training.entity.Exercise;
import com.rtms.backend.training.enums.ExerciseCategory;
import com.rtms.backend.training.enums.IntensityLevel;
import com.rtms.backend.training.enums.SurfaceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Một bài tập như API trả về, để entity không bị lộ thẳng ra JSON.
 * (An exercise as the API returns it, so the entity is not exposed straight into the JSON.)
 */
public record ExerciseResponse(Long id,
                               ExerciseCategory category,
                               Long trainerId,
                               String name,
                               String description,
                               SurfaceType surfaceType,
                               BigDecimal targetDistanceMeters,
                               IntensityLevel intensityLevel,
                               Integer durationMinutes,
                               LocalDateTime createdAt,
                               LocalDateTime updatedAt) {

    public static ExerciseResponse of(Exercise exercise) {
        return new ExerciseResponse(exercise.getId(), exercise.getCategory(), exercise.getTrainerId(), exercise.getName(),
                exercise.getDescription(), exercise.getSurfaceType(), exercise.getTargetDistanceMeters(),
                exercise.getIntensityLevel(), exercise.getDurationMinutes(), exercise.getCreatedAt(), exercise.getUpdatedAt());
    }
}
