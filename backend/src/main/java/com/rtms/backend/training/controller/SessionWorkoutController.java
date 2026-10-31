package com.rtms.backend.training.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CompleteWorkoutRequest;
import com.rtms.backend.training.dto.SessionWorkoutResponse;
import com.rtms.backend.training.dto.WorkoutResultPreviewResponse;
import com.rtms.backend.training.service.SessionWorkoutService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller chỉ nhận request và gọi service; luật ghi kết quả nằm ở SessionWorkoutService.
 * (The controller only receives requests and calls the service; the recording rules live in SessionWorkoutService.)
 */
@RestController
@RequestMapping("/api/session-workouts")
public class SessionWorkoutController {

    private final SessionWorkoutService workoutService;

    public SessionWorkoutController(SessionWorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    /** Số đo mô phỏng mới sau khi buổi đã hết giờ; không lưu. (A fresh simulated measurement after the Session ended; not saved.) */
    @GetMapping("/{id}/result-preview")
    @PreAuthorize("hasAuthority('TRAINING_WORKOUT_UPDATE')")
    public ResponseEntity<ApiResponse<WorkoutResultPreviewResponse>> previewResult(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(workoutService.previewResult(id, currentUser)));
    }

    @PatchMapping("/{id}/result")
    @PreAuthorize("hasAuthority('TRAINING_WORKOUT_UPDATE')")
    public ApiResponse<SessionWorkoutResponse> recordResult(
            @PathVariable Long id,
            @Valid @RequestBody CompleteWorkoutRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(workoutService.recordResult(id, request, currentUser));
    }

    @PutMapping("/{id}/result")
    @PreAuthorize("hasAuthority('TRAINING_WORKOUT_UPDATE')")
    public ApiResponse<SessionWorkoutResponse> updateResult(
            @PathVariable Long id,
            @Valid @RequestBody CompleteWorkoutRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(workoutService.updateResult(id, request, currentUser));
    }

    @PostMapping("/{id}/did-not-run")
    @PreAuthorize("hasAuthority('TRAINING_WORKOUT_UPDATE')")
    public ApiResponse<SessionWorkoutResponse> markDidNotRun(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(workoutService.markDidNotRun(id, currentUser));
    }
}
