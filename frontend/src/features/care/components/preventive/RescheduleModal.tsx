'use client';

import { useState, type FormEvent } from 'react';
import { Button, FormField, Input, Modal, Notice, Select, Textarea } from '@/components/ui';
import { displayError, formatDate } from '@/lib/display';
import { toast } from '@/lib/toast';
import { preventiveCareService } from '../../services/preventiveCareService';
import type { CareScheduleResponse } from '../../types';
import type { RescheduleReason } from '../../types/preventive';
import { addDaysIso, RESCHEDULE_REASONS, todayIso } from './preventiveDisplay';

interface RescheduleModalProps {
  schedule: CareScheduleResponse | null;
  horseName: string;
  onClose: () => void;
  onRescheduled: () => void;
}

const FORM_ID = 'reschedule-care-form';

export function RescheduleModal(props: RescheduleModalProps) {
  if (!props.schedule) return null;
  return <RescheduleForm key={props.schedule.id} {...props} schedule={props.schedule} />;
}

function RescheduleForm({
  schedule,
  horseName,
  onClose,
  onRescheduled,
}: RescheduleModalProps & { schedule: CareScheduleResponse }) {
  const current = schedule.scheduledAt ?? schedule.requestedAt;
  const [newDate, setNewDate] = useState(addDaysIso(todayIso(), 7));
  const [reason, setReason] = useState<RescheduleReason>('UNDER_TREATMENT');
  const [note, setNote] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (newDate <= todayIso()) return setError('Pick a date after today.');
    setError(null);
    setSubmitting(true);
    try {
      const result = await preventiveCareService.reschedule(schedule.id, {
        newDate,
        reason,
        note: note.trim() || undefined,
      });
      toast.success('Appointment moved', `${horseName} is now booked for ${formatDate(newDate)}.`);
      result.warnings.forEach((warning) => toast.warning('Vaccination due date', warning));
      onRescheduled();
      onClose();
    } catch (cause) {
      setError(displayError(cause, 'Could not move this appointment.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      open
      onClose={onClose}
      size="sm"
      title="Move appointment"
      description={`${horseName} · currently ${formatDate(current)}`}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>Cancel</Button>
          <Button variant="primary" type="submit" form={FORM_ID} loading={submitting}>Move</Button>
        </>
      }
    >
      <form id={FORM_ID} onSubmit={handleSubmit} className="space-y-4">
        {error && <Notice tone="error">{error}</Notice>}
        {schedule.status === 'SCHEDULED' && (
          <Notice tone="info">
            The assigned vet and time slot are released. A vet is assigned again automatically for the new date.
          </Notice>
        )}
        <FormField label="New date" required>
          <Input type="date" value={newDate} min={addDaysIso(todayIso(), 1)} onChange={(e) => setNewDate(e.target.value)} />
        </FormField>
        <FormField label="Reason" required>
          <Select value={reason} onChange={(e) => setReason(e.target.value as RescheduleReason)}>
            {RESCHEDULE_REASONS.map((option) => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </Select>
        </FormField>
        <FormField label="Note">
          <Textarea rows={2} value={note} maxLength={1000} onChange={(e) => setNote(e.target.value)} />
        </FormField>
      </form>
    </Modal>
  );
}
