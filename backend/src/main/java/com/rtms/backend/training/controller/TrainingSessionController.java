package com.rtms.backend.training.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.PlanSessionResponse;
import com.rtms.backend.training.dto.RescheduleSessionRequest;
import com.rtms.backend.training.service.TrainingSessionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller chỉ nhận request và gọi service; luật "chỉ khi buổi chưa bắt đầu" nằm ở TrainingSessionService.
 * (The controller only receives requests and calls the service; the "only before the Session starts" rule lives in
 * TrainingSessionService.)
 */
@RestController
@RequestMapping("/api/training-sessions")
public class TrainingSessionController {

    private final TrainingSessionService sessionService;

    public TrainingSessionController(TrainingSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PatchMapping("/{id}/reschedule")
    @PreAuthorize("hasAuthority('SESSION_UPDATE')")
    public ApiResponse<PlanSessionResponse> reschedule(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleSessionRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(sessionService.reschedule(id, request.newStartTime(), currentUser));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('SESSION_UPDATE')")
    public ApiResponse<PlanSessionResponse> cancel(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(sessionService.cancel(id, currentUser));
    }
}
