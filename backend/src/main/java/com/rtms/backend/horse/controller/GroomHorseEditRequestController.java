package com.rtms.backend.horse.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.horse.dto.HorseEditRequestResponse;
import com.rtms.backend.horse.dto.HorseEditReviewRequest;
import com.rtms.backend.horse.enums.HorseEditRequestStatus;
import com.rtms.backend.horse.service.HorseEditRequestService;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Groom side of horse edit requests, scoped to horses in the groom's stalls. */
@RestController
@RequestMapping("/api/groom/horse-edit-requests")
public class GroomHorseEditRequestController {

    private final HorseEditRequestService service;

    public GroomHorseEditRequestController(HorseEditRequestService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('HORSE_EDIT_REQUEST_REVIEW')")
    public ApiResponse<List<HorseEditRequestResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser groom,
            @RequestParam(required = false) HorseEditRequestStatus status) {
        return ApiResponse.success(service.listForGroom(groom.getUserId(), status));
    }

    @GetMapping("/{requestId}")
    @PreAuthorize("hasAuthority('HORSE_EDIT_REQUEST_REVIEW')")
    public ApiResponse<HorseEditRequestResponse> detail(
            @PathVariable Long requestId,
            @AuthenticationPrincipal AuthenticatedUser groom) {
        return ApiResponse.success(service.getForGroom(groom.getUserId(), requestId));
    }

    @PostMapping("/{requestId}/review")
    @PreAuthorize("hasAuthority('HORSE_EDIT_REQUEST_REVIEW')")
    public ApiResponse<HorseEditRequestResponse> review(
            @PathVariable Long requestId,
            @AuthenticationPrincipal AuthenticatedUser groom,
            @RequestBody HorseEditReviewRequest request) {
        return ApiResponse.success(service.review(groom.getUserId(), requestId, request));
    }
}
