import { apiGet, apiPost } from '@/services/api';
import { AdmissionSummaryResponse, AdmissionDetailResponse, GroomQueueFilters, GroomQueueResponse, GroomReviewRequest, ManagerReviewRequest } from '../types';

interface ApiResponse<T> {
  data: T;
  message?: string;
  error?: string;
}

export const admissionsApi = {
  getGroomQueue: async (filters: GroomQueueFilters): Promise<GroomQueueResponse> => {
    const params = new URLSearchParams();
    if (filters.candidateName.trim()) params.set('candidateName', filters.candidateName.trim());
    if (filters.status) params.set('status', filters.status);
    if (filters.submittedFrom) params.set('submittedFrom', filters.submittedFrom);
    if (filters.submittedTo) params.set('submittedTo', filters.submittedTo);
    params.set('page', String(filters.page));
    params.set('size', '10');
    const response = await apiGet<ApiResponse<GroomQueueResponse>>(`/api/admissions/groom/queue?${params}`);
    return response.data;
  },
  getAdmissions: async (status?: string): Promise<AdmissionSummaryResponse[]> => {
    const url = status ? `/api/admissions?status=${status}` : '/api/admissions';
    const response = await apiGet<ApiResponse<AdmissionSummaryResponse[]>>(url);
    return response.data;
  },
  getAdmissionDetail: async (id: number): Promise<AdmissionDetailResponse> => {
    const response = await apiGet<ApiResponse<AdmissionDetailResponse>>(`/api/admissions/${id}`);
    return response.data;
  },
  managerReview: async (id: number, request: ManagerReviewRequest): Promise<void> => {
    await apiPost<ApiResponse<void>>(`/api/admissions/${id}/manager-review`, request);
  },
  groomReview: async (id: number, request: GroomReviewRequest): Promise<AdmissionDetailResponse> => {
    const response = await apiPost<ApiResponse<AdmissionDetailResponse>>(`/api/admissions/${id}/groom-review`, request);
    return response.data;
  },
  retryQuarantineAllocation: async (id: number): Promise<AdmissionDetailResponse> => {
    const response = await apiPost<ApiResponse<AdmissionDetailResponse>>(`/api/admissions/${id}/quarantine-allocation`, {});
    return response.data;
  },
  assetUrl: (url: string): string => {
    if (/^https?:\/\//i.test(url)) return url;
    const baseUrl = process.env.NEXT_PUBLIC_API_URL ?? '';
    return `${baseUrl.replace(/\/$/, '')}/${url.replace(/^\//, '')}`;
  }
};
