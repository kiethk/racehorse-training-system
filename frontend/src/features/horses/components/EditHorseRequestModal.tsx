'use client';

import { useMemo, useState } from 'react';
import { Button, ConfirmDialog, FormField, Input, Modal, Notice } from '@/components/ui';
import { toast } from '@/lib/toast';
import { displayError, formatDate } from '@/lib/display';
import { formatHorseSex } from '../lib/sex';
import { diffFields, normalizeFields, uelnFormatError } from '../lib/horseEdit';
import { horseEditRequestService } from '../services/horseEditRequestService';
import { DocumentSlotGrid } from './DocumentSlotGrid';
import type { Horse } from '@/features/stable/types';
import type { HorseDocumentSlot, HorseDocumentType, HorseEditFields, HorseEditRequest, HorsePedigree } from '../types';

interface EditHorseRequestModalProps {
  open: boolean;
  horse: Horse;
  pedigree: HorsePedigree | null;
  slots: HorseDocumentSlot[];
  onClose: () => void;
  onSubmitted: (request: HorseEditRequest) => void;
}

export function initialFields(horse: Horse, pedigree: HorsePedigree | null): HorseEditFields {
  return {
    name: horse.name ?? '',
    breed: horse.breed ?? '',
    sireName: pedigree?.sireName ?? '',
    sireRegistrationNumber: pedigree?.sireRegistrationNumber ?? '',
    damName: pedigree?.damName ?? '',
    damRegistrationNumber: pedigree?.damRegistrationNumber ?? '',
  };
}

/**
 * Owner's change request for an eligible horse. Nothing changes on the horse until the groom
 * of its stall approves; identity facts (UELN, sex, birth date, registry) are shown read-only.
 */
export function EditHorseRequestModal({ open, horse, pedigree, slots, onClose, onSubmitted }: EditHorseRequestModalProps) {
  const original = useMemo(() => initialFields(horse, pedigree), [horse, pedigree]);
  const [form, setForm] = useState<HorseEditFields>(original);
  const [files, setFiles] = useState<Partial<Record<HorseDocumentType, File>>>({});
  const [touched, setTouched] = useState<Partial<Record<keyof HorseEditFields, boolean>>>({});
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmDiscard, setConfirmDiscard] = useState(false);

  const changes = diffFields(original, form);
  const fileCount = Object.keys(files).length;
  const dirty = changes.length > 0 || fileCount > 0;

  const errors: Partial<Record<keyof HorseEditFields, string>> = {};
  if (!normalizeFields(form).name) errors.name = 'Enter the horse name.';
  const sireError = uelnFormatError(form.sireRegistrationNumber);
  if (sireError) errors.sireRegistrationNumber = sireError;
  const damError = uelnFormatError(form.damRegistrationNumber);
  if (damError) errors.damRegistrationNumber = damError;
  const normalized = normalizeFields(form);
  if (normalized.sireRegistrationNumber && normalized.sireRegistrationNumber === normalized.damRegistrationNumber) {
    errors.damRegistrationNumber = 'Sire and dam must be different horses.';
  }
  const valid = Object.keys(errors).length === 0;

  const reset = () => {
    setForm(original);
    setFiles({});
    setTouched({});
    setError(null);
  };

  const close = () => {
    if (submitting) return;
    if (dirty) {
      setConfirmDiscard(true);
      return;
    }
    reset();
    onClose();
  };

  const submit = async () => {
    setTouched({ name: true, sireRegistrationNumber: true, damRegistrationNumber: true });
    if (!valid || !dirty) return;
    setSubmitting(true);
    setError(null);
    try {
      const uploads = (Object.entries(files) as [HorseDocumentType, File][])
        .map(([documentType, file]) => ({ documentType, file }));
      const request = await horseEditRequestService.submit(horse.id, normalized, uploads);
      toast.success('Change request sent', 'The groom of this horse will review it. Nothing changes until it is approved.');
      reset();
      onSubmitted(request);
    } catch (cause) {
      const message = displayError(cause, 'Unable to submit the change request.');
      setError(message);
      toast.error('Change request not sent', message);
    } finally {
      setSubmitting(false);
    }
  };

  const field = (key: keyof HorseEditFields, label: string, hint?: string) => (
    <FormField label={label} required={key === 'name'} error={touched[key] ? errors[key] : undefined} hint={hint}>
      <Input
        value={form[key] ?? ''}
        maxLength={key.endsWith('RegistrationNumber') ? 15 : 255}
        onChange={(event) => setForm((prev) => ({ ...prev, [key]: event.target.value }))}
        onBlur={() => setTouched((prev) => ({ ...prev, [key]: true }))}
      />
    </FormField>
  );

  return (
    <>
      <Modal
        open={open}
        onClose={close}
        title={`Edit ${horse.name}`}
        description="Submit changes for review. The groom of this horse approves them before they apply."
        size="lg"
        dismissible={!submitting}
        footer={(
          <>
            <Button variant="tertiary" onClick={close} disabled={submitting}>Cancel</Button>
            <Button variant="primary" onClick={() => void submit()} loading={submitting} disabled={!dirty || submitting}>
              Submit for review
            </Button>
          </>
        )}
      >
        <div className="space-y-6">
          {error && <Notice tone="error">{error}</Notice>}

          <section className="space-y-3">
            <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">Details</h3>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              {field('name', 'Name')}
              {field('breed', 'Breed')}
            </div>
            <dl className="grid grid-cols-1 gap-x-4 gap-y-1 rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] p-3 text-xs sm:grid-cols-2">
              <ReadOnly label="UELN" value={horse.registrationNumber || 'Not recorded'} />
              <ReadOnly label="Sex" value={formatHorseSex(horse.sex)} />
              <ReadOnly label="Date of birth" value={horse.dateOfBirth ? formatDate(horse.dateOfBirth) : 'Not recorded'} />
              <ReadOnly label="Registry" value={pedigree?.registryName || 'Not recorded'} />
              <p className="text-[var(--color-text-muted)] sm:col-span-2">These identity details cannot be changed.</p>
            </dl>
          </section>

          <section className="space-y-3">
            <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">Pedigree</h3>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              {field('sireName', 'Sire name')}
              {field('sireRegistrationNumber', 'Sire UELN', '15 letters or digits; the sire must be male if registered here.')}
              {field('damName', 'Dam name')}
              {field('damRegistrationNumber', 'Dam UELN', '15 letters or digits; the dam must be female if registered here.')}
            </div>
          </section>

          <section className="space-y-3">
            <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">Documents</h3>
            <p className="text-xs text-[var(--color-text-muted)]">
              Replace a document or add a missing one. New files replace the current ones only after approval.
            </p>
            <DocumentSlotGrid
              slots={slots}
              chosen={files}
              onChoose={(type, file) => setFiles((prev) => {
                const next = { ...prev };
                if (file) next[type] = file; else delete next[type];
                return next;
              })}
            />
          </section>
        </div>
      </Modal>

      <ConfirmDialog
        open={confirmDiscard}
        title="Discard your changes?"
        description="The changes you made in this form will be lost."
        confirmLabel="Discard"
        tone="danger"
        onCancel={() => setConfirmDiscard(false)}
        onConfirm={() => {
          setConfirmDiscard(false);
          reset();
          onClose();
        }}
      />
    </>
  );
}

function ReadOnly({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-2">
      <dt className="text-[var(--color-text-muted)]">{label}</dt>
      <dd className="font-medium text-[var(--color-text-primary)]">{value}</dd>
    </div>
  );
}
