import { createContext, useContext, type ReactNode } from 'react';
import { Panel, SectionTitle } from '@/components/ui/Panel';
import { cn } from '@/lib/cn';

const InfoRowContext = createContext<{ defaultLayout: 'aligned' | 'between' }>({
  defaultLayout: 'aligned',
});

/**
 * Key-value row for admission details.
 * - 'aligned' (default for main cards): Label has a neat left-aligned column (180px),
 *   and the value sits directly next to it. Eliminates horizontal scanning fatigue.
 * - 'between' (default for side cards): Space-between layout for narrow sidebars.
 */
export function InfoRow({
  label,
  value,
  layout,
  className,
}: {
  label: string;
  value: ReactNode;
  layout?: 'aligned' | 'between';
  className?: string;
}) {
  const { defaultLayout } = useContext(InfoRowContext);
  const activeLayout = layout ?? defaultLayout;

  if (activeLayout === 'between') {
    return (
      <div className={cn('flex items-start justify-between gap-4', className)}>
        <span className="shrink-0 text-sm text-[var(--color-text-muted)]">{label}</span>
        <span className="min-w-0 break-words text-right text-sm font-medium text-[var(--color-text-primary)]">
          {value || <span className="font-normal italic text-[var(--color-text-muted)]">N/A</span>}
        </span>
      </div>
    );
  }

  return (
    <div className={cn('grid grid-cols-1 gap-1 sm:grid-cols-[180px_1fr] sm:gap-4 sm:items-baseline', className)}>
      <span className="shrink-0 text-sm text-[var(--color-text-muted)]">{label}</span>
      <div className="min-w-0 break-words text-left text-sm font-medium text-[var(--color-text-primary)]">
        {value || <span className="font-normal italic text-[var(--color-text-muted)]">N/A</span>}
      </div>
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
    <InfoRowContext.Provider value={{ defaultLayout: 'aligned' }}>
      <Panel padded>
        <div className="flex items-center justify-between gap-3">
          <SectionTitle>{title}</SectionTitle>
          {aside}
        </div>
        <div className="mt-4 space-y-3 text-sm">{children}</div>
      </Panel>
    </InfoRowContext.Provider>
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
    <InfoRowContext.Provider value={{ defaultLayout: 'between' }}>
      <Panel padded tone={tone}>
        <SectionTitle>{title}</SectionTitle>
        <div className={cn('mt-4 space-y-3 text-sm', className)}>{children}</div>
      </Panel>
    </InfoRowContext.Provider>
  );
}

/** Uppercase label separating groups inside one section (e.g. per-reviewer blocks). */
export function InfoGroupTitle({ children }: { children: ReactNode }) {
  return (
    <h3 className="text-xs font-semibold uppercase tracking-wide text-[var(--color-text-secondary)]">{children}</h3>
  );
}
