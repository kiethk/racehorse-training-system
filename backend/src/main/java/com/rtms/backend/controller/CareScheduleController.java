package com.rtms.backend.controller;

import com.rtms.backend.dto.*;
import com.rtms.backend.enums.CareScheduleStatus;
import com.rtms.backend.enums.CareType;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.service.CareScheduleService;
import com.rtms.backend.service.UrgentAlertStreamService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping({"/api/care-schedules", "/api/vet/schedules"})
public class CareScheduleController {

    private final CareScheduleService careScheduleService;
    private final UrgentAlertStreamService urgentAlertStreamService;

    public CareScheduleController(CareScheduleService careScheduleService,
            UrgentAlertStreamService urgentAlertStreamService) {
        this.careScheduleService = careScheduleService;
        this.urgentAlertStreamService = urgentAlertStreamService;
    }

    @GetMapping("/urgent-alerts/pending")
    @PreAuthorize("principal.role == 'VETERINARIAN'")
    public ApiResponse<List<UrgentAssignmentAlert>> pendingUrgentAlerts(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(careScheduleService.getPendingUrgentAlerts(currentUser.getUserId()));
    }

    @GetMapping("/urgent-alerts/{scheduleId}")
    @PreAuthorize("principal.role == 'VETERINARIAN'")
    public ApiResponse<UrgentAssignmentAlert> urgentCase(
            @PathVariable Long scheduleId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(careScheduleService.getUrgentCase(scheduleId, currentUser.getUserId()));
    }

    @GetMapping(value = "/urgent-alerts/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("principal.role == 'VETERINARIAN'")
    public SseEmitter streamUrgentAlerts(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return urgentAlertStreamService.subscribe(currentUser.getUserId());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VET_EXAM_VIEW')")
    public ApiResponse<Page<CareScheduleResponse>> list(
            @RequestParam(required = false) CareScheduleStatus status,
            @RequestParam(required = false) CareType careType,
            @RequestParam(required = false) Long horseId,
            @RequestParam(required = false) Long veterinarianId,
            @RequestParam(required = false) Long admissionId,
            Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        if ("VETERINARIAN".equals(currentUser.getRole())) {
            veterinarianId = currentUser.getUserId();
        }
        if ("HEAD_TRAINER".equals(currentUser.getRole()) || "GROOM".equals(currentUser.getRole())) {
            return ApiResponse.success(careScheduleService.listSchedulesForAdmissionAssignee(status, careType,
                    horseId, veterinarianId, admissionId, currentUser.getUserId(), currentUser.getRole(), pageable));
        }
        return ApiResponse.success(careScheduleService.listSchedules(status, careType, horseId, veterinarianId, admissionId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VET_EXAM_VIEW')")
    public ApiResponse<CareScheduleDetailResponse> getById(@PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        if ("VETERINARIAN".equals(currentUser.getRole())) {
            return ApiResponse.success(careScheduleService.getAssignedScheduleDetail(id, currentUser.getUserId()));
        }
        if ("HEAD_TRAINER".equals(currentUser.getRole()) || "GROOM".equals(currentUser.getRole())) {
            return ApiResponse.success(careScheduleService.getScheduleDetailForAdmissionAssignee(
                    id, currentUser.getUserId(), currentUser.getRole()));
        }
        return ApiResponse.success(careScheduleService.getScheduleDetail(id));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('VET_EXAM_MANAGE') or hasAuthority('ROLE_VETERINARIAN') or principal.role == 'VETERINARIAN'")
    public ApiResponse<CareScheduleResponse> start(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(careScheduleService.startCareSchedule(id, currentUser.getUserId()));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('VET_EXAM_MANAGE') or hasAuthority('ROLE_VETERINARIAN') or principal.role == 'VETERINARIAN'")
    public ApiResponse<CareScheduleResponse> complete(
            @PathVariable Long id,
            @Valid @RequestBody CompleteCareScheduleRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(careScheduleService.completeCareSchedule(id, request, currentUser.getUserId()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CLUB_MANAGER') or (hasAuthority('VET_EXAM_MANAGE') and principal.role == 'CLUB_MANAGER')")
    public ApiResponse<CareScheduleResponse> cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelCareScheduleRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(careScheduleService.cancelCareSchedule(id, request, currentUser.getUserId()));
    }

    @PostMapping("/create-next")
    @PreAuthorize("hasAuthority('VET_EXAM_MANAGE') or hasAuthority('ROLE_VETERINARIAN') or principal.role == 'VETERINARIAN'")
    public ApiResponse<CareScheduleResponse> createNext(
            @Valid @RequestBody CreateNextScheduleRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(careScheduleService.createNextSchedule(request, currentUser.getUserId()));
    }
}
