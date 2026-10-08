import type { ReactNode } from 'react';

export function FormField({
  label,
  htmlFor,
  required = false,
  hint,
  error,
  children,
  className = '',
}: {
  label: string;
  htmlFor?: string;
  required?: boolean;
  hint?: string;
  error?: string;
  children: ReactNode;
  className?: string;
}) {
  const descriptionId = htmlFor ? `${htmlFor}-description` : undefined;
  const errorId = htmlFor ? `${htmlFor}-error` : undefined;
  return (
    <div className={`min-w-0 space-y-1.5 ${className}`}>
      <label htmlFor={htmlFor} className="block text-xs font-medium text-[var(--color-text-primary)]">
        {label}{required && <span className="ml-1 text-[var(--color-danger)]" aria-hidden="true">*</span>}
        {required && <span className="sr-only"> (required)</span>}
      </label>
      {children}
      {hint && <p id={descriptionId} className="text-xs text-[var(--color-text-muted)]">{hint}</p>}
      {error && <p id={errorId} role="alert" className="text-xs text-[var(--color-danger)]">{error}</p>}
    </div>
  );
}

export const controlClassName =
  'min-h-10 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text-primary)] outline-none transition-colors placeholder:text-[var(--color-text-muted)] focus-visible:border-[var(--color-focus)] focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]/30 disabled:cursor-not-allowed disabled:opacity-60';
