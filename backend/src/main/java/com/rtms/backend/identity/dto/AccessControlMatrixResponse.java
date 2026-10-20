package com.rtms.backend.identity.dto;

import java.util.List;

public class AccessControlMatrixResponse {

    private List<RolePermissionSummary> roles;
    private List<PermissionSummary> permissions;

    public AccessControlMatrixResponse() {}

    public AccessControlMatrixResponse(List<RolePermissionSummary> roles, List<PermissionSummary> permissions) {
        this.roles = roles;
        this.permissions = permissions;
    }

    public List<RolePermissionSummary> getRoles() {
        return roles;
    }

    public void setRoles(List<RolePermissionSummary> roles) {
        this.roles = roles;
    }

    public List<PermissionSummary> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PermissionSummary> permissions) {
        this.permissions = permissions;
    }

    public static class RolePermissionSummary {
        private Long roleId;
        private String roleName;
        private List<String> permissionCodes;

        public RolePermissionSummary(Long roleId, String roleName, List<String> permissionCodes) {
            this.roleId = roleId;
            this.roleName = roleName;
            this.permissionCodes = permissionCodes;
        }

        public Long getRoleId() { return roleId; }
        public void setRoleId(Long roleId) { this.roleId = roleId; }
        public String getRoleName() { return roleName; }
        public void setRoleName(String roleName) { this.roleName = roleName; }
        public List<String> getPermissionCodes() { return permissionCodes; }
        public void setPermissionCodes(List<String> permissionCodes) { this.permissionCodes = permissionCodes; }
    }

    public static class PermissionSummary {
        private Long id;
        private String code;
        private String description;

        public PermissionSummary(Long id, String code, String description) {
            this.id = id;
            this.code = code;
            this.description = description;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
