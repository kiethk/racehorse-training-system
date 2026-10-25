'use client';

import { useState, type FormEvent } from 'react';
import { z } from 'zod';
import { Button, Checkbox, FormField, Input, Modal, Notice, Select, Textarea } from '@/components/ui';
import { displayError, formatDate } from '@/lib/display';
import { toast } from '@/lib/toast';
import { preventiveCareService } from '../../services/preventiveCareService';
import type { PreventiveDueHorse, PreventiveProtocol } from '../../types/preventive';
import { DueStatusPill, todayIso } from './preventiveDisplay';

interface CompleteVaccinationModalProps {
  horse: PreventiveDueHorse | null;
  protocols: PreventiveProtocol[];
  onClose: () => void;
  onRecorded: () => void;
}

interface DoseLine {
  protocolId: number;
  selected: boolean;
  dosage: string;
  route: string;
}

const FORM_ID = 'complete-vaccination-form';

const vaccinationSchema = z.object({
  administeredOn: z
    .string()
    .min(1, 'Date given is required.')
    .refine((value) => value <= todayIso(), { message: 'The vaccination date cannot be in the future.' }),
  doses: z
    .array(
      z.object({
        protocolId: z.number(),
        dosage: z.string().min(1, 'Enter a dosage for every selected protocol.'),
        route: z.string().optional(),
      }),
    )
    .min(1, 'Select at least one protocol.'),
});

export function CompleteVaccinationModal(props: CompleteVaccinationModalProps) {
  // Remount per horse so the lines start from that horse's due protocols.
  if (!props.horse) return null;
  return <CompleteVaccinationForm key={props.horse.horseId} {...props} horse={props.horse} />;
}

function CompleteVaccinationForm({
  horse,
  protocols,
  onClose,
  onRecorded,
}: CompleteVaccinationModalProps & { horse: PreventiveDueHorse }) {
  const lineFor = (protocolId: number, selected: boolean): DoseLine => {
    const protocol = protocols.find((p) => p.id === protocolId);
    return { protocolId, selected, dosage: protocol?.defaultDosage ?? '', route: protocol?.defaultRoute ?? '' };
  };
  const [lines, setLines] = useState<DoseLine[]>(() => horse.protocols.map((p) => lineFor(p.protocolId, true)));
  const [administeredOn, setAdministeredOn] = useState(todayIso());
  const [instructions, setInstructions] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const listed = new Set(lines.map((line) => line.protocolId));
  const addable = protocols.filter((p) => !listed.has(p.id));
  const dueStatus = (protocolId: number) => horse.protocols.find((p) => p.protocolId === protocolId);
  const protocolName = (protocolId: number) => protocols.find((p) => p.id === protocolId)?.name ?? `Protocol #${protocolId}`;
  const update = (protocolId: number, patch: Partial<DoseLine>) =>
    setLines((current) => current.map((line) => (line.protocolId === protocolId ? { ...line, ...patch } : line)));

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const chosen = lines.filter((line) => line.selected);
    const parsed = vaccinationSchema.safeParse({
      administeredOn,
      doses: chosen.map((line) => ({
        protocolId: line.protocolId,
        dosage: line.dosage.trim(),
        route: line.route.trim() || undefined,
      })),
    });

    if (!parsed.success) {
      const errors: Record<string, string> = {};
      let formError: string | null = null;
      for (const issue of parsed.error.issues) {
        const segments = issue.path.map((segment) => String(segment));
        const [root, index, field] = segments;
        if (root === 'administeredOn') {
          errors.administeredOn = issue.message;
          continue;
        }
        const lineIndex = Number(index);
        const dosageLine = Number.isInteger(lineIndex) ? chosen[lineIndex] : undefined;
        if (dosageLine && field) {
          errors[String(dosageLine.protocolId)] = issue.message;
          continue;
        }
        formError = issue.message;
      }
      setFieldErrors(errors);
      setError(formError);
      return;
    }

    setFieldErrors({});
    setError(null);
    setSubmitting(true);
    try {
      await preventiveCareService.recordForHorse(horse.horseId, {
        administeredOn,
        instructions: instructions.trim() || undefined,
        doses: chosen.map((line) => ({
          protocolId: line.protocolId,
          dosage: line.dosage.trim(),
          route: line.route.trim() || undefined,
        })),
      });
      toast.success(`Vaccination recorded for ${horse.horseName}`,
        `${chosen.length} protocol${chosen.length === 1 ? '' : 's'} completed; the next due date is updated.`);
      onRecorded();
      onClose();
    } catch (cause) {
      setError(displayError(cause, 'Could not record the vaccination. Nothing was saved.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      open
      onClose={onClose}
      size="lg"
      title={`Complete vaccination · ${horse.horseName}`}
      description="Records the doses given to this horse. Training status is not changed."
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>Cancel</Button>
          <Button variant="primary" type="submit" form={FORM_ID} loading={submitting} icon="check">Complete</Button>
        </>
      }
    >
      <form id={FORM_ID} onSubmit={handleSubmit} className="space-y-4">
        {error && <Notice tone="error">{error}</Notice>}
        <ul className="divide-y divide-[var(--color-border)] rounded-[var(--radius-md)] border border-[var(--color-border)]">
          {lines.map((line) => {
            const status = dueStatus(line.protocolId);
            return (
              <li key={line.protocolId} className="grid grid-cols-1 gap-3 p-3 md:grid-cols-[1fr_8rem_8rem] md:items-end">
                <Checkbox
                  checked={line.selected}
                  onChange={(e) => update(line.protocolId, { selected: e.target.checked })}
                  label={
                    <span className="flex flex-wrap items-center gap-2">
                      {protocolName(line.protocolId)}
                      {status && <DueStatusPill status={status.status} />}
                    </span>
                  }
                  description={status ? (status.nextDueOn ? `Due ${formatDate(status.nextDueOn)}` : 'Never recorded') : 'Added manually'}
                />
                <FormField label="Dosage" required={line.selected} error={fieldErrors[String(line.protocolId)]}>
                  <Input value={line.dosage} maxLength={100} disabled={!line.selected}
                    onChange={(e) => update(line.protocolId, { dosage: e.target.value })} />
                </FormField>
                <FormField label="Route">
                  <Input value={line.route} maxLength={100} disabled={!line.selected}
                    onChange={(e) => update(line.protocolId, { route: e.target.value })} />
                </FormField>
              </li>
            );
          })}
        </ul>
        {addable.length > 0 && (
          <FormField label="Add another protocol" className="w-72">
            <Select value="" onChange={(e) => e.target.value && setLines((current) => [...current, lineFor(Number(e.target.value), true)])}>
              <option value="">Choose a protocol…</option>
              {addable.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
            </Select>
          </FormField>
        )}
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <FormField label="Date given" required error={fieldErrors.administeredOn}>
            <Input type="date" value={administeredOn} max={todayIso()} onChange={(e) => setAdministeredOn(e.target.value)} />
          </FormField>
        </div>
        <FormField label="Notes" hint="Post-vaccination monitoring, batch reaction, etc.">
          <Textarea rows={2} value={instructions} onChange={(e) => setInstructions(e.target.value)} />
        </FormField>
      </form>
    </Modal>
  );
}
