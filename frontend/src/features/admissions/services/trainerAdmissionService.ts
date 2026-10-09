import { apiGet, apiPost } from '@/services/api';
import type {
  TrainerAdmissionQueue,
  TrainerAdmissionView,
  TrainerReviewRequest,
} from '../types/trainer';

/** Matches dto/ApiResponse.java — { success, data, message }. */
interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

const API_URL = process.env.NEXT_PUBLIC_API_URL;

export const trainerAdmissionsApi = {
  /**
   * The signed-in Trainer's OWN queue — both groups in one call.
   */
  getQueue: async (): Promise<TrainerAdmissionQueue> => {
    const res = await apiGet<ApiResponse<TrainerAdmissionQueue>>(
      '/api/admissions/trainer/queue',
    );
    return res.data;
  },

  /** Candidate dossier — everything the Trainer needs in ONE call. */
  getView: async (id: number): Promise<TrainerAdmissionView> => {
    const res = await apiGet<ApiResponse<TrainerAdmissionView>>(
      `/api/admissions/${id}/trainer-view`,
    );
    return res.data;
  },

  /** Submit the evaluation -> the backend moves the admission to MANAGER_REVIEW ITSELF. */
  submitReview: async (id: number, body: TrainerReviewRequest): Promise<void> => {
    await apiPost<ApiResponse<unknown>>(
      `/api/admissions/${id}/trainer-review`,
      body,
    );
  },

  /** Document download link — opened with an <a> tag; the jwt_token cookie is sent automatically. */
  documentFileUrl: (admissionId: number, documentId: number): string =>
    `${API_URL}/api/admissions/${admissionId}/documents/${documentId}/file`,
};