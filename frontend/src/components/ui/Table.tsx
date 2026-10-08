import type { HTMLAttributes, ReactNode, TableHTMLAttributes } from 'react';
import { Button } from './Button';

type TableAlign = 'left' | 'center' | 'right';
export type SortDirection = 'asc' | 'desc' | null;

const alignClasses: Record<TableAlign, string> = {
  left: 'text-left',
  center: 'text-center',
  right: 'text-right',
};

export function TableShell({ children, className = '', ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={`overflow-x-auto rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-[var(--shadow-sm)] ${className}`}
      {...props}
    >
      {children}
    </div>
  );
}

export function Table({ className = '', ...props }: TableHTMLAttributes<HTMLTableElement>) {
  return (
    <table
      className={`w-full min-w-[640px] border-collapse text-left text-[var(--text-md)] ${className}`}
      {...props}
    />
  );
}

export function TableHeader({ children, className = '', ...props }: HTMLAttributes<HTMLTableSectionElement>) {
  return (
    <thead
      className={`border-b border-[var(--color-border)] bg-[var(--color-surface-muted)] text-[var(--text-sm)] font-semibold text-[var(--color-text-secondary)] ${className}`}
      {...props}
    >
      {children}
    </thead>
  );
}

export function TableHead({
  align = 'left',
  className = '',
  ...props
}: HTMLAttributes<HTMLTableCellElement> & { align?: TableAlign }) {
  return (
    <th
      scope="col"
      className={`px-4 py-3 ${alignClasses[align]} ${className}`}
      {...props}
    />
  );
}

export function TableBody({ children, className = '', ...props }: HTMLAttributes<HTMLTableSectionElement>) {
  return (
    <tbody
      className={`divide-y divide-[var(--color-border)] text-[var(--color-text-primary)] ${className}`}
      {...props}
    >
      {children}
    </tbody>
  );
}

export function TableRow({ children, className = '', ...props }: HTMLAttributes<HTMLTableRowElement>) {
  return (
    <tr
      className={`transition-colors hover:bg-[var(--color-surface-muted)] ${className}`}
      {...props}
    >
      {children}
    </tr>
  );
}

export function TableCell({
  align = 'left',
  className = '',
  ...props
}: HTMLAttributes<HTMLTableCellElement> & { align?: TableAlign }) {
  return (
    <td
      className={`px-4 py-3 align-middle ${alignClasses[align]} ${className}`}
      {...props}
    />
  );
}

export function TableToolbar({
  children,
  actions,
  className = '',
}: {
  children?: ReactNode;
  actions?: ReactNode;
  className?: string;
}) {
  return (
    <div className={`flex flex-col gap-3 md:flex-row md:items-center md:justify-between ${className}`}>
      <div className="flex min-w-0 flex-1 flex-wrap items-center gap-2">{children}</div>
      {actions && <div className="flex shrink-0 items-center gap-2">{actions}</div>}
    </div>
  );
}

export function TableSortButton({
  label,
  direction,
  onSort,
}: {
  label: string;
  direction: SortDirection;
  onSort: () => void;
}) {
  const indicator = direction === 'asc' ? ' ↑' : direction === 'desc' ? ' ↓' : '';

  return (
    <button
      type="button"
      onClick={onSort}
      className="inline-flex items-center rounded-[var(--radius-xs)] font-semibold transition-colors hover:text-[var(--color-text-primary)] focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
      aria-label={`Sort by ${label}${direction ? ` (${direction}ending)` : ''}`}
      aria-pressed={direction !== null}
    >
      {label}{indicator}
    </button>
  );
}

export function TablePagination({
  page,
  pageSize,
  total,
  onPageChange,
}: {
  page: number;
  pageSize: number;
  total: number;
  onPageChange: (page: number) => void;
}) {
  const pageCount = Math.max(1, Math.ceil(total / pageSize));
  const first = total === 0 ? 0 : page * pageSize + 1;
  const last = Math.min((page + 1) * pageSize, total);

  return (
    <div className="flex flex-col gap-2 border-t border-[var(--color-border)] px-4 py-3 text-[var(--text-sm)] text-[var(--color-text-secondary)] sm:flex-row sm:items-center sm:justify-between">
      <span>
        Showing <strong className="font-semibold text-[var(--color-text-primary)]">{first}</strong>–
        <strong className="font-semibold text-[var(--color-text-primary)]">{last}</strong> of{' '}
        <strong className="font-semibold text-[var(--color-text-primary)]">{total}</strong>
      </span>
      <div className="flex items-center gap-2">
        <Button size="sm" variant="secondary" disabled={page <= 0} onClick={() => onPageChange(page - 1)}>
          Previous
        </Button>
        <span aria-live="polite" className="min-w-16 text-center">
          Page {page + 1} of {pageCount}
        </span>
        <Button size="sm" variant="secondary" disabled={page >= pageCount - 1} onClick={() => onPageChange(page + 1)}>
          Next
        </Button>
      </div>
    </div>
  );
}

export function TableMessage({
  children,
  error = false,
  className = '',
}: {
  children: ReactNode;
  error?: boolean;
  className?: string;
}) {
  return (
    <div
      className={`flex min-h-28 items-center justify-center px-6 py-8 text-center text-[var(--text-sm)] ${
        error ? 'text-[var(--color-danger)]' : 'text-[var(--color-text-secondary)]'
      } ${className}`}
      role={error ? 'alert' : 'status'}
    >
      {children}
    </div>
  );
}

