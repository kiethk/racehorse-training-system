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
 * Collapses every intermediate step into "in progress".
 *
 * The Trainer screens only need the final outcome, not who the admission is waiting on or
 * which steps it has passed. Manager and Owner do NOT use this: the Manager must be able to
 * tell an admission in MANAGER_REVIEW (waiting on them) from one still at the vet stage.
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

/** Three-state label, used by the badge and by sentences that describe the status. */
export function simpleStatusLabel(status: AdmissionStatus | string): string {
  return SIMPLE_LABEL[simplifyStatus(status)];
}

interface AdmissionStatusBadgeProps {
  status: AdmissionStatus | string;
  size?: 'sm' | 'md';
  /**
   * Show only three states: In Progress / Approved / Rejected.
   * Off by default so Manager and Owner keep the detailed per-step labels.
   */
  simplified?: boolean;
}

export function AdmissionStatusBadge({ status, size = 'md', simplified = false }: AdmissionStatusBadgeProps) {
  const tone = simplified ? SIMPLE_TONE[simplifyStatus(status)] : statusTone(status);
  const label = simplified ? simpleStatusLabel(status) : prettyStatus(status);

  return <Pill tone={tone} size={size}>{label}</Pill>;
}
