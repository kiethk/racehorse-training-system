import type { FormHTMLAttributes, ReactNode } from 'react';

export function FilterBar({
  children,
  className = '',
  layout = 'flex',
  ...props
}: FormHTMLAttributes<HTMLFormElement> & { children: ReactNode; layout?: 'flex' | 'grid' }) {
  return (
    <form
      className={`${layout === 'grid' ? 'grid' : 'flex flex-wrap'} items-end gap-3 rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-[var(--shadow-panel)] ${className}`}
      {...props}
    >
      {children}
    </form>
  );
}
