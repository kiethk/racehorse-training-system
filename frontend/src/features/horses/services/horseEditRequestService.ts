import { apiGet, apiPost, apiUpload } from '@/services/api';
import type { ApiResponse } from '@/types/horse';
import type {
  HorseDocumentSlot,
  HorseDocumentType,
  HorseEditFields,
  HorseEditRequest,
  HorseEditRequestStatus,
} from '../types';

export interface HorseEditUpload {
  documentType: HorseDocumentType;
  file: File;
}

function unwrap<T>(response: ApiResponse<T>, fallback: string): T {
  if (!response?.success) throw new Error(response?.message || fallback);
  return response.data;
}

export const horseEditRequestService = {
  /** Current file per document type (or an empty slot), for owners and the horse's groom. */
  documents: async (horseId: number): Promise<HorseDocumentSlot[]> =>
    (await apiGet<ApiResponse<HorseDocumentSlot[]>>(`/api/horses/${horseId}/documents`)).data,

  /** The horse's most recent change request, or null when there has never been one. */
  latest: async (horseId: number): Promise<HorseEditRequest | null> =>
    (await apiGet<ApiResponse<HorseEditRequest | null>>(`/api/owner/horses/${horseId}/edit-requests/latest`)).data,

  submit: async (horseId: number, changes: HorseEditFields, uploads: HorseEditUpload[]): Promise<HorseEditRequest> => {
    const body = new FormData();
    body.append('changes', new Blob([JSON.stringify(changes)], { type: 'application/json' }));
    if (uploads.length > 0) {
      body.append('documentTypes', new Blob([JSON.stringify(uploads.map((u) => u.documentType))], { type: 'application/json' }));
      uploads.forEach((u) => body.append('files', u.file));
    }
    return unwrap(await apiUpload<ApiResponse<HorseEditRequest>>(`/api/owner/horses/${horseId}/edit-requests`, body),
      'Unable to submit the change request.');
  },

  cancel: async (horseId: number, requestId: number): Promise<HorseEditRequest> =>
    (await apiPost<ApiResponse<HorseEditRequest>>(`/api/owner/horses/${horseId}/edit-requests/${requestId}/cancel`, {})).data,

  listForGroom: async (status?: HorseEditRequestStatus): Promise<HorseEditRequest[]> =>
    (await apiGet<ApiResponse<HorseEditRequest[]>>(
      `/api/groom/horse-edit-requests${status ? `?status=${status}` : ''}`)).data,

  getForGroom: async (requestId: number): Promise<HorseEditRequest> =>
    (await apiGet<ApiResponse<HorseEditRequest>>(`/api/groom/horse-edit-requests/${requestId}`)).data,

  review: async (requestId: number, decision: 'APPROVED' | 'REJECTED', feedback?: string): Promise<HorseEditRequest> =>
    (await apiPost<ApiResponse<HorseEditRequest>>(`/api/groom/horse-edit-requests/${requestId}/review`, { decision, feedback })).data,
};
