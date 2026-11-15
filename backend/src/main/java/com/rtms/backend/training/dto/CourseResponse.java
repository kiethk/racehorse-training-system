package com.rtms.backend.training.dto;

import com.rtms.backend.training.entity.Course;

import java.time.LocalDateTime;

/**
 * Một khóa như API trả về, để entity không bị lộ thẳng ra JSON.
 * (A course as the API returns it, so the entity is not exposed straight into the JSON.)
 */
public record CourseResponse(Long id,
                             String name,
                             String targetGoal,
                             Integer totalSessions,
                             Long createdById,
                             Long trainerId,
                             LocalDateTime createdAt,
                             LocalDateTime updatedAt) {

    public static CourseResponse of(Course course) {
        return new CourseResponse(course.getId(), course.getName(), course.getTargetGoal(), course.getTotalSessions(),
                course.getCreatedById(), course.getTrainerId(), course.getCreatedAt(), course.getUpdatedAt());
    }
}
