package com.rtms.backend.training.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CreateHorseTrainingPlanRequest;
import com.rtms.backend.training.dto.HorseTrainingPlanDetailResponse;
import com.rtms.backend.training.dto.JoinableCohortResponse;
import com.rtms.backend.training.dto.PlanPreviewRequest;
import com.rtms.backend.training.dto.PlanPreviewResponse;
import com.rtms.backend.training.dto.PlanSummaryResponse;
import com.rtms.backend.training.service.HorseTrainingPlanService;
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

@RestController
@RequestMapping("/api/training-plans")
public class HorseTrainingPlanController {

    private final HorseTrainingPlanService planService;

    public HorseTrainingPlanController(HorseTrainingPlanService planService) {
        this.planService = planService;
    }

    /**
     * Danh sách kế hoạch kèm TÊN ngựa, TÊN khóa và tiến độ. Cần quyền TRAINING_PLAN_LIST riêng (chỉ Trainer và
     * Manager), để Owner và Thú y chỉ xem được từng kế hoạch theo id.
     * (Plan list with horse NAME, course NAME and progress. Needs the separate TRAINING_PLAN_LIST permission
     * (trainer and manager only), so owners and vets can only open a single plan by id.)
     */
    @PreAuthorize("hasAuthority('TRAINING_PLAN_LIST')")
    @GetMapping
    public ApiResponse<List<PlanSummaryResponse>> getAllPlans(
            @RequestParam(required = false) Long horseId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(planService.getPlanSummaries(horseId, currentUser));
    }

    @PreAuthorize("hasAuthority('TRAINING_PLAN_VIEW')")
    @GetMapping("/{id}")
    public ApiResponse<HorseTrainingPlanDetailResponse> getPlanById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(planService.getPlanById(id, currentUser));
    }

    @PreAuthorize("hasAuthority('TRAINING_PLAN_CREATE')")
    @PostMapping
    public ApiResponse<List<HorseTrainingPlanDetailResponse>> createPlan(
            @Valid @RequestBody CreateHorseTrainingPlanRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(planService.createPlan(request, currentUser));
    }

    /**
     * Hủy một kế hoạch đang chạy hoặc sắp chạy. Dùng quyền TRAINING_PLAN_CREATE vì quản lý kế hoạch là việc của Trainer.
     * (Cancels a running or upcoming plan. Guarded by TRAINING_PLAN_CREATE because managing plans is the trainer's job.)
     */
    @PreAuthorize("hasAuthority('TRAINING_PLAN_CREATE')")
    @PostMapping("/{id}/cancel")
    public ApiResponse<HorseTrainingPlanDetailResponse> cancelPlan(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(planService.cancelPlan(id, currentUser));
    }

    /**
     * Xem trước kế hoạch: khoảng ngày của khóa và ngựa nào rảnh, bận hay bị chặn, để Trainer chọn đúng trước khi tạo.
     * Chỉ đọc, nhưng dùng quyền TRAINING_PLAN_CREATE vì đây là một bước của việc lập kế hoạch.
     * (Previews a plan: the course date range and which horses are free, busy or blocked, so the trainer chooses right
     * before creating. Read-only, but guarded by TRAINING_PLAN_CREATE because it is a step of creating a plan.)
     */
    @PreAuthorize("hasAuthority('TRAINING_PLAN_CREATE')")
    @PostMapping("/preview")
    public ApiResponse<PlanPreviewResponse> previewPlan(
            @Valid @RequestBody PlanPreviewRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(planService.previewPlan(request, currentUser));
    }

    /**
     * Gợi ý nhóm để ghi danh chung (tiết kiệm khe giờ vàng); hệ thống chỉ gợi ý, Trainer tự quyết dựa trên
     * waitDays so với sharedSessions.
     * (Suggests cohorts to enroll together (saves golden-window slots); the system only suggests and the trainer
     * decides from waitDays versus sharedSessions.)
     */
    @PreAuthorize("hasAuthority('TRAINING_PLAN_VIEW')")
    @GetMapping("/joinable-cohorts")
    public ApiResponse<List<JoinableCohortResponse>> getJoinableCohorts(
            @RequestParam Long courseId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(planService.getJoinableCohorts(courseId, currentUser));
    }
}
