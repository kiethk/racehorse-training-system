'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/Button';
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
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        className="absolute inset-0 bg-black/30 backdrop-blur-sm"
        onClick={() => !submitting && onClose()}
        aria-hidden="true"
      />
      <div
        role="dialog"
        aria-modal="true"
        className="relative w-full max-w-md rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-6 shadow-xl"
      >
        <h2 className="text-[16px] font-semibold text-[var(--color-text-primary)]">
          Assign horse to stall {stall.stallCode}
        </h2>
        <p className="mt-1 text-[13px] text-[var(--color-text-secondary)]">
          Choose an unassigned horse for this stall.
        </p>

        {isStallOccupied && (
          <div className="mt-3 rounded-[var(--radius-md)] bg-[var(--color-danger-soft)] p-3 text-[12px] text-[var(--color-danger)]">
            This stall is occupied. Remove or move the current horse before assigning another horse.
          </div>
        )}

        {error && (
          <div className="mt-3 rounded-[var(--radius-md)] bg-[var(--color-danger-soft)] p-3 text-[12px] text-[var(--color-danger)]">
            {error}
          </div>
        )}

        <div className="mt-4">
          <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
            Available horses ({availableHorses.length})
          </label>
          {availableHorses.length === 0 ? (
            <p className="mt-2 text-[12px] text-[var(--color-text-muted)]">
              There are no unassigned horses available.
            </p>
          ) : (
            <select
              value={selectedHorseId}
              onChange={(e) => setSelectedHorseId(e.target.value ? Number(e.target.value) : '')}
              disabled={isStallOccupied || submitting}
              className="mt-1.5 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            >
              <option value="">-- Select a horse --</option>
              {availableHorses.map((h) => (
                <option key={h.id} value={h.id}>
                  {h.name} ({h.breed || 'Breed unknown'} · Status: {h.currentStatus})
                </option>
              ))}
            </select>
          )}
        </div>

        <div className="mt-6 flex justify-end gap-2">
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
        </div>
      </div>
    </div>
  );
}
