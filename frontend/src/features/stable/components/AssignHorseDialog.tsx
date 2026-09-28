'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/Button';
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
      setError(err instanceof Error ? err.message : 'Xếp ngựa vào chuồng thất bại.');
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
          Xếp ngựa vào chuồng {stall.stallCode}
        </h2>
        <p className="mt-1 text-[13px] text-[var(--color-text-secondary)]">
          Chọn một chiến mã chưa có chuồng để xếp vào ô chuồng này (BR-07: mỗi ngựa 1 chuồng).
        </p>

        {isStallOccupied && (
          <div className="mt-3 rounded-[var(--radius-md)] bg-[var(--color-danger-soft)] p-3 text-[12px] text-[var(--color-danger)]">
            Chuồng này hiện đang có ngựa ở (OCCUPIED). Vui lòng dọn chuồng hoặc chuyển ngựa cũ trước khi xếp ngựa mới.
          </div>
        )}

        {error && (
          <div className="mt-3 rounded-[var(--radius-md)] bg-[var(--color-danger-soft)] p-3 text-[12px] text-[var(--color-danger)]">
            {error}
          </div>
        )}

        <div className="mt-4">
          <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
            Chiến mã sẵn sàng ({availableHorses.length})
          </label>
          {availableHorses.length === 0 ? (
            <p className="mt-2 text-[12px] text-[var(--color-text-muted)]">
              Không có chiến mã nào chưa được xếp chuồng.
            </p>
          ) : (
            <select
              value={selectedHorseId}
              onChange={(e) => setSelectedHorseId(e.target.value ? Number(e.target.value) : '')}
              disabled={isStallOccupied || submitting}
              className="mt-1.5 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            >
              <option value="">-- Chọn chiến mã --</option>
              {availableHorses.map((h) => (
                <option key={h.id} value={h.id}>
                  {h.name} ({h.breed || 'Chưa rõ giống'} · Trạng thái: {h.currentStatus})
                </option>
              ))}
            </select>
          )}
        </div>

        <div className="mt-6 flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Huỷ
          </Button>
          <Button
            variant="primary"
            onClick={handleAssign}
            disabled={!selectedHorseId || isStallOccupied || submitting}
          >
            {submitting ? 'Đang xếp...' : 'Xác nhận xếp chuồng'}
          </Button>
        </div>
      </div>
    </div>
  );
}
