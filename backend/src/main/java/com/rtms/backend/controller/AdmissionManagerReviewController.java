package com.rtms.backend.controller;

import com.rtms.backend.dto.AdmissionDetailResponse;
import com.rtms.backend.dto.AdmissionSummaryResponse;
import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.dto.ManagerReviewRequest;
import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.service.AdmissionManagerReviewService;
import com.rtms.backend.service.AdmissionQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admissions")
public class AdmissionManagerReviewController {

    private final AdmissionManagerReviewService admissionManagerReviewService;
    private final AdmissionQueryService admissionQueryService;
    private final com.rtms.backend.service.OwnerAdmissionService ownerAdmissionService;

    public AdmissionManagerReviewController(
            AdmissionManagerReviewService admissionManagerReviewService,
            AdmissionQueryService admissionQueryService,
            com.rtms.backend.service.OwnerAdmissionService ownerAdmissionService) {
        this.admissionManagerReviewService = admissionManagerReviewService;
        this.admissionQueryService = admissionQueryService;
        this.ownerAdmissionService = ownerAdmissionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_VIEW')")
    public ApiResponse<List<AdmissionSummaryResponse>> getAdmissions(
            @RequestParam(required = false) AdmissionStatus status,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return ApiResponse.success(
                "HORSE_OWNER".equals(currentUser.getRole())
                        ? ownerAdmissionService.getMyAdmissions(currentUser.getUserId(), status)
                        : admissionQueryService.getAdmissions(status)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_VIEW')")
    public ApiResponse<?> getAdmissionDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return ApiResponse.success(
                "HORSE_OWNER".equals(currentUser.getRole())
                        ? ownerAdmissionService.getMyAdmission(currentUser.getUserId(), id)
                        : admissionQueryService.getAdmissionDetail(id)
        );
    }

    @PostMapping("/{id}/manager-review")
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_MANAGER_REVIEW')")
    public ApiResponse<AdmissionApplication> review(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestBody ManagerReviewRequest request) {

        AdmissionApplication result =
                admissionManagerReviewService.review(
                        id,
                        currentUser.getUserId(),
                        request);

        return ApiResponse.success(result);
    }
}