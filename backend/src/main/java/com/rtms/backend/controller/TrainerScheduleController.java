package com.rtms.backend.controller;

import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.dto.TrainerAdmissionReviewRequest;
import com.rtms.backend.dto.TrainerScheduleDetailResponse;
import com.rtms.backend.dto.TrainerScheduleResponse;
import com.rtms.backend.enums.TrainerScheduleStatus;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.service.TrainerScheduleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trainer-schedules")
public class TrainerScheduleController {

    private final TrainerScheduleService trainerScheduleService;

    public TrainerScheduleController(TrainerScheduleService trainerScheduleService) {
        this.trainerScheduleService = trainerScheduleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_VIEW')")
    public ApiResponse<List<TrainerScheduleResponse>> list(
            @RequestParam(required = false) Long trainerId,
            @RequestParam(required = false) TrainerScheduleStatus status,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        Long effectiveTrainerId;
        if ("HEAD_TRAINER".equals(currentUser.getRole())) {
            effectiveTrainerId = currentUser.getUserId();
        } else {
            effectiveTrainerId = trainerId;
        }
        return ApiResponse.success(trainerScheduleService.listSchedules(effectiveTrainerId, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_VIEW')")
    public ApiResponse<TrainerScheduleDetailResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(trainerScheduleService.getScheduleDetail(id, currentUser.getUserId(), currentUser.getRole()));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_TRAINER_REVIEW') or principal.role == 'HEAD_TRAINER'")
    public ApiResponse<TrainerScheduleResponse> start(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(trainerScheduleService.startSchedule(id, currentUser.getUserId()));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_TRAINER_REVIEW') and hasAuthority('RACING_READINESS_ASSESSMENT_CREATE')")
    public ApiResponse<TrainerScheduleResponse> complete(
            @PathVariable Long id,
            @RequestBody TrainerAdmissionReviewRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(trainerScheduleService.completeSchedule(id, request, currentUser.getUserId()));
    }
}
