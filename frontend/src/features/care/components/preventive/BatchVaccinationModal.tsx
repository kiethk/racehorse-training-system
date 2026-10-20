'use client';

import { useState, type FormEvent } from 'react';
import { Button, FormField, Input, Modal, Notice, Pill, Select, Textarea } from '@/components/ui';
import { displayError } from '@/lib/display';
import { toast } from '@/lib/toast';
import { preventiveCareService } from '../../services/preventiveCareService';
import type { PreventiveProtocol } from '../../types/preventive';
import { todayIso } from './preventiveDisplay';

interface BatchVaccinationModalProps {
  open: boolean;
  onClose: () => void;
  horses: { id: number; name: string }[];
  protocols: PreventiveProtocol[];
  initialProtocolId?: number;
  onRecorded: () => void;
}

const FORM_ID = 'batch-vaccination-form';

export function BatchVaccinationModal(props: BatchVaccinationModalProps) {
  // Remount the form each time the modal opens so it starts from the current selection.
  if (!props.open) return null;
  return <BatchVaccinationForm {...props} />;
}

function BatchVaccinationForm({ onClose, horses, protocols, initialProtocolId, onRecorded }: BatchVaccinationModalProps) {
  const firstProtocol = protocols.find((p) => p.id === initialProtocolId) ?? protocols[0];
  const [protocolId, setProtocolId] = useState<number | undefined>(firstProtocol?.id);
  const [dosage, setDosage] = useState(firstProtocol?.defaultDosage ?? '');
  const [route, setRoute] = useState(firstProtocol?.defaultRoute ?? '');
  const [administeredOn, setAdministeredOn] = useState(todayIso());
  const [instructions, setInstructions] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const selectProtocol = (id: number) => {
    setProtocolId(id);
    const protocol = protocols.find((p) => p.id === id);
    setDosage(protocol?.defaultDosage ?? '');
    setRoute(protocol?.defaultRoute ?? '');
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!protocolId) return setError('Choose a protocol.');
    if (!dosage.trim()) return setError('Dosage is required.');
    if (administeredOn > todayIso()) return setError('The vaccination date cannot be in the future.');
    setError(null);
    setSubmitting(true);
    try {
      const results = await preventiveCareService.recordBatch({
        horseIds: horses.map((h) => h.id),
        protocolId,
        dosage: dosage.trim(),
        route: route.trim() || undefined,
        administeredOn,
        instructions: instructions.trim() || undefined,
      });
      toast.success(`Recorded for ${results.length} horse${results.length === 1 ? '' : 's'}`,
        'Each horse now has a vaccination record and an updated next due date.');
      onRecorded();
      onClose();
    } catch (cause) {
      setError(displayError(cause, 'Could not record the batch. Nothing was saved.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      open
      onClose={onClose}
      size="md"
      title="Record vaccination"
      // description="One dose of the same protocol for every selected horse. Training status is not changed."
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>Cancel</Button>
          <Button variant="primary" type="submit" form={FORM_ID} loading={submitting} icon="check">
            Record for {horses.length} horse{horses.length === 1 ? '' : 's'}
          </Button>
        </>
      }
    >
      <form id={FORM_ID} onSubmit={handleSubmit} className="space-y-4">
        {error && <Notice tone="error">{error}</Notice>}
        <div className="flex flex-wrap gap-1.5">
          {horses.map((horse) => (
            <Pill key={horse.id} tone="neutral" size="sm">{horse.name}</Pill>
          ))}
        </div>
        <FormField label="Protocol" required>
          <Select value={protocolId ?? ''} onChange={(e) => selectProtocol(Number(e.target.value))}>
            {protocols.map((protocol) => (
              <option key={protocol.id} value={protocol.id}>{protocol.name}</option>
            ))}
          </Select>
        </FormField>
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
