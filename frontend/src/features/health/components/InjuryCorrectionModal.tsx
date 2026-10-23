'use client';

import React, { useState } from 'react';
import { Button, FormField, Input, Modal, Notice, Pill, Select, Textarea } from '@/components/ui';
import { healthApi } from '../services/healthApi';
import { displayError, formatDate } from '@/lib/display';
import type { InjuryRecord, CorrectInjuryRequest } from '../types';

interface InjuryCorrectionModalProps {
  open: boolean;
  onClose: () => void;
  horseId: number;
  injury: InjuryRecord | null;
  onSuccess: () => void;
}

const FORM_ID = 'injury-correction-form';

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
      setError(displayError(err, 'Failed to update injury record.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      open
      onClose={onClose}
      dismissible={!submitting}
      title={isEditing ? 'Correct injury record' : 'Injury & condition details'}
      description={`Record #${injury.id} · Horse #${horseId}`}
      size="md"
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Close
          </Button>
          {isEditing ? (
            <Button type="submit" form={FORM_ID} variant="primary" icon="check" loading={submitting}>
              Save correction
            </Button>
          ) : (
            <Button variant="primary" icon="plus" onClick={() => setIsEditing(true)}>
              Edit record
            </Button>
          )}
        </>
      }
    >
      <div className="space-y-4">
        {error && <Notice tone="error">{error}</Notice>}

        <div className="space-y-3 rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-4">
          <div className="flex items-center justify-between gap-2">
            <span className="text-sm font-bold text-[var(--color-text-primary)]">{injury.injuryType}</span>
            <div className="flex gap-2">
              <Pill tone={injury.severity === 'CRITICAL' || injury.severity === 'HIGH' ? 'danger' : 'warning'}>
                {injury.severity}
              </Pill>
              <Pill tone={injury.status === 'ACTIVE' ? 'info' : 'success'}>{injury.status}</Pill>
            </div>
          </div>
          <p className="text-sm text-[var(--color-text-secondary)]">
            {injury.description || 'No detailed clinical description provided.'}
          </p>
          <p className="border-t border-[var(--color-border)] pt-2 text-xs text-[var(--color-text-muted)]">
            Diagnosed: {formatDate(injury.diagnosedAt)}
          </p>
        </div>

        {isEditing && (
          <form id={FORM_ID} onSubmit={handleCorrect} className="space-y-4 border-t border-[var(--color-border)] pt-4">
            <h3 className="text-sm font-bold uppercase text-[var(--color-text-primary)]">Audit correction details</h3>

            <FormField label="Correction reason" required>
              <Input
                value={correctionReason}
                onChange={(e) => setCorrectionReason(e.target.value)}
                placeholder="e.g. Updated diagnosis following radiographic examination..."
              />
            </FormField>

            <div className="grid grid-cols-2 gap-3">
              <FormField label="Severity">
                <Select value={severity} onChange={(e) => setSeverity(e.target.value)}>
                  <option value="LOW">LOW</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="HIGH">HIGH</option>
                  <option value="CRITICAL">CRITICAL</option>
                </Select>
              </FormField>

              <FormField label="Status">
                <Select value={status} onChange={(e) => setStatus(e.target.value)}>
                  <option value="ACTIVE">ACTIVE (Under treatment)</option>
                  <option value="RECOVERED">RECOVERED (Resolved)</option>
                  <option value="MONITORING">MONITORING (Observation)</option>
                </Select>
              </FormField>
            </div>

            <FormField label="Updated clinical notes">
              <Textarea rows={3} value={description} onChange={(e) => setDescription(e.target.value)} />
            </FormField>
          </form>
        )}
      </div>
    </Modal>
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
