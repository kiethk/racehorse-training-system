'use client';

import React, { useState } from 'react';
import { z } from 'zod';
import dynamic from 'next/dynamic';
import { Button, FormField, Input, Modal, Notice, Pill, Select, Textarea } from '@/components/ui';
import { healthApi } from '../services/healthApi';
import { displayError, formatDate } from '@/lib/display';
import type { InjuryRecord, CorrectInjuryRequest } from '../types';
import { resolveInjuryWorldPosition, resolveInjuryNormal, isNormalizedPoint } from '@/features/vet/lib/injuryZones';

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
    needsSurfaceSnap?: boolean;
  } | null>(() => {
    if (injury.positionX == null) return null;
    const needsSurfaceSnap =
      injury.normalX == null ||
      isNormalizedPoint({
        x: Number(injury.positionX),
        y: Number(injury.positionY),
        z: Number(injury.positionZ),
      });
    const resolved = resolveInjuryWorldPosition(injury);
    const normal =
      injury.normalX != null && injury.normalY != null && injury.normalZ != null
        ? resolveInjuryNormal(
            [Number(injury.normalX), Number(injury.normalY), Number(injury.normalZ)],
            resolved,
          )
        : resolveInjuryNormal(undefined, resolved);
    return {
      positionX: resolved[0],
      positionY: resolved[1],
      positionZ: resolved[2],
      zoneId: injury.zoneId ?? '',
      normalX: normal[0],
      normalY: normal[1],
      normalZ: normal[2],
      needsSurfaceSnap,
    };
  });

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
      onSuccess();
      onClose();
    } catch (err) {
      setError(displayError(err, 'Failed to save correction'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      open={true}
      onClose={onClose}
      title={`Correct Injury Record #${injury.id}`}
      description="Medical audit trail: all updates create an immutable correction event."
      size="xl"
      footer={
        <div className="flex items-center justify-between gap-3">
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          {!isEditing ? (
            <Button variant="primary" onClick={() => setIsEditing(true)}>
              Edit Record
            </Button>
          ) : (
            <Button
              type="submit"
              form={FORM_ID}
              variant="primary"
              loading={submitting}
              disabled={submitting}
            >
              Save Correction
            </Button>
          )}
        </div>
      }
    >
      <div className="space-y-4">
        {error && <Notice tone="error">{error}</Notice>}

        {/* Read-only summary panel when not editing */}
        {!isEditing ? (
          <div className="space-y-3">
            <div className="grid grid-cols-2 gap-3 text-xs bg-[var(--color-surface-subtle)] p-3 rounded-[var(--radius-sm)] border border-[var(--color-border)]">
              <div>
                <span className="text-[var(--color-text-muted)] block text-[10px] uppercase font-semibold">
                  Injury Type
                </span>
                <span className="font-semibold text-[var(--color-text-primary)]">
                  {injury.injuryType}
                </span>
              </div>
              <div>
                <span className="text-[var(--color-text-muted)] block text-[10px] uppercase font-semibold">
                  Severity
                </span>
                <Pill
                  tone={
                    injury.severity === 'CRITICAL'
                      ? 'danger'
                      : injury.severity === 'HIGH'
                        ? 'warning'
                        : 'neutral'
                  }
                  size="sm"
                >
                  {injury.severity}
                </Pill>
              </div>
              <div>
                <span className="text-[var(--color-text-muted)] block text-[10px] uppercase font-semibold">
                  Status
                </span>
                <Pill
                  tone={
                    injury.status === 'ACTIVE'
                      ? 'danger'
                      : injury.status === 'RECOVERED'
                        ? 'success'
                        : 'neutral'
                  }
                  size="sm"
                >
                  {injury.status}
                </Pill>
              </div>
              <div>
                <span className="text-[var(--color-text-muted)] block text-[10px] uppercase font-semibold">
                  Diagnosed At
                </span>
                <span className="text-[var(--color-text-primary)]">
                  {injury.diagnosedAt ? formatDate(injury.diagnosedAt) : 'Undated'}
                </span>
              </div>
              {injury.expectedRecoveryDate && (
                <div>
                  <span className="text-[var(--color-text-muted)] block text-[10px] uppercase font-semibold">
                    Expected Recovery
                  </span>
                  <span className="text-[var(--color-text-primary)]">
                    {formatDate(injury.expectedRecoveryDate)}
                  </span>
                </div>
              )}
            </div>

            {injury.description && (
              <div className="text-xs">
                <span className="text-[var(--color-text-muted)] block text-[10px] uppercase font-semibold">
                  Clinical Notes
                </span>
                <p className="mt-0.5 text-[var(--color-text-secondary)]">{injury.description}</p>
              </div>
            )}

            {/* Read-only 3D preview */}
            {markerDraft && (
              <div className="space-y-1">
                <span className="text-[var(--color-text-muted)] block text-[10px] uppercase font-semibold">
                  3D Injury Location
                </span>
                <UrgentHorseInjuryViewer
                  injuries={[
                    {
                      id: String(injury.id),
                      position: [markerDraft.positionX, markerDraft.positionY, markerDraft.positionZ],
                      normal: [markerDraft.normalX, markerDraft.normalY, markerDraft.normalZ],
                      needsSurfaceSnap: markerDraft.needsSurfaceSnap,
                      zoneId: markerDraft.zoneId || undefined,
                      label: injury.injuryType,
                      severity: injury.severity,
                      status: injury.status,
                    },
                  ]}
                  isAddMode={false}
                  readOnly={true}
                />
              </div>
            )}
          </div>
        ) : (
          /* Edit form with interactive repositioning */
          <form id={FORM_ID} onSubmit={handleCorrect} className="space-y-3">
            <Notice tone="info">
              Veterinary audit requirement: provide a reason explaining this correction.
            </Notice>

            {/* 3D Reposition canvas */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-[var(--color-text-primary)]">
                  3D Marker Location
                </span>
                <Button
                  type="button"
                  variant={reposition ? 'tertiary' : 'secondary'}
                  size="sm"
                  onClick={() => setReposition((r) => !r)}
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
                          normal: [markerDraft.normalX, markerDraft.normalY, markerDraft.normalZ],
                          needsSurfaceSnap: markerDraft.needsSurfaceSnap,
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
