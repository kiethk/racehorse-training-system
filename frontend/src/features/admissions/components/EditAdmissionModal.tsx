'use client';

import { useMemo, useState } from 'react';
import { Button, ConfirmDialog, FormField, Input, Modal, Notice, Select, Textarea } from '@/components/ui';
import { toast } from '@/lib/toast';
import { displayError } from '@/lib/display';
import { HORSE_SEX_OPTIONS } from '@/features/horses/lib/sex';
import { uelnFormatError } from '@/features/horses/lib/horseEdit';
import { DocumentSlotGrid } from '@/features/horses/components/DocumentSlotGrid';
import type { HorseDocumentSlot, HorseDocumentType } from '@/features/horses/types';
import type { HorseSex } from '@/types/horse';
import { ownerAdmissionApi, type OwnerAdmissionDetail } from '../services/ownerApi';
import type { CreateOwnerAdmissionRequest } from '../types/owner';

type FormState = Record<
  'name' | 'breed' | 'dateOfBirth' | 'sex' | 'registrationNumber' | 'registryName'
  | 'sireName' | 'sireRegistrationNumber' | 'damName' | 'damRegistrationNumber' | 'pedigreeNotes',
  string
>;

function fromDetail(detail: OwnerAdmissionDetail): FormState {
  const c = detail.candidate;
  return {
    name: c.name ?? '', breed: c.breed ?? '', dateOfBirth: c.dateOfBirth ?? '', sex: c.sex ?? '',
    registrationNumber: c.registrationNumber ?? '', registryName: c.registryName ?? '',
    sireName: c.sireName ?? '', sireRegistrationNumber: c.sireRegistrationNumber ?? '',
    damName: c.damName ?? '', damRegistrationNumber: c.damRegistrationNumber ?? '',
    pedigreeNotes: c.pedigreeNotes ?? '',
  };
}

const optional = (value: string) => (value.trim() ? value.trim() : undefined);

/**
 * Owner edits an application that no groom has reviewed yet. Changes save directly (there is no
 * approval step before the groom review); the server refuses once a groom has started reviewing.
 */
export function EditAdmissionModal({ detail, onClose, onSaved }: {
  detail: OwnerAdmissionDetail;
  onClose: () => void;
  onSaved: (updated: OwnerAdmissionDetail) => void;
}) {
  const original = useMemo(() => fromDetail(detail), [detail]);
  const [form, setForm] = useState<FormState>(original);
  const [files, setFiles] = useState<Partial<Record<HorseDocumentType, File>>>({});
  const [touched, setTouched] = useState<Partial<Record<keyof FormState, boolean>>>({});
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmDiscard, setConfirmDiscard] = useState(false);

  const slots: HorseDocumentSlot[] = detail.documents.map((doc) => ({
    documentType: doc.documentType as HorseDocumentType,
    source: 'ADMISSION',
    documentId: doc.id,
    originalFileName: doc.originalFileName,
    fileUrl: doc.fileUrl,
    updatedAt: doc.uploadedAt,
  }));

  const dirty = (Object.keys(form) as (keyof FormState)[]).some((key) => form[key].trim() !== original[key].trim())
    || Object.keys(files).length > 0;

  const errors: Partial<Record<keyof FormState, string>> = {};
  if (!form.name.trim()) errors.name = 'Enter the horse name.';
  if (!form.sex) errors.sex = 'Select the sex.';
  for (const key of ['registrationNumber', 'sireRegistrationNumber', 'damRegistrationNumber'] as const) {
    const message = uelnFormatError(form[key]);
    if (message) errors[key] = message;
  }
  const valid = Object.keys(errors).length === 0;

  const close = () => {
    if (saving) return;
    if (dirty) setConfirmDiscard(true); else onClose();
  };

  const save = async () => {
    setTouched(Object.fromEntries(Object.keys(form).map((key) => [key, true])));
    if (!valid || !dirty) return;
    setSaving(true);
    setError(null);
    try {
      const request: CreateOwnerAdmissionRequest = {
        name: form.name.trim(),
        sex: form.sex as HorseSex,
        breed: optional(form.breed),
        dateOfBirth: optional(form.dateOfBirth),
        registrationNumber: optional(form.registrationNumber),
        registryName: optional(form.registryName),
        sireName: optional(form.sireName),
        sireRegistrationNumber: optional(form.sireRegistrationNumber),
        damName: optional(form.damName),
        damRegistrationNumber: optional(form.damRegistrationNumber),
        pedigreeNotes: optional(form.pedigreeNotes),
      };
      const uploads = (Object.entries(files) as [HorseDocumentType, File][])
        .map(([documentType, file]) => ({ documentType, file }));
      const updated = await ownerAdmissionApi.update(detail.admissionId, request, uploads);
      toast.success('Application updated', 'The groom will review the updated details and documents.');
      onSaved(updated);
    } catch (cause) {
      const message = displayError(cause, 'Unable to update the application.');
      setError(message);
      toast.error('Application not updated', message);
    } finally {
      setSaving(false);
    }
  };

  const field = (key: keyof FormState, label: string, options: { required?: boolean; type?: string; max?: number } = {}) => (
    <FormField label={label} required={options.required} error={touched[key] ? errors[key] : undefined}>
      <Input
        type={options.type ?? 'text'}
        value={form[key]}
        maxLength={options.max ?? 255}
        onChange={(event) => setForm((prev) => ({ ...prev, [key]: event.target.value }))}
        onBlur={() => setTouched((prev) => ({ ...prev, [key]: true }))}
      />
    </FormField>
  );

  return (
    <>
      <Modal
        open
        onClose={close}
        title="Edit application"
        description="You can change the application until a groom reviews it."
        size="lg"
        dismissible={!saving}
        footer={(
          <>
            <Button variant="tertiary" onClick={close} disabled={saving}>Cancel</Button>
            <Button variant="primary" onClick={() => void save()} loading={saving} disabled={!dirty || saving}>Save changes</Button>
          </>
        )}
      >
        <div className="space-y-6">
          {error && <Notice tone="error">{error}</Notice>}
          <section className="space-y-3">
            <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">Candidate horse</h3>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              {field('name', 'Name', { required: true })}
              {field('breed', 'Breed')}
              <FormField label="Sex" required error={touched.sex ? errors.sex : undefined}>
                <Select value={form.sex} onChange={(event) => setForm((prev) => ({ ...prev, sex: event.target.value }))}>
                  <option value="">Select sex</option>
                  {HORSE_SEX_OPTIONS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                </Select>
              </FormField>
              {field('dateOfBirth', 'Date of birth', { type: 'date' })}
              {field('registrationNumber', 'UELN', { max: 15 })}
              {field('registryName', 'Registry')}
            </div>
          </section>
          <section className="space-y-3">
            <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">Pedigree</h3>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              {field('sireName', 'Sire name')}
              {field('sireRegistrationNumber', 'Sire UELN', { max: 15 })}
              {field('damName', 'Dam name')}
              {field('damRegistrationNumber', 'Dam UELN', { max: 15 })}
            </div>
            <FormField label="Pedigree notes">
              <Textarea value={form.pedigreeNotes} maxLength={2000}
                onChange={(event) => setForm((prev) => ({ ...prev, pedigreeNotes: event.target.value }))} />
            </FormField>
          </section>
          <section className="space-y-3">
            <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">Documents</h3>
            <p className="text-xs text-[var(--color-text-muted)]">A new file replaces the current one of the same type when you save.</p>
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
        onConfirm={() => { setConfirmDiscard(false); onClose(); }}
      />
    </>
  );
}
