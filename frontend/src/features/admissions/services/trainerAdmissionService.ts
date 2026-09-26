import { apiGet } from '@/services/api';
import type { AdmissionSummaryResponse } from '../types';
import type { TrainerAdmissionView, TrainerReviewRequest } from '../types/trainer';

/** Khớp dto/ApiResponse.java — { success, data, message }. */
interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

const API_URL = process.env.NEXT_PUBLIC_API_URL;

/**
 * TODO(nhóm): xoá hàm này khi services/api.ts được sửa để giữ lại message lỗi.
 *
 * NGOẠI LỆ CÓ CHỦ ĐÍCH so với FRONTEND_GUIDE.md §8.
 *
 * apiPost dùng chung VỨT BỎ body lỗi — nó chỉ ném new Error("API error: 400").
 * Màn hình này bắt buộc hiện nguyên văn lỗi nghiệp vụ, ví dụ:
 *   "Đơn đang ở bước MANAGER_REVIEW, không phải TRAINER_REVIEW — không thể đánh giá!"
 * Dùng apiPost thì vi phạm §12 (hiện lỗi) và §19 (không được nuốt lỗi).
 *
 * apiUpload trong CHÍNH services/api.ts đã xử lý đúng — xem đề xuất ở PHẦN D.
 * Hàm này đặt trong tầng service, KHÔNG đặt trong component.
 */
async function postWithMessage<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',      // bắt buộc — cookie jwt_token là HttpOnly
    body: JSON.stringify(body),
  });
  const payload = await res.json().catch(() => null);
  if (!res.ok) {
    throw new Error(payload?.message || `API error: ${res.status}`);
  }
  return payload as T;
}

export const trainerAdmissionsApi = {
  /** Hàng đợi đơn đang chờ Trainer đánh giá. */
  getQueue: async (): Promise<AdmissionSummaryResponse[]> => {
    const res = await apiGet<ApiResponse<AdmissionSummaryResponse[]>>(
      '/api/admissions?status=TRAINER_REVIEW',
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
    await postWithMessage<ApiResponse<unknown>>(
      `/api/admissions/${id}/trainer-review`,
      body,
    );
  },

  /** Link tải giấy tờ — mở bằng thẻ <a>, cookie jwt_token tự gửi kèm. */
  documentFileUrl: (admissionId: number, documentId: number): string =>
    `${API_URL}/api/admissions/${admissionId}/documents/${documentId}/file`,
};