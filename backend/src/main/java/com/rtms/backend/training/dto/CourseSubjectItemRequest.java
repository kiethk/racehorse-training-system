package com.rtms.backend.training.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CourseSubjectItemRequest {

    @NotNull(message = "Subject is required")
    private Long subjectId;

    /** Vị trí trong vòng xoay, từ 1; để trống thì xếp cuối. (Position in the rotation, from 1; leave empty to put it last.) */
    @Min(value = 1, message = "Subject order must start at 1")
    private Integer orderIndex;

    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
}
