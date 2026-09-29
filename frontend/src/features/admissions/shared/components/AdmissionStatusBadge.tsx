import type { AdmissionStatus } from '../../types';

export function prettyStatus(status: AdmissionStatus | string) {
  return status.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase());
}

type Tone = 'success' | 'warning' | 'danger' | 'info' | 'primary' | 'neutral';

export function statusTone(status: AdmissionStatus | string): Tone {
  if (status === 'GROOM_REVIEW') return 'primary';
  if (status === 'WAITING_FOR_STALL') return 'warning';
  if (status === 'PENDING_RECHECK') return 'warning';
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

export function AdmissionStatusBadge({ status, size = 'md' }: { status: AdmissionStatus | string; size?: 'sm' | 'md' }) {
  const tone = statusTone(status);
  const style = toneStyles[tone];
  
  const sizeStyle = size === 'sm' 
    ? 'px-2 py-0.5 text-[11px]' 
    : 'px-3 py-1 text-xs';
  
  return (
    <span className={`inline-flex items-center rounded-md font-medium whitespace-nowrap ${sizeStyle} ${style}`}>
      {prettyStatus(status)}
    </span>
  );
}
