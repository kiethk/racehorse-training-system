import { apiGet, apiPut } from '@/services/api';
import type { Area, Horse, StableStall, UserSummary, HorseStatus } from '../types';

interface ApiResponse<T> { success: boolean; data: T; message?: string; }

/**
 * Hàm putWithMessage cục bộ ĐÃ XOÁ — services/api.ts giờ đã có apiPut, và
 * responseError() ở đó đọc payload.message nên lỗi nghiệp vụ (BR-07 "Chuồng
 * X đang có chiến mã Y") vẫn hiện nguyên văn.
 *
 * Quan trọng hơn chuyện gọn gàng: hàm cũ tự gọi fetch() nên KHÔNG đi qua
 * doFetch(), tức là bỏ qua luôn cơ chế tự làm mới token khi gặp 401. JWT hết
 * hạn giữa lúc đang xem sơ đồ chuồng thì thao tác xếp ngựa thất bại thẳng,
 * trong khi mọi lời gọi qua apiGet/apiPut đều tự refresh rồi thử lại một lần.
 *
 * Body {} là chỗ giữ chỗ: hai endpoint dưới đây nhận @RequestParam, không có
 * @RequestBody, nên Spring bỏ qua phần thân.
 */
export const stableApi = {
  getAreas: async (): Promise<Area[]> =>
    (await apiGet<ApiResponse<Area[]>>('/api/areas')).data,

  getStalls: async (areaCode?: string): Promise<StableStall[]> => {
    const url = areaCode ? `/api/stalls?areaCode=${areaCode}` : '/api/stalls';
    return (await apiGet<ApiResponse<StableStall[]>>(url)).data;
  },

  /** mine=true -> chỉ ngựa trong khu Trainer phụ trách (BE-2.1). */
  /**
   * mine       -> ngựa trong khu Trainer phụ trách (đã xếp chuồng).
   * unassigned -> ngựa CHƯA xếp chuồng, dùng cho hộp thoại xếp ngựa.
   *
   * Hai cờ loại trừ nhau: "khu của tôi" suy ra TỪ chuồng, nên ngựa chưa có
   * chuồng không bao giờ thoả mine=true. Truyền cả hai sẽ luôn ra rỗng.
   */
  getHorses: async (opts?: {
    mine?: boolean;
    unassigned?: boolean;
    status?: HorseStatus;
  }): Promise<Horse[]> => {
    const params = new URLSearchParams();
    if (opts?.mine) params.set('mine', 'true');
    if (opts?.unassigned) params.set('unassigned', 'true');
    if (opts?.status) params.set('status', opts.status);
    const qs = params.toString();
    return (await apiGet<ApiResponse<Horse[]>>(`/api/horses${qs ? `?${qs}` : ''}`)).data;
  },

  getGrooms: async (): Promise<UserSummary[]> =>
    (await apiGet<ApiResponse<UserSummary[]>>('/api/users?role=GROOM')).data,

  /** LƯU Ý: backend nhận @RequestParam, KHÔNG phải body. */
  assignHorseToStall: async (horseId: number, stallId: number): Promise<void> => {
    await apiPut(`/api/horses/${horseId}/assign-stall?stallId=${stallId}`, {});
  },

  /**
   * Gỡ chiến mã khỏi chuồng — bỏ trống stallId.
   *
   * Các buổi tập chưa diễn ra sẽ thành "chưa phân công Groom".
   */
  unassignHorseFromStall: async (horseId: number): Promise<void> => {
    await apiPut(`/api/horses/${horseId}/assign-stall`, {});
  },

  // ĐÃ XOÁ: assignGroomToStall — phân công Groom cho chuồng chuyển sang Quản
  // lý câu lạc bộ (V63). PUT /api/stalls/{id}/assign-groom vẫn tồn tại nhưng
  // đòi quyền STALL_GROOM_ASSIGN mà Huấn luyện viên không có, nên gọi từ đây
  // chỉ nhận 403. Trainer vẫn đổi chuồng cho ngựa bằng assignHorseToStall.
};
