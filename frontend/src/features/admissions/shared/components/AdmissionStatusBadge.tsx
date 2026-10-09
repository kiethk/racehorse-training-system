import { Pill } from '@/components/ui/StatusBadge';
import type { AdmissionStatus } from '../../types';

export function prettyStatus(status: AdmissionStatus | string) {
  return status.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase());
}

type Tone = 'success' | 'warning' | 'danger' | 'info' | 'primary' | 'neutral';

export function statusTone(status: AdmissionStatus | string): Tone {
  if (status === 'GROOM_REVIEW') return 'primary';
  if (status === 'WAITING_FOR_STALL' || status === 'WAITING_FOR_ARRIVAL' || status === 'ARRIVAL_EXPIRED') return 'warning';
  if (status === 'APPROVED') return 'success';
  if (status === 'REJECTED') return 'danger';
  if (status === 'VET_REVIEW' || status === 'TRAINER_REVIEW' || status === 'MANAGER_REVIEW') return 'info';
  return 'neutral';
}

export type SimpleAdmissionStatus = 'IN_PROGRESS' | 'APPROVED' | 'REJECTED';

/**
 * Gộp mọi bước trung gian thành "đang xử lý".
 *
 * Màn Trainer chỉ cần biết kết quả cuối, không cần biết đơn đang chờ ai hay đã
 * qua những bước nào. Manager và Owner KHÔNG dùng hàm này: Manager phải phân
 * biệt được đơn MANAGER_REVIEW (đang chờ mình) với đơn còn ở khâu Thú y.
 */
export function simplifyStatus(status: AdmissionStatus | string): SimpleAdmissionStatus {
  if (status === 'APPROVED') return 'APPROVED';
  if (status === 'REJECTED') return 'REJECTED';
  return 'IN_PROGRESS';
}

const SIMPLE_LABEL: Record<SimpleAdmissionStatus, string> = {
  IN_PROGRESS: 'In Progress',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
};

const SIMPLE_TONE: Record<SimpleAdmissionStatus, Tone> = {
  IN_PROGRESS: 'info',
  APPROVED: 'success',
  REJECTED: 'danger',
};

/** Nhãn 3 trạng thái, dùng cả cho badge lẫn các dòng chữ mô tả trạng thái. */
export function simpleStatusLabel(status: AdmissionStatus | string): string {
  return SIMPLE_LABEL[simplifyStatus(status)];
}

interface AdmissionStatusBadgeProps {
  status: AdmissionStatus | string;
  size?: 'sm' | 'md';
  /**
   * Chỉ hiện 3 trạng thái: In Progress / Approved / Rejected.
   * Mặc định tắt để Manager và Owner giữ nguyên nhãn chi tiết từng bước.
   */
  simplified?: boolean;
}

export function AdmissionStatusBadge({ status, size = 'md', simplified = false }: AdmissionStatusBadgeProps) {
  const tone = simplified ? SIMPLE_TONE[simplifyStatus(status)] : statusTone(status);
  const label = simplified ? simpleStatusLabel(status) : prettyStatus(status);

  return <Pill tone={tone} size={size}>{label}</Pill>;
}
