package com.rtms.backend.training.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.training.dto.ManagerDashboardSummaryResponse;
import com.rtms.backend.training.service.ManagerDashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/manager")
public class ManagerDashboardController {

    private final ManagerDashboardService managerDashboardService;

    public ManagerDashboardController(ManagerDashboardService managerDashboardService) {
        this.managerDashboardService = managerDashboardService;
    }

    /**
     * Executive Operations Hub summary for Club Manager.
     */
    @PreAuthorize("hasRole('CLUB_MANAGER')")
    @GetMapping
    public ApiResponse<ManagerDashboardSummaryResponse> getSummary() {
        return ApiResponse.success(managerDashboardService.getSummary());
    }
}
