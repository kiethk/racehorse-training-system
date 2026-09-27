import { apiGet, apiPost, apiUpload } from '@/services/api';
import type { IncidentReport, CreateIncidentRequest } from '../types';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export const incidentApi = {
  list: async (status?: string): Promise<IncidentReport[]> => {
    const url = status
      ? `/api/groom-incident-reports?status=${status}`
      : '/api/groom-incident-reports';
    return (await apiGet<ApiResponse<IncidentReport[]>>(url)).data;
  },

  create: async (body: CreateIncidentRequest): Promise<IncidentReport> =>
    (await apiPost<ApiResponse<IncidentReport>>('/api/groom-incident-reports', body)).data,

  /**
   * Đính ảnh — gọi SAU khi create đã trả về id.
   * apiUpload đã xử lý đúng: không đặt Content-Type thủ công (để trình duyệt
   * tự sinh boundary), có credentials, và ĐỌC message lỗi từ body.
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
   * Địa chỉ để hiển thị ảnh.
   * imageUrl bắt đầu bằng "local:" -> ảnh nằm trong hệ thống, gọi endpoint.
   * Ngược lại -> địa chỉ ngoài, dùng thẳng.
   */
  imageSrc: (report: IncidentReport): string | null => {
    if (!report.imageUrl) return null;
    return report.imageUrl.startsWith('local:')
      ? `${API_URL}/api/groom-incident-reports/${report.id}/image`
      : report.imageUrl;
  },
};
