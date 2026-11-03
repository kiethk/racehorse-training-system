/* eslint-disable no-restricted-syntax */
'use client';

import { useState } from 'react';
import { Modal } from '@/components/ui/Modal';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { toast } from '@/lib/toast';
import { auditLogApi } from '../services/api';
import type { AuditLogFilters } from '../types';

interface AuditLogExportModalProps {
  open: boolean;
  onClose: () => void;
  currentFilters: AuditLogFilters;
  totalCount?: number;
  filteredCount?: number;
}

type ExportMode = 'current_filter' | 'all' | 'custom';

function parseLocalDateStart(dateStr: string): string {
  const [y, m, d] = dateStr.split('-').map(Number);
  const date = new Date(y, m - 1, d, 0, 0, 0, 0);
  return date.toISOString();
}

function parseLocalDateEnd(dateStr: string): string {
  const [y, m, d] = dateStr.split('-').map(Number);
  const date = new Date(y, m - 1, d, 23, 59, 59, 999);
  return date.toISOString();
}

export function AuditLogExportModal({
  open,
  onClose,
  currentFilters,
  totalCount,
  filteredCount,
}: AuditLogExportModalProps) {
  const hasActiveDateFilter = Boolean(currentFilters.from || currentFilters.to);
  const [mode, setMode] = useState<ExportMode>(hasActiveDateFilter ? 'current_filter' : 'all');
  const [fromDate, setFromDate] = useState<string>(currentFilters.from || '');
  const [toDate, setToDate] = useState<string>(currentFilters.to || '');
  const [downloading, setDownloading] = useState<boolean>(false);

  const isSelectionEmpty = (() => {
    if (mode === 'all') {
      return totalCount !== undefined && totalCount === 0;
    }
    if (mode === 'current_filter') {
      return filteredCount !== undefined && filteredCount === 0;
    }
    return false;
  })();

  const isCustomInvalidRange = Boolean(mode === 'custom' && fromDate && toDate && fromDate > toDate);

  const handleExport = async () => {
    if (mode !== 'custom' && isSelectionEmpty) {
      toast.error('No logs to export', 'There are no audit logs found for the selected export scope.');
      return;
    }
    if (mode === 'custom') {
      if (!fromDate && !toDate) {
        toast.error('Date range required', 'Please select at least a start or end date.');
        return;
      }
      if (fromDate && toDate && fromDate > toDate) {
        toast.error('Invalid date range', 'From Date cannot be later than To Date. Please swap the dates.');
        return;
      }
    }
    try {
      setDownloading(true);

      let fromIso: string | undefined;
      let toIso: string | undefined;

      if (mode === 'current_filter') {
        if (currentFilters.from) {
          fromIso = new Date(currentFilters.from).toISOString();
        }
        if (currentFilters.to) {
          const t = new Date(currentFilters.to);
          t.setHours(23, 59, 59, 999);
          toIso = t.toISOString();
        }
      } else if (mode === 'custom') {
        if (!fromDate && !toDate) {
          toast.error('Date range required', 'Please select at least a start or end date.');
          setDownloading(false);
          return;
        }
        if (fromDate && toDate && fromDate > toDate) {
          toast.error('Invalid date range', 'From date cannot be after to date.');
          setDownloading(false);
          return;
        }
        if (fromDate) {
          fromIso = parseLocalDateStart(fromDate);
        }
        if (toDate) {
          toIso = parseLocalDateEnd(toDate);
        }
      }

      toast.info('Generating CSV export...', 'Querying server records and preparing download.');
      await auditLogApi.downloadArchiveCsv({
        from: fromIso,
        to: toIso,
      });

      toast.success('Export completed', 'Full audit log CSV downloaded to your device.');
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to export audit logs.';
      toast.error('Export failed', msg);
    } finally {
      setDownloading(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={downloading ? () => {} : onClose}
      size="md"
      title={
        <div className="flex items-center gap-2">
          <Icon name="download" className="size-5 text-[var(--color-text-primary)]" />
          <span className="text-base font-bold text-[var(--color-text-primary)]">
            Export Audit Logs to CSV
          </span>
        </div>
      }
      footer={
        <div className="flex items-center justify-end gap-2 w-full">
          <Button variant="secondary" onClick={onClose} disabled={downloading}>
            Cancel
          </Button>
          <Button
            variant="primary"
            icon={downloading ? undefined : 'download'}
            loading={downloading}
            onClick={handleExport}
            disabled={downloading || (mode !== 'custom' && isSelectionEmpty)}
            title={mode !== 'custom' && isSelectionEmpty ? 'No matching logs available to export' : undefined}
          >
            Download CSV
          </Button>
        </div>
      }
    >
      <div className="space-y-4 pt-1 text-xs text-[var(--color-text-primary)]">
        <div className="space-y-2">
          <div className="font-semibold text-[var(--color-text-secondary)] uppercase tracking-wider text-[11px]">
            Export Scope
          </div>

          <div className="space-y-2">
            <label
              className={`flex items-start gap-2 p-2.5 rounded border ${
                mode === 'all' && isSelectionEmpty
                  ? 'border-amber-400 dark:border-amber-500'
                  : 'border-[var(--color-border)]'
              } hover:bg-[var(--color-surface-muted)] cursor-pointer`}
            >
              <input
                type="radio"
                name="export_mode"
                value="all"
                checked={mode === 'all'}
                onChange={() => setMode('all')}
                className="mt-0.5"
              />
              <div className="space-y-0.5">
                <div className="font-medium text-[var(--color-text-primary)]">
                  All recorded logs (Complete History)
                </div>
                <div className="text-[11px] text-[var(--color-text-muted)]">
                  Exports every activity record in the database from inception to date{totalCount === 0 ? ' (0 records — nothing to export)' : ''}.
                </div>
              </div>
            </label>

            <label
              className={`flex items-start gap-2 p-2.5 rounded border ${
                mode === 'current_filter' && isSelectionEmpty
                  ? 'border-amber-400 dark:border-amber-500'
                  : 'border-[var(--color-border)]'
              } hover:bg-[var(--color-surface-muted)] cursor-pointer`}
            >
              <input
                type="radio"
                name="export_mode"
                value="current_filter"
                checked={mode === 'current_filter'}
                onChange={() => setMode('current_filter')}
                className="mt-0.5"
              />
              <div className="space-y-0.5">
                <div className="font-medium text-[var(--color-text-primary)]">
                  Current active date filter
                </div>
                <div className="text-[11px] text-[var(--color-text-muted)]">
                  {hasActiveDateFilter
                    ? `From ${currentFilters.from || 'beginning'} to ${currentFilters.to || 'now'}${filteredCount === 0 ? ' (0 records — nothing to export)' : ''}`
                    : 'No date filter currently applied on the search toolbar.'}
                </div>
              </div>
            </label>

            <label
              className={`flex items-start gap-2 p-2.5 rounded border ${
                mode === 'custom' && isSelectionEmpty
                  ? 'border-amber-400 dark:border-amber-500'
                  : 'border-[var(--color-border)]'
              } hover:bg-[var(--color-surface-muted)] cursor-pointer`}
            >
              <input
                type="radio"
                name="export_mode"
                value="custom"
                checked={mode === 'custom'}
                onChange={() => setMode('custom')}
                className="mt-0.5"
              />
              <div className="space-y-0.5 flex-1">
                <div className="font-medium text-[var(--color-text-primary)]">Custom Date Range</div>
                <div className="text-[11px] text-[var(--color-text-muted)]">
                  Specify exact start and end dates for this export.
                </div>
              </div>
            </label>
          </div>

          {mode === 'custom' && (
            <div className="grid grid-cols-2 gap-2 pt-1 pl-6">
              <div>
                <label className="block text-[11px] font-medium text-[var(--color-text-muted)] mb-1">
                  From Date
                </label>
                <input
                  type="date"
                  value={fromDate}
                  onChange={(e) => setFromDate(e.target.value)}
                  className={`w-full h-8 px-2 rounded border ${
                    isCustomInvalidRange
                      ? 'border-rose-400 dark:border-rose-600'
                      : 'border-[var(--color-border)]'
                  } bg-[var(--color-surface)] text-xs text-[var(--color-text-primary)]`}
                />
              </div>
              <div>
                <label className="block text-[11px] font-medium text-[var(--color-text-muted)] mb-1">
                  To Date
                </label>
                <input
                  type="date"
                  value={toDate}
                  onChange={(e) => setToDate(e.target.value)}
                  className={`w-full h-8 px-2 rounded border ${
                    isCustomInvalidRange
                      ? 'border-rose-400 dark:border-rose-600'
                      : 'border-[var(--color-border)]'
                  } bg-[var(--color-surface)] text-xs text-[var(--color-text-primary)]`}
                />
              </div>
              {isCustomInvalidRange && (
                <div className="col-span-2 flex items-center justify-between text-[11px] text-rose-500 dark:text-rose-400 pt-0.5">
                  <span>From Date cannot be later than To Date.</span>
                  <button
                    type="button"
                    onClick={() => {
                      const temp = fromDate;
                      setFromDate(toDate);
                      setToDate(temp);
                    }}
                    className="underline hover:text-rose-600 dark:hover:text-rose-300 font-medium cursor-pointer"
                  >
                    ⇄ Swap Dates
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </Modal>
  );
}

