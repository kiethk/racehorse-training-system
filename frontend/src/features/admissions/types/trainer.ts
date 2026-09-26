import type { AdmissionDetailResponse } from './index';

/**
 * Khớp enums/RacingReadinessStatus.java.
 * DB có CHECK constraint chk_rra_status chỉ nhận đúng 3 giá trị này.
 *
 * UNSUITABLE đổi tên từ NOT_READY (V47) vì tên cũ không phân biệt được với
 * NEEDS_MORE_TRAINING. Đây là kênh DUY NHẤT để Trainer báo hiệu "không nên
 * nhận con này" — Trainer không có quyền từ chối đơn.
 */
export type RacingReadinessStatus = 'READY' | 'NEEDS_MORE_TRAINING' | 'UNSUITABLE';

export interface RacingReadinessAssessment {
  id: number;
  horseId: number;
  readinessStatus: RacingReadinessStatus;
  /** LUÔN null ở bước tiếp nhận — ngựa đang cách ly, không đo được thể lực. */
  fitnessScore: number | null;
  conformationScore: number | null;
  temperamentScore: number | null;
  gaitQualityScore: number | null;
  estimatedMonthsToRace: number | null;
  assessmentDate: string;
  /** null ở bước tiếp nhận (CHECK chk_rra_valid_until). */
  validUntil: string | null;
  remarks: string | null;
  trainerId: number | null;
  /** NOT NULL = đánh giá lúc tiếp nhận. null = đánh giá định kỳ. */
  admissionId: number | null;
  createdAt: string;
}

export interface HorseSummary {
  id: number;
  name: string;
  breed: string | null;
  dateOfBirth: string | null;
  currentStatus: string;
  currentStallId: number | null;
  registryName: string | null;
  registrationNumber: string | null;
}

export interface TrainerAdmissionView {
  admission: AdmissionDetailResponse;
  /** null nếu bước Groom chưa tạo hồ sơ Horse. Khi đó KHÔNG nộp đánh giá được. */
  horse: HorseSummary | null;
  /**
   * Backend đang trả rỗng CỨNG — module Thú y chưa ghi dữ liệu.
   * Đây KHÔNG phải lỗi. Giao diện phải hiện "chưa có dữ liệu",
   * khi nhóm Thú y xong thì backend đổi 2 dòng, frontend không sửa gì.
   */
  healthRecords: unknown[];
  healthMetrics: unknown[];
  /** Khác null = đã đánh giá rồi -> form chuyển sang chỉ đọc. */
  existingAssessment: RacingReadinessAssessment | null;
}

export interface TrainerReviewRequest {
  readinessStatus: RacingReadinessStatus;
  conformationScore?: number | null;
  temperamentScore?: number | null;
  gaitQualityScore?: number | null;
  estimatedMonthsToRace?: number | null;
  remarks?: string | null;
}