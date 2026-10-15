package com.rtms.backend.incident.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.incident.dto.CreateIncidentReportRequest;
import com.rtms.backend.incident.dto.HandleIncidentRequest;
import com.rtms.backend.incident.entity.HorseIncidentReport;
import com.rtms.backend.incident.enums.IncidentStatus;
import com.rtms.backend.incident.service.HorseIncidentReportService;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping({"/api/incidents", "/api/groom-incident-reports"})
public class HorseIncidentReportController {

    private final HorseIncidentReportService incidentReportService;

    public HorseIncidentReportController(HorseIncidentReportService incidentReportService) {
        this.incidentReportService = incidentReportService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('GROOM_INCIDENT_REPORT_CREATE')")
    public ApiResponse<HorseIncidentReport> createReport(
            @RequestBody CreateIncidentReportRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        HorseIncidentReport created = incidentReportService.createReport(request, currentUser);
        return ApiResponse.success(created);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('GROOM_INCIDENT_REPORT_VIEW')")
    public ApiResponse<List<HorseIncidentReport>> getReports(
            @RequestParam(required = false) Long horseId,
            @RequestParam(required = false) IncidentStatus status,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                incidentReportService.getReports(horseId, status, currentUser));
    }

    @PatchMapping("/{id}/handle")
    @PreAuthorize("hasAuthority('GROOM_INCIDENT_REPORT_HANDLE')")
    public ApiResponse<HorseIncidentReport> handleReport(
            @PathVariable Long id,
            @RequestBody HandleIncidentRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                incidentReportService.handleReport(id, request, currentUser));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('GROOM_INCIDENT_REPORT_VIEW')")
    public ApiResponse<HorseIncidentReport> getReportById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        HorseIncidentReport report = incidentReportService.getReportById(id, currentUser);
        return ApiResponse.success(report);
    }

    @PostMapping(value = "/{id}/image", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('GROOM_INCIDENT_REPORT_CREATE')")
    public ApiResponse<HorseIncidentReport> uploadImage(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                incidentReportService.attachImage(id, file, currentUser));
    }

    @GetMapping("/{id}/image")
    @PreAuthorize("hasAuthority('GROOM_INCIDENT_REPORT_VIEW')")
    public ResponseEntity<Resource> getImage(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        Resource file = incidentReportService.loadImage(id, currentUser);
        return ResponseEntity.ok()
                .contentType(incidentReportService.imageMediaType(id, currentUser))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=incident-" + id)
                .header("X-Content-Type-Options", "nosniff")
                .body(file);
    }
}
