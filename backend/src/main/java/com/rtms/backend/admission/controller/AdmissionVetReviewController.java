package com.rtms.backend.admission.controller;
import com.rtms.backend.admission.dto.AdmissionDetailResponse;
import com.rtms.backend.admission.dto.AdmissionDocumentResponse;
import com.rtms.backend.admission.dto.VetAdmissionQueueItemResponse;
import com.rtms.backend.admission.dto.VetQueueSummaryResponse;
import com.rtms.backend.admission.dto.VetReviewRequest;
import com.rtms.backend.admission.dto.VetReviewResponse;
import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.medical.dto.CareScheduleResponse;

import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.admission.repository.AdmissionDocumentRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.admission.service.AdmissionQueryService;
import com.rtms.backend.admission.service.AdmissionReviewService;
import com.rtms.backend.medical.service.CareScheduleService;
import com.rtms.backend.admission.service.OwnerAdmissionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admissions")
public class AdmissionVetReviewController {
    private final AdmissionDocumentRepository documentRepository;
    private final AdmissionReviewService reviewService;
    private final OwnerAdmissionService ownerAdmissionService;
    private final CareScheduleService careScheduleService;
    private final AdmissionQueryService admissionQueryService;

    public AdmissionVetReviewController(AdmissionDocumentRepository documentRepository,
            AdmissionReviewService reviewService, OwnerAdmissionService ownerAdmissionService,
            CareScheduleService careScheduleService,
            AdmissionQueryService admissionQueryService) {
        this.documentRepository = documentRepository;
        this.reviewService = reviewService;
        this.ownerAdmissionService = ownerAdmissionService;
        this.careScheduleService = careScheduleService;
        this.admissionQueryService = admissionQueryService;
    }

    @GetMapping("/vet/queue")
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_VET_REVIEW') or principal.role == 'VETERINARIAN' or principal.role == 'CLUB_MANAGER'")
    public ApiResponse<Page<VetAdmissionQueueItemResponse>> getVetQueue(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String pill,
            @RequestParam(required = false) AdmissionStatus admissionStatus,
            @RequestParam(required = false) CareScheduleStatus scheduleStatus,
            @RequestParam(required = false) CareType careType,
            @RequestParam(required = false) String priority,
            Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(admissionQueryService.getVetQueue(
                currentUser.getUserId(), search, pill, admissionStatus, scheduleStatus, careType, priority, pageable));
    }

    @GetMapping("/vet/summary")
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_VET_REVIEW') or principal.role == 'VETERINARIAN' or principal.role == 'CLUB_MANAGER'")
    public ApiResponse<VetQueueSummaryResponse> getVetQueueSummary(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(admissionQueryService.getVetQueueSummary(currentUser.getUserId()));
    }

    @GetMapping("/vet/{id}")
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_VET_REVIEW') or principal.role == 'VETERINARIAN' or principal.role == 'CLUB_MANAGER'")
    public ApiResponse<AdmissionDetailResponse> getVetAdmissionDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        admissionQueryService.assertVetAssignedOrManager(id, currentUser);
        return ApiResponse.success(admissionQueryService.getVetAdmissionDetail(id));
    }

    @PostMapping("/{id}/initial-exam")
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_GROOM_REVIEW') or principal.role == 'GROOM' or principal.role == 'CLUB_MANAGER'")
    public ApiResponse<CareScheduleResponse> createInitialExam(@PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(careScheduleService.createInitialSchedule(id, null));
    }

    @PostMapping("/{id}/vet-review")
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_VET_REVIEW')")
    public ApiResponse<VetReviewResponse> reviewByVet(@PathVariable Long id,
            @Valid @RequestBody VetReviewRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(reviewService.reviewByVet(id, request, currentUser.getUserId()));
    }

    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAuthority('ADMISSION_DOCUMENT_VIEW')")
    public ApiResponse<List<AdmissionDocumentResponse>> getDocuments(@PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        if ("VETERINARIAN".equals(currentUser.getRole())) {
            admissionQueryService.assertVetAssignedOrManager(id, currentUser);
        } else {
            ownerAdmissionService.assertViewerCanRead(id, currentUser);
        }
        return ApiResponse.success(documentRepository.findByAdmissionId(id).stream()
                .map(ownerAdmissionService::toDocumentResponse).toList());
    }
}
