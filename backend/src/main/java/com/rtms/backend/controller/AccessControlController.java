package com.rtms.backend.controller;

import com.rtms.backend.dto.AccessControlMatrixResponse;
import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.dto.RolePermissionUpdateRequest;
import com.rtms.backend.service.AccessControlService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/access-control")
public class AccessControlController {

    private final AccessControlService accessControlService;

    public AccessControlController(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<AccessControlMatrixResponse> getAccessControlMatrix() {
        return ApiResponse.success(accessControlService.getAccessControlMatrix());
    }

    @PutMapping("/roles/{roleId}/permissions")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<AccessControlMatrixResponse.RolePermissionSummary> updateRolePermissions(
            @PathVariable Long roleId,
            @RequestBody RolePermissionUpdateRequest request) {
        return ApiResponse.success(accessControlService.updateRolePermissions(roleId, request));
    }
}
