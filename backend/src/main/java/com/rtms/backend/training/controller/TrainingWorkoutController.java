package com.rtms.backend.training.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CompleteWorkoutRequest;
import com.rtms.backend.training.dto.PlanWorkoutItemResponse;
import com.rtms.backend.training.dto.WorkoutResultPreviewResponse;
import com.rtms.backend.training.service.TrainingWorkoutService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workouts")
public class TrainingWorkoutController {

    private final TrainingWorkoutService workoutService;

    public TrainingWorkoutController(TrainingWorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    /** Generates a fresh, non-persistent measurement preview after the lot has ended. */
    @GetMapping("/{id}/result-preview")
    @PreAuthorize("hasAuthority('TRAINING_WORKOUT_UPDATE')")
    public ResponseEntity<ApiResponse<WorkoutResultPreviewResponse>> previewWorkoutResult(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        WorkoutResultPreviewResponse preview = workoutService.generateResultPreview(id, currentUser);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(preview));
    }

    /**
     * Trainer đóng buổi tập và ghi nhận chỉ số chuyên môn.
     * (The trainer closes a workout and records professional metrics.)
     */
    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('TRAINING_WORKOUT_UPDATE')")
    public ApiResponse<PlanWorkoutItemResponse> completeWorkout(
            @PathVariable Long id,
            @Valid @RequestBody CompleteWorkoutRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(workoutService.completeWorkout(id, request, currentUser));
    }
}
