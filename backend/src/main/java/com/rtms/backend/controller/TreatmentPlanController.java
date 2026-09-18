package com.rtms.backend.controller;

import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.dto.CreateTreatmentPlanRequest;
import com.rtms.backend.dto.TreatmentPlanResponse;
import com.rtms.backend.service.TreatmentPlanService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medical-records")
public class TreatmentPlanController {
    private final TreatmentPlanService treatmentPlanService;

    public TreatmentPlanController(TreatmentPlanService treatmentPlanService) {
        this.treatmentPlanService = treatmentPlanService;
    }

    @PostMapping("/{medicalRecordId}/treatment-plans")
    @PreAuthorize("hasAuthority('TREATMENT_PLAN_MANAGE')")
    public ApiResponse<TreatmentPlanResponse> createTreatmentPlan(
            @PathVariable Long medicalRecordId,
            @Valid @RequestBody CreateTreatmentPlanRequest request
            ) {
        return ApiResponse.success(
                treatmentPlanService.createTreatmentPlan(medicalRecordId, request)
        );
    }

    @GetMapping("/{medicalRecordId}/treatment-plans")
    @PreAuthorize("hasAuthority('TREATMENT_PLAN_VIEW')")
    public ApiResponse<List<TreatmentPlanResponse>> getTreatmentPlans(
            @PathVariable Long medicalRecordId
    ) {
        return ApiResponse.success(
                treatmentPlanService.getByMedicalRecordId(medicalRecordId)
        );
    }
}
