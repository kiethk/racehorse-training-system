package com.rtms.backend.training.dto;

import java.util.List;

public class CourseDetailResponse {
    private CourseResponse course;
    private List<CourseExerciseResponse> exercises;

    public CourseDetailResponse(CourseResponse course, List<CourseExerciseResponse> exercises) {
        this.course = course;
        this.exercises = exercises;
    }

    public CourseResponse getCourse() { return course; }
    public void setCourse(CourseResponse course) { this.course = course; }

    public List<CourseExerciseResponse> getExercises() { return exercises; }
    public void setExercises(List<CourseExerciseResponse> exercises) { this.exercises = exercises; }
}