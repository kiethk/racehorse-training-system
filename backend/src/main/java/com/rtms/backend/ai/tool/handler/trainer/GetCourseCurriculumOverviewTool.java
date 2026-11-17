package com.rtms.backend.ai.tool.handler.trainer;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.Exercise;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.ExerciseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetCourseCurriculumOverviewTool implements AiToolHandler {

    private final CourseRepository courseRepository;
    private final ExerciseRepository exerciseRepository;

    @Autowired
    public GetCourseCurriculumOverviewTool(@Autowired(required = false) CourseRepository courseRepository,
                                           @Autowired(required = false) ExerciseRepository exerciseRepository) {
        this.courseRepository = courseRepository;
        this.exerciseRepository = exerciseRepository;
    }

    @Override
    public String getToolName() {
        return "getCourseCurriculumOverview";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        List<Course> courses = courseRepository != null ? courseRepository.findAll() : Collections.emptyList();
        List<Exercise> exercises = exerciseRepository != null ? exerciseRepository.findAll() : Collections.emptyList();

        List<Map<String, Object>> courseList = new ArrayList<>();
        for (Course c : courses) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("courseName", c.getName());
            item.put("targetGoal", c.getTargetGoal() != null ? c.getTargetGoal() : "Physical conditioning");
            item.put("totalSessions", c.getTotalSessions() != null ? c.getTotalSessions() : 0);
            courseList.add(item);
        }

        List<Map<String, Object>> exerciseList = new ArrayList<>();
        for (Exercise s : exercises) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("exerciseName", s.getName());
            item.put("technicalGroup", s.getCategory() != null ? s.getCategory().name() : "N/A");
            item.put("surface", s.getSurfaceType() != null ? s.getSurfaceType().name() : "N/A");
            item.put("targetDistance", s.getTargetDistanceMeters() != null ? s.getTargetDistanceMeters() + "m" : "N/A");
            item.put("intensity", s.getIntensityLevel() != null ? s.getIntensityLevel().name() : "MEDIUM");
            item.put("durationMinutes", s.getDurationMinutes() != null ? s.getDurationMinutes() : 60);
            exerciseList.add(item);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("courseCurriculumList", courseList);
        response.put("technicalExerciseList", exerciseList);
        response.put("actionUrl", "/trainer/courses");
        return response;
    }
}
