'use client';

import React, { useState } from 'react';
import { z } from 'zod';
import dynamic from 'next/dynamic';
import { Button, FormField, Input, Modal, Notice, Pill, Select, Textarea } from '@/components/ui';
import { healthApi } from '../services/healthApi';
import { displayError, formatDate } from '@/lib/display';
import type { InjuryRecord, CorrectInjuryRequest } from '../types';

// The 3D canvas (three.js + GLB) must stay out of the server bundle.
const UrgentHorseInjuryViewer = dynamic(
  () => import('@/features/vet/components/UrgentHorseInjuryViewer'),
  { ssr: false, loading: () => <div className="h-[320px] w-full rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] animate-pulse" /> },
);

interface InjuryCorrectionModalProps {
  open: boolean;
  onClose: () => void;
  horseId: number;
  injury: InjuryRecord | null;
  onSuccess: () => void;
}

const FORM_ID = 'injury-correction-form';

const correctInjurySchema = z.object({
  correctionReason: z.string().min(1, 'Correction reason is required for the audit history log.'),
  severity: z.string().optional(),
  status: z.string().optional(),
  description: z.string().optional(),
});

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
  const [fieldErrors, setFieldErrors] = useState<{ correctionReason?: string }>({});
  const [reposition, setReposition] = useState(false);
  const [markerDraft, setMarkerDraft] = useState<{
    positionX: number;
    positionY: number;
    positionZ: number;
    zoneId: string;
    normalX: number;
    normalY: number;
    normalZ: number;
  } | null>(injury.positionX != null
    ? {
        positionX: injury.positionX,
        positionY: injury.positionY ?? 0,
        positionZ: injury.positionZ ?? 0,
        zoneId: injury.zoneId ?? '',
        normalX: injury.normalX ?? 0,
        normalY: injury.normalY ?? 1,
        normalZ: injury.normalZ ?? 0,
      }
    : null);

  const handleCorrect = async (e: React.FormEvent) => {
    e.preventDefault();
    const parsed = correctInjurySchema.safeParse({
      correctionReason: correctionReason.trim(),
      severity,
      status,
      description: description.trim(),
    });

    if (!parsed.success) {
      const errors: { correctionReason?: string } = {};
      for (const issue of parsed.error.issues) {
        const field = issue.path[0];
        if (field === 'correctionReason') {
          errors.correctionReason = issue.message;
        }
      }
      setFieldErrors(errors);
      return;
    }

    setFieldErrors({});
    setSubmitting(true);
    setError(null);
    try {
      const payload: CorrectInjuryRequest = {
        correctionReason: correctionReason.trim(),
        severity,
        status,
        description: description.trim(),
      };
      if (markerDraft && reposition) {
        payload.marker = {
          positionX: markerDraft.positionX,
          positionY: markerDraft.positionY,
          positionZ: markerDraft.positionZ,
          zoneId: markerDraft.zoneId || 'trunk',
          normalX: markerDraft.normalX,
          normalY: markerDraft.normalY,
          normalZ: markerDraft.normalZ,
        };
      }
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

            {/* Marker reposition on the 3D model (optional) */}
            <div className="space-y-2 rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-3">
              <div className="flex items-center justify-between gap-2">
                <span className="text-xs font-semibold text-[var(--color-text-primary)]">
                  3D marker placement
                </span>
                <Button
                  type="button"
                  size="sm"
                  variant={reposition ? 'warning' : 'secondary'}
                  onClick={() => setReposition(!reposition)}
                  disabled={!markerDraft && !reposition}
                  title={
                    markerDraft
                      ? 'Reposition the marker by clicking a new spot on the model'
                      : 'This record has no 3D marker yet'
                  }
                >
                  {reposition ? 'Click the model to place' : 'Reposition marker'}
                </Button>
              </div>

              {!markerDraft && !reposition && (
                <p className="text-xs text-[var(--color-text-muted)]">
                  This injury has no 3D marker stored. Reposition mode cannot place a new marker yet.
                </p>
              )}

              <UrgentHorseInjuryViewer
                injuries={
                  markerDraft
                    ? [
                        {
                          id: 'draft',
                          position: [markerDraft.positionX, markerDraft.positionY, markerDraft.positionZ],
                          zoneId: markerDraft.zoneId || undefined,
                          label: 'New position',
                        },
                      ]
                    : []
                }
                isAddMode={reposition}
                readOnly={!reposition}
                onPointerHit={(hit) =>
                  setMarkerDraft({
                    positionX: hit.position[0],
                    positionY: hit.position[1],
                    positionZ: hit.position[2],
                    zoneId: hit.zoneId,
                    normalX: hit.normal[0],
                    normalY: hit.normal[1],
                    normalZ: hit.normal[2],
                  })
                }
              />
              {reposition && markerDraft && (
                <p className="text-xs text-[var(--color-success)]">
                  New placement captured — Save correction to persist it (zone: {markerDraft.zoneId}).
                </p>
              )}
            </div>

            <FormField label="Correction reason" required error={fieldErrors.correctionReason}>
              <Input
                value={correctionReason}
                onChange={(e) => {
                  setCorrectionReason(e.target.value);
                  setFieldErrors({});
                }}
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
