import { apiGet, apiPost, apiUpload } from '@/services/api';
import type { IncidentReport, CreateIncidentRequest } from '../types';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

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
    (await apiPost<ApiResponse<IncidentReport>>('/api/incidents', body)).data,

  /**
   * Attach an image after create returns the report id.
   * apiUpload handles the multipart boundary, credentials, and error message
   * parsing from the response body.
   */
  uploadImage: async (reportId: number, file: File): Promise<IncidentReport> => {
    const body = new FormData();
    body.append('file', file);
    const res = await apiUpload<ApiResponse<IncidentReport>>(
      `/api/incidents/${reportId}/image`,
      body,
    );
    return res.data;
  },

  /**
   * Resolve the image URL for display.
   * A "local:" imageUrl points to the system endpoint; external URLs are used as-is.
   */
  imageSrc: (report: IncidentReport): string | null => {
    if (!report.imageUrl) return null;
    return report.imageUrl.startsWith('local:')
      ? `${API_URL}/api/incidents/${report.id}/image`
      : report.imageUrl;
  },
};
