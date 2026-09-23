package com.rtms.backend.controller;

import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.dto.CreateGroomIncidentReportRequest;
import com.rtms.backend.dto.HandleIncidentRequest;
import com.rtms.backend.entity.GroomIncidentReport;
import com.rtms.backend.enums.IncidentStatus;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.service.GroomIncidentReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groom-incident-reports")
public class GroomIncidentReportController {

    private final GroomIncidentReportService incidentReportService;

    public GroomIncidentReportController(GroomIncidentReportService incidentReportService) {
        this.incidentReportService = incidentReportService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('GROOM_INCIDENT_REPORT_CREATE')")
    public ApiResponse<GroomIncidentReport> createReport(
            @RequestBody CreateGroomIncidentReportRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        GroomIncidentReport created = incidentReportService.createReport(request, currentUser);
        return ApiResponse.success(created);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('GROOM_INCIDENT_REPORT_VIEW')")
    public ApiResponse<List<GroomIncidentReport>> getReports(
            @RequestParam(required = false) Long horseId,
            @RequestParam(required = false) IncidentStatus status,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                incidentReportService.getReports(horseId, status, currentUser));
    }

    /**
     * Thú y tiếp nhận hoặc kết luận sự cố.
     * Dashboard Thú y gọi GET /api/groom-incident-reports?status=REPORTED
     * để lấy danh sách chờ khám, rồi gọi endpoint này.
     */
    @PatchMapping("/{id}/handle")
    @PreAuthorize("hasAuthority('GROOM_INCIDENT_REPORT_HANDLE')")
    public ApiResponse<GroomIncidentReport> handleReport(
            @PathVariable Long id,
            @RequestBody HandleIncidentRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                incidentReportService.handleReport(id, request, currentUser));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('GROOM_INCIDENT_REPORT_VIEW')")
    public ApiResponse<GroomIncidentReport> getReportById(
            @PathVariable Long id) {
        GroomIncidentReport report = incidentReportService.getReportById(id);
        return ApiResponse.success(report);
    }
}
