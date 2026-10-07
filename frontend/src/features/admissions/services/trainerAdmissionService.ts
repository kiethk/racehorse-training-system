import { apiGet, apiPost } from '@/services/api';
import type {
  TrainerAdmissionQueue,
  TrainerAdmissionView,
  TrainerReviewRequest,
} from '../types/trainer';

/** Khớp dto/ApiResponse.java — { success, data, message }. */
interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

const API_URL = process.env.NEXT_PUBLIC_API_URL;

export const trainerAdmissionsApi = {
  /**
   * Hàng chờ của CHÍNH Trainer đang đăng nhập — hai nhóm trong một lời gọi.
   */
  getQueue: async (): Promise<TrainerAdmissionQueue> => {
    const res = await apiGet<ApiResponse<TrainerAdmissionQueue>>(
      '/api/admissions/trainer/queue',
    );
    return res.data;
  },

  /** Hồ sơ ứng viên — gộp mọi thứ Trainer cần vào MỘT lời gọi. */
  getView: async (id: number): Promise<TrainerAdmissionView> => {
    const res = await apiGet<ApiResponse<TrainerAdmissionView>>(
      `/api/admissions/${id}/trainer-view`,
    );
    return res.data;
  },

  /** Nộp đánh giá -> backend TỰ chuyển đơn sang MANAGER_REVIEW. */
  submitReview: async (id: number, body: TrainerReviewRequest): Promise<void> => {
    await apiPost<ApiResponse<unknown>>(
      `/api/admissions/${id}/trainer-review`,
      body,
    );
  },

  /** Link tải giấy tờ — mở bằng thẻ <a>, cookie jwt_token tự gửi kèm. */
  documentFileUrl: (admissionId: number, documentId: number): string =>
    `${API_URL}/api/admissions/${admissionId}/documents/${documentId}/file`,
};