import { Button } from './Button';

export function Pagination({
  page,
  pageSize,
  total,
  onPageChange,
  disabled = false,
}: {
  page: number;
  pageSize: number;
  total: number;
  onPageChange: (page: number) => void;
  disabled?: boolean;
}) {
  const pageCount = Math.max(1, Math.ceil(total / pageSize));
  const firstItem = total === 0 ? 0 : page * pageSize + 1;
  const lastItem = Math.min((page + 1) * pageSize, total);

  return (
    <nav aria-label="Pagination" className="flex flex-col gap-3 border-t border-[var(--color-border)] px-4 py-3 sm:flex-row sm:items-center sm:justify-between sm:px-5">
      <p className="text-xs text-[var(--color-text-secondary)]" aria-live="polite">
        Showing <span className="font-medium text-[var(--color-text-primary)]">{firstItem}</span> to{' '}
        <span className="font-medium text-[var(--color-text-primary)]">{lastItem}</span> of{' '}
        <span className="font-medium text-[var(--color-text-primary)]">{total}</span> results
      </p>
      <div className="flex items-center gap-2">
        <Button variant="secondary" size="sm" disabled={disabled || page <= 0} onClick={() => onPageChange(page - 1)}>
          Previous
        </Button>
        <span className="min-w-20 text-center text-xs text-[var(--color-text-secondary)]" aria-current="page">
          Page {Math.min(page + 1, pageCount)} of {pageCount}
        </span>
        <Button variant="secondary" size="sm" disabled={disabled || page + 1 >= pageCount} onClick={() => onPageChange(page + 1)}>
          Next
        </Button>
      </div>
    </nav>
  );
}
