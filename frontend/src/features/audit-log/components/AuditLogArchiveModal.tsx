/* eslint-disable no-restricted-syntax */
'use client';

import { useState } from 'react';
import { Modal } from '@/components/ui/Modal';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { toast } from '@/lib/toast';
import { auditLogApi } from '../services/api';
import type { AuditArchiveStatus } from '../types';

interface AuditLogArchiveModalProps {
  open: boolean;
  onClose: () => void;
  status: AuditArchiveStatus | null;
  onPurged: () => void;
}

type ArchivePreset = '30_days' | '60_days' | 'custom';

export function AuditLogArchiveModal({
  open,
  onClose,
  status,
  onPurged,
}: AuditLogArchiveModalProps) {
  const [preset, setPreset] = useState<ArchivePreset>('30_days');
  const [fromDate, setFromDate] = useState<string>('');
  const [toDate, setToDate] = useState<string>('');
  const [reason, setReason] = useState<string>('');
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [showConfirm, setShowConfirm] = useState<boolean>(false);

  const getPresetDescription = () => {
    if (preset === '30_days') {
      return `Logs older than 30 days (${status?.agedCount ?? 0} records)`;
    }
    if (preset === '60_days') {
      return 'Logs older than 60 days';
    }
    return `Custom range (${fromDate || 'beginning'} to ${toDate || 'now'})`;
  };

  const promptConfirm = () => {
    if (preset === 'custom' && !fromDate && !toDate) {
      toast.error('Date range required', 'Please select at least a start or end date.');
      return;
    }
    setShowConfirm(true);
  };

  const handleArchiveAndPurge = async () => {
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
          fromIso = new Date(fromDate).toISOString();
        }
        if (toDate) {
          const t = new Date(toDate);
          t.setHours(23, 59, 59, 999);
          beforeIso = t.toISOString();
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

      setShowConfirm(false);
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
    <>
      <Modal
        open={open}
        onClose={submitting ? () => {} : onClose}
        size="md"
        title={
          <div className="flex items-center gap-2">
            <Icon name="archive" className="size-5 text-[var(--color-text-primary)]" />
            <span className="text-base font-bold text-[var(--color-text-primary)]">
              Archive & Purge Audit Trail
            </span>
          </div>
        }
        footer={
          <div className="flex items-center justify-end gap-2 w-full">
            <Button variant="secondary" onClick={onClose} disabled={submitting}>
              Cancel
            </Button>
            <Button
              variant="destructive"
              icon="download"
              disabled={submitting}
              onClick={promptConfirm}
            >
              Download CSV & Purge
            </Button>
          </div>
        }
      >
        <div className="space-y-4 pt-1 text-xs text-[var(--color-text-primary)]">
          {/* Database Status Overview */}
          {status && (
            <div className="grid grid-cols-3 gap-2 p-2.5 rounded border border-[var(--color-border)] bg-[var(--color-surface)]">
              <div>
                <div className="text-[11px] text-[var(--color-text-muted)]">Total Records</div>
                <div className="font-semibold text-sm">{status.totalCount.toLocaleString()}</div>
              </div>
              <div>
                <div className="text-[11px] text-[var(--color-text-muted)]">Logs &gt; 30 Days</div>
                <div className="font-semibold text-sm text-rose-600 dark:text-rose-400">
                  {status.agedCount.toLocaleString()}
                </div>
              </div>
              <div>
                <div className="text-[11px] text-[var(--color-text-muted)]">Oldest Record</div>
                <div className="font-semibold text-sm">
                  {status.oldestLogDate ? new Date(status.oldestLogDate).toLocaleDateString('en-GB') : 'None'}
                </div>
              </div>
            </div>
          )}

          {/* Retention Range Presets */}
          <div className="space-y-2">
            <div className="font-semibold text-[var(--color-text-secondary)] uppercase tracking-wider text-[11px]">
              Retention & Purge Range
            </div>

            <div className="space-y-2">
              <label className="flex items-start gap-2 p-2.5 rounded border border-[var(--color-border)] hover:bg-[var(--color-surface-muted)] cursor-pointer">
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
                    Keeps recent operational history intact while clearing older logs ({status?.agedCount ?? 0} records).
                  </div>
                </div>
              </label>

              <label className="flex items-start gap-2 p-2.5 rounded border border-[var(--color-border)] hover:bg-[var(--color-surface-muted)] cursor-pointer">
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
                    Retains 2 full months of history and purges older records.
                  </div>
                </div>
              </label>

              <label className="flex items-start gap-2 p-2.5 rounded border border-[var(--color-border)] hover:bg-[var(--color-surface-muted)] cursor-pointer">
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
                    onChange={(e) => setFromDate(e.target.value)}
                    className="w-full h-8 px-2 rounded border border-[var(--color-border)] bg-[var(--color-surface)] text-xs text-[var(--color-text-primary)]"
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
                    className="w-full h-8 px-2 rounded border border-[var(--color-border)] bg-[var(--color-surface)] text-xs text-[var(--color-text-primary)]"
                  />
                </div>
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
        </div>
      </Modal>

      {/* Confirmation Step Dialog */}
      <ConfirmDialog
        open={showConfirm}
        title="Confirm Archive & Purge"
        tone="danger"
        confirmLabel="Download CSV & Purge"
        cancelLabel="Back"
        loading={submitting}
        onCancel={() => setShowConfirm(false)}
        onConfirm={handleArchiveAndPurge}
        description={
          <div className="space-y-3 text-xs">
            <div className="flex items-start gap-2 rounded border border-amber-300 bg-amber-50/70 p-2.5 text-amber-900 dark:border-amber-800/60 dark:bg-amber-950/30 dark:text-amber-200">
              <Icon name="alert-triangle" className="size-4 shrink-0 text-amber-600 dark:text-amber-400 mt-0.5" />
              <div className="leading-relaxed">
                All records matching this selection will be downloaded as a permanent CSV archive first, then <strong>permanently deleted</strong> from the database.
              </div>
            </div>

            <div className="rounded border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-2.5 space-y-1 font-mono text-[11px] text-[var(--color-text-primary)]">
              <div>
                <span className="text-[var(--color-text-muted)] font-sans">Scope: </span>
                {getPresetDescription()}
              </div>
              {reason.trim() && (
                <div>
                  <span className="text-[var(--color-text-muted)] font-sans">Reason: </span>
                  {reason.trim()}
                </div>
              )}
            </div>

            <p className="text-[var(--color-text-muted)] text-[11px]">
              An immutable audit trail entry will record this purge action and your account ID.
            </p>
          </div>
        }
      />
    </>
  );
}
