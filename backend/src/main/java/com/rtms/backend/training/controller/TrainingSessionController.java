package com.rtms.backend.training.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.SessionSummaryResponse;
import com.rtms.backend.training.dto.SessionDetailResponse;
import com.rtms.backend.training.dto.RescheduleSessionRequest;
import com.rtms.backend.training.service.TrainingSessionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

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

    /** Lịch theo khoảng ngày: lịch hôm nay và lịch tuần dùng chung truy vấn này. (The schedule over a date range: Today and Week share this query.) */
    @GetMapping
    @PreAuthorize("hasAuthority('SESSION_VIEW')")
    public ApiResponse<List<SessionSummaryResponse>> getSessions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false, defaultValue = "false") boolean needsResult,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(sessionService.getSessions(from, to, groupId, needsResult, currentUser));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SESSION_VIEW')")
    public ApiResponse<SessionDetailResponse> getSession(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(sessionService.getSession(id, currentUser));
    }

    @PatchMapping("/{id}/reschedule")
    @PreAuthorize("hasAuthority('SESSION_UPDATE')")
    public ApiResponse<SessionSummaryResponse> reschedule(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleSessionRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(sessionService.reschedule(id, request.newStartTime(), currentUser));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('SESSION_UPDATE')")
    public ApiResponse<SessionSummaryResponse> cancel(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(sessionService.cancel(id, currentUser));
    }
}
