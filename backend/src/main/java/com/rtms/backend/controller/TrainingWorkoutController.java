package com.rtms.backend.controller;

import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.dto.CompleteWorkoutRequest;
import com.rtms.backend.dto.PlanWorkoutItemResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.service.HorseTrainingPlanService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workouts")
public class TrainingWorkoutController {

    private final HorseTrainingPlanService planService;

    public TrainingWorkoutController(HorseTrainingPlanService planService) {
        this.planService = planService;
    }

    /**
     * Trainer đóng buổi tập và ghi nhận chỉ số chuyên môn.
     * Permission TRAINING_WORKOUT_UPDATE đã tồn tại từ V22 nhưng tới giờ
     * chưa endpoint nào dùng tới.
     */
    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('TRAINING_WORKOUT_UPDATE')")
    public ApiResponse<PlanWorkoutItemResponse> completeWorkout(
            @PathVariable Long id,
            @RequestBody CompleteWorkoutRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(planService.completeWorkout(id, request, currentUser));
    }
}
