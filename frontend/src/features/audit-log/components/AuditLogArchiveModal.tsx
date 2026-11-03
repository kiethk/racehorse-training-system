/* eslint-disable no-restricted-syntax */
'use client';

import { useState, useEffect } from 'react';
import { Modal } from '@/components/ui/Modal';
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
type Step = 'config' | 'confirm';

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

  // Reset to initial step when opened
  useEffect(() => {
    if (open) {
      setStep('config');
    }
  }, [open]);

  const getPresetDescription = () => {
    if (preset === '30_days') {
      return `Logs older than 30 days (${status?.agedCount ?? 0} records)`;
    }
    if (preset === '60_days') {
      return 'Logs older than 60 days';
    }
    return `Custom range (${fromDate || 'beginning'} to ${toDate || 'now'})`;
  };

  const handleNextStep = () => {
    if (preset === 'custom' && !fromDate && !toDate) {
      toast.error('Date range required', 'Please select at least a start or end date.');
      return;
    }
    setStep('confirm');
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
              <Button variant="destructive" onClick={handleNextStep}>
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
                Retention &amp; Purge Range
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
