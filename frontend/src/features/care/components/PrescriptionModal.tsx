'use client';

import { useState } from 'react';
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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!medicationName.trim()) {
      setError('Medication name is required.');
      return;
    }
    if (!dosage.trim()) {
      setError('Dosage is required.');
      return;
    }
    if (endDate && startDate && endDate < startDate) {
      setError('Target end date cannot be earlier than start date.');
      return;
    }

    try {
      setLoading(true);
      setError(null);

      await careService.createPrescription(treatmentPlanId, {
        medicationName: medicationName.trim(),
        dosage: dosage.trim(),
        frequency: frequency.trim(),
        route: route.trim(),
        startDate,
        endDate: endDate || undefined,
        instructions: instructions.trim() || undefined,
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

        <FormField label="Medication Name" required>
          <Input
            placeholder="e.g. Omeprazole"
            value={medicationName}
            onChange={(e) => setMedicationName(e.target.value)}
            disabled={loading}
          />
        </FormField>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <FormField label="Dosage" required>
            <Input
              placeholder="e.g. 4 mg/kg"
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
              <option value="Subcutaneous (SC)">Subcutaneous (SC)</option>
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

          <FormField label="Start Date" required>
            <Input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              disabled={loading}
            />
          </FormField>
        </div>

        <FormField label="Target End Date">
          <Input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
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
