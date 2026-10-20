import { cn } from '@/lib/cn';

type Size = 'sm' | 'md' | 'lg';

const sizes: Record<Size, string> = {
  sm: 'h-3.5 w-3.5 border-2',
  md: 'h-5 w-5 border-2',
  lg: 'h-7 w-7 border-[3px]',
};

export function Spinner({
  size = 'md',
  label = 'Loading…',
  className,
}: {
  size?: Size;
  /** Announced to assistive tech; pass an empty string when a visible label sits next to it. */
  label?: string;
  className?: string;
}) {
  return (
    <span
      role={label ? 'status' : undefined}
      aria-label={label || undefined}
      aria-hidden={label ? undefined : true}
      className={cn(
        'inline-block shrink-0 animate-spin rounded-full border-[var(--color-border-strong)] border-t-[var(--color-primary)]',
        sizes[size],
        className,
      )}
    />
  );
}

/** Centered spinner with a caption, for a whole screen or a content region. */
export function LoadingScreen({ fullScreen = false }: { fullScreen?: boolean }) {
  return (
    <div
      className={cn(
        'flex items-center justify-center bg-[var(--color-background)]',
        fullScreen ? 'min-h-screen' : 'min-h-[50vh]',
      )}
    >
      <div className="flex flex-col items-center gap-3">
        <Spinner size="lg" label="" />
        <p className="text-xs text-[var(--color-text-muted)]">Loading…</p>
      </div>
    </div>
  );
}
