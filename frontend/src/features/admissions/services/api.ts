import { apiGet, apiPost } from '@/services/api';
import {
  AdmissionSummaryResponse,
  AdmissionDetailResponse,
  AdmissionDocument,
  HorseHealthMetricResponse,
  ManagerReviewRequest,
  PageResponse,
  VetExamResponse,
  VetExamStatus,
  VetExamType,
  VetReviewRequest,
  VetReviewResponse,
} from '../types';

interface ApiResponse<T> {
  data: T;
  message?: string;
  error?: string;
}

export const admissionsApi = {
  getAdmissions: async (status?: string): Promise<AdmissionSummaryResponse[]> => {
    const url = status ? `/api/admissions?status=${status}` : '/api/admissions';
    const response = await apiGet<ApiResponse<AdmissionSummaryResponse[]>>(url);
    return response.data;
  },
  getAdmissionDetail: async (id: number): Promise<AdmissionDetailResponse> => {
    const response = await apiGet<ApiResponse<AdmissionDetailResponse>>(`/api/admissions/${id}`);
    return response.data;
  },
  getDocuments: async (id: number): Promise<AdmissionDocument[]> => {
    const response = await apiGet<ApiResponse<AdmissionDocument[]>>(`/api/admissions/${id}/documents`);
    return response.data;
  },
  getVetExams: async (filters: {
    status?: VetExamStatus;
    type?: VetExamType;
    page?: number;
    size?: number;
  } = {}): Promise<PageResponse<VetExamResponse>> => {
    const params = new URLSearchParams();
    if (filters.status) params.set('status', filters.status);
    if (filters.type) params.set('type', filters.type);
    params.set('page', String(filters.page ?? 0));
    params.set('size', String(Math.min(filters.size ?? 50, 50)));
    params.append('sort', 'priority,desc');
    params.append('sort', 'createdAt,asc');
    const response = await apiGet<ApiResponse<PageResponse<VetExamResponse>>>(`/api/vet-exams?${params}`);
    return response.data;
  },
  getHorseHealthMetrics: async (horseId: number): Promise<HorseHealthMetricResponse[]> => {
    const response = await apiGet<ApiResponse<HorseHealthMetricResponse[]>>(`/api/horses/${horseId}/health-metrics`);
    return response.data;
  },
  startVetExam: async (id: number): Promise<VetExamResponse> => {
    const response = await apiPost<ApiResponse<VetExamResponse>>(`/api/vet-exams/${id}/start`, {});
    return response.data;
  },
  vetReview: async (id: number, request: VetReviewRequest): Promise<VetReviewResponse> => {
    const response = await apiPost<ApiResponse<VetReviewResponse>>(`/api/admissions/${id}/vet-review`, request);
    return response.data;
  },
  managerReview: async (id: number, request: ManagerReviewRequest): Promise<void> => {
    await apiPost<ApiResponse<void>>(`/api/admissions/${id}/manager-review`, request);
  }
};
