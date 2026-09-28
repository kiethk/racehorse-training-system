package com.rtms.backend.controller;

import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.dto.StaffCreationRequest;
import com.rtms.backend.dto.StaffStatusUpdateRequest;
import com.rtms.backend.dto.StaffSummaryResponse;
import com.rtms.backend.service.StaffManagementService;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<StaffSummaryResponse> createStaff(@RequestBody StaffCreationRequest request) {
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
