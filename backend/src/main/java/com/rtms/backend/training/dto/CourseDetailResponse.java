package com.rtms.backend.training.dto;

import com.rtms.backend.training.entity.Course;
import java.util.List;

public class CourseDetailResponse {
    private Course course;
    private List<CourseExerciseResponse> exercises;

    public CourseDetailResponse(Course course, List<CourseExerciseResponse> exercises) {
        this.course = course;
        this.exercises = exercises;
    }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public List<CourseExerciseResponse> getExercises() { return exercises; }
    public void setExercises(List<CourseExerciseResponse> exercises) { this.exercises = exercises; }
}