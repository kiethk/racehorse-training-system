'use client';

import { useState } from 'react';
import { Button, FormField, Modal, Notice, Select } from '@/components/ui';
import { displayError } from '@/lib/display';
import { toast } from '@/lib/toast';
import { stableApi } from '../services/stableService';
import type { Area, Horse, StableStall } from '../types';

interface MoveHorseDialogProps {
  horse: Horse;
  from: StableStall;
  /** Stalls the horse may move to: empty and AVAILABLE, in the trainer's areas. */
  targets: StableStall[];
  areas: Area[];
  /** Pre-selected destination, e.g. the stall the horse was dropped on in the 3D barn. */
  initialTargetId?: number | null;
  groomName: (groomId: number | null) => string;
  onClose: () => void;
  onMoved: (to: StableStall) => Promise<void> | void;
}

/** Moves a horse straight to another stall in one request; the old stall is released by the backend. */
export function MoveHorseDialog({ horse, from, targets, areas, initialTargetId, groomName, onClose, onMoved }: MoveHorseDialogProps) {
  const [targetId, setTargetId] = useState<number | ''>(
    initialTargetId != null && targets.some((stall) => stall.id === initialTargetId) ? initialTargetId : '',
  );
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const target = targets.find((stall) => stall.id === targetId);
  const groomChanges = target != null && target.groomId !== from.groomId;

  async function handleMove() {
    if (!target) return;
    setError(null);
    setSubmitting(true);
    try {
      await stableApi.assignHorseToStall(horse.id, target.id);
      toast.success(`${horse.name} moved to stall ${target.stallCode}.`);
      onClose();
      await onMoved(target);
    } catch (err) {
      // The stall may have been taken meanwhile; the backend message says so.
      setError(displayError(err, 'Unable to move the horse. Refresh the map and try again.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal
      open
      onClose={onClose}
      size="sm"
      dismissible={!submitting}
      title={`Move ${horse.name}`}
      description={`Currently in stall ${from.stallCode}. Stall ${from.stallCode} becomes available after the move.`}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>Cancel</Button>
          <Button variant="primary" icon="check" onClick={handleMove} disabled={!target} loading={submitting}>
            Move horse
          </Button>
        </>
      }
    >
      <div className="space-y-3">
        {error && <Notice tone="error">{error}</Notice>}
        <FormField label={`Destination stall (${targets.length} available)`}>
          {targets.length === 0 ? (
            <p className="text-xs text-[var(--color-text-muted)]">There is no empty stall in your areas right now.</p>
          ) : (
            <Select
              value={targetId}
              onChange={(event) => setTargetId(event.target.value ? Number(event.target.value) : '')}
              disabled={submitting}
            >
              <option value="">Select a stall</option>
              {areas.map((area) => {
                const options = targets.filter((stall) => stall.areaId === area.id);
                return options.length === 0 ? null : (
                  <optgroup key={area.id} label={area.name}>
                    {options.map((stall) => (
                      <option key={stall.id} value={stall.id}>
                        {stall.stallCode} · {groomName(stall.groomId)}
                      </option>
                    ))}
                  </optgroup>
                );
              })}
            </Select>
          )}
        </FormField>
        {groomChanges && (
          <Notice tone="info">
            The groom changes from {groomName(from.groomId)} to {groomName(target.groomId)}. Upcoming workouts follow the new stall&apos;s groom.
          </Notice>
        )}
      </div>
    </Modal>
  );
}
