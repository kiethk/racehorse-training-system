import { apiGet, apiGetBlob, apiPost } from '@/services/api';
import {
  AuditLogItem,
  AuditLogFilters,
  PageResponse,
  AuditArchiveStatus,
  AuditPurgeRequest,
  AuditPurgeResponse,
} from '../types';

interface ApiResponse<T> {
  data: T;
  message?: string;
  error?: string;
}

export const auditLogApi = {
  getAuditLogs: async (filters: AuditLogFilters): Promise<PageResponse<AuditLogItem>> => {
    const params = new URLSearchParams();
    
    if (filters.search.trim()) params.set('search', filters.search.trim());
    if (filters.httpMethod && filters.httpMethod !== 'ALL') params.set('httpMethod', filters.httpMethod);
    if (filters.statusCode && filters.statusCode !== 'ALL') params.set('statusCode', filters.statusCode);
    if (filters.module && filters.module !== 'ALL') params.set('module', filters.module);
    if (filters.role && filters.role !== 'ALL') params.set('role', filters.role);
    
    if (filters.from) {
      const fromDate = new Date(filters.from);
      params.set('from', fromDate.toISOString());
    }
    
    if (filters.to) {
      const toDate = new Date(filters.to);
      toDate.setHours(23, 59, 59, 999);
      params.set('to', toDate.toISOString());
    }
    
    params.set('page', String(filters.page));
    params.set('size', String(filters.size));
    
    const response = await apiGet<ApiResponse<PageResponse<AuditLogItem>>>(`/api/manager/audit-logs?${params}`);
    return response.data;
  },

  getArchiveStatus: async (): Promise<AuditArchiveStatus> => {
    const response = await apiGet<ApiResponse<AuditArchiveStatus>>('/api/manager/audit-logs/archive-status');
    return response.data;
  },

  downloadArchiveCsv: async (params?: { from?: string; to?: string; before?: string }): Promise<void> => {
    const query = new URLSearchParams();
    if (params?.from) query.set('from', params.from);
    if (params?.to) query.set('to', params.to);
    if (params?.before) query.set('before', params.before);

    const qs = query.toString();
    const path = `/api/manager/audit-logs/export-archive${qs ? `?${qs}` : ''}`;
    const blob = await apiGetBlob(path);

    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '');
    a.download = `audit_archive_${dateStr}.csv`;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  },

  purgeAuditLogs: async (request: AuditPurgeRequest): Promise<AuditPurgeResponse> => {
    const response = await apiPost<ApiResponse<AuditPurgeResponse>>('/api/manager/audit-logs/purge', request);
    return response.data;
  },
};
