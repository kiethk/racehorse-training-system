package com.rtms.backend.analytics.controller;

import com.rtms.backend.analytics.dto.manager.ManagerEdaOverviewResponse;
import com.rtms.backend.analytics.service.ManagerEdaAnalyticsService;
import com.rtms.backend.common.dto.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager-analytics/eda")
public class ManagerEdaAnalyticsController {

    private final ManagerEdaAnalyticsService managerEdaAnalyticsService;

    public ManagerEdaAnalyticsController(ManagerEdaAnalyticsService managerEdaAnalyticsService) {
        this.managerEdaAnalyticsService = managerEdaAnalyticsService;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('CLUB_MANAGER', 'ADMIN')")
    public ApiResponse<ManagerEdaOverviewResponse> getOverview(
            @RequestParam(required = false, defaultValue = "90") Integer days
    ) {
        return ApiResponse.success(managerEdaAnalyticsService.getOverview(days));
    }
}
