package com.rtms.backend.medical.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.medical.dto.HorsePreventiveStatusResponse;
import com.rtms.backend.medical.dto.HorseVaccinationRequest;
import com.rtms.backend.medical.dto.HorseVaccinationResult;
import com.rtms.backend.medical.dto.PreventiveCoverageResponse;
import com.rtms.backend.medical.dto.PreventiveDueHorse;
import com.rtms.backend.medical.dto.PreventiveDueSummary;
import com.rtms.backend.medical.dto.PreventiveProtocolResponse;
import com.rtms.backend.medical.enums.PreventiveDueStatus;
import com.rtms.backend.medical.service.HorseVaccinationService;
import com.rtms.backend.medical.service.PreventiveCareQueryService;
import com.rtms.backend.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/** Hub Preventive Care: phác đồ, việc cần làm, độ phủ, trạng thái từng ngựa, ghi nhận tiêm cho từng ngựa. */
@RestController
@RequestMapping("/api")
public class PreventiveCareHubController {

    /** Danh sách toàn trại: không mở cho chủ ngựa. */
    private static final String STAFF_VIEW =
            "hasAuthority('PREVENTIVE_CARE_VIEW') and principal.role != 'HORSE_OWNER'";

    private final PreventiveCareQueryService queryService;
    private final HorseVaccinationService vaccinationService;

    public PreventiveCareHubController(PreventiveCareQueryService queryService,
            HorseVaccinationService vaccinationService) {
        this.queryService = queryService;
        this.vaccinationService = vaccinationService;
    }

    @GetMapping("/preventive-protocols")
    @PreAuthorize(STAFF_VIEW)
    public ApiResponse<List<PreventiveProtocolResponse>> protocols() {
        return ApiResponse.success(queryService.listProtocols());
    }

    @GetMapping("/preventive-care/due")
    @PreAuthorize(STAFF_VIEW)
    public ApiResponse<Page<PreventiveDueHorse>> due(
            @RequestParam(required = false) Set<PreventiveDueStatus> status,
            @RequestParam(required = false) Long protocolId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(queryService.dueHorses(status, protocolId, search, page, size));
    }

    @GetMapping("/preventive-care/due/summary")
    @PreAuthorize(STAFF_VIEW)
    public ApiResponse<PreventiveDueSummary> dueSummary() {
        return ApiResponse.success(queryService.dueSummary());
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

    @PostMapping("/horses/{horseId}/vaccinations")
    @PreAuthorize("hasAuthority('PREVENTIVE_CARE_CREATE') and principal.role == 'VETERINARIAN'")
    public ApiResponse<HorseVaccinationResult> recordVaccinations(
            @PathVariable Long horseId,
            @Valid @RequestBody HorseVaccinationRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(vaccinationService.record(horseId, request, currentUser.getUserId()));
    }
}
