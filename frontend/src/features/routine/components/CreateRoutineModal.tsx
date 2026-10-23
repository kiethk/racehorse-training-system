'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { admissionsApi } from '@/features/admissions/services/api';
import type { Horse } from '@/features/stable/types';
import { validateCreateRoutineForm, getMinRoutineDate } from '../utils/routineValidation';

interface Props {
  open: boolean;
  horses: Horse[];
  onClose: () => void;
  onSuccess: () => void;
}

export function CreateRoutineModal({ open, horses, onClose, onSuccess }: Props) {
  const minDate = getMinRoutineDate();
  const [horseId, setHorseId] = useState<number>(0);
  const [careType, setCareType] = useState<string>('ROUTINE_EXAM');
  const [scheduledDate, setScheduledDate] = useState<string>(() => minDate);
  const [description, setDescription] = useState<string>('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  if (!open) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitError(null);

    const validationErrors = validateCreateRoutineForm({
      horseId,
      scheduledDate,
      description,
      todayDateStr: minDate,
    });

    setErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) return;

    try {
      setSubmitting(true);
      await admissionsApi.createPreventiveCareSchedule({
        horseId,
        careType,
        scheduledDate,
        description: description.trim(),
      });
      onSuccess();
      onClose();
    } catch (err: unknown) {
      let msg = 'Unable to schedule preventive care.';
      const raw = err instanceof Error ? err.message : String(err);
      if (raw.includes('previously examined') || raw.includes('403')) {
        msg = 'You can only schedule preventive care for horses you have previously examined.';
      } else if (raw.includes('IDEMPOTENCY_CONFLICT') || raw.includes('409')) {
        msg = 'A care schedule of this type already exists for this horse on the selected date.';
      } else if (raw.includes('INVALID_DATE') || raw.includes('future') || raw.includes('400')) {
        msg = 'Scheduled date is invalid or past today cutoff time (14:00). Please select the next available date.';
      } else {
        msg = raw;
      }
      setSubmitError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
      <div
        role="dialog"
        aria-modal="true"
        className="w-full max-w-lg rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-2xl overflow-hidden"
      >
        {/* Header */}
        <div className="flex items-center justify-between border-b border-[var(--color-border)] px-6 py-4 bg-[var(--color-surface-subtle)]">
          <div className="flex items-center gap-2">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-[var(--color-primary-soft)] text-[var(--color-primary)]">
              <Icon name="calendar" size={18} />
            </span>
            <h2 className="text-base font-bold text-[var(--color-text-primary)]">
              Schedule Preventive Care
            </h2>
          </div>
          <Button variant="secondary" size="sm" icon="x" onClick={onClose} disabled={submitting}>
            Close
          </Button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {submitError && (
            <div className="rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-3 text-xs text-[var(--color-danger)]">
              {submitError}
            </div>
          )}

          {/* Horse Select */}
          <div>
            <label className="block text-xs font-semibold text-[var(--color-text-primary)] mb-1">
              Horse <span className="text-[var(--color-danger)]">*</span>
            </label>
            <select
              value={horseId}
              onChange={(e) => setHorseId(Number(e.target.value))}
              disabled={submitting}
              className={`w-full rounded-[var(--radius-sm)] border bg-[var(--color-surface)] px-3 py-2 text-xs text-[var(--color-text-primary)] outline-none ${
                errors.horseId ? 'border-[var(--color-danger)]' : 'border-[var(--color-border)] focus:border-[var(--color-primary)]'
              }`}
            >
              <option value={0}>-- Select horse --</option>
              {horses.map((h) => (
                <option key={h.id} value={h.id}>
                  {h.name} (#{h.id}) - {h.breed || 'Unknown breed'}
                </option>
              ))}
            </select>
            {errors.horseId && <p className="mt-1 text-[11px] text-[var(--color-danger)]">{errors.horseId}</p>}
          </div>

          {/* Care Type */}
          <div>
            <label className="block text-xs font-semibold text-[var(--color-text-primary)] mb-1">
              Care Type
            </label>
            <select
              value={careType}
              onChange={(e) => setCareType(e.target.value)}
              disabled={submitting}
              className="w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-2 text-xs text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]"
            >
              <option value="ROUTINE_EXAM">Routine Exam / Follow-up</option>
              <option value="VACCINATION">Vaccination</option>
              <option value="DEWORMING">Deworming</option>
            </select>
          </div>

          {/* Scheduled Date */}
          <div>
            <label className="block text-xs font-semibold text-[var(--color-text-primary)] mb-1">
              Scheduled Date <span className="text-[var(--color-danger)]">*</span>
            </label>
            <input
              type="date"
              min={minDate}
              value={scheduledDate}
              onChange={(e) => setScheduledDate(e.target.value)}
              disabled={submitting}
              className={`w-full rounded-[var(--radius-sm)] border bg-[var(--color-surface)] px-3 py-2 text-xs text-[var(--color-text-primary)] outline-none ${
                errors.scheduledDate ? 'border-[var(--color-danger)]' : 'border-[var(--color-border)] focus:border-[var(--color-primary)]'
              }`}
            />
            {errors.scheduledDate && <p className="mt-1 text-[11px] text-[var(--color-danger)]">{errors.scheduledDate}</p>}
          </div>

          {/* Description */}
          <div>
            <label className="block text-xs font-semibold text-[var(--color-text-primary)] mb-1">
              Clinical Reason / Description <span className="text-[var(--color-danger)]">*</span>
            </label>
            <textarea
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="e.g. Follow-up check for fetlock joint after treatment..."
              disabled={submitting}
              className={`w-full rounded-[var(--radius-sm)] border bg-[var(--color-surface)] px-3 py-2 text-xs text-[var(--color-text-primary)] outline-none ${
                errors.description ? 'border-[var(--color-danger)]' : 'border-[var(--color-border)] focus:border-[var(--color-primary)]'
              }`}
            />
            {errors.description && <p className="mt-1 text-[11px] text-[var(--color-danger)]">{errors.description}</p>}
          </div>

          {/* Footer buttons */}
          <div className="flex items-center justify-end gap-2 border-t border-[var(--color-border)] pt-4">
            <Button variant="secondary" size="sm" onClick={onClose} disabled={submitting}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" size="sm" loading={submitting} icon="check">
              Schedule Care
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
