/* eslint-disable no-restricted-syntax */
'use client';

import { useState, useEffect } from 'react';
import { Modal } from '@/components/ui/Modal';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { toast } from '@/lib/toast';
import { formatDate, formatNumber } from '@/lib/display';
import { auditLogApi } from '../services/api';
import type { AuditArchiveStatus } from '../types';

interface AuditLogArchiveModalProps {
  open: boolean;
  onClose: () => void;
  status: AuditArchiveStatus | null;
  onPurged: () => void;
}

type ArchivePreset = '30_days' | '60_days' | 'custom';
type Step = 'config' | 'confirm';

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

export function AuditLogArchiveModal({
  open,
  onClose,
  status,
  onPurged,
}: AuditLogArchiveModalProps) {
  const [step, setStep] = useState<Step>('config');
  const [preset, setPreset] = useState<ArchivePreset>('30_days');
  const [fromDate, setFromDate] = useState<string>('');
  const [toDate, setToDate] = useState<string>('');
  const [reason, setReason] = useState<string>('');
  const [submitting, setSubmitting] = useState<boolean>(false);

  // eslint-disable-next-line react-hooks/purity
  const thirtyDaysAgoStr = new Date(Date.now() - 30 * 86400000).toISOString().split('T')[0];

  // Reset to initial step when opened
  useEffect(() => {
    if (open) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setStep('config');
    }
  }, [open]);

  const isTotalEmpty = (status?.totalCount ?? 0) === 0;
  const isAgedEmpty = (status?.agedCount ?? 0) === 0;

  const isSelectionEmpty = (() => {
    if (!status || isTotalEmpty) return true;
    if (preset === '30_days') return isAgedEmpty;
    if (preset === '60_days') return isAgedEmpty;
    return false;
  })();

  const isCustomInvalidRange = Boolean(preset === 'custom' && fromDate && toDate && fromDate > toDate);
  const isCustomRetentionViolation = Boolean(
    preset === 'custom' && ((toDate && toDate > thirtyDaysAgoStr) || (fromDate && fromDate > thirtyDaysAgoStr))
  );

  const getPresetDescription = () => {
    if (preset === '30_days') {
      return `Logs older than 30 days (${status?.agedCount ?? 0} records)`;
    }
    if (preset === '60_days') {
      return `Logs older than 60 days (${isAgedEmpty ? '0' : 'older than 60d'} records)`;
    }
    return `Custom range (${fromDate || 'beginning'} to ${toDate || 'now'})`;
  };

  const handleNextStep = () => {
    if (preset !== 'custom' && isSelectionEmpty) {
      toast.error('No records to purge', 'There are no audit logs matching the selected retention criteria.');
      return;
    }
    if (preset === 'custom') {
      if (!fromDate && !toDate) {
        toast.error('Date range required', 'Please select at least a start or end date.');
        return;
      }
      if (fromDate && toDate && fromDate > toDate) {
        toast.error('Invalid date range', 'From Date cannot be later than To Date. Please swap the dates.');
        return;
      }
      if (isCustomRetentionViolation) {
        toast.error('Retention policy violation', 'Audit logs within the last 30 days cannot be purged (BR-MGR-19).');
        return;
      }
    }
    setStep('confirm');
  };

  const handleArchiveAndPurge = async () => {
    if (isSelectionEmpty) {
      toast.error('No records to purge', 'No audit logs found for the selected criteria.');
      return;
    }
    try {
      setSubmitting(true);

      let beforeIso: string | undefined;
      let fromIso: string | undefined;

      if (preset === '30_days') {
        const d = new Date();
        d.setDate(d.getDate() - 30);
        beforeIso = d.toISOString();
      } else if (preset === '60_days') {
        const d = new Date();
        d.setDate(d.getDate() - 60);
        beforeIso = d.toISOString();
      } else {
        if (fromDate) {
          fromIso = parseLocalDateStart(fromDate);
        }
        if (toDate) {
          beforeIso = parseLocalDateEnd(toDate);
        }
      }

      // Step 1: Download CSV
      toast.info('Exporting archive...', 'Downloading complete CSV records to your device.');
      await auditLogApi.downloadArchiveCsv({
        from: fromIso,
        before: beforeIso,
      });

      // Step 2: Purge logs from database
      const purgeResult = await auditLogApi.purgeAuditLogs({
        fromDate: fromIso,
        beforeDate: beforeIso,
        reason: reason.trim() || undefined,
      });

      toast.success(
        'Archive & purge complete',
        `Downloaded CSV file and purged ${purgeResult.purgedCount} records from the database.`
      );

      onPurged();
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to archive and purge audit logs.';
      toast.error('Operation failed', msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={submitting ? () => {} : onClose}
      size="md"
      title={
        <div className="flex items-center gap-2">
          <Icon
            name={step === 'confirm' ? 'alert-triangle' : 'archive'}
            className={`size-5 ${
              step === 'confirm'
                ? 'text-rose-600 dark:text-rose-400'
                : 'text-[var(--color-text-primary)]'
            }`}
          />
          <span className="text-base font-bold text-[var(--color-text-primary)]">
            {step === 'confirm' ? 'Confirm Database Purge' : 'Archive & Purge Audit Trail'}
          </span>
        </div>
      }
      footer={
        <div className="flex items-center justify-end gap-2 w-full">
          {step === 'config' ? (
            <>
              <Button variant="secondary" onClick={onClose} disabled={submitting}>
                Cancel
              </Button>
              <Button
                variant="destructive"
                onClick={handleNextStep}
                disabled={submitting || (preset !== 'custom' && isSelectionEmpty) || isCustomRetentionViolation}
                title={
                  isCustomRetentionViolation
                    ? 'Logs within the last 30 days must be retained'
                    : preset !== 'custom' && isSelectionEmpty
                    ? 'No matching logs available to purge'
                    : undefined
                }
              >
                Review &amp; Purge &rarr;
              </Button>
            </>
          ) : (
            <>
              <Button
                variant="secondary"
                onClick={() => setStep('config')}
                disabled={submitting}
              >
                &larr; Back
              </Button>
              <Button
                variant="destructive"
                icon={submitting ? undefined : 'download'}
                loading={submitting}
                onClick={handleArchiveAndPurge}
                disabled={isSelectionEmpty || submitting}
                title={isSelectionEmpty ? 'No matching logs available to purge' : undefined}
              >
                Download CSV &amp; Purge
              </Button>
            </>
          )}
        </div>
      }
    >
      <div className="space-y-4 pt-1 text-xs text-[var(--color-text-primary)]">
        {step === 'config' ? (
          <>
            {/* Database Status Overview */}
            {status && (
              <div className="grid grid-cols-3 gap-2 p-2.5 rounded border border-[var(--color-border)] bg-[var(--color-surface)]">
                <div>
                  <div className="text-[11px] text-[var(--color-text-muted)]">Total Records</div>
                  <div className="font-semibold text-sm">{formatNumber(status.totalCount)}</div>
                </div>
                <div>
                  <div className="text-[11px] text-[var(--color-text-muted)]">Logs &gt; 30 Days</div>
                  <div className="font-semibold text-sm">
                    {formatNumber(status.agedCount)}
                  </div>
                </div>
                <div>
                  <div className="text-[11px] text-[var(--color-text-muted)]">Oldest Record</div>
                  <div className="font-semibold text-sm">
                    {status.oldestLogDate ? formatDate(status.oldestLogDate) : 'None'}
                  </div>
                </div>
              </div>
            )}

            {/* Retention Range Presets */}
            <div className="space-y-2">
              <div className="font-semibold text-[var(--color-text-secondary)] uppercase tracking-wider text-[11px]">
                Retention &amp; Purge Range
              </div>

              <div className="space-y-2">
                <label
                  className={`flex items-start gap-2 p-2.5 rounded border ${
                    preset === '30_days' && isAgedEmpty
                      ? 'border-amber-400 dark:border-amber-500'
                      : 'border-[var(--color-border)]'
                  } hover:bg-[var(--color-surface-muted)] cursor-pointer`}
                >
                  <input
                    type="radio"
                    name="archive_preset"
                    value="30_days"
                    checked={preset === '30_days'}
                    onChange={() => setPreset('30_days')}
                    className="mt-0.5"
                  />
                  <div className="space-y-0.5">
                    <div className="font-medium text-[var(--color-text-primary)]">
                      Logs older than 30 days (Recommended)
                    </div>
                    <div className="text-[11px] text-[var(--color-text-muted)]">
                      Keeps recent operational history intact while clearing older logs ({status?.agedCount ?? 0} records{isAgedEmpty ? ' — nothing to purge' : ''}).
                    </div>
                  </div>
                </label>

                <label
                  className={`flex items-start gap-2 p-2.5 rounded border ${
                    preset === '60_days' && isAgedEmpty
                      ? 'border-amber-400 dark:border-amber-500'
                      : 'border-[var(--color-border)]'
                  } hover:bg-[var(--color-surface-muted)] cursor-pointer`}
                >
                  <input
                    type="radio"
                    name="archive_preset"
                    value="60_days"
                    checked={preset === '60_days'}
                    onChange={() => setPreset('60_days')}
                    className="mt-0.5"
                  />
                  <div className="space-y-0.5">
                    <div className="font-medium text-[var(--color-text-primary)]">
                      Logs older than 60 days
                    </div>
                    <div className="text-[11px] text-[var(--color-text-muted)]">
                      Retains 2 full months of history and purges older records{isAgedEmpty ? ' (0 records — nothing to purge)' : ''}.
                    </div>
                  </div>
                </label>

                <label
                  className={`flex items-start gap-2 p-2.5 rounded border ${
                    preset === 'custom' && isTotalEmpty
                      ? 'border-amber-400 dark:border-amber-500'
                      : 'border-[var(--color-border)]'
                  } hover:bg-[var(--color-surface-muted)] cursor-pointer`}
                >
                  <input
                    type="radio"
                    name="archive_preset"
                    value="custom"
                    checked={preset === 'custom'}
                    onChange={() => setPreset('custom')}
                    className="mt-0.5"
                  />
                  <div className="space-y-0.5 flex-1">
                    <div className="font-medium text-[var(--color-text-primary)]">Custom Date Range</div>
                    <div className="text-[11px] text-[var(--color-text-muted)]">
                      Specify exact start and end cutoff dates.
                    </div>
                  </div>
                </label>
              </div>

              {preset === 'custom' && (
                <div className="grid grid-cols-2 gap-2 pt-1 pl-6">
                  <div>
                    <label className="block text-[11px] font-medium text-[var(--color-text-muted)] mb-1">
                      From Date
                    </label>
                    <input
                      type="date"
                      value={fromDate}
                      max={thirtyDaysAgoStr}
                      onChange={(e) => setFromDate(e.target.value)}
                      className={`w-full h-8 px-2 rounded border ${
                        isCustomInvalidRange || (fromDate && fromDate > thirtyDaysAgoStr)
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
                      max={thirtyDaysAgoStr}
                      onChange={(e) => setToDate(e.target.value)}
                      className={`w-full h-8 px-2 rounded border ${
                        isCustomInvalidRange || (toDate && toDate > thirtyDaysAgoStr)
                          ? 'border-rose-400 dark:border-rose-600'
                          : 'border-[var(--color-border)]'
                      } bg-[var(--color-surface)] text-xs text-[var(--color-text-primary)]`}
                    />
                  </div>
                  {isCustomRetentionViolation && (
                    <div className="col-span-2 text-[11px] text-rose-500 dark:text-rose-400 pt-0.5">
                      Cutoff date must be at least 30 days ago (on or before {thirtyDaysAgoStr}).
                    </div>
                  )}
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

            {/* Justification / Note */}
            <div>
              <label className="block text-[11px] font-semibold text-[var(--color-text-secondary)] uppercase tracking-wider mb-1">
                Reason (Optional)
              </label>
              <input
                type="text"
                placeholder="e.g., Routine storage optimization for semester audit"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                className="w-full h-8 px-2.5 rounded border border-[var(--color-border)] bg-[var(--color-surface)] text-xs text-[var(--color-text-primary)] placeholder:text-[var(--color-text-muted)]"
              />
            </div>
          </>
        ) : (
          /* Step 2: Minimal Direct Confirmation */
          <div className="space-y-3 py-1 text-sm text-[var(--color-text-primary)]">
            <p className="leading-relaxed">
              Once you click <strong>Download CSV &amp; Purge</strong>, a CSV copy will be downloaded to your computer and these records will be <strong>permanently deleted from the database</strong>.
            </p>

            <div className="rounded border border-[var(--color-border)] bg-[var(--color-surface-muted)] px-3 py-2 text-xs font-mono">
              <span className="text-[var(--color-text-muted)] font-sans">Scope: </span>
              <span className="font-semibold text-[var(--color-text-primary)]">{getPresetDescription()}</span>
              {reason.trim() && (
                <div className="mt-1 pt-1 border-t border-[var(--color-border)]">
                  <span className="text-[var(--color-text-muted)] font-sans">Reason: </span>
                  <span className="text-[var(--color-text-secondary)]">{reason.trim()}</span>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
}
