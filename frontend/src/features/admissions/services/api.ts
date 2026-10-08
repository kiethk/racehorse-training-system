import { apiGet, apiGetBlob, apiPost } from '@/services/api';
import {
  AdmissionSummaryResponse,
  AdmissionDetailResponse,
  AdmissionDocument,
  CareSchedule,
  CareScheduleDetail,
  CareScheduleFilters,
  CompleteCareScheduleRequest,
  GroomQueueFilters,
  GroomQueueResponse,
  GroomReviewRequest,
  HorseHealthMetricResponse,
  ManagerReviewRequest,
  PageResponse,
  UrgentAssignmentAlert,
  VetAdmissionQueueItem,
  VetQueueSummary,
  VetQueueFilters,
  VetReviewRequest,
  VetReviewResponse,
} from '../types';

interface ApiResponse<T> {
  data: T;
  message?: string;
  error?: string;
}

export const admissionsApi = {
  getDocumentFile: (fileUrl: string, signal?: AbortSignal): Promise<Blob> => apiGetBlob(fileUrl, signal),
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
  getDocuments: async (id: number): Promise<AdmissionDocument[]> => {
    const response = await apiGet<ApiResponse<AdmissionDocument[]>>(`/api/admissions/${id}/documents`);
    return response.data;
  },
  getHorseHealthMetrics: async (horseId: number): Promise<HorseHealthMetricResponse[]> => {
    const response = await apiGet<ApiResponse<HorseHealthMetricResponse[]>>(`/api/horses/${horseId}/health-metrics`);
    return response.data;
  },

  getVetQueue: async (filters: VetQueueFilters = {}): Promise<PageResponse<VetAdmissionQueueItem>> => {
    const params = new URLSearchParams();
    if (filters.search?.trim()) params.set('search', filters.search.trim());
    if (filters.pill && filters.pill !== 'ALL') params.set('pill', filters.pill);
    if (filters.admissionStatus && filters.admissionStatus !== 'ALL') params.set('admissionStatus', filters.admissionStatus);
    if (filters.scheduleStatus && filters.scheduleStatus !== 'ALL') params.set('scheduleStatus', filters.scheduleStatus);
    if (filters.careType && filters.careType !== 'ALL') params.set('careType', filters.careType);
    if (filters.priority && filters.priority !== 'ALL') params.set('priority', filters.priority);
    params.set('page', String(filters.page ?? 0));
    params.set('size', String(filters.size ?? 10));
    const response = await apiGet<ApiResponse<PageResponse<VetAdmissionQueueItem>>>(`/api/admissions/vet/queue?${params}`);
    return response.data;
  },

  getVetQueueSummary: async (): Promise<VetQueueSummary> => {
    const response = await apiGet<ApiResponse<VetQueueSummary>>('/api/admissions/vet/summary');
    return response.data;
  },

  getVetAdmissionDetail: async (id: number): Promise<AdmissionDetailResponse> => {
    const response = await apiGet<ApiResponse<AdmissionDetailResponse>>(`/api/admissions/vet/${id}`);
    return response.data;
  },

  getPendingUrgentAlerts: async (): Promise<UrgentAssignmentAlert[]> => {
    const response = await apiGet<ApiResponse<UrgentAssignmentAlert[]>>('/api/care-schedules/urgent-alerts/pending');
    return response.data;
  },

  getUrgentCase: async (scheduleId: number): Promise<UrgentAssignmentAlert> => {
    const response = await apiGet<ApiResponse<UrgentAssignmentAlert>>(`/api/care-schedules/urgent-alerts/${scheduleId}`);
    return response.data;
  },

  getCareSchedules: async (filters: CareScheduleFilters = {}): Promise<PageResponse<CareSchedule>> => {
    const params = new URLSearchParams();
    if (filters.status) params.set('status', filters.status);
    if (filters.careType) params.set('careType', filters.careType);
    if (filters.horseId) params.set('horseId', String(filters.horseId));
    if (filters.admissionId) params.set('admissionId', String(filters.admissionId));
    const vetId = filters.veterinarianId ?? filters.vetId;
    if (vetId) params.set('veterinarianId', String(vetId));
    params.set('page', String(filters.page ?? 0));
    params.set('size', String(filters.size ?? 50));
    const response = await apiGet<unknown>(`/api/care-schedules?${params}`);
    if (response && typeof response === 'object' && 'data' in response) {
      const data = (response as { data: unknown }).data;
      if (data && typeof data === 'object' && 'content' in data && Array.isArray((data as { content: unknown }).content)) {
        return data as PageResponse<CareSchedule>;
      }
      if (Array.isArray(data)) {
        return {
          content: data as CareSchedule[],
          totalElements: data.length,
          totalPages: 1,
          number: 0,
          size: data.length,
        };
      }
    }
    if (response && typeof response === 'object' && 'content' in response && Array.isArray((response as { content: unknown }).content)) {
      return response as PageResponse<CareSchedule>;
    }
    return { content: [], totalElements: 0, totalPages: 0, number: 0, size: 50 };
  },

  getCareScheduleDetail: async (id: number): Promise<CareScheduleDetail> => {
    const response = await apiGet<ApiResponse<CareScheduleDetail>>(`/api/care-schedules/${id}`);
    return response.data;
  },

  startCareSchedule: async (id: number): Promise<CareSchedule> => {
    const response = await apiPost<ApiResponse<CareSchedule> | CareSchedule>(`/api/care-schedules/${id}/start`, {});
    if (response && 'data' in response && response.data) return response.data;
    return response as CareSchedule;
  },

  completeCareSchedule: async (id: number, data: CompleteCareScheduleRequest): Promise<CareSchedule> => {
    const response = await apiPost<ApiResponse<CareSchedule> | CareSchedule>(`/api/care-schedules/${id}/complete`, data);
    if (response && 'data' in response && response.data) return response.data;
    return response as CareSchedule;
  },

  vetReview: async (id: number, request: VetReviewRequest): Promise<VetReviewResponse> => {
    const response = await apiPost<ApiResponse<VetReviewResponse>>(`/api/admissions/${id}/vet-review`, request);
    return response.data;
  },

  managerReview: async (id: number, request: ManagerReviewRequest): Promise<void> => {
    await apiPost<ApiResponse<void>>(`/api/admissions/${id}/manager-review`, request);
  },
  groomReview: async (id: number, request: GroomReviewRequest): Promise<AdmissionDetailResponse> => {
    const response = await apiPost<ApiResponse<AdmissionDetailResponse>>(`/api/admissions/${id}/groom-review`, request);
    return response.data;
  },
  confirmHorseArrival: async (id: number, request: {
    confirmed: boolean;
    feedback?: string;
  }): Promise<AdmissionDetailResponse> => {
    const response = await apiPost<ApiResponse<AdmissionDetailResponse>>(
      '/api/admissions/' + id + '/arrival-confirmation', request);
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
  },
};
