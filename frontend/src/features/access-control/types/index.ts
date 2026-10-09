export interface PermissionSummary {
  id: number;
  code: string;
  description: string;
}

export interface RolePermissionSummary {
  roleId: number;
  roleName: string;
  permissionCodes: string[];
}

export interface AccessControlMatrixResponse {
  roles: RolePermissionSummary[];
  permissions: PermissionSummary[];
}

export interface RolePermissionUpdateRequest {
  permissionCodes: string[];
}
