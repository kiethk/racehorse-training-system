import type { ReactNode } from 'react';
import { Icon, type IconName } from './Icon';

export function EmptyState({
  icon = 'search',
  title,
  description,
  action,
}: {
  icon?: IconName;
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center px-6 py-12 text-center">
      <span className="mb-3 flex h-10 w-10 items-center justify-center rounded-full bg-[var(--color-surface-muted)] text-[var(--color-text-muted)]">
        <Icon name={icon} size={18} />
      </span>
      <p className="text-[13px] font-semibold text-[var(--color-text-primary)]">{title}</p>
      {description && (
        <p className="mt-1 max-w-xs text-[12px] text-[var(--color-text-secondary)]">{description}</p>
      )}
      {action && <div className="mt-3">{action}</div>}
    </div>
  );
}

export function ListSkeleton({ rows = 6 }: { rows?: number }) {
  return (
    <div className="space-y-1.5 p-2" aria-hidden="true">
      {Array.from({ length: rows }).map((_, i) => (
        <div
          key={i}
          className="flex items-center gap-2.5 rounded-[var(--radius-sm)] p-2"
        >
          <div className="h-9 w-9 shrink-0 animate-pulse rounded-[var(--radius-sm)] bg-[var(--color-surface-muted)]" />
          <div className="flex-1 space-y-1.5">
            <div className="h-3 w-2/3 animate-pulse rounded bg-[var(--color-surface-muted)]" />
            <div className="h-2.5 w-1/2 animate-pulse rounded bg-[var(--color-surface-muted)]" />
          </div>
          <div className="h-4 w-12 animate-pulse rounded-full bg-[var(--color-surface-muted)]" />
        </div>
      ))}
    </div>
  );
}

export function DetailSkeleton() {
  return (
    <div className="space-y-4 p-4" aria-hidden="true">
      <div className="flex items-center gap-3">
        <div className="h-14 w-14 animate-pulse rounded-[var(--radius-md)] bg-[var(--color-surface-muted)]" />
        <div className="space-y-2">
          <div className="h-4 w-40 animate-pulse rounded bg-[var(--color-surface-muted)]" />
          <div className="h-3 w-56 animate-pulse rounded bg-[var(--color-surface-muted)]" />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-3">
        {Array.from({ length: 4 }).map((_, i) => (
          <div
            key={i}
            className="h-28 animate-pulse rounded-[var(--radius-md)] bg-[var(--color-surface-muted)]"
          />
        ))}
      </div>
    </div>
  );
}
