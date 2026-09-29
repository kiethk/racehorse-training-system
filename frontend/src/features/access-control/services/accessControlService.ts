import { apiGet, apiPut } from '@/services/api';
import { AccessControlMatrixResponse, RolePermissionUpdateRequest, RolePermissionSummary } from '../types';

interface ApiResponseData<T> {
  success: boolean;
  data: T;
  message?: string;
}

export const accessControlService = {
  getAccessControlMatrix: async (): Promise<AccessControlMatrixResponse> => {
    const res = await apiGet<ApiResponseData<AccessControlMatrixResponse>>('/api/access-control');
    return res.data;
  },

  updateRolePermissions: async (roleId: number, data: RolePermissionUpdateRequest): Promise<RolePermissionSummary> => {
    const res = await apiPut<ApiResponseData<RolePermissionSummary>>(`/api/access-control/roles/${roleId}/permissions`, data);
    return res.data;
  }
};
