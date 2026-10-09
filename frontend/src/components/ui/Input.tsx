'use client';

import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react';
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

export function Input({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return <input {...props} {...useFieldProps(props)} className={cn(controlClassName, className)} />;
}

export function Textarea({ className, rows = 3, ...props }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <textarea
      rows={rows}
      {...props}
      {...useFieldProps(props)}
      className={cn(controlClassName, 'py-2 leading-relaxed', className)}
    />
  );
}

export function Select({ className, children, ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
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
}: Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> & {
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
}: Omit<InputHTMLAttributes<HTMLInputElement>, 'value' | 'onChange' | 'type'> & {
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
