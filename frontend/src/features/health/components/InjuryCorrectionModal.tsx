'use client';

import React, { useState, useEffect } from 'react';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { Pill } from '@/components/ui/StatusBadge';
import { healthApi } from '../services/healthApi';
import type { InjuryRecord, CorrectInjuryRequest } from '../types';

interface InjuryCorrectionModalProps {
  open: boolean;
  onClose: () => void;
  horseId: number;
  injury: InjuryRecord | null;
  onSuccess: () => void;
}

function InjuryCorrectionModalContent({
  onClose,
  horseId,
  injury,
  onSuccess,
}: {
  onClose: () => void;
  horseId: number;
  injury: InjuryRecord;
  onSuccess: () => void;
}) {
  const [isEditing, setIsEditing] = useState(false);
  const [severity, setSeverity] = useState(injury.severity);
  const [status, setStatus] = useState(injury.status);
  const [description, setDescription] = useState(injury.description || '');
  const [correctionReason, setCorrectionReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !submitting) {
        onClose();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose, submitting]);

  const handleCorrect = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!correctionReason.trim()) {
      setError('Correction reason is required for the audit history log.');
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      const payload: CorrectInjuryRequest = {
        correctionReason: correctionReason.trim(),
        severity,
        status,
        description: description.trim(),
      };
      await healthApi.correctInjury(horseId, injury.id, payload);
      setIsEditing(false);
      onSuccess();
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update injury record.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        className="absolute inset-0 bg-black/40 backdrop-blur-sm"
        onClick={() => !submitting && onClose()}
        aria-hidden="true"
      />
      <div
        role="dialog"
        aria-modal="true"
        className="relative flex w-full max-w-lg flex-col rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-2xl"
      >
        <div className="flex items-center justify-between border-b border-[var(--color-border)] px-6 py-4">
          <div>
            <h2 className="text-base font-bold text-[var(--color-text-primary)]">
              {isEditing ? 'Correct Injury Record' : 'Injury & Condition Details'}
            </h2>
            <p className="text-xs text-[var(--color-text-secondary)] mt-0.5">
              Record ID: #{injury.id} · Horse: #{horseId}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded p-1 text-[var(--color-text-muted)] hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text-primary)]"
          >
            <Icon name="x" size={16} />
          </button>
        </div>

        <div className="p-6 space-y-4 max-h-[75vh] overflow-y-auto">
          {error && (
            <div className="flex items-start gap-2 rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-3 text-xs text-[var(--color-danger)]">
              <Icon name="alert-triangle" size={14} className="mt-0.5 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <div className="rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-4 space-y-3">
            <div className="flex items-center justify-between">
              <span className="font-bold text-sm text-[var(--color-text-primary)]">
                {injury.injuryType}
              </span>
              <div className="flex gap-2">
                <Pill tone={injury.severity === 'CRITICAL' || injury.severity === 'HIGH' ? 'danger' : 'warning'}>
                  {injury.severity}
                </Pill>
                <Pill tone={injury.status === 'ACTIVE' ? 'info' : 'success'}>
                  {injury.status}
                </Pill>
              </div>
            </div>
            <p className="text-xs text-[var(--color-text-secondary)]">
              {injury.description || 'No detailed clinical description provided.'}
            </p>
            <div className="text-[11px] text-[var(--color-text-muted)] border-t border-[var(--color-border)] pt-2">
              Diagnosed: {new Date(injury.diagnosedAt).toLocaleDateString('en-US')}
            </div>
          </div>

          {isEditing && (
            <form onSubmit={handleCorrect} className="space-y-4 border-t border-[var(--color-border)] pt-4">
              <h3 className="font-bold text-[var(--color-text-primary)] uppercase text-[11px]">
                Audit Correction Details
              </h3>

              <div>
                <label className="block text-xs font-semibold text-[var(--color-danger)] mb-1">
                  Correction Reason <span className="text-[var(--color-danger)]">*</span>
                </label>
                <input
                  type="text"
                  placeholder="e.g. Updated diagnosis following radiographic examination..."
                  value={correctionReason}
                  onChange={(e) => setCorrectionReason(e.target.value)}
                  className="w-full rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-surface)] px-3 py-2 text-xs text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-[var(--color-text-primary)] mb-1">
                    Severity
                  </label>
                  <select
                    value={severity}
                    onChange={(e) => setSeverity(e.target.value)}
                    className="w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-2 text-xs text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]"
                  >
                    <option value="LOW">LOW</option>
                    <option value="MEDIUM">MEDIUM</option>
                    <option value="HIGH">HIGH</option>
                    <option value="CRITICAL">CRITICAL</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-[var(--color-text-primary)] mb-1">
                    Status
                  </label>
                  <select
                    value={status}
                    onChange={(e) => setStatus(e.target.value)}
                    className="w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-2 text-xs text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]"
                  >
                    <option value="ACTIVE">ACTIVE (Under treatment)</option>
                    <option value="RECOVERED">RECOVERED (Resolved)</option>
                    <option value="MONITORING">MONITORING (Observation)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-[var(--color-text-primary)] mb-1">
                  Updated Clinical Notes
                </label>
                <textarea
                  rows={3}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] p-2.5 text-xs text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]"
                />
              </div>
            </form>
          )}
        </div>

        <div className="flex items-center justify-end gap-3 border-t border-[var(--color-border)] px-6 py-4">
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Close
          </Button>
          {!isEditing ? (
            <Button variant="primary" icon="plus" onClick={() => setIsEditing(true)}>
              Edit Record
            </Button>
          ) : (
            <Button variant="primary" icon="check" onClick={handleCorrect} loading={submitting}>
              Save Correction
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}

export function InjuryCorrectionModal({
  open,
  onClose,
  horseId,
  injury,
  onSuccess,
}: InjuryCorrectionModalProps) {
  if (!open || !injury) return null;

  return (
    <InjuryCorrectionModalContent
      key={`${horseId}-${injury.id}`}
      onClose={onClose}
      horseId={horseId}
      injury={injury}
      onSuccess={onSuccess}
    />
  );
}
