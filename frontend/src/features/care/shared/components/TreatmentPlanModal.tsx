'use client';

import { useState } from 'react';
import { z } from 'zod';
import { Button, Checkbox, FormField, Input, Modal, Notice, Select, Textarea } from '@/components/ui';
import { careService } from '@/features/care/services/careService';
import { displayError, formatLocalDateIso } from '@/lib/display';

interface TreatmentPlanModalProps {
  open: boolean;
  onClose: () => void;
  healthRecordId: number;
  horseName?: string;
  onSuccess: () => void;
}

const treatmentPlanSchema = z
  .object({
    treatmentName: z.string().min(1, 'Treatment plan name is required.'),
    instructions: z.string().min(1, 'Clinical instructions are required.'),
    startDate: z.string().min(1, 'Start date is required.'),
    endDate: z.string().optional(),
  })
  .refine((data) => !data.endDate || data.endDate >= data.startDate, {
    message: 'Target end date cannot be earlier than start date.',
    path: ['endDate'],
  });

export function TreatmentPlanModal({
  open,
  onClose,
  healthRecordId,
  horseName,
  onSuccess,
}: TreatmentPlanModalProps) {
  const [treatmentName, setTreatmentName] = useState('');
  const [instructions, setInstructions] = useState('');
  const [startDate, setStartDate] = useState(() => formatLocalDateIso());
  const [endDate, setEndDate] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() + 14);
    return formatLocalDateIso(d);
  });

  // Initial Prescription
  const [includePrescription, setIncludePrescription] = useState(false);
  const [medicationName, setMedicationName] = useState('');
  const [dosage, setDosage] = useState('');
  const [frequency, setFrequency] = useState('Once daily');
  const [route, setRoute] = useState('Oral');
  const [prescriptionInstructions, setPrescriptionInstructions] = useState('');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const parsed = treatmentPlanSchema.safeParse({
      treatmentName: treatmentName.trim(),
      instructions: instructions.trim(),
      startDate,
      endDate: endDate || undefined,
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
    setLoading(true);
    setError(null);

    try {
      const plan = await careService.createTreatmentPlan(healthRecordId, {
        treatmentName: treatmentName.trim(),
        instructions: instructions.trim(),
        startDate,
        endDate: endDate || undefined,
      });

      if (includePrescription && medicationName.trim() && dosage.trim()) {
        try {
          await careService.createPrescription(plan.id, {
            medicationName: medicationName.trim(),
            dosage: dosage.trim(),
            frequency: frequency.trim(),
            route: route.trim(),
            startDate,
            endDate: endDate || undefined,
            instructions: prescriptionInstructions.trim() || undefined,
          });
        } catch (rxErr) {
          setError(
            displayError(
              rxErr,
              'Treatment plan was created, but initial prescription failed. Please add the prescription manually.',
            ),
          );
          return;
        }
      }

      onSuccess();
    } catch (err) {
      setError(displayError(err, 'Failed to create treatment plan.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Create Treatment Plan"
      description={horseName ? `Design care protocol for ${horseName}` : 'Design medical care protocol'}
      size="lg"
      footer={
        <div className="flex items-center justify-end gap-2">
          <Button variant="secondary" onClick={onClose} disabled={loading}>
            Cancel
          </Button>
          <Button variant="primary" onClick={handleSubmit} disabled={loading}>
            {loading ? 'Saving...' : 'Create Plan'}
          </Button>
        </div>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && <Notice tone="error">{error}</Notice>}

        <FormField label="Treatment Plan Name" required error={fieldErrors.treatmentName}>
          <Input
            placeholder="e.g. Left Forelimb Desmitis Rehabilitation"
            value={treatmentName}
            onChange={(e) => setTreatmentName(e.target.value)}
            disabled={loading}
          />
        </FormField>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <FormField label="Start Date" required error={fieldErrors.startDate}>
            <Input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              disabled={loading}
            />
          </FormField>
          <FormField label="Target End Date" error={fieldErrors.endDate}>
            <Input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              disabled={loading}
            />
          </FormField>
        </div>

        <FormField label="Clinical Instructions & Regimen" required error={fieldErrors.instructions}>
          <Textarea
            rows={3}
            placeholder="e.g. Hand walking 15 mins daily, cold hosing 2x/day for 20 mins, stall rest."
            value={instructions}
            onChange={(e) => setInstructions(e.target.value)}
            disabled={loading}
          />
        </FormField>

        {/* Optional quick prescription */}
        <div className="rounded-lg border border-[var(--color-border)] p-4 bg-[var(--color-surface-subtle)] space-y-3">
          <div className="flex items-center justify-between">
            <div>
              <span className="text-sm font-semibold text-[var(--color-text-primary)]">
                Attach Initial Prescription
              </span>
              <p className="text-xs text-[var(--color-text-muted)]">
                Optionally add the primary medication immediately.
              </p>
            </div>
            <Checkbox
              label=""
              id="includePrescription"
              checked={includePrescription}
              onChange={(e) => setIncludePrescription(e.target.checked)}
              disabled={loading}
            />
          </div>

          {includePrescription && (
            <div className="space-y-3 pt-3 border-t border-[var(--color-border)]">
              <FormField label="Medication Name" required>
                <Input
                  placeholder="e.g. Phenylbutazone Paste"
                  value={medicationName}
                  onChange={(e) => setMedicationName(e.target.value)}
                  disabled={loading}
                />
              </FormField>

              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <FormField label="Dosage" required>
                  <Input
                    placeholder="e.g. 0.6 mg/kg"
                    value={dosage}
                    onChange={(e) => setDosage(e.target.value)}
                    disabled={loading}
                  />
                </FormField>

                <FormField label="Route">
                  <Select value={route} onChange={(e) => setRoute(e.target.value)} disabled={loading}>
                    <option value="Oral">Oral</option>
                    <option value="Intravenous (IV)">Intravenous (IV)</option>
                    <option value="Intramuscular (IM)">Intramuscular (IM)</option>
                    <option value="Topical">Topical</option>
                  </Select>
                </FormField>
              </div>

              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <FormField label="Frequency">
                  <Select value={frequency} onChange={(e) => setFrequency(e.target.value)} disabled={loading}>
                    <option value="Once daily">Once daily</option>
                    <option value="Twice daily">Twice daily</option>
                    <option value="Every 8 hours">Every 8 hours</option>
                    <option value="As needed">As needed</option>
                  </Select>
                </FormField>

                <FormField label="Prescription Instructions">
                  <Input
                    placeholder="e.g. Administer in morning grain"
                    value={prescriptionInstructions}
                    onChange={(e) => setPrescriptionInstructions(e.target.value)}
                    disabled={loading}
                  />
                </FormField>
              </div>
            </div>
          )}
        </div>
      </form>
    </Modal>
  );
}
