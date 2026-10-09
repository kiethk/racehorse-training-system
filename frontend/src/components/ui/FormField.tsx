'use client';

import { createContext, useContext, useId, type ReactNode } from 'react';
import { cn } from '@/lib/cn';

interface FieldContextValue {
  id: string;
  describedBy?: string;
  invalid: boolean;
  required: boolean;
}

const FieldContext = createContext<FieldContextValue | null>(null);

/** Lets Input/Select/Textarea pick up the id and aria wiring of the surrounding FormField. */
export function useFieldContext() {
  return useContext(FieldContext);
}

export function FormField({
  label,
  htmlFor,
  required = false,
  hint,
  error,
  children,
  className,
}: {
  label: string;
  htmlFor?: string;
  required?: boolean;
  hint?: string;
  error?: string;
  children: ReactNode;
  className?: string;
}) {
  const generatedId = useId();
  const id = htmlFor ?? generatedId;
  const descriptionId = `${id}-description`;
  const errorId = `${id}-error`;
  const describedBy = [hint && descriptionId, error && errorId].filter(Boolean).join(' ') || undefined;

  return (
    <FieldContext.Provider value={{ id, describedBy, invalid: Boolean(error), required }}>
      <div className={cn('min-w-0 space-y-1.5', className)}>
        <label htmlFor={id} className="block text-xs font-medium text-[var(--color-text-primary)]">
          {label}
          {required && <span className="ml-1 text-[var(--color-danger)]" aria-hidden="true">*</span>}
          {required && <span className="sr-only"> (required)</span>}
        </label>
        {children}
        {hint && <p id={descriptionId} className="text-xs text-[var(--color-text-muted)]">{hint}</p>}
        {error && <p id={errorId} role="alert" className="text-xs text-[var(--color-danger)]">{error}</p>}
      </div>
    </FieldContext.Provider>
  );
}

export const controlClassName =
  'min-h-10 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text-primary)] outline-none ' +
  'transition-[color,border-color,box-shadow] duration-[var(--duration-fast)] placeholder:text-[var(--color-text-muted)] ' +
  'focus-visible:border-[var(--color-focus)] focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]/30 ' +
  'aria-invalid:border-[var(--color-danger)] aria-invalid:focus-visible:ring-[var(--color-danger)]/30 ' +
  'disabled:cursor-not-allowed disabled:opacity-60 [&[readonly]]:bg-[var(--color-surface-muted)]';
