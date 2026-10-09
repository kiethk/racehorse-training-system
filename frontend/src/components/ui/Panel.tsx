import type { HTMLAttributes, ReactNode } from 'react';
import { cn } from '@/lib/cn';

type Tone = 'default' | 'primary' | 'danger' | 'warning';

interface PanelProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  padded?: boolean;
  /** Coloured border for a panel that needs attention. */
  tone?: Tone;
  /** Hover lift for a panel that acts as a link or button. */
  interactive?: boolean;
}

const tones: Record<Tone, string> = {
  default: 'border-[var(--color-border)]',
  primary: 'border-[var(--color-primary)]',
  danger: 'border-[var(--color-danger)]',
  warning: 'border-[var(--color-warning)]',
};

/** The standard card surface. FilterBar and DataTable share the same radius and shadow. */
export function Panel({ children, padded = false, tone = 'default', interactive = false, className, ...props }: PanelProps) {
  return (
    <div
      className={cn(
        'rounded-[var(--radius-lg)] border bg-[var(--color-surface)] shadow-[var(--shadow-panel)]',
        tones[tone],
        padded && 'p-4',
        interactive &&
          'transition-[box-shadow,border-color,transform] duration-[var(--duration-base)] hover:-translate-y-0.5 hover:border-[var(--color-border-strong)] hover:shadow-[var(--shadow-popover)]',
        className,
      )}
      {...props}
    >
      {children}
    </div>
  );
}

export function SectionTitle({ children }: { children: ReactNode }) {
  return (
    <h2 className="text-sm font-semibold tracking-tight text-[var(--color-text-primary)]">
      {children}
    </h2>
  );
}

export function FieldLabel({ children }: { children: ReactNode }) {
  return (
    <div className="text-[11px] font-medium uppercase tracking-wide text-[var(--color-text-muted)]">
      {children}
    </div>
  );
}
