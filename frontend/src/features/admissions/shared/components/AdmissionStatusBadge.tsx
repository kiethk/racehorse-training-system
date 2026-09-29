import type { AdmissionStatus } from '../../types';

export function prettyStatus(status: AdmissionStatus | string) {
  return status.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase());
}

type Tone = 'success' | 'warning' | 'danger' | 'info' | 'primary' | 'neutral';

export function statusTone(status: AdmissionStatus | string): Tone {
  if (status === 'GROOM_REVIEW') return 'primary';
  if (status === 'WAITING_FOR_STALL') return 'warning';
  if (status === 'APPROVED') return 'success';
  if (status === 'REJECTED') return 'danger';
  if (status === 'VET_REVIEW' || status === 'TRAINER_REVIEW' || status === 'MANAGER_REVIEW') return 'info';
  return 'neutral';
}

const toneStyles: Record<Tone, string> = {
  success: 'bg-[var(--color-success-soft)] text-[var(--color-success)] border border-[var(--color-success-soft)]',
  warning: 'bg-[var(--color-warning-soft)] text-[var(--color-warning)] border border-[var(--color-warning-soft)]',
  danger: 'bg-[var(--color-danger-soft)] text-[var(--color-danger)] border border-[var(--color-danger-soft)]',
  info: 'bg-[var(--color-info-soft)] text-[var(--color-info)] border border-[var(--color-info-soft)]',
  primary: 'bg-[var(--color-primary-soft)] text-[var(--color-primary)] border border-[var(--color-primary-soft)]',
  neutral: 'bg-[var(--color-surface-muted)] text-[var(--color-text-secondary)] border border-[var(--color-border)]',
};

export function AdmissionStatusBadge({ status, size }: { status: AdmissionStatus | string; size?: 'sm' | 'md' }) {
  const tone = statusTone(status);
  const style = toneStyles[tone];
  
  // Custom design for Admissions per user feedback:
  // - No dot
  // - More horizontal & vertical padding
  // - ~12px text (text-xs)
  // - font-medium
  // - rounded-md instead of full pill
  
  return (
    <span className={`inline-flex items-center rounded-md font-medium px-3 py-1 text-xs whitespace-nowrap ${style}`}>
      {prettyStatus(status)}
    </span>
  );
}
