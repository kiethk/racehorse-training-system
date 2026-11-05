package com.rtms.backend.horse.controller;

import com.rtms.backend.admission.enums.AdmissionDocumentType;
import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.horse.dto.HorseEditFields;
import com.rtms.backend.horse.dto.HorseEditRequestResponse;
import com.rtms.backend.horse.service.HorseEditRequestService;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Owner side of horse edit requests: submit, see the latest one, cancel while pending. */
@RestController
@RequestMapping("/api/owner/horses/{horseId}/edit-requests")
public class OwnerHorseEditRequestController {

    private final HorseEditRequestService service;

    public OwnerHorseEditRequestController(HorseEditRequestService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('HORSE_EDIT_REQUEST_CREATE') and principal.role == 'HORSE_OWNER'")
    public ResponseEntity<ApiResponse<HorseEditRequestResponse>> submit(
            @PathVariable Long horseId,
            @AuthenticationPrincipal AuthenticatedUser owner,
            @RequestPart("changes") HorseEditFields changes,
            @RequestPart(value = "documentTypes", required = false) List<AdmissionDocumentType> documentTypes,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                service.submit(owner.getUserId(), horseId, changes, documentTypes, files)));
    }

    @GetMapping("/latest")
    @PreAuthorize("hasAuthority('HORSE_EDIT_REQUEST_CREATE') and principal.role == 'HORSE_OWNER'")
    public ApiResponse<HorseEditRequestResponse> latest(
            @PathVariable Long horseId,
            @AuthenticationPrincipal AuthenticatedUser owner) {
        return ApiResponse.success(service.latestForOwner(owner.getUserId(), horseId));
    }

    @PostMapping("/{requestId}/cancel")
    @PreAuthorize("hasAuthority('HORSE_EDIT_REQUEST_CREATE') and principal.role == 'HORSE_OWNER'")
    public ApiResponse<HorseEditRequestResponse> cancel(
            @PathVariable Long horseId,
            @PathVariable Long requestId,
            @AuthenticationPrincipal AuthenticatedUser owner) {
        return ApiResponse.success(service.cancel(owner.getUserId(), horseId, requestId));
    }
}
