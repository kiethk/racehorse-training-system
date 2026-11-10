package com.rtms.backend.training.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CreateExerciseRequest;
import com.rtms.backend.training.entity.Exercise;
import com.rtms.backend.training.enums.ExerciseCategory;
import com.rtms.backend.training.service.ExerciseService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @PreAuthorize("hasAuthority('EXERCISE_VIEW')")
    @GetMapping
    public ApiResponse<List<Exercise>> getAllExercises(
            @RequestParam(required = false) Long trainerId,
            @RequestParam(required = false) ExerciseCategory category,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(exerciseService.getAllExercises(trainerId, category, currentUser));
    }

    @PreAuthorize("hasAuthority('EXERCISE_VIEW')")
    @GetMapping("/{id}")
    public ApiResponse<Exercise> getExerciseById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(exerciseService.getExerciseById(id, currentUser));
    }

    @PreAuthorize("hasAuthority('EXERCISE_MANAGE')")
    @PostMapping
    public ApiResponse<Exercise> createExercise(
            @Valid @RequestBody CreateExerciseRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(exerciseService.createExercise(request, currentUser));
    }
}