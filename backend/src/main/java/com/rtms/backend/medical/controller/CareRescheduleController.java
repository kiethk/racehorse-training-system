package com.rtms.backend.medical.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.medical.dto.RescheduleCareScheduleRequest;
import com.rtms.backend.medical.dto.RescheduleCareScheduleResponse;
import com.rtms.backend.medical.service.CareRescheduleService;
import com.rtms.backend.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/care-schedules")
public class CareRescheduleController {

    private final CareRescheduleService rescheduleService;

    public CareRescheduleController(CareRescheduleService rescheduleService) {
        this.rescheduleService = rescheduleService;
    }

    /** Vet được giao (lịch SCHEDULED) hoặc Manager; service kiểm tra chi tiết theo trạng thái. */
    @PatchMapping("/{id}/reschedule")
    @PreAuthorize("hasRole('CLUB_MANAGER') or principal.role == 'VETERINARIAN'")
    public ApiResponse<RescheduleCareScheduleResponse> reschedule(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleCareScheduleRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(rescheduleService.reschedule(id, request, currentUser));
    }
}
