import { apiGet, apiPost, apiUpload } from '@/services/api';
import type { IncidentReport, CreateIncidentRequest } from '../types';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}


export const incidentApi = {
  list: async (params?: { horseId?: number; status?: string } | string): Promise<IncidentReport[]> => {
    let url = '/api/incidents';
    if (typeof params === 'string') {
      url += `?status=${encodeURIComponent(params)}`;
    } else if (params) {
      const q = new URLSearchParams();
      if (params.horseId) q.append('horseId', String(params.horseId));
      if (params.status) q.append('status', params.status);
      const qs = q.toString();
      if (qs) url += `?${qs}`;
    }
    return (await apiGet<ApiResponse<IncidentReport[]>>(url)).data;
  },

  create: async (body: CreateIncidentRequest): Promise<IncidentReport> =>
    (await apiPost<ApiResponse<IncidentReport>>('/api/groom-incident-reports', body)).data,

  /**
   * Attach an image after create returns the report id.
   * apiUpload handles the multipart boundary, credentials, and error message
   * parsing from the response body.
   */
  uploadImage: async (reportId: number, file: File): Promise<IncidentReport> => {
    const body = new FormData();
    body.append('file', file);
    const res = await apiUpload<ApiResponse<IncidentReport>>(
      `/api/groom-incident-reports/${reportId}/image`,
      body,
    );
    return res.data;
  },

  /**
   * API path of the report's image, or null when it has none. The endpoint checks the viewer,
   * so render it with <ProtectedImage>, never a plain <img>.
   */
  imageSrc: (report: IncidentReport): string | null =>
    report.imageUrl ? `/api/groom-incident-reports/${report.id}/image` : null,
};
