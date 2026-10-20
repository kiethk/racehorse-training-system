'use client';

import { useState, type FormEvent } from 'react';
import { Button, FormField, Input, Modal, Notice, Textarea } from '@/components/ui';
import { displayError } from '@/lib/display';
import { toast } from '@/lib/toast';
import { preventiveCareService } from '../../services/preventiveCareService';
import type { PreventiveDueItem, PreventiveProtocol } from '../../types/preventive';
import { todayIso } from './preventiveDisplay';

interface RecordVaccinationModalProps {
  /** The row being recorded; null keeps the modal closed. */
  item: PreventiveDueItem | null;
  protocols: PreventiveProtocol[];
  onClose: () => void;
  onRecorded: () => void;
}

const FORM_ID = 'record-vaccination-form';

export function RecordVaccinationModal({ item, ...props }: RecordVaccinationModalProps) {
  // Remount the form for each row so it starts from that protocol's defaults.
  if (!item) return null;
  return <RecordVaccinationForm key={`${item.horseId}:${item.protocol.protocolId}`} item={item} {...props} />;
}

function RecordVaccinationForm({
  item,
  protocols,
  onClose,
  onRecorded,
}: RecordVaccinationModalProps & { item: PreventiveDueItem }) {
  const protocol = protocols.find((p) => p.id === item.protocol.protocolId);
  const [dosage, setDosage] = useState(protocol?.defaultDosage ?? '');
  const [route, setRoute] = useState(protocol?.defaultRoute ?? '');
  const [administeredOn, setAdministeredOn] = useState(todayIso());
  const [instructions, setInstructions] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!dosage.trim()) return setError('Dosage is required.');
    if (administeredOn > todayIso()) return setError('The date given cannot be in the future.');
    setError(null);
    setSubmitting(true);
    try {
      await preventiveCareService.recordVaccination(item.horseId, {
        protocolId: item.protocol.protocolId,
        dosage: dosage.trim(),
        route: route.trim() || undefined,
        administeredOn,
        instructions: instructions.trim() || undefined,
      });
      toast.success(`${item.protocol.name} recorded for ${item.horseName}`);
      onRecorded();
      onClose();
    } catch (cause) {
      setError(displayError(cause, 'Could not record the dose. Nothing was saved.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      open
      onClose={onClose}
      size="md"
      title={`Record ${item.protocol.name}`}
      description={`Dose given to ${item.horseName}. Training status is not changed.`}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>Cancel</Button>
          <Button variant="primary" type="submit" form={FORM_ID} loading={submitting} icon="check">
            Record
          </Button>
        </>
      }
    >
      <form id={FORM_ID} onSubmit={handleSubmit} className="space-y-4">
        {error && <Notice tone="error">{error}</Notice>}
        <div className="grid grid-cols-2 gap-3">
          <FormField label="Dosage" required>
            <Input value={dosage} onChange={(e) => setDosage(e.target.value)} maxLength={255} />
          </FormField>
          <FormField label="Route" hint="IM, SC, Oral…">
            <Input value={route} onChange={(e) => setRoute(e.target.value)} maxLength={100} />
          </FormField>
        </div>
        <FormField label="Date given" required>
          <Input type="date" value={administeredOn} max={todayIso()} onChange={(e) => setAdministeredOn(e.target.value)} />
        </FormField>
        <FormField label="Notes" hint="Post-vaccination monitoring, location, etc.">
          <Textarea rows={2} value={instructions} onChange={(e) => setInstructions(e.target.value)} />
        </FormField>
      </form>
    </Modal>
  );
}
