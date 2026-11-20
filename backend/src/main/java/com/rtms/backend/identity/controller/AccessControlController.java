package com.rtms.backend.identity.controller;

import com.rtms.backend.identity.dto.AccessControlMatrixResponse;
import com.rtms.backend.identity.dto.BulkRolePermissionUpdateRequest;
import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.identity.dto.RolePermissionUpdateRequest;
import com.rtms.backend.identity.service.AccessControlService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PutMapping("/roles/permissions/bulk")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<List<AccessControlMatrixResponse.RolePermissionSummary>> updateBulkRolePermissions(
            @RequestBody List<BulkRolePermissionUpdateRequest> requests) {
        return ApiResponse.success(accessControlService.updateBulkRolePermissions(requests));
    }
}
