import type { ReactNode } from 'react';

import { Icon, Panel, SectionTitle, type IconName } from '@/components/ui';
import { cn } from '@/lib/cn';

/**
 * Label + value row inside a HorseInfoSection.
 * Supports stacked layout (label on top, value below) for multi-column grids,
 * while keeping the default horizontal row for backward compatibility.
 * Missing values are explicit; zero remains a valid value.
 */
export function HorseInfoRow({
  label,
  value,
  stacked = false,
  className,
}: {
  label: string;
  value: ReactNode;
  stacked?: boolean;
  className?: string;
}) {
  if (stacked) {
    return (
      <div className={cn('min-w-0 space-y-1', className)}>
        <span className="block text-xs font-medium text-[var(--color-text-muted)]">
          {label}
        </span>
        <div className="break-words text-sm font-medium text-[var(--color-text-primary)]">
          {value === null || value === undefined || value === '' ? <span className="font-normal text-[var(--color-text-muted)]">Not recorded</span> : value}
        </div>
      </div>
    );
  }

  return (
    <div className={cn('flex items-start justify-between gap-4', className)}>
      <span className="shrink-0 text-[var(--color-text-muted)]">{label}</span>
      <span className="min-w-0 break-words text-right font-medium text-[var(--color-text-primary)]">
        {value === null || value === undefined || value === '' ? <span className="font-normal text-[var(--color-text-muted)]">Not recorded</span> : value}
      </span>
    </div>
  );
}

/**
 * Titled card for a section in the main column of a horse detail screen.
 * Matches the visual pattern of AdmissionInfoSection.
 */
export function HorseInfoSection({
  title,
  icon,
  description,
  aside,
  children,
}: {
  title: string;
  icon?: IconName;
  description?: string;
  /** Small element at the right of the title, e.g. a badge. */
  aside?: ReactNode;
  children: ReactNode;
}) {
  return (
    <Panel className="min-w-0 overflow-hidden">
      <div className="flex items-start justify-between gap-3 border-b border-[var(--color-border)] px-5 py-4">
        <div className="min-w-0 space-y-1">
          <div className="flex items-center gap-2">
            {icon && <Icon name={icon} size={16} className="shrink-0 text-[var(--color-primary)]" />}
            <SectionTitle>{title}</SectionTitle>
          </div>
          {description && <p className="text-xs leading-relaxed text-[var(--color-text-muted)]">{description}</p>}
        </div>
        {aside}
      </div>
      <div className="space-y-4 p-5 text-sm">{children}</div>
    </Panel>
  );
}

/**
 * Titled card for the side column of a horse detail screen (actions, notes, summaries).
 */
export function HorseSideCard({
  title,
  tone = 'default',
  children,
  className,
}: {
  title: string;
  tone?: 'default' | 'primary' | 'danger' | 'warning';
  children: ReactNode;
  className?: string;
}) {
  return (
    <Panel padded tone={tone}>
      <SectionTitle>{title}</SectionTitle>
      <div className={cn('mt-4 space-y-3 text-sm', className)}>{children}</div>
    </Panel>
  );
}

/**
 * Uppercase label separating groups inside one section.
 */
export function HorseInfoGroupTitle({ children }: { children: ReactNode }) {
  return (
    <h3 className="text-xs font-semibold uppercase tracking-wide text-[var(--color-text-secondary)]">
      {children}
    </h3>
  );
}
