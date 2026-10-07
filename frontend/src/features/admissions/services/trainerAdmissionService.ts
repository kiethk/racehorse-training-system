import { apiGet, apiPost } from '@/services/api';
import type {
  TrainerAdmissionQueue,
  TrainerAdmissionView,
  TrainerReviewRequest,
  TrainerScheduleDetailResponse,
  TrainerScheduleResponse,
  TrainerScheduleStatus,
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
   * Lấy danh sách lịch đánh giá của Trainer (/api/trainer-schedules)
   */
  getSchedules: async (status?: TrainerScheduleStatus): Promise<TrainerScheduleResponse[]> => {
    const query = status ? `?status=${status}` : '';
    const res = await apiGet<ApiResponse<TrainerScheduleResponse[]>>(`/api/trainer-schedules${query}`);
    return res.data;
  },

  /**
   * Chi tiết lịch đánh giá gồm đơn nhập viện, hồ sơ ngựa, kết quả khám của Vet (/api/trainer-schedules/:id)
   */
  getScheduleDetail: async (scheduleId: number): Promise<TrainerScheduleDetailResponse> => {
    const res = await apiGet<ApiResponse<TrainerScheduleDetailResponse>>(
      `/api/trainer-schedules/${scheduleId}`,
    );
    return res.data;
  },

  /**
   * Bắt đầu đánh giá (SCHEDULED -> IN_PROGRESS)
   */
  startSchedule: async (scheduleId: number): Promise<TrainerScheduleResponse> => {
    const res = await apiPost<ApiResponse<TrainerScheduleResponse>>(
      `/api/trainer-schedules/${scheduleId}/start`,
      {},
    );
    return res.data;
  },

  /**
   * Hoàn thành đánh giá (IN_PROGRESS -> COMPLETED)
   */
  completeSchedule: async (
    scheduleId: number,
    body: TrainerReviewRequest,
  ): Promise<TrainerScheduleResponse> => {
    const res = await apiPost<ApiResponse<TrainerScheduleResponse>>(
      `/api/trainer-schedules/${scheduleId}/complete`,
      body,
    );
    return res.data;
  },

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