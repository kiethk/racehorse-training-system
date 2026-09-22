import type { HTMLAttributes, ReactNode } from 'react';

interface PanelProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  padded?: boolean;
}

export function Panel({ children, padded = false, className = '', ...props }: PanelProps) {
  return (
    <div
      className={`rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] ${
        padded ? 'p-4' : ''
      } ${className}`}
      {...props}
    >
      {children}
    </div>
  );
}

export function SectionTitle({ children }: { children: ReactNode }) {
  return (
    <h2 className="text-[13px] font-semibold tracking-tight text-[var(--color-text-primary)]">
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
