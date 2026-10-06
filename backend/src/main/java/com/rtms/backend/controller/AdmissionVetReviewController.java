package com.rtms.backend.controller;
import com.rtms.backend.dto.AdmissionDocumentResponse;
import com.rtms.backend.dto.VetReviewRequest;
import com.rtms.backend.dto.VetReviewResponse;
import com.rtms.backend.repository.AdmissionDocumentRepository;
import com.rtms.backend.service.AdmissionReviewService;
import com.rtms.backend.service.OwnerAdmissionService;
import com.rtms.backend.dto.CareScheduleResponse;
import com.rtms.backend.service.CareScheduleService;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
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

    public AdmissionVetReviewController(AdmissionDocumentRepository documentRepository,
            AdmissionReviewService reviewService, OwnerAdmissionService ownerAdmissionService,
            CareScheduleService careScheduleService) {
        this.documentRepository = documentRepository;
        this.reviewService = reviewService;
        this.ownerAdmissionService = ownerAdmissionService;
        this.careScheduleService = careScheduleService;
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
        ownerAdmissionService.assertViewerCanRead(id, currentUser);
        return ApiResponse.success(documentRepository.findByAdmissionId(id).stream()
                .map(ownerAdmissionService::toDocumentResponse).toList());
    }
}