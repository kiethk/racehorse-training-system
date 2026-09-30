package com.rtms.backend.controller;

import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.dto.CreateHorseTrainingPlanRequest;
import com.rtms.backend.dto.HorseTrainingPlanDetailResponse;
import com.rtms.backend.dto.JoinableCohortResponse;
import com.rtms.backend.dto.PlanSummaryResponse;
import com.rtms.backend.dto.TrainerDashboardHorseResponse;
import com.rtms.backend.dto.UpdatePlanStatusRequest;
import com.rtms.backend.entity.HorseTrainingPlan;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.service.HorseTrainingPlanService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/training-plans")
public class HorseTrainingPlanController {

    private final HorseTrainingPlanService planService;

    public HorseTrainingPlanController(HorseTrainingPlanService planService) {
        this.planService = planService;
    }

    /**
     * Dashboard tổng quan tiến độ và thể lực toàn khu của Trainer (FE-7.1).
     */
    @PreAuthorize("hasAuthority('TRAINING_PLAN_VIEW')")
    @GetMapping("/dashboard")
    public ApiResponse<List<TrainerDashboardHorseResponse>> getDashboard(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(planService.getTrainerDashboard(currentUser));
    }

    /**
     * Danh sách kế hoạch kèm TÊN ngựa, TÊN khoá và tiến độ.
     *
     * Trước đây trả thẳng entity nên màn hình chỉ hiện được "kế hoạch #27,
     * ngựa #15, khoá #3". Không có hàm nào ở frontend dùng dạng cũ nên đổi
     * kiểu trả về không phá màn hình nào.
     */
    @PreAuthorize("hasAuthority('TRAINING_PLAN_VIEW')")
    @GetMapping
    public ApiResponse<List<PlanSummaryResponse>> getAllPlans(
            @RequestParam(required = false) Long horseId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(planService.getPlanSummaries(horseId, currentUser));
    }

    @PreAuthorize("hasAuthority('TRAINING_PLAN_VIEW')")
    @GetMapping("/{id}")
    public ApiResponse<HorseTrainingPlanDetailResponse> getPlanById(@PathVariable Long id) {
        return ApiResponse.success(planService.getPlanById(id));
    }

    @PreAuthorize("hasAuthority('TRAINING_PLAN_CREATE')")
    @PostMapping
    public ApiResponse<List<HorseTrainingPlanDetailResponse>> createPlan(
            @RequestBody CreateHorseTrainingPlanRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(planService.createPlan(request, currentUser));
    }

    /**
     * Gợi ý nhóm để ghi danh chung (tiết kiệm khe giờ vàng).
     * Hệ thống chỉ gợi ý, Trainer tự quyết dựa trên waitDays vs sharedSessions.
     */
    @PreAuthorize("hasAuthority('TRAINING_PLAN_VIEW')")
    @GetMapping("/joinable-cohorts")
    public ApiResponse<List<JoinableCohortResponse>> getJoinableCohorts(
            @RequestParam Long courseId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(planService.getJoinableCohorts(courseId, currentUser));
    }

    @PreAuthorize("hasAuthority('TRAINING_PLAN_UPDATE')")
    @PutMapping("/{id}/status")
    public ApiResponse<HorseTrainingPlan> updatePlanStatus(@PathVariable Long id,
                                                           @RequestBody UpdatePlanStatusRequest request) {
        return ApiResponse.success(planService.updatePlanStatus(id, request));
    }
}