import { apiGet } from '@/services/api';
import { AuditLogItem, AuditLogFilters, PageResponse } from '../types';

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
};
