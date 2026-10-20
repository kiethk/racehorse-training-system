'use client';

import type { ComponentProps, ReactNode } from 'react';
import { cn } from '@/lib/cn';
import { controlClassName, useFieldContext } from './FormField';
import { Icon } from './Icon';

/** id / aria attributes inherited from the surrounding FormField; explicit props win. */
function useFieldProps(props: { id?: string; required?: boolean; 'aria-invalid'?: unknown; 'aria-describedby'?: string }) {
  const field = useFieldContext();
  return {
    id: props.id ?? field?.id,
    required: props.required ?? field?.required,
    'aria-invalid': (props['aria-invalid'] as boolean | undefined) ?? (field?.invalid || undefined),
    'aria-describedby': props['aria-describedby'] ?? field?.describedBy,
  };
}

export function Input({ className, ...props }: ComponentProps<'input'>) {
  return <input {...props} {...useFieldProps(props)} className={cn(controlClassName, className)} />;
}

export function Textarea({ className, rows = 3, ...props }: ComponentProps<'textarea'>) {
  return (
    <textarea
      rows={rows}
      {...props}
      {...useFieldProps(props)}
      className={cn(controlClassName, 'py-2 leading-relaxed', className)}
    />
  );
}

export function Select({ className, children, ...props }: ComponentProps<'select'>) {
  return (
    <span className="relative block">
      <select
        {...props}
        {...useFieldProps(props)}
        className={cn(controlClassName, 'appearance-none pr-9', className)}
      >
        {children}
      </select>
      <Icon
        name="chevron-down"
        size={15}
        className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-[var(--color-text-muted)]"
      />
    </span>
  );
}

/** Checkbox or radio with its label. Pass `type="radio"` for a radio button. */
export function Checkbox({
  label,
  description,
  className,
  type = 'checkbox',
  ...props
}: Omit<ComponentProps<'input'>, 'type'> & {
  label: ReactNode;
  description?: ReactNode;
  type?: 'checkbox' | 'radio';
}) {
  return (
    <label
      className={cn(
        'flex cursor-pointer items-start gap-2.5 text-sm text-[var(--color-text-primary)] has-[:disabled]:cursor-not-allowed has-[:disabled]:opacity-60',
        className,
      )}
    >
      <input
        type={type}
        className="mt-0.5 h-4 w-4 shrink-0 accent-[var(--color-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)] focus-visible:ring-offset-1"
        {...props}
      />
      <span className="min-w-0">
        {label}
        {description && <span className="mt-0.5 block text-xs text-[var(--color-text-muted)]">{description}</span>}
      </span>
    </label>
  );
}

export function SearchInput({
  value,
  onChange,
  placeholder = 'Search',
  label = placeholder,
  className,
  ...props
}: Omit<ComponentProps<'input'>, 'value' | 'onChange' | 'type'> & {
  value: string;
  onChange: (value: string) => void;
  /** Accessible name; defaults to the placeholder. */
  label?: string;
}) {
  return (
    <span className={cn('relative block min-w-0', className)}>
      <Icon
        name="search"
        size={15}
        className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[var(--color-text-muted)]"
      />
      <input
        type="search"
        aria-label={label}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        {...props}
        className={cn(controlClassName, 'pl-9')}
      />
    </span>
  );
}

/**
 * A bare checkbox or radio, for custom option cards where the surrounding
 * <label> already provides the text. Prefer Checkbox when a plain label is enough.
 */
export function ChoiceInput({
  className,
  type = 'checkbox',
  ...props
}: Omit<ComponentProps<'input'>, 'type'> & { type?: 'checkbox' | 'radio' }) {
  return (
    <input
      type={type}
      {...props}
      className={cn(
        'h-4 w-4 shrink-0 accent-[var(--color-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)] focus-visible:ring-offset-1 disabled:cursor-not-allowed',
        className,
      )}
    />
  );
}

/** File picker styled like the other controls. */
export function FileInput({ className, ...props }: Omit<ComponentProps<'input'>, 'type'>) {
  return (
    <input
      type="file"
      {...props}
      {...useFieldProps(props)}
      className={cn(
        controlClassName,
        'block min-w-0 p-1.5 text-[var(--color-text-secondary)] file:mr-3 file:h-6 file:cursor-pointer file:rounded-[var(--radius-xs)] file:border-0 file:bg-[var(--color-surface-muted)] file:px-2.5 file:text-xs file:font-medium file:text-[var(--color-text-primary)]',
        className,
      )}
    />
  );
}
