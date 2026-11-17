package com.rtms.backend.analytics.controller;

import com.rtms.backend.analytics.dto.FleetEdaResponse;
import com.rtms.backend.analytics.dto.IndividualHorseEdaResponse;
import com.rtms.backend.analytics.service.HorseEdaAnalyticsService;
import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller providing endpoints for the Exploratory Data Analysis (EDA) Analytics Widget module.
 */
@RestController
public class HorseEdaAnalyticsController {

    private final HorseEdaAnalyticsService analyticsService;

    public HorseEdaAnalyticsController(HorseEdaAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Analyzes correlations and statistical distributions for the entire horse population using EDA.
     */
    @PreAuthorize("hasAuthority('TRAINING_PLAN_VIEW')")
    @GetMapping("/api/analytics/eda/fleet")
    public ApiResponse<FleetEdaResponse> getFleetEdaAnalytics(
            @RequestParam(required = false, defaultValue = "180") Integer days,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(analyticsService.getFleetEdaAnalytics(currentUser, days));
    }

    /**
     * Analyzes correlations and provides detailed statistics for each workout of an individual horse using EDA.
     */
    @PreAuthorize("hasAuthority('HORSE_VIEW')")
    @GetMapping("/api/analytics/eda/horses/{id}")
    public ApiResponse<IndividualHorseEdaResponse> getIndividualEdaAnalytics(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "365") Integer days,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(analyticsService.getIndividualEdaAnalytics(id, currentUser, days));
    }
}
