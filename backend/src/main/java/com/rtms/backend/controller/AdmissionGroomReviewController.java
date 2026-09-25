package com.rtms.backend.controller;

import com.rtms.backend.dto.AdmissionDetailResponse;
import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.dto.GroomAdmissionReviewRequest;
import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.service.AdmissionGroomReviewService;
import com.rtms.backend.service.AdmissionQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admissions")
public class AdmissionGroomReviewController {

    private final AdmissionGroomReviewService admissionGroomReviewService;
    private final AdmissionQueryService admissionQueryService;

    public AdmissionGroomReviewController(
            AdmissionGroomReviewService admissionGroomReviewService,
            AdmissionQueryService admissionQueryService) {
        this.admissionGroomReviewService = admissionGroomReviewService;
        this.admissionQueryService = admissionQueryService;
    }

    @PostMapping("/{id}/groom-review")
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_GROOM_REVIEW')")
    public ApiResponse<AdmissionDetailResponse> review(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestBody GroomAdmissionReviewRequest request) {

        AdmissionApplication admission = admissionGroomReviewService.review(
                id,
                currentUser.getUserId(),
                request);

        return ApiResponse.success(admissionQueryService.getAdmissionDetail(admission.getId()));
    }

    @PostMapping("/{id}/quarantine-allocation")
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_GROOM_REVIEW')")
    public ApiResponse<AdmissionDetailResponse> processWaitingForStall(
            @PathVariable Long id) {

        AdmissionApplication admission = admissionGroomReviewService.processWaitingForStall(id);
        return ApiResponse.success(admissionQueryService.getAdmissionDetail(admission.getId()));
    }
}
