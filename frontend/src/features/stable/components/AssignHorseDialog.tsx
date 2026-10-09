'use client';

import { useState } from 'react';
import { Button, FormField, Modal, Notice, Select } from '@/components/ui';
import { displayError } from '@/lib/display';
import { stableApi } from '../services/stableService';
import type { StableStall, Horse } from '../types';

interface AssignHorseDialogProps {
  open: boolean;
  stall: StableStall | null;
  horses: Horse[];
  onClose: () => void;
  onAssigned: () => Promise<void> | void;
}

export function AssignHorseDialog({
  open,
  stall,
  horses,
  onClose,
  onAssigned,
}: AssignHorseDialogProps) {
  const [selectedHorseId, setSelectedHorseId] = useState<number | ''>('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!open || !stall) return null;

  // BR-07: Chỉ liệt kê ngựa chưa có chuồng (currentStallId == null)
  const availableHorses = horses.filter((h) => h.currentStallId === null);
  const isStallOccupied = stall.status === 'OCCUPIED';

  async function handleAssign() {
    if (!selectedHorseId || !stall) return;
    setError(null);
    setSubmitting(true);
    try {
      await stableApi.assignHorseToStall(Number(selectedHorseId), stall.id);
      await onAssigned();
      onClose();
    } catch (err) {
      setError(displayError(err, 'Unable to assign the horse to this stall.'));
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
      title={`Assign horse to stall ${stall.stallCode}`}
      description="Choose an unassigned horse for this stall."
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button
            variant="primary"
            onClick={handleAssign}
            disabled={!selectedHorseId || isStallOccupied || submitting}
          >
            {submitting ? 'Assigning…' : 'Confirm assignment'}
          </Button>
        </>
      }
    >
      <div className="space-y-3">
        {isStallOccupied && (
          <Notice tone="error">
            This stall is occupied. Remove or move the current horse before assigning another horse.
          </Notice>
        )}

        {error && <Notice tone="error">{error}</Notice>}

        <FormField label={`Available horses (${availableHorses.length})`}>
          {availableHorses.length === 0 ? (
            <p className="text-xs text-[var(--color-text-muted)]">
              There are no unassigned horses available.
            </p>
          ) : (
            <Select
              value={selectedHorseId}
              onChange={(e) => setSelectedHorseId(e.target.value ? Number(e.target.value) : '')}
              disabled={isStallOccupied || submitting}
            >
              <option value="">-- Select a horse --</option>
              {availableHorses.map((h) => (
                <option key={h.id} value={h.id}>
                  {h.name} ({h.breed || 'Breed unknown'} · Status: {h.currentStatus})
                </option>
              ))}
            </Select>
          )}
        </FormField>
      </div>
    </Modal>
  );
}
