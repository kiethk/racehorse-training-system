import { cn } from '@/lib/cn';

/** Placeholder block while content loads. Size it with className (e.g. "h-4 w-40"). */
export function Skeleton({ className }: { className?: string }) {
  return (
    <div
      aria-hidden="true"
      className={cn('animate-pulse rounded-[var(--radius-sm)] bg-[var(--color-surface-muted)]', className)}
    />
  );
}
