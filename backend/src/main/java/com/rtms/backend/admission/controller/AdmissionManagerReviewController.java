package com.rtms.backend.admission.controller;
import com.rtms.backend.admission.service.OwnerAdmissionService;

import com.rtms.backend.admission.dto.AdmissionDetailResponse;
import com.rtms.backend.admission.dto.AdmissionSummaryResponse;
import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.admission.dto.ManagerReviewRequest;
import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.admission.service.AdmissionManagerReviewService;
import com.rtms.backend.admission.service.AdmissionQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admissions")
public class AdmissionManagerReviewController {

    private final AdmissionManagerReviewService admissionManagerReviewService;
    private final AdmissionQueryService admissionQueryService;
    private final com.rtms.backend.admission.service.OwnerAdmissionService ownerAdmissionService;

    public AdmissionManagerReviewController(
            AdmissionManagerReviewService admissionManagerReviewService,
            AdmissionQueryService admissionQueryService,
            com.rtms.backend.admission.service.OwnerAdmissionService ownerAdmissionService) {
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
                        : "VETERINARIAN".equals(currentUser.getRole())
                                ? admissionQueryService.getAdmissionsForVet(currentUser.getUserId(), status)
                                : "HEAD_TRAINER".equals(currentUser.getRole())
                                        ? admissionQueryService.getAdmissionsForTrainer(currentUser.getUserId(), status)
                                        : "GROOM".equals(currentUser.getRole())
                                                ? admissionQueryService.getAdmissionsForGroom(currentUser.getUserId(), status)
                                                : admissionQueryService.getAdmissions(status)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_VIEW')")
    public ApiResponse<?> getAdmissionDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        if ("HORSE_OWNER".equals(currentUser.getRole())) {
            return ApiResponse.success(ownerAdmissionService.getMyAdmission(currentUser.getUserId(), id));
        }
        if ("VETERINARIAN".equals(currentUser.getRole())) {
            admissionQueryService.assertVetAssignedOrManager(id, currentUser);
        } else {
            ownerAdmissionService.assertViewerCanRead(id, currentUser);
        }
        return ApiResponse.success(admissionQueryService.getAdmissionDetail(id));
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
