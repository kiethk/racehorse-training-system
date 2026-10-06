import { apiGet, apiPost } from '@/services/api';
import type { AdmissionSummaryResponse } from '../types';
import type { TrainerAdmissionView, TrainerReviewRequest } from '../types/trainer';

/** Khớp dto/ApiResponse.java — { success, data, message }. */
interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

const API_URL = process.env.NEXT_PUBLIC_API_URL;

export const trainerAdmissionsApi = {
  /**
   * TẤT CẢ hồ sơ tiếp nhận — màn hình tự chia thành "chờ đánh giá" và
   * "đã đánh giá".
   *
   * Vì sao không lọc sẵn theo status ở đây: sau khi Trainer đánh giá xong,
   * hồ sơ chuyển sang bước Quản lý nên không còn trạng thái nào nghĩa là
   * "Trainer đã duyệt". Lấy hết rồi chia ở client là cách rẻ nhất để vẫn
   * xem lại được hồ sơ cũ, mà chỉ tốn một lời gọi.
   */
  getAll: async (): Promise<AdmissionSummaryResponse[]> => {
    const res = await apiGet<ApiResponse<AdmissionSummaryResponse[]>>('/api/admissions');
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