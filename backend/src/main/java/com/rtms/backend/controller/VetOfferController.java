package com.rtms.backend.controller;

import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.dto.VetOfferResponse;
import com.rtms.backend.enums.VetOfferStatus;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.service.CareScheduleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/vet-offers", "/api/vet/offers"})
public class VetOfferController {

    private final CareScheduleService careScheduleService;

    public VetOfferController(CareScheduleService careScheduleService) {
        this.careScheduleService = careScheduleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_VETERINARIAN') or hasAuthority('VET_EXAM_MANAGE') or principal.role == 'VETERINARIAN'")
    public ApiResponse<Page<VetOfferResponse>> listMyOffers(
            @RequestParam(required = false) VetOfferStatus status,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            Pageable pageable) {
        return ApiResponse.success(careScheduleService.listOffersForVet(currentUser.getUserId(), status, pageable));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('ROLE_VETERINARIAN') or hasAuthority('VET_EXAM_MANAGE') or principal.role == 'VETERINARIAN'")
    public ApiResponse<List<VetOfferResponse>> listMyPendingOffers(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(careScheduleService.listPendingOffersForVet(currentUser.getUserId()));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('ROLE_VETERINARIAN') or hasAuthority('VET_EXAM_MANAGE') or principal.role == 'VETERINARIAN'")
    public ApiResponse<VetOfferResponse> acceptOffer(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(careScheduleService.acceptOffer(id, currentUser.getUserId()));
    }

    @PostMapping("/{id}/decline")
    @PreAuthorize("hasAuthority('ROLE_VETERINARIAN') or hasAuthority('VET_EXAM_MANAGE') or principal.role == 'VETERINARIAN'")
    public ApiResponse<VetOfferResponse> declineOffer(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(careScheduleService.declineOffer(id, currentUser.getUserId()));
    }
}