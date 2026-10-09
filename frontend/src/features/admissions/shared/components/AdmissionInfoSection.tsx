import type { ReactNode } from 'react';
import { Panel, SectionTitle } from '@/components/ui/Panel';
import { cn } from '@/lib/cn';

export function InfoRow({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-4">
      <span className="shrink-0 text-[var(--color-text-muted)]">{label}</span>
      <span className="min-w-0 break-words text-right font-medium text-[var(--color-text-primary)]">
        {value || <span className="font-normal italic text-[var(--color-text-muted)]">N/A</span>}
      </span>
    </div>
  );
}

/** Titled card for the main column of an admission detail screen. */
export function AdmissionInfoSection({
  title,
  aside,
  children,
}: {
  title: string;
  /** Small element at the right of the title, e.g. a count. */
  aside?: ReactNode;
  children: ReactNode;
}) {
  return (
    <Panel padded>
      <div className="flex items-center justify-between gap-3">
        <SectionTitle>{title}</SectionTitle>
        {aside}
      </div>
      <div className="mt-4 space-y-3 text-sm">{children}</div>
    </Panel>
  );
}

/** Titled card for the side column: summaries and the role's decision/action form. */
export function AdmissionSideCard({
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

/** Uppercase label separating groups inside one section (e.g. per-reviewer blocks). */
export function InfoGroupTitle({ children }: { children: ReactNode }) {
  return (
    <h3 className="text-xs font-semibold uppercase tracking-wide text-[var(--color-text-secondary)]">{children}</h3>
  );
}
