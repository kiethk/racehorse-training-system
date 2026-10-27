'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { FormField } from '@/components/ui/FormField';
import { Input, Select, Textarea } from '@/components/ui/Input';
import { Notice } from '@/components/ui/Notice';
import { admissionsApi } from '@/features/admissions/services/api';
import { ApiError } from '@/services/api';
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

  const resetForm = () => {
    setHorseId(0);
    setCareType('ROUTINE_EXAM');
    setScheduledDate(getMinRoutineDate());
    setDescription('');
    setErrors({});
    setSubmitError(null);
  };

  const handleClose = () => {
    resetForm();
    onClose();
  };

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
      resetForm();
      onSuccess();
      onClose();
    } catch (err: unknown) {
      let msg = 'Unable to schedule preventive care.';
      const raw = err instanceof Error ? err.message : String(err);
      const code = err instanceof ApiError ? (err.errorCode || '') : '';
      const status = err instanceof ApiError ? err.status : 0;

      if (raw.includes('previously examined')) {
        msg = 'A veterinarian may only schedule preventive care for an active horse.';
      } else if (code === 'FORBIDDEN' || raw.includes('FORBIDDEN') || status === 403) {
        msg = 'You do not have permission to schedule preventive care for this horse.';
      } else if (code === 'IDEMPOTENCY_CONFLICT' || raw.includes('IDEMPOTENCY_CONFLICT') || status === 409 || raw.includes('already exists')) {
        msg = 'A care schedule of this type already exists for this horse on the selected date.';
      } else if (
        code === 'INVALID_DATE' ||
        raw.includes('INVALID_DATE') ||
        raw.includes('future') ||
        raw.includes('past today cutoff') ||
        (status === 400 && (raw.toLowerCase().includes('date') || raw.toLowerCase().includes('time')))
      ) {
        msg = 'Scheduled date is invalid or past today cutoff time (14:00). Please select the next available date.';
      } else if (code === 'HORSE_NOT_FOUND' || raw.includes('HORSE_NOT_FOUND') || status === 404) {
        msg = 'Selected horse was not found.';
      } else {
        msg = raw || 'Unable to schedule preventive care.';
      }
      setSubmitError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const footer = (
    <div className="flex items-center justify-end gap-2">
      <Button variant="secondary" size="sm" onClick={handleClose} disabled={submitting}>
        Cancel
      </Button>
      <Button type="submit" form="create-routine-form" variant="primary" size="sm" loading={submitting} icon="check">
        Schedule Care
      </Button>
    </div>
  );

  return (
    <Modal
      open={open}
      onClose={handleClose}
      title="Schedule Preventive Care"
      description="Create a routine exam, vaccination, or deworming"
      size="md"
      footer={footer}
      dismissible={!submitting}
    >
      <form id="create-routine-form" onSubmit={handleSubmit} className="space-y-4">
        {submitError && <Notice tone="error">{submitError}</Notice>}

        <FormField label="Horse" required error={errors.horseId}>
          <Select
            value={horseId}
            onChange={(e) => setHorseId(Number(e.target.value))}
            disabled={submitting}
          >
            <option value={0}>-- Select horse --</option>
            {horses.map((h) => (
              <option key={h.id} value={h.id}>
                {h.name} (#{h.id}) - {h.breed || 'Unknown breed'}
              </option>
            ))}
          </Select>
        </FormField>

        <FormField label="Care Type">
          <Select
            value={careType}
            onChange={(e) => setCareType(e.target.value)}
            disabled={submitting}
          >
            <option value="ROUTINE_EXAM">Routine Exam / Follow-up</option>
            <option value="VACCINATION">Vaccination</option>
            <option value="DEWORMING">Deworming</option>
          </Select>
        </FormField>

        <FormField label="Scheduled Date" required error={errors.scheduledDate}>
          <Input
            type="date"
            min={minDate}
            value={scheduledDate}
            onChange={(e) => setScheduledDate(e.target.value)}
            disabled={submitting}
          />
        </FormField>

        <FormField label="Clinical Reason / Description" required error={errors.description}>
          <Textarea
            rows={3}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="e.g. Follow-up check for fetlock joint after treatment..."
            disabled={submitting}
          />
        </FormField>
      </form>
    </Modal>
  );
}
