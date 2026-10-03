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

/**
 * Hàm postWithMessage cục bộ ĐÃ XOÁ — ngoại lệ so với FRONTEND_GUIDE.md §8
 * không còn lý do tồn tại: responseError() trong services/api.ts giờ đã đọc
 * payload.message, nên apiPost giữ nguyên văn lỗi nghiệp vụ, ví dụ
 * "Hồ sơ này đã được phân công cho Huấn luyện viên khác đánh giá!".
 *
 * Và hàm cũ còn một khiếm khuyết nữa: nó tự gọi fetch() nên không đi qua
 * doFetch(), tức là bỏ qua cơ chế tự làm mới token khi gặp 401. Trainer ngồi
 * nhập ba điểm số rồi bấm nộp đúng lúc JWT vừa hết hạn sẽ mất trắng phần đã
 * nhập, thay vì được refresh rồi gửi lại.
 */
export const trainerAdmissionsApi = {
  /**
   * Hàng chờ của CHÍNH Trainer đang đăng nhập — hai nhóm trong một lời gọi.
   *
   * Thay cho getAll() cũ (GET /api/admissions không truyền status). Cái cũ
   * rơi vào nhánh findAll() của backend, trả về TOÀN BỘ hồ sơ của mọi trạng
   * thái và mọi Trainer, rồi màn hình tự lọc ở client. Hai vấn đề: dữ liệu
   * của Trainer khác vẫn nằm trong phản hồi (chỉ bị ẩn khỏi bảng), và lượng
   * truyền tăng tuyến tính theo số đơn toàn hệ thống.
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
    await apiPost<ApiResponse<unknown>>(`/api/admissions/${id}/trainer-review`, body);
  },

  /** Link tải giấy tờ — mở bằng thẻ <a>, cookie jwt_token tự gửi kèm. */
  documentFileUrl: (admissionId: number, documentId: number): string =>
    `${API_URL}/api/admissions/${admissionId}/documents/${documentId}/file`,
};