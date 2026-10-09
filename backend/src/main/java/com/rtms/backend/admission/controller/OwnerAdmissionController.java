package com.rtms.backend.admission.controller;
import com.rtms.backend.admission.dto.AdmissionDocumentMetadataRequest;
import com.rtms.backend.admission.dto.AdmissionDocumentResponse;
import com.rtms.backend.admission.dto.AdmissionSummaryResponse;
import com.rtms.backend.admission.dto.CreateOwnerAdmissionRequest;
import com.rtms.backend.admission.dto.OwnerAdmissionDetailResponse;
import com.rtms.backend.admission.enums.AdmissionDocumentType;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.common.dto.ApiResponse;

import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.admission.service.OwnerAdmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/owner/admissions")
public class OwnerAdmissionController {
    private final OwnerAdmissionService service;

    public OwnerAdmissionController(OwnerAdmissionService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(
            "hasAuthority('ADMISSION_APPLICATION_CREATE')"
            + " and hasAuthority('ADMISSION_DOCUMENT_CREATE')"
            + " and principal.role == 'HORSE_OWNER'"
    )
    public ResponseEntity<ApiResponse<OwnerAdmissionDetailResponse>> submit(
            @AuthenticationPrincipal AuthenticatedUser owner,
            @Valid @RequestPart("candidate")
            CreateOwnerAdmissionRequest request,
            @RequestPart("documents")
            List<AdmissionDocumentMetadataRequest> metadata,
            @RequestPart("files")
            List<MultipartFile> files) {
        OwnerAdmissionDetailResponse result = service.submit(
                owner.getUserId(),
                request,
                metadata,
                files);
     return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(result));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_VIEW') and principal.role == 'HORSE_OWNER'")
    public ApiResponse<List<AdmissionSummaryResponse>> myAdmissions(
            @AuthenticationPrincipal AuthenticatedUser owner,
            @RequestParam(required = false) AdmissionStatus status) {
        return ApiResponse.success(service.getMyAdmissions(owner.getUserId(), status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_VIEW') and principal.role == 'HORSE_OWNER'")
    public ApiResponse<OwnerAdmissionDetailResponse> detail(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser owner) {
        return ApiResponse.success(service.getMyAdmission(owner.getUserId(), id));
    }

    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAuthority('ADMISSION_DOCUMENT_VIEW') and principal.role == 'HORSE_OWNER'")
    public ApiResponse<List<AdmissionDocumentResponse>> documents(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser owner) {
        return ApiResponse.success(service.getMyDocuments(owner.getUserId(), id));
    }

    @PostMapping(value = "/{id}/documents", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('ADMISSION_DOCUMENT_CREATE') and principal.role == 'HORSE_OWNER'")
    public ResponseEntity<ApiResponse<AdmissionDocumentResponse>> upload(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser owner,
            @RequestParam AdmissionDocumentType documentType,
            @RequestParam(required = false) LocalDate recordDate,
            @RequestParam(required = false) String note,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.upload(owner.getUserId(), id, documentType,
                        recordDate, note, file)));
    }
}
