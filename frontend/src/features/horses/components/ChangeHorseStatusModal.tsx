'use client';

import { useEffect, useState } from 'react';
import { Button, FormField, Modal, Notice, Select, StatusBadge, Textarea } from '@/components/ui';
import { displayError } from '@/lib/display';
import { toast } from '@/lib/toast';
import { horseService } from '@/features/horses/services/horseService';
import { stableApi } from '@/features/stable/services/stableService';
import {
  type Horse,
  type HorseStatus,
  type StableStall,
  DEACTIVATION_REASON_OPTIONS,
  REACTIVATION_REASON_OPTIONS,
} from '@/features/stable/types';

interface ChangeHorseStatusModalProps {
  horse: Horse | null;
  open: boolean;
  onClose: () => void;
  onSuccess: (updatedHorse: Horse) => void;
}

export function ChangeHorseStatusModal({
  horse,
  open,
  onClose,
  onSuccess,
}: ChangeHorseStatusModalProps) {
  const currentStatus = horse?.currentStatus;
  const isTransitionToInactive = currentStatus === 'ELIGIBLE';
  const targetStatus: HorseStatus = isTransitionToInactive ? 'INACTIVE' : 'ELIGIBLE';

  const reasonOptions = isTransitionToInactive
    ? DEACTIVATION_REASON_OPTIONS
    : REACTIVATION_REASON_OPTIONS;

  const [reasonCode, setReasonCode] = useState<string>('');
  const [customReason, setCustomReason] = useState<string>('');
  const [availableStalls, setAvailableStalls] = useState<StableStall[]>([]);
  const [loadingStalls, setLoadingStalls] = useState(false);
  const [selectedStallId, setSelectedStallId] = useState<string>('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setReasonCode(reasonOptions[0]?.value || (isTransitionToInactive ? 'RETIRED' : 'VET_CLEARED'));
      setCustomReason('');
      setSelectedStallId('');
      setError(null);
      setSubmitting(false);

      if (!isTransitionToInactive) {
        setLoadingStalls(true);
        stableApi.getAvailableRegularStalls()
          .then((stalls) => setAvailableStalls(stalls))
          .catch((err) => {
            setError(displayError(err, 'Failed to check available stalls.'));
          })
          .finally(() => setLoadingStalls(false));
      } else {
        setAvailableStalls([]);
        setLoadingStalls(false);
      }
    }
  }, [open, horse?.id, isTransitionToInactive]);

  if (!horse) return null;

  const isOther = reasonCode === 'OTHER';
  const isValid = !isOther || (customReason.trim().length > 0 && customReason.trim().length <= 1000);
  const hasNoStallsOnRestore = !isTransitionToInactive && !loadingStalls && availableStalls.length === 0;
  const canSubmit = isValid && !submitting && !loadingStalls && !hasNoStallsOnRestore;

  const handleSubmit = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!canSubmit) return;

    setSubmitting(true);
    setError(null);

    try {
      const selectedOption = reasonOptions.find((opt) => opt.value === reasonCode);
      const effectiveReason = isOther ? customReason.trim() : (selectedOption?.label || reasonCode);

      const updated = await horseService.changeStatus(horse.id, {
        status: targetStatus,
        reasonCode,
        customReason: isOther ? customReason.trim() : undefined,
        reason: effectiveReason,
        stallId: !isTransitionToInactive && selectedStallId ? Number(selectedStallId) : undefined,
      });
      toast.success(
        isTransitionToInactive
          ? `${horse.name} marked as Inactive`
          : `${horse.name} restored to Eligible`,
        isTransitionToInactive
          ? 'Stall released and pending schedules cancelled.'
          : 'Assigned to regular stall with Groom and Trainer.',
      );
      onSuccess(updated);
      onClose();
    } catch (err: unknown) {
      setError(displayError(err, 'Failed to update horse status.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={isTransitionToInactive ? 'Deactivate Horse' : 'Restore Horse'}
      size="md"
      layer="confirm"
      tone="default"
      dismissible={!submitting}
      footer={
        <div className="flex w-full items-center justify-end gap-3">
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button
            variant={isTransitionToInactive ? 'destructive' : 'primary'}
            onClick={() => void handleSubmit()}
            loading={submitting}
            disabled={!canSubmit}
          >
            {isTransitionToInactive ? 'Mark as Inactive' : 'Restore to Eligible'}
          </Button>
        </div>
      }
    >
      <form onSubmit={(e) => void handleSubmit(e)} className="space-y-4">
        {error && <Notice tone="error">{error}</Notice>}

        {/* Target Horse Row */}
        <div className="flex items-center justify-between rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-hover)] px-4 py-3">
          <div>
            <span className="font-semibold text-[var(--color-text-primary)]">{horse.name}</span>
            <span className="ml-2 text-xs text-[var(--color-text-muted)]">
              {horse.breed || 'Breed not recorded'}
            </span>
          </div>
          <div className="flex items-center gap-2">
            <StatusBadge status={horse.currentStatus} />
            <span className="text-xs text-[var(--color-text-muted)]">➔</span>
            <StatusBadge status={targetStatus} />
          </div>
        </div>

        {/* Concise Warning Notice */}
        {isTransitionToInactive ? (
          <Notice tone="warning" title="Automatic Cascade Cleanup">
            Deactivating this horse will automatically vacate their assigned stall, block training clearance, and cancel any scheduled workouts, plans, and care appointments.
          </Notice>
        ) : hasNoStallsOnRestore ? (
          <Notice tone="error" title="No Regular Stalls Available">
            Cannot restore horse: No regular stalls are currently available in the stable. Please vacate or assign an existing regular stall before restoring this horse.
          </Notice>
        ) : (
          <Notice tone="info" title="Restoration to Active">
            Restoring this horse will re-enable training and assign them to an available regular stall, linking them with their Area Trainer and Groom.
          </Notice>
        )}

        {/* Regular Stall Selection (when restoring to ELIGIBLE) */}
        {!isTransitionToInactive && !hasNoStallsOnRestore && (
          <FormField
            label="Regular stall assignment"
            
            required
          >
            <Select
              value={selectedStallId}
              onChange={(e) => setSelectedStallId(e.target.value)}
              disabled={submitting || loadingStalls}
            >
              <option value="">Auto-assign first available regular stall</option>
              {availableStalls.map((stall) => (
                <option key={stall.id} value={stall.id}>
                  Stall {stall.stallCode}
                </option>
              ))}
            </Select>
          </FormField>
        )}

        {/* Reason Dropdown */}
        <FormField label="Reason for status change" required>
          <Select
            value={reasonCode}
            onChange={(e) => setReasonCode(e.target.value)}
            disabled={submitting}
          >
            {reasonOptions.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </Select>
        </FormField>

        {/* Conditional Custom Reason Textarea when 'OTHER' is selected */}
        {isOther && (
          <FormField
            label="Specify details"
            required
            hint="Please provide specific context for this status change."
            error={customReason.length > 1000 ? 'Details cannot exceed 1000 characters' : undefined}
          >
            <Textarea
              value={customReason}
              onChange={(e) => setCustomReason(e.target.value)}
              rows={3}
              placeholder="Enter specific explanation..."
              maxLength={1000}
              disabled={submitting}
              autoFocus
            />
            <div className="mt-1 flex justify-end text-xs text-[var(--color-text-muted)]">
              {customReason.length} / 1000
            </div>
          </FormField>
        )}
      </form>
    </Modal>
  );
}
