package com.rtms.backend.identity.dto;

import java.util.List;

public class RolePermissionUpdateRequest {
    private List<String> permissionCodes;

    public List<String> getPermissionCodes() {
        return permissionCodes;
    }

    public void setPermissionCodes(List<String> permissionCodes) {
        this.permissionCodes = permissionCodes;
    }
}
