'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { stableApi } from '../services/stableService';
import type { StableStall, UserSummary } from '../types';

interface AssignGroomDialogProps {
  open: boolean;
  stall: StableStall | null;
  grooms: UserSummary[];
  stallCountByGroom: Map<number, number>;
  onClose: () => void;
  onAssigned: () => Promise<void> | void;
}

export function AssignGroomDialog({
  open,
  stall,
  grooms,
  stallCountByGroom,
  onClose,
  onAssigned,
}: AssignGroomDialogProps) {
  const [selectedGroomId, setSelectedGroomId] = useState<string>(
    stall?.groomId ? String(stall.groomId) : ''
  );
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!open || !stall) return null;

  async function handleAssign() {
    if (!stall) return;
    setError(null);
    setSubmitting(true);
    try {
      const groomId = selectedGroomId ? Number(selectedGroomId) : null;
      await stableApi.assignGroomToStall(stall.id, groomId);
      await onAssigned();
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Phân công Groom thất bại.');
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
          Phân công Groom cho chuồng {stall.stallCode}
        </h2>
        <p className="mt-1 text-[13px] text-[var(--color-text-secondary)]">
          Mỗi Groom phụ trách tối đa 3 chuồng (BR-06). Bạn cũng có thể chọn gỡ Groom khỏi chuồng này.
        </p>

        {error && (
          <div className="mt-3 rounded-[var(--radius-md)] bg-[var(--color-danger-soft)] p-3 text-[12px] text-[var(--color-danger)]">
            {error}
          </div>
        )}

        <div className="mt-4 space-y-3">
          <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
            Chọn Groom phụ trách
          </label>
          <div className="max-h-60 space-y-2 overflow-y-auto pr-1">
            <label
              className={`flex items-center justify-between rounded-[var(--radius-md)] border p-2.5 text-[13px] cursor-pointer transition ${
                selectedGroomId === ''
                  ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)] text-[var(--color-primary)] font-medium'
                  : 'border-[var(--color-border)] hover:bg-[var(--color-surface-muted)]'
              }`}
            >
              <div className="flex items-center gap-2">
                <input
                  type="radio"
                  name="groom"
                  value=""
                  checked={selectedGroomId === ''}
                  onChange={() => setSelectedGroomId('')}
                  disabled={submitting}
                />
                <span>-- Không gán (Gỡ Groom khỏi chuồng) --</span>
              </div>
            </label>

            {grooms.map((g) => {
              const currentCount = stallCountByGroom.get(g.id) ?? 0;
              const isCurrentStallGroom = stall.groomId === g.id;
              // BR-06: Vượt quá hoặc bằng 3 chuồng (trừ khi đang là Groom của chính chuồng này)
              const isMaxedOut = currentCount >= 3 && !isCurrentStallGroom;

              return (
                <label
                  key={g.id}
                  className={`flex items-center justify-between rounded-[var(--radius-md)] border p-2.5 text-[13px] transition ${
                    isMaxedOut
                      ? 'cursor-not-allowed border-[var(--color-border)] opacity-50 bg-[var(--color-surface-muted)]'
                      : selectedGroomId === String(g.id)
                      ? 'cursor-pointer border-[var(--color-primary)] bg-[var(--color-primary-soft)] text-[var(--color-primary)] font-medium'
                      : 'cursor-pointer border-[var(--color-border)] hover:bg-[var(--color-surface-muted)]'
                  }`}
                >
                  <div className="flex items-center gap-2">
                    <input
                      type="radio"
                      name="groom"
                      value={String(g.id)}
                      checked={selectedGroomId === String(g.id)}
                      onChange={() => !isMaxedOut && setSelectedGroomId(String(g.id))}
                      disabled={isMaxedOut || submitting}
                    />
                    <div>
                      <div className="font-medium text-[var(--color-text-primary)]">{g.fullName}</div>
                      <div className="text-[11px] text-[var(--color-text-muted)]">{g.email}</div>
                    </div>
                  </div>
                  <span
                    className={`text-[11px] font-medium rounded-full px-2 py-0.5 ${
                      isMaxedOut
                        ? 'bg-[var(--color-danger-soft)] text-[var(--color-danger)]'
                        : 'bg-[var(--color-surface-muted)] text-[var(--color-text-secondary)]'
                    }`}
                  >
                    {currentCount}/3 chuồng {isMaxedOut ? '(Đã kín)' : ''}
                  </span>
                </label>
              );
            })}
          </div>
        </div>

        <div className="mt-6 flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Huỷ
          </Button>
          <Button variant="primary" onClick={handleAssign} disabled={submitting}>
            {submitting ? 'Đang lưu...' : 'Lưu phân công'}
          </Button>
        </div>
      </div>
    </div>
  );
}
