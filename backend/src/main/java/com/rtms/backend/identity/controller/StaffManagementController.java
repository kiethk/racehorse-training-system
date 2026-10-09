package com.rtms.backend.identity.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.identity.dto.StaffCreationRequest;
import com.rtms.backend.identity.dto.StaffCreationResponse;
import com.rtms.backend.identity.dto.StaffDetailResponse;
import com.rtms.backend.identity.dto.StaffStatusUpdateRequest;
import com.rtms.backend.identity.dto.StaffSummaryResponse;
import com.rtms.backend.identity.dto.StaffUpdateRequest;
import com.rtms.backend.identity.service.StaffManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
public class StaffManagementController {

    private final StaffManagementService staffManagementService;

    public StaffManagementController(StaffManagementService staffManagementService) {
        this.staffManagementService = staffManagementService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<List<StaffSummaryResponse>> getAllStaff() {
        return ApiResponse.success(staffManagementService.getAllStaff());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<StaffDetailResponse> getStaffDetail(@PathVariable Long id) {
        return ApiResponse.success(staffManagementService.getStaffDetail(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<StaffDetailResponse> updateStaff(
            @PathVariable Long id,
            @RequestBody StaffUpdateRequest request) {
        return ApiResponse.success(staffManagementService.updateStaff(id, request));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<StaffCreationResponse> createStaff(@RequestBody StaffCreationRequest request) {
        return ApiResponse.success(staffManagementService.createStaff(request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<StaffSummaryResponse> updateStaffStatus(
            @PathVariable Long id,
            @RequestBody StaffStatusUpdateRequest request) {
        return ApiResponse.success(staffManagementService.updateStaffStatus(id, request.isActive()));
    }
}
