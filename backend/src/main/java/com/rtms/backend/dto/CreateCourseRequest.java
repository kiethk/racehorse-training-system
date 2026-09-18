package com.rtms.backend.dto;

import java.util.List;

public class CreateCourseRequest {
    private String name;
    private String description;
    private String targetGoal;
    private Integer totalSessions;
    private List<CreateCourseSubjectRequest> subjects;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTargetGoal() { return targetGoal; }
    public void setTargetGoal(String targetGoal) { this.targetGoal = targetGoal; }

    public Integer getTotalSessions() { return totalSessions; }
    public void setTotalSessions(Integer totalSessions) { this.totalSessions = totalSessions; }

    public List<CreateCourseSubjectRequest> getSubjects() { return subjects; }
    public void setSubjects(List<CreateCourseSubjectRequest> subjects) { this.subjects = subjects; }
}