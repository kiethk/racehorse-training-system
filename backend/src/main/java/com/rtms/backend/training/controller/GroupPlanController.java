package com.rtms.backend.training.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.GroupPlanDetailResponse;
import com.rtms.backend.training.dto.GroupPlanPreviewResponse;
import com.rtms.backend.training.dto.GroupPlanRequest;
import com.rtms.backend.training.dto.GroupPlanSummaryResponse;
import com.rtms.backend.training.dto.PlanHorseResponse;
import com.rtms.backend.training.dto.SessionHorseOutcomeResponse;
import com.rtms.backend.training.service.GroupPlanService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller chỉ nhận request và gọi service; mọi luật lập lịch và phân quyền theo chủ kế hoạch nằm ở GroupPlanService.
 * (The controller only receives requests and calls the service; every scheduling rule and the plan owner check live in
 * GroupPlanService.)
 */
@RestController
@RequestMapping("/api/group-plans")
public class GroupPlanController {

    private final GroupPlanService planService;

    public GroupPlanController(GroupPlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('GROUP_PLAN_LIST')")
    public ApiResponse<List<GroupPlanSummaryResponse>> getPlans(
            @RequestParam(required = false) Long groupId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(planService.getPlans(groupId, currentUser));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('GROUP_PLAN_VIEW')")
    public ApiResponse<GroupPlanDetailResponse> getPlan(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(planService.getPlan(id, currentUser));
    }

    @GetMapping("/{id}/horses")
    @PreAuthorize("hasAuthority('GROUP_PLAN_VIEW')")
    public ApiResponse<List<PlanHorseResponse>> getPlanHorses(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(planService.getPlanHorses(id, currentUser));
    }

    @GetMapping("/{id}/sessions/{sessionId}/horses")
    @PreAuthorize("hasAuthority('GROUP_PLAN_VIEW')")
    public ApiResponse<List<SessionHorseOutcomeResponse>> getSessionHorses(
            @PathVariable Long id, @PathVariable Long sessionId, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(planService.getSessionHorses(id, sessionId, currentUser));
    }

    @PostMapping("/preview")
    @PreAuthorize("hasAuthority('GROUP_PLAN_CREATE')")
    public ApiResponse<GroupPlanPreviewResponse> preview(
            @Valid @RequestBody GroupPlanRequest request, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(planService.previewPlan(request, currentUser));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('GROUP_PLAN_CREATE')")
    public ApiResponse<GroupPlanDetailResponse> createPlan(
            @Valid @RequestBody GroupPlanRequest request, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(planService.createPlan(request, currentUser));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('GROUP_PLAN_CREATE')")
    public ApiResponse<GroupPlanDetailResponse> cancelPlan(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(planService.cancelPlan(id, currentUser));
    }
}
