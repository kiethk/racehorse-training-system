import { apiGet, apiPost } from '@/services/api';
import type { AdmissionSummaryResponse, AdmissionStatus } from '../types';

export type CandidateForm = {
  name: string; breed: string; dateOfBirth: string | null;
  registrationNumber: string | null; registryName: string | null;
  sireName: string | null; sireRegistrationNumber: string | null;
  damName: string | null; damRegistrationNumber: string | null;
  pedigreeNotes: string | null;
};

export type AdmissionDocument = {
  id: number; documentType: string; fileUrl: string; recordDate: string | null;
  note: string | null; uploadedAt: string; medical: boolean;
};

export type OwnerAdmissionDetail = {
  admissionId: number; status: AdmissionStatus; submittedAt: string;
  candidate: CandidateForm; documents: AdmissionDocument[];
  groomFeedback: string | null; vetFeedback: string | null;
  trainerFeedback: string | null; managerFeedback: string | null;
};

type ApiResponse<T> = { success: boolean; data: T; message: string };
const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export const ownerAdmissionApi = {
  list: async () => (await apiGet<ApiResponse<AdmissionSummaryResponse[]>>('/api/owner/admissions')).data,
  detail: async (id: number) =>
    (await apiGet<ApiResponse<OwnerAdmissionDetail>>(`/api/owner/admissions/${id}`)).data,
  create: async (data: CandidateForm) =>
    (await apiPost<ApiResponse<OwnerAdmissionDetail>>('/api/owner/admissions', data)).data,
  upload: async (id: number, file: File, documentType: string, recordDate: string, note: string) => {
    const body = new FormData();
    body.append('file', file);
    body.append('documentType', documentType);
    if (recordDate) body.append('recordDate', recordDate);
    if (note) body.append('note', note);
    const response = await fetch(`${API_URL}/api/owner/admissions/${id}/documents`, {
      method: 'POST', credentials: 'include', body,
    });
    const json: ApiResponse<AdmissionDocument> = await response.json();
    if (!response.ok || !json.success) throw new Error(json.message || 'Document upload failed');
    return json.data;
  },
};
