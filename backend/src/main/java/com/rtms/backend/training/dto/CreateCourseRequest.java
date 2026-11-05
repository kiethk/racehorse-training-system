package com.rtms.backend.training.dto;

import com.rtms.backend.config.TrainingLimits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Tạo một khóa học. Chủ của khóa luôn là người đang đăng nhập (không có trainerId trong request, như CreateSubjectRequest).
 * (Creates a course. The owner is always the signed-in user, with no trainerId in the request, like CreateSubjectRequest.)
 */
public class CreateCourseRequest {

    @NotBlank(message = "Course name is required")
    @Size(max = TrainingLimits.NAME_MAX_LENGTH, message = "Course name cannot exceed 255 characters")
    private String name;

    @Size(max = TrainingLimits.GOAL_MAX_LENGTH, message = "Course goal cannot exceed 1000 characters")
    private String targetGoal;

    @NotNull(message = "Total sessions is required")
    @Min(value = TrainingLimits.MIN_COURSE_SESSIONS, message = "A course needs at least 1 session")
    @Max(value = TrainingLimits.MAX_COURSE_SESSIONS, message = "A course can have at most 60 sessions")
    private Integer totalSessions;

    @NotEmpty(message = "Select at least one subject for the course")
    @Size(max = TrainingLimits.MAX_COURSE_SUBJECTS, message = "A course can have at most 30 subjects")
    private List<@NotNull(message = "A subject entry is empty") @Valid CourseSubjectItemRequest> subjects;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTargetGoal() { return targetGoal; }
    public void setTargetGoal(String targetGoal) { this.targetGoal = targetGoal; }

    public Integer getTotalSessions() { return totalSessions; }
    public void setTotalSessions(Integer totalSessions) { this.totalSessions = totalSessions; }

    public List<CourseSubjectItemRequest> getSubjects() { return subjects; }
    public void setSubjects(List<CourseSubjectItemRequest> subjects) { this.subjects = subjects; }
}
