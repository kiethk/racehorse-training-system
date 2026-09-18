package com.rtms.backend.dto;

import com.rtms.backend.entity.Course;
import com.rtms.backend.entity.CourseSubject;
import java.util.List;

public class CourseDetailResponse {
    private Course course;
    private List<CourseSubject> subjects;

    public CourseDetailResponse(Course course, List<CourseSubject> subjects) {
        this.course = course;
        this.subjects = subjects;
    }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public List<CourseSubject> getSubjects() { return subjects; }
    public void setSubjects(List<CourseSubject> subjects) { this.subjects = subjects; }
}