package com.rtms.backend.medical.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.medical.dto.BatchVaccinationRequest;
import com.rtms.backend.medical.dto.BatchVaccinationResult;
import com.rtms.backend.medical.dto.HorsePreventiveStatusResponse;
import com.rtms.backend.medical.dto.PreventiveCoverageResponse;
import com.rtms.backend.medical.dto.PreventiveDueItem;
import com.rtms.backend.medical.dto.PreventiveProtocolResponse;
import com.rtms.backend.medical.enums.PreventiveDueStatus;
import com.rtms.backend.medical.service.BatchVaccinationService;
import com.rtms.backend.medical.service.PreventiveCareQueryService;
import com.rtms.backend.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/** Hub Preventive Care: phác đồ, việc cần làm, độ phủ, trạng thái từng ngựa, ghi nhận hàng loạt. */
@RestController
@RequestMapping("/api")
public class PreventiveCareHubController {

    /** Danh sách toàn trại: không mở cho chủ ngựa. */
    private static final String STAFF_VIEW =
            "hasAuthority('PREVENTIVE_CARE_VIEW') and principal.role != 'HORSE_OWNER'";

    private final PreventiveCareQueryService queryService;
    private final BatchVaccinationService batchService;

    public PreventiveCareHubController(PreventiveCareQueryService queryService, BatchVaccinationService batchService) {
        this.queryService = queryService;
        this.batchService = batchService;
    }

    @GetMapping("/preventive-protocols")
    @PreAuthorize(STAFF_VIEW)
    public ApiResponse<List<PreventiveProtocolResponse>> protocols() {
        return ApiResponse.success(queryService.listProtocols());
    }

    @GetMapping("/preventive-care/due")
    @PreAuthorize(STAFF_VIEW)
    public ApiResponse<List<PreventiveDueItem>> due(
            @RequestParam(required = false) Set<PreventiveDueStatus> status,
            @RequestParam(required = false) Integer withinDays,
            @RequestParam(required = false) Long protocolId) {
        return ApiResponse.success(queryService.due(status, withinDays, protocolId));
    }

    @GetMapping("/preventive-care/coverage")
    @PreAuthorize(STAFF_VIEW)
    public ApiResponse<List<PreventiveCoverageResponse>> coverage() {
        return ApiResponse.success(queryService.coverage());
    }

    /** Chủ ngựa chỉ xem được ngựa của mình (kiểm tra trong service). */
    @GetMapping("/horses/{horseId}/preventive-status")
    @PreAuthorize("hasAuthority('PREVENTIVE_CARE_VIEW')")
    public ApiResponse<HorsePreventiveStatusResponse> horseStatus(
            @PathVariable Long horseId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(queryService.horseStatus(horseId, currentUser));
    }

    @PostMapping("/preventive-care/batch-vaccinations")
    @PreAuthorize("hasAuthority('PREVENTIVE_CARE_CREATE') and principal.role == 'VETERINARIAN'")
    public ApiResponse<List<BatchVaccinationResult>> batchVaccinations(
            @Valid @RequestBody BatchVaccinationRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(batchService.record(request, currentUser.getUserId()));
    }
}
