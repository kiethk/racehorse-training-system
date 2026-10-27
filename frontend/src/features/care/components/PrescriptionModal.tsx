'use client';

import { useState } from 'react';
import { z } from 'zod';
import { Button, FormField, Input, Modal, Notice, Select, Textarea } from '@/components/ui';
import { careService } from '../services/careService';
import { displayError, formatLocalDateIso } from '@/lib/display';

interface PrescriptionModalProps {
  open: boolean;
  onClose: () => void;
  treatmentPlanId: number;
  treatmentName: string;
  onSuccess: () => void;
}

const createPrescriptionSchema = z
  .object({
    medicationName: z.string().min(1, 'Medication name is required.'),
    dosage: z.string().min(1, 'Dosage is required.'),
    frequency: z.string().min(1, 'Frequency is required.'),
    route: z.string().min(1, 'Route is required.'),
    startDate: z.string().min(1, 'Start date is required.'),
    endDate: z.string().optional(),
    instructions: z.string().optional(),
  })
  .refine((data) => !data.endDate || data.endDate >= data.startDate, {
    message: 'Target end date cannot be earlier than start date.',
    path: ['endDate'],
  });

export function PrescriptionModal({
  open,
  onClose,
  treatmentPlanId,
  treatmentName,
  onSuccess,
}: PrescriptionModalProps) {
  const [medicationName, setMedicationName] = useState('');
  const [dosage, setDosage] = useState('');
  const [frequency, setFrequency] = useState('Once daily');
  const [route, setRoute] = useState('Oral');
  const [startDate, setStartDate] = useState(() => formatLocalDateIso());
  const [endDate, setEndDate] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() + 7);
    return formatLocalDateIso(d);
  });
  const [instructions, setInstructions] = useState('');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const parsed = createPrescriptionSchema.safeParse({
      medicationName: medicationName.trim(),
      dosage: dosage.trim(),
      frequency: frequency.trim(),
      route: route.trim(),
      startDate,
      endDate: endDate || undefined,
      instructions: instructions.trim() || undefined,
    });

    if (!parsed.success) {
      const errors: Record<string, string> = {};
      for (const issue of parsed.error.issues) {
        const field = issue.path[0];
        if (typeof field === 'string') {
          errors[field] = issue.message;
        }
      }
      setFieldErrors(errors);
      return;
    }

    setFieldErrors({});
    try {
      setLoading(true);
      setError(null);

      await careService.createPrescription(treatmentPlanId, {
        medicationName: parsed.data.medicationName,
        dosage: parsed.data.dosage,
        frequency: parsed.data.frequency,
        route: parsed.data.route,
        startDate: parsed.data.startDate,
        endDate: parsed.data.endDate || undefined,
        instructions: parsed.data.instructions || undefined,
      });

      onSuccess();
    } catch (err) {
      setError(displayError(err, 'Failed to add prescription.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Add Prescription"
      description={`Prescribing medication under: ${treatmentName}`}
      size="md"
      footer={
        <div className="flex items-center justify-end gap-2">
          <Button variant="secondary" onClick={onClose} disabled={loading}>
            Cancel
          </Button>
          <Button variant="primary" onClick={handleSubmit} disabled={loading}>
            {loading ? 'Adding...' : 'Add Prescription'}
          </Button>
        </div>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && <Notice tone="error">{error}</Notice>}

        <FormField label="Medication Name" required error={fieldErrors.medicationName}>
          <Input
            placeholder="e.g. Omeprazole"
            value={medicationName}
            onChange={(e) => {
              setMedicationName(e.target.value);
              setFieldErrors({});
            }}
            disabled={loading}
          />
        </FormField>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <FormField label="Dosage" required error={fieldErrors.dosage}>
            <Input
              placeholder="e.g. 4 mg/kg"
              value={dosage}
              onChange={(e) => {
                setDosage(e.target.value);
                setFieldErrors({});
              }}
              disabled={loading}
            />
          </FormField>

          <FormField label="Route" error={fieldErrors.route}>
            <Select
              value={route}
              onChange={(e) => {
                setRoute(e.target.value);
                setFieldErrors({});
              }}
              disabled={loading}
            >
              <option value="Oral">Oral</option>
              <option value="Intravenous (IV)">Intravenous (IV)</option>
              <option value="Intramuscular (IM)">Intramuscular (IM)</option>
              <option value="Topical">Topical</option>
              <option value="Subcutaneous (SC)">Subcutaneous (SC)</option>
            </Select>
          </FormField>
        </div>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <FormField label="Frequency" error={fieldErrors.frequency}>
            <Select
              value={frequency}
              onChange={(e) => {
                setFrequency(e.target.value);
                setFieldErrors({});
              }}
              disabled={loading}
            >
              <option value="Once daily">Once daily</option>
              <option value="Twice daily">Twice daily</option>
              <option value="Every 8 hours">Every 8 hours</option>
              <option value="As needed">As needed</option>
            </Select>
          </FormField>

          <FormField label="Start Date" required error={fieldErrors.startDate}>
            <Input
              type="date"
              value={startDate}
              onChange={(e) => {
                setStartDate(e.target.value);
                setFieldErrors({});
              }}
              disabled={loading}
            />
          </FormField>
        </div>

        <FormField label="Target End Date" error={fieldErrors.endDate}>
          <Input
            type="date"
            value={endDate}
            onChange={(e) => {
              setEndDate(e.target.value);
              setFieldErrors({});
            }}
            disabled={loading}
          />
        </FormField>

        <FormField label="Administration Instructions">
          <Textarea
            rows={2}
            placeholder="e.g. Give 30 minutes before morning feed on an empty stomach."
            value={instructions}
            onChange={(e) => setInstructions(e.target.value)}
            disabled={loading}
          />
        </FormField>
      </form>
    </Modal>
  );
}
