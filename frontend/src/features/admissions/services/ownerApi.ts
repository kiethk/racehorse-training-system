import { apiGet, apiPost, apiUpload } from '@/services/api';
import type {
  ApiResult, CreateOwnerAdmissionRequest, OwnerAdmissionDetail,
  AdmissionSummaryResponse, AdmissionDocument, AdmissionDocumentType,
} from '../types/owner';

const endpoint = '/api/owner/admissions';

export const ownerAdmissionsApi = {
  create: async (request: CreateOwnerAdmissionRequest) =>
    (await apiPost<ApiResult<OwnerAdmissionDetail>>(endpoint, request)).data,
  list: async () =>
    (await apiGet<ApiResult<AdmissionSummaryResponse[]>>(endpoint)).data,
  detail: async (id: number) =>
    (await apiGet<ApiResult<OwnerAdmissionDetail>>(`${endpoint}/${id}`)).data,
  documents: async (id: number) =>
    (await apiGet<ApiResult<AdmissionDocument[]>>(`${endpoint}/${id}/documents`)).data,
  uploadDocument: async (
    id: number, file: File, documentType: AdmissionDocumentType,
    recordDate?: string, note?: string
  ) => {
    const body = new FormData();
    body.append('file', file);
    body.append('documentType', documentType);
    if (recordDate) body.append('recordDate', recordDate);
    if (note) body.append('note', note);
    return (await apiUpload<ApiResult<AdmissionDocument>>(
      `${endpoint}/${id}/documents`, body
    )).data;
  },
};
