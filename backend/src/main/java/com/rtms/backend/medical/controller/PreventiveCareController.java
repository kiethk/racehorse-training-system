package com.rtms.backend.medical.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.medical.dto.CompletePreventiveCareScheduleRequest;
import com.rtms.backend.medical.dto.CreatePreventiveCareScheduleRequest;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.medical.service.PreventiveCareService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.AccessDeniedException;
import java.util.List;

/**
 * @deprecated Thay bằng hub mới (docs/preventive-care-flow.md mục 6): {@link PreventiveCareHubController}
 * cho trạng thái tiêm / ghi nhận hàng loạt, và {@code POST /api/care-schedules/{id}/complete} kèm
 * {@code vaccinations[]}. Giữ lại tới khi FE bỏ {@code SchedulePreventiveCareModal} và
 * {@code HorseHealthSummary} chuyển sang {@code GET /api/horses/{id}/preventive-status}.
 */
@Deprecated
@RestController
@RequestMapping("/api/preventive-care-schedules")
public class PreventiveCareController {

    private final PreventiveCareService preventiveCareService;

    public PreventiveCareController(PreventiveCareService preventiveCareService) {
        this.preventiveCareService = preventiveCareService;
    }

    @PreAuthorize("hasAuthority('PREVENTIVE_CARE_CREATE')")
    @PostMapping
    public ApiResponse<CareSchedule> createSchedule(
            @RequestBody CreatePreventiveCareScheduleRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(preventiveCareService.createSchedule(request, currentUser));
    }

    @PreAuthorize("hasAuthority('PREVENTIVE_CARE_VIEW')")
    @GetMapping
    public ApiResponse<List<CareSchedule>> getUpcomingByHorse(@RequestParam Long horseId)
            throws AccessDeniedException {
        AuthenticatedUser currentUser = (AuthenticatedUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return ApiResponse.success(preventiveCareService.getSchedulesByHorse(horseId, currentUser));
    }

    @PreAuthorize("hasAuthority('PREVENTIVE_CARE_CREATE')")
    @PostMapping("/{id}/records")
    public ApiResponse<HealthRecord> recordCompletion(@PathVariable Long id,
            @RequestBody CompletePreventiveCareScheduleRequest request) {
        AuthenticatedUser currentUser = (AuthenticatedUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        HealthRecord savedRecord = preventiveCareService.recordCompletion(id, request, currentUser);
        return ApiResponse.success(savedRecord);
    }
}
