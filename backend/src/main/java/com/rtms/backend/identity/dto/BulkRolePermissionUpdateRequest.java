package com.rtms.backend.identity.dto;

import java.util.List;

public class BulkRolePermissionUpdateRequest {

    private Long roleId;
    private List<String> permissionCodes;

    public BulkRolePermissionUpdateRequest() {
    }

    public BulkRolePermissionUpdateRequest(Long roleId, List<String> permissionCodes) {
        this.roleId = roleId;
        this.permissionCodes = permissionCodes;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public List<String> getPermissionCodes() {
        return permissionCodes;
    }

    public void setPermissionCodes(List<String> permissionCodes) {
        this.permissionCodes = permissionCodes;
    }
}
