'use client';

import { useId, useMemo, useState, type ReactNode } from 'react';
import { EmptyState } from './states';
import { Pagination } from './Pagination';

export interface DataTableColumn<Row> {
  id: string;
  header: ReactNode;
  render: (row: Row) => ReactNode;
  sortValue?: (row: Row) => string | number | Date | null | undefined;
  align?: 'left' | 'center' | 'right';
  className?: string;
}

export interface DataTablePagination {
  page: number;
  pageSize: number;
  total: number;
  onPageChange: (page: number) => void;
  disabled?: boolean;
}

export function DataTable<Row>({
  rows,
  columns,
  getRowKey,
  onRowClick,
  ariaLabel = 'Data table',
  loading = false,
  error,
  emptyTitle = 'No records found',
  emptyDescription,
  emptyAction,
  pageSize,
  pagination,
  filterValue,
  onFilterValueChange,
  getSearchText,
  searchPlaceholder = 'Search records…',
  className = '',
}: {
  rows: Row[];
  columns: DataTableColumn<Row>[];
  getRowKey: (row: Row) => string | number;
  onRowClick?: (row: Row) => void;
  ariaLabel?: string;
  loading?: boolean;
  error?: ReactNode;
  emptyTitle?: string;
  emptyDescription?: string;
  emptyAction?: ReactNode;
  pageSize?: number;
  pagination?: DataTablePagination;
  filterValue?: string;
  onFilterValueChange?: (value: string) => void;
  getSearchText?: (row: Row) => string;
  searchPlaceholder?: string;
  className?: string;
}) {
  const [sort, setSort] = useState<{ id: string; direction: 'asc' | 'desc' } | null>(null);
  const [localPage, setLocalPage] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const searchId = useId();
  const activeSearch = filterValue ?? searchQuery;

  const filtered = useMemo(() => {
    const query = activeSearch.trim().toLocaleLowerCase('en-GB');
    if (!query || !getSearchText) return rows;
    return rows.filter((row) => getSearchText(row).toLocaleLowerCase('en-GB').includes(query));
  }, [activeSearch, getSearchText, rows]);

  const sorted = useMemo(() => {
    if (!sort) return filtered;
    const column = columns.find((item) => item.id === sort.id);
    if (!column?.sortValue) return filtered;
    const direction = sort.direction === 'asc' ? 1 : -1;
    return [...filtered].sort((left, right) => {
      const a = column.sortValue?.(left);
      const b = column.sortValue?.(right);
      if (a == null) return b == null ? 0 : 1;
      if (b == null) return -1;
      const result = a instanceof Date && b instanceof Date
        ? a.getTime() - b.getTime()
        : typeof a === 'number' && typeof b === 'number'
          ? a - b
          : String(a).localeCompare(String(b), 'en-GB', { numeric: true, sensitivity: 'base' });
      return result * direction;
    });
  }, [columns, filtered, sort]);

  const activePage = pagination?.page ?? localPage;
  const activePageSize = pagination?.pageSize ?? pageSize;
  const visibleRows = activePageSize && !pagination
    ? sorted.slice(activePage * activePageSize, (activePage + 1) * activePageSize)
    : sorted;
  const pageChange = pagination?.onPageChange ?? setLocalPage;

  return (
    <div className={`overflow-hidden rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-[var(--shadow-panel)] ${className}`}>
      {error ? (
        <div role="alert" className="p-5 text-sm text-[var(--color-danger)]">{error}</div>
      ) : (
        <>
          {getSearchText && (
            <div className="border-b border-[var(--color-border)] p-3">
              <label className="sr-only" htmlFor={searchId}>Search records</label>
              <input
                id={searchId}
                type="search"
                value={activeSearch}
                onChange={(event) => {
                  if (onFilterValueChange) onFilterValueChange(event.target.value);
                  else setSearchQuery(event.target.value);
                  setLocalPage(0);
                }}
                placeholder={searchPlaceholder}
                className="min-h-10 w-full max-w-sm rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text-primary)] outline-none placeholder:text-[var(--color-text-muted)] focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
              />
            </div>
          )}
          <div className="overflow-x-auto">
            <table aria-label={ariaLabel} aria-busy={loading} className="w-full min-w-[36rem] text-left text-sm">
              <thead className="border-b border-[var(--color-border)] bg-[var(--color-surface-muted)] text-xs font-semibold text-[var(--color-text-secondary)]">
                <tr>
                  {columns.map((column) => {
                    const alignClass = column.align === 'right' ? 'text-right' : column.align === 'center' ? 'text-center' : 'text-left';
                    const sortState = sort?.id === column.id ? sort.direction : undefined;
                    return (
                      <th key={column.id} scope="col" aria-sort={sortState === 'asc' ? 'ascending' : sortState === 'desc' ? 'descending' : undefined} className={`px-4 py-3 ${alignClass} ${column.className ?? ''}`}>
                        {column.sortValue ? (
                          <button
                            type="button"
                            className="inline-flex items-center gap-1 rounded-sm text-inherit hover:text-[var(--color-text-primary)] focus-visible:outline-2"
                            onClick={() => {
                              setSort((current) => current?.id === column.id
                                ? { id: column.id, direction: current.direction === 'asc' ? 'desc' : 'asc' }
                                : { id: column.id, direction: 'asc' });
                              setLocalPage(0);
                              if (pagination) pagination.onPageChange(0);
                            }}
                          >
                            {column.header}<span aria-hidden="true">{sortState === 'asc' ? '↑' : sortState === 'desc' ? '↓' : '↕'}</span>
                          </button>
                        ) : column.header}
                      </th>
                    );
                  })}
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--color-border)]">
                {loading && rows.length === 0 ? (
                  Array.from({ length: 5 }, (_, index) => (
                    <tr key={`skeleton-${index}`} aria-hidden="true">
                      {columns.map((column) => <td key={column.id} className="px-4 py-4"><div className="h-3 animate-pulse rounded bg-[var(--color-surface-muted)]" /></td>)}
                    </tr>
                  ))
                ) : visibleRows.length === 0 ? (
                  <tr><td colSpan={columns.length}><EmptyState title={emptyTitle} description={emptyDescription} action={emptyAction} /></td></tr>
                ) : visibleRows.map((row) => (
                  <tr
                    key={getRowKey(row)}
                    tabIndex={onRowClick ? 0 : undefined}
                    onClick={onRowClick ? () => onRowClick(row) : undefined}
                    onKeyDown={onRowClick ? (event) => {
                      if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault();
                        onRowClick(row);
                      }
                    } : undefined}
                    className={`transition-colors hover:bg-[var(--color-surface-subtle)] ${onRowClick ? 'cursor-pointer focus-visible:outline-2 focus-visible:outline-[var(--color-focus)]' : ''}`}
                  >
                    {columns.map((column) => (
                      <td key={column.id} className={`px-4 py-3 text-[var(--color-text-primary)] ${column.align === 'right' ? 'text-right' : column.align === 'center' ? 'text-center' : ''} ${column.className ?? ''}`}>
                        {column.render(row)}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {activePageSize && (pagination || pageSize) && (
            <Pagination
              page={activePage}
              pageSize={activePageSize}
              total={pagination?.total ?? sorted.length}
              onPageChange={(nextPage) => pageChange(nextPage)}
              disabled={loading || pagination?.disabled}
            />
          )}
        </>
      )}
    </div>
  );
}
