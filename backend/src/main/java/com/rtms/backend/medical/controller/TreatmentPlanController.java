package com.rtms.backend.medical.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.medical.dto.CreateTreatmentPlanRequest;
import com.rtms.backend.medical.dto.TreatmentPlanResponse;
import com.rtms.backend.medical.service.TreatmentPlanService;
import com.rtms.backend.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/health-records", "/api/treatment-plans"})
public class TreatmentPlanController {
    private final TreatmentPlanService treatmentPlanService;

    public TreatmentPlanController(TreatmentPlanService treatmentPlanService) {
        this.treatmentPlanService = treatmentPlanService;
    }

    @PostMapping("/{healthRecordId}/treatment-plans")
    @PreAuthorize("hasAuthority('TREATMENT_PLAN_MANAGE')")
    public ApiResponse<TreatmentPlanResponse> createTreatmentPlan(
            @PathVariable Long healthRecordId,
            @Valid @RequestBody CreateTreatmentPlanRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(
                treatmentPlanService.createTreatmentPlan(healthRecordId, request, currentUser)
        );
    }

    @GetMapping("/{healthRecordId}/treatment-plans")
    @PreAuthorize("hasAuthority('TREATMENT_PLAN_VIEW')")
    public ApiResponse<List<TreatmentPlanResponse>> getTreatmentPlans(
            @PathVariable Long healthRecordId
    ) {
        return ApiResponse.success(
                treatmentPlanService.getByHealthRecordId(healthRecordId)
        );
    }

    @PatchMapping(value = {"/{planId}/status", "/treatment-plans/{planId}/status"})
    @PreAuthorize("hasAuthority('TREATMENT_PLAN_MANAGE')")
    public ApiResponse<TreatmentPlanResponse> updateTreatmentPlanStatus(
            @PathVariable Long planId,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        String resolvedStatus = status;
        if (body != null && body.containsKey("status") && body.get("status") != null) {
            resolvedStatus = body.get("status").toString();
        }
        return ApiResponse.success(
                treatmentPlanService.updateTreatmentPlanStatus(planId, resolvedStatus, currentUser)
        );
    }

    @PutMapping(value = {"/{planId}/status", "/treatment-plans/{planId}/status"})
    @PreAuthorize("hasAuthority('TREATMENT_PLAN_MANAGE')")
    public ApiResponse<TreatmentPlanResponse> putTreatmentPlanStatus(
            @PathVariable Long planId,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return updateTreatmentPlanStatus(planId, body, status, currentUser);
    }
}
