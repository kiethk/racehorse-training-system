package com.rtms.backend.medical.analytics.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.medical.analytics.dto.BioSpcSeriesResponse;
import com.rtms.backend.medical.analytics.dto.SurvivalEdaResponse;
import com.rtms.backend.medical.analytics.service.BioSpcAnalyticsService;
import com.rtms.backend.medical.analytics.service.SurvivalEdaAnalyticsService;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/medical-analytics")
public class MedicalEdaAnalyticsController {

    private final BioSpcAnalyticsService bioSpcAnalyticsService;
    private final SurvivalEdaAnalyticsService survivalEdaAnalyticsService;

    public MedicalEdaAnalyticsController(
            BioSpcAnalyticsService bioSpcAnalyticsService,
            SurvivalEdaAnalyticsService survivalEdaAnalyticsService
    ) {
        this.bioSpcAnalyticsService = bioSpcAnalyticsService;
        this.survivalEdaAnalyticsService = survivalEdaAnalyticsService;
    }

    @GetMapping("/bio-spc/{horseId}")
    @PreAuthorize("hasAnyAuthority('HORSE_HEALTH_METRIC_VIEW', 'INJURY_RECORD_VIEW') or hasAnyRole('VETERINARIAN', 'CLUB_MANAGER', 'HEAD_TRAINER', 'HORSE_OWNER', 'GROOM')")
    public ApiResponse<BioSpcSeriesResponse> getBioSpc(
            @PathVariable Long horseId,
            @RequestParam(required = false, defaultValue = "TEMPERATURE") String metric,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        BioSpcSeriesResponse response = bioSpcAnalyticsService.getBioSpcSeries(horseId, metric, actor);
        return ApiResponse.success(response);
    }

    @GetMapping("/survival")
    @PreAuthorize("hasAnyAuthority('HORSE_HEALTH_METRIC_VIEW', 'INJURY_RECORD_VIEW') or hasAnyRole('VETERINARIAN', 'CLUB_MANAGER', 'HEAD_TRAINER')")
    public ApiResponse<SurvivalEdaResponse> getSurvival(
            @RequestParam(required = false, defaultValue = "TREATMENT_PLAN") String stratifyBy
    ) {
        SurvivalEdaResponse response = survivalEdaAnalyticsService.getSurvivalAnalytics(stratifyBy);
        return ApiResponse.success(response);
    }
}
