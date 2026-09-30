import type { ReactNode } from 'react';
import type { IconName } from './Icon';
import { Icon } from './Icon';

export type HorseStatus = 'CANDIDATE' | 'ELIGIBLE' | 'MONITORING' | 'INJURED' | 'QUARANTINED' | 'REJECTED';

type Tone = 'success' | 'warning' | 'danger' | 'info' | 'isolated' | 'primary' | 'neutral';

interface ToneStyle {
  bg: string;
  fg: string;
  dot: string;
}

const tones: Record<Tone, ToneStyle> = {
  success: {
    bg: 'bg-[var(--color-success-soft)]',
    fg: 'text-[var(--color-success)]',
    dot: 'bg-[var(--color-success)]',
  },
  warning: {
    bg: 'bg-[var(--color-warning-soft)]',
    fg: 'text-[var(--color-warning)]',
    dot: 'bg-[var(--color-warning)]',
  },
  danger: {
    bg: 'bg-[var(--color-danger-soft)]',
    fg: 'text-[var(--color-danger)]',
    dot: 'bg-[var(--color-danger)]',
  },
  info: {
    bg: 'bg-[var(--color-info-soft)]',
    fg: 'text-[var(--color-info)]',
    dot: 'bg-[var(--color-info)]',
  },
  isolated: {
    bg: 'bg-[var(--color-isolated-soft)]',
    fg: 'text-[var(--color-isolated)]',
    dot: 'bg-[var(--color-isolated)]',
  },
  primary: {
    bg: 'bg-[var(--color-primary-soft)]',
    fg: 'text-[var(--color-primary)]',
    dot: 'bg-[var(--color-primary)]',
  },
  neutral: {
    bg: 'bg-[var(--color-surface-muted)]',
    fg: 'text-[var(--color-text-secondary)]',
    dot: 'bg-[var(--color-text-muted)]',
  },
};

const horseStatusMap: Record<HorseStatus, { tone: Tone; icon: IconName; label: string }> = {
  CANDIDATE: { tone: 'info', icon: 'clipboard', label: 'Candidate' },
  ELIGIBLE: { tone: 'success', icon: 'check', label: 'Eligible' },
  MONITORING: { tone: 'warning', icon: 'activity', label: 'Monitoring' },
  INJURED: { tone: 'danger', icon: 'alert-triangle', label: 'Injured' },
  QUARANTINED: { tone: 'isolated', icon: 'shield', label: 'Quarantined' },
  REJECTED: { tone: 'neutral', icon: 'x', label: 'Rejected' },
};

export function Pill({
  tone,
  children,
  icon,
  size = 'md',
}: {
  tone: Tone;
  children: ReactNode;
  icon?: IconName;
  size?: 'sm' | 'md';
}) {
  const t = tones[tone];
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full font-medium ${t.bg} ${t.fg} ${
        size === 'sm' ? 'px-1.5 py-0.5 text-[10px]' : 'px-2 py-0.5 text-[11px]'
      }`}
    >
      {icon ? (
        <Icon name={icon} size={size === 'sm' ? 10 : 12} />
      ) : (
        <span className={`h-1.5 w-1.5 rounded-full ${t.dot}`} aria-hidden="true" />
      )}
      {children}
    </span>
  );
}

export function StatusBadge({
  status,
  size = 'md',
}: {
  status: HorseStatus;
  size?: 'sm' | 'md';
}) {
  const c = horseStatusMap[status];
  return (
    <Pill tone={c.tone} icon={c.icon} size={size}>
      {c.label}
    </Pill>
  );
}
