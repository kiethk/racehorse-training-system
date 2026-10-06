package com.rtms.backend.controller;
import com.rtms.backend.dto.CancelCareScheduleRequest;
import com.rtms.backend.dto.CareScheduleDetailResponse;
import com.rtms.backend.dto.CareScheduleResponse;
import com.rtms.backend.dto.CompleteCareScheduleRequest;
import com.rtms.backend.dto.CreateNextScheduleRequest;
import com.rtms.backend.enums.CareScheduleStatus;
import com.rtms.backend.enums.CareType;
import com.rtms.backend.service.CareScheduleService;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/care-schedules", "/api/vet/schedules"})
public class CareScheduleController {

    private final CareScheduleService careScheduleService;

    public CareScheduleController(CareScheduleService careScheduleService) {
        this.careScheduleService = careScheduleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VET_EXAM_VIEW')")
    public ApiResponse<Page<CareScheduleResponse>> list(
            @RequestParam(required = false) CareScheduleStatus status,
            @RequestParam(required = false) CareType careType,
            @RequestParam(required = false) Long horseId,
            @RequestParam(required = false) Long veterinarianId,
            @RequestParam(required = false) Long admissionId,
            Pageable pageable) {
        return ApiResponse.success(careScheduleService.listSchedules(status, careType, horseId, veterinarianId, admissionId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VET_EXAM_VIEW')")
    public ApiResponse<CareScheduleDetailResponse> getById(@PathVariable Long id) {
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
    @PreAuthorize("hasAuthority('VET_EXAM_MANAGE') or principal.role == 'VETERINARIAN' or principal.role == 'CLUB_MANAGER'")
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