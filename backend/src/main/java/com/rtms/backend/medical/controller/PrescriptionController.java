package com.rtms.backend.medical.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.medical.dto.CreatePrescriptionRequest;
import com.rtms.backend.medical.dto.PrescriptionResponse;
import com.rtms.backend.medical.service.TreatmentPlanService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/treatment-plans")
public class PrescriptionController {
    private final TreatmentPlanService treatmentPlanService;

    public PrescriptionController(TreatmentPlanService treatmentPlanService) {
        this.treatmentPlanService = treatmentPlanService;
    }

    @PostMapping("/{treatmentPlanId}/prescriptions")
    @PreAuthorize("hasAuthority('TREATMENT_PLAN_MANAGE')")
    public ApiResponse<PrescriptionResponse> createPrescription(
            @PathVariable Long treatmentPlanId,
            @Valid @RequestBody CreatePrescriptionRequest request
    ) {
        return ApiResponse.success(
                treatmentPlanService.createPrescription(treatmentPlanId, request)
        );
    }
}
