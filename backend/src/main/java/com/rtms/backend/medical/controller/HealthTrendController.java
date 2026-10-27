package com.rtms.backend.medical.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.medical.dto.HealthTrendResponse;
import com.rtms.backend.medical.service.HealthTrendService;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Biểu đồ sức khỏe gộp cho dashboard của mọi vai trò xem được số đo sức khỏe.
 * (The pooled health chart for the dashboard of every role that may view health metrics.)
 */
@RestController
@RequestMapping("/api/health-metrics")
public class HealthTrendController {

    private final HealthTrendService healthTrendService;

    public HealthTrendController(HealthTrendService healthTrendService) {
        this.healthTrendService = healthTrendService;
    }

    @PreAuthorize("hasAuthority('HORSE_HEALTH_METRIC_VIEW')")
    @GetMapping("/trend")
    public ApiResponse<HealthTrendResponse> getTrend(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(healthTrendService.getTrend(currentUser));
    }
}
