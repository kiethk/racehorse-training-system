import { apiGet, apiUpload } from '@/services/api';
import type { AdmissionSummaryResponse, AdmissionStatus } from '../types';
import type { CreateOwnerAdmissionRequest, AdmissionDocumentUpload } from '../types/owner';
import type { HorseSex } from '@/types/horse';

export type CandidateForm = {
  name: string; breed: string; dateOfBirth: string | null;
  sex: HorseSex | null;
  registrationNumber: string | null; registryName: string | null;
  sireName: string | null; sireRegistrationNumber: string | null;
  damName: string | null; damRegistrationNumber: string | null;
  pedigreeNotes: string | null;
};

export type AdmissionDocument = {
  id: number; documentType: string; fileUrl: string; recordDate: string | null;
  note: string | null; uploadedAt: string; medical: boolean;
  originalFileName: string | null;
};

export type OwnerAdmissionDetail = {
  admissionId: number; status: AdmissionStatus; submittedAt: string;
  candidate: CandidateForm; documents: AdmissionDocument[];
  groomFeedback: string | null; vetFeedback: string | null;
  trainerFeedback: string | null; managerFeedback: string | null;
  /** GROOM_REVIEW only: no groom can take a new horse right now. */
  quarantineFull: boolean;
};

export interface OcrExtractionData {
  name?: string | null;
  sex?: HorseSex | null;
  rawSexText?: string | null;
  breed?: string | null;
  dateOfBirth?: string | null;
  registrationNumber?: string | null;
  registryName?: string | null;
  sireName?: string | null;
  sireRegistrationNumber?: string | null;
  damName?: string | null;
  damRegistrationNumber?: string | null;
  fileReference?: {
    tempFileId: string;
    fileUrl: string;
    originalFilename: string;
  } | null;
  confidenceScore?: number | null;
  hasConflict?: boolean;
  conflictMessage?: string | null;
}

export interface OcrExtractionResponse {
  success: boolean;
  message: string;
  data: OcrExtractionData;
  extractedFieldsCount: number;
}

type ApiResponse<T> = { success: boolean; data: T; message: string };

export const ownerAdmissionApi = {
  checkUeln: async (ueln: string) =>
    (await apiGet<ApiResponse<{ available: boolean; message: string | null }>>(
      `/api/owner/admissions/ueln-availability?${new URLSearchParams({ ueln })}`,
    )).data,
  list: async () => (await apiGet<ApiResponse<AdmissionSummaryResponse[]>>('/api/owner/admissions')).data,
  detail: async (id: number) =>
    (await apiGet<ApiResponse<OwnerAdmissionDetail>>(`/api/owner/admissions/${id}`)).data,
  submit: async (candidate: CreateOwnerAdmissionRequest, uploads: AdmissionDocumentUpload[]) => {
    const body = new FormData();
    body.append('candidate', new Blob([JSON.stringify(candidate)], { type: 'application/json' }));
    body.append('documents', new Blob([JSON.stringify(uploads.map(upload => ({
      documentType: upload.documentType,
      recordDate: upload.recordDate || null,
      note: upload.note?.trim() || null,
    })))], { type: 'application/json' }));
    // Metadata at index i describes the file at index i.
    uploads.forEach(upload => body.append('files', upload.file));
    const result = await apiUpload<ApiResponse<OwnerAdmissionDetail>>('/api/owner/admissions', body);
    if (!result?.success) throw new Error(result?.message || 'Unable to submit admission.');
    return result.data;
  },
  /** Edit details and replace/add documents while no groom has reviewed the application (409 afterwards). */
  update: async (id: number, candidate: CreateOwnerAdmissionRequest, uploads: { documentType: string; file: File }[]) => {
    const body = new FormData();
    body.append('candidate', new Blob([JSON.stringify(candidate)], { type: 'application/json' }));
    if (uploads.length > 0) {
      body.append('documentTypes', new Blob([JSON.stringify(uploads.map(u => u.documentType))], { type: 'application/json' }));
      uploads.forEach(upload => body.append('files', upload.file));
    }
    const result = await apiUpload<ApiResponse<OwnerAdmissionDetail>>(`/api/owner/admissions/${id}`, body, 'PUT');
    if (!result?.success) throw new Error(result?.message || 'Unable to update the application.');
    return result.data;
  },
  scanOcr: async (file: File, documentType: string) => {
    const body = new FormData();
    body.append('file', file);
    body.append('documentType', documentType);
    const json = await apiUpload<ApiResponse<OcrExtractionResponse>>('/api/owner/admissions/ocr-scan', body);
    if (!json?.success) throw new Error(json?.message || 'OCR extraction failed');
    return json.data;
  },
};
