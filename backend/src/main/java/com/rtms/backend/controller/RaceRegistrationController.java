package com.rtms.backend.controller;

import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.dto.CreateRaceRegistrationRequest;
import com.rtms.backend.dto.RaceRegistrationResponse;
import com.rtms.backend.dto.ReviewRaceRegistrationRequest;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.service.RaceRegistrationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/race-registrations")
public class RaceRegistrationController {

    private final RaceRegistrationService raceRegistrationService;

    public RaceRegistrationController(RaceRegistrationService raceRegistrationService) {
        this.raceRegistrationService = raceRegistrationService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('RACE_REGISTRATION_CREATE')")
    public ApiResponse<RaceRegistrationResponse> create(
            @Valid @RequestBody CreateRaceRegistrationRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(raceRegistrationService.create(request, user));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('RACE_REGISTRATION_VIEW')")
    public ApiResponse<List<RaceRegistrationResponse>> list(
            @RequestParam(required = false) Long horseId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(raceRegistrationService.listVisible(user, horseId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RACE_REGISTRATION_VIEW')")
    public ApiResponse<RaceRegistrationResponse> detail(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(raceRegistrationService.getVisible(id, user));
    }

    @PreAuthorize("hasAuthority('RACE_REGISTRATION_REVIEW')")
    @PatchMapping("/{id}/review")
    public ApiResponse<RaceRegistrationResponse> review(
            @PathVariable Long id,
            @RequestBody ReviewRaceRegistrationRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(raceRegistrationService.review(id, request, user));
    }
}
