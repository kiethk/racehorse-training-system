package com.rtms.backend.training.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CourseExerciseItemRequest {

    @NotNull(message = "Exercise is required")
    private Long exerciseId;

    /** Vị trí trong vòng xoay, từ 1; để trống thì xếp cuối. (Position in the rotation, from 1; leave empty to put it last.) */
    @Min(value = 1, message = "Exercise order must start at 1")
    private Integer orderIndex;

    public Long getExerciseId() { return exerciseId; }
    public void setExerciseId(Long exerciseId) { this.exerciseId = exerciseId; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
}
