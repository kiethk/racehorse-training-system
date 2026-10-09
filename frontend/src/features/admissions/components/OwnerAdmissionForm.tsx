'use client';

import { useRef, useState, type FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { AdmissionInfoSection } from '../shared/components/AdmissionInfoSection';
import { ownerAdmissionApi } from '../services/ownerApi';
import type { CreateOwnerAdmissionRequest, AdmissionDocumentType, AdmissionDocumentUpload } from '../types/owner';

const DOCUMENTS: { type: AdmissionDocumentType; label: string; required: boolean }[] = [
  { type: 'HORSE_PHOTO', label: 'Horse photo', required: true },
  { type: 'REGISTRATION_DOCUMENT', label: 'Registration document', required: true },
  { type: 'PEDIGREE_CERTIFICATE', label: 'Pedigree certificate', required: true },
  { type: 'VACCINATION_RECORD', label: 'Vaccination record', required: true },
  { type: 'DEWORMING_RECORD', label: 'Deworming record', required: false },
  { type: 'HEALTH_CERTIFICATE', label: 'Health certificate', required: false },
  { type: 'PREVIOUS_MEDICAL_RECORD', label: 'Previous medical record', required: false },
  { type: 'PREVIOUS_INJURY_RECORD', label: 'Previous injury record', required: false },
];
const FIELDS: { key: keyof CreateOwnerAdmissionRequest; label: string; required?: boolean; type?: string; hint?: string }[] = [
  { key: 'name', label: 'Horse name', required: true },
  { key: 'breed', label: 'Breed' },
  { key: 'dateOfBirth', label: 'Date of birth', type: 'date' },
  { key: 'registrationNumber', label: 'UELN / registration number', hint: '15 letters or numbers, if known.' },
  { key: 'registryName', label: 'Registry name' },
  { key: 'sireName', label: 'Sire name' },
  { key: 'sireRegistrationNumber', label: 'Sire UELN', hint: '15 letters or numbers, if known.' },
  { key: 'damName', label: 'Dam name' },
  { key: 'damRegistrationNumber', label: 'Dam UELN', hint: '15 letters or numbers, if known.' },
];
const inputClass = 'w-full rounded-md border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)] aria-invalid:border-[var(--color-danger)] aria-invalid:focus-visible:ring-[var(--color-danger)]';
type Selection = { file?: File; recordDate: string; note: string };
type DocumentErrors = Partial<Record<AdmissionDocumentType, string>>;
type FieldErrors = Partial<Record<keyof CreateOwnerAdmissionRequest, string>>;

function validateField(key: keyof CreateOwnerAdmissionRequest, raw: string, input?: HTMLInputElement | HTMLTextAreaElement | null) {
  const value = raw.trim();
  if (key === 'name' && !value) return 'Enter the horse name.';
  if (key.toLowerCase().includes('registration') && value && !/^[a-zA-Z0-9]{15}$/.test(value)) {
    return 'Enter 15 letters or numbers, or leave this field empty.';
  }
  if (key === 'dateOfBirth' && input?.validity.badInput) return 'Enter a valid date of birth.';
  if (key === 'dateOfBirth' && input?.validity.rangeOverflow) return 'Date of birth must be today or earlier.';
  const limit = key === 'pedigreeNotes' ? 2000 : 255;
  return value.length > limit ? `Use ${limit} characters or fewer.` : '';
}

export function OwnerAdmissionForm() {
  const router = useRouter();
  const submitting = useRef(false);
  const fileInputs = useRef<Partial<Record<AdmissionDocumentType, HTMLInputElement | null>>>({});
  const fieldInputs = useRef<Partial<Record<keyof CreateOwnerAdmissionRequest, HTMLInputElement | HTMLTextAreaElement | null>>>({});
  const optionalDocuments = useRef<HTMLDetailsElement>(null);
  const recordInputs = useRef<Partial<Record<AdmissionDocumentType, HTMLInputElement | null>>>({});
  const metadataDetails = useRef<Partial<Record<AdmissionDocumentType, HTMLDetailsElement | null>>>({});
  const [form, setForm] = useState<CreateOwnerAdmissionRequest>({ name: '' });
  const [selections, setSelections] = useState<Partial<Record<AdmissionDocumentType, Selection>>>({});
  const [documentErrors, setDocumentErrors] = useState<DocumentErrors>({});
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [recordErrors, setRecordErrors] = useState<DocumentErrors>({});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const completed = DOCUMENTS.filter(section => section.required && selections[section.type]?.file).length;
  const optionalSelected = DOCUMENTS.filter(section => !section.required && selections[section.type]?.file).length;
  const now = new Date();
  const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;

  function updateSelection(type: AdmissionDocumentType, update: Partial<Selection>) {
    setSelections(current => ({ ...current, [type]: { recordDate: '', note: '', ...current[type], ...update } }));
  }

  function selectFile(type: AdmissionDocumentType, file?: File) {
    const imageOnly = type === 'HORSE_PHOTO';
    const allowed = ['image/jpeg', 'image/png', 'image/webp', ...(imageOnly ? [] : ['application/pdf'])];
    let message = '';
    if (file && (file.size === 0 || file.size > 10 * 1024 * 1024)) {
      message = 'File must be between 1 byte and 10 MB.';
    } else if (file && !allowed.includes(file.type)) {
      message = imageOnly ? 'Select a JPEG, PNG or WebP image.' : 'Select a PDF, JPEG, PNG or WebP file.';
    }
    updateSelection(type, { file: message ? undefined : file });
    setDocumentErrors(current => ({ ...current, [type]: message }));
    setError('');
    const input = fileInputs.current[type];
    if (message && input) input.value = '';
  }

  function focusDocument(type: AdmissionDocumentType, recordDate = false) {
    if (!DOCUMENTS.find(section => section.type === type)?.required && optionalDocuments.current) {
      optionalDocuments.current.open = true;
    }
    if (recordDate) {
      const details = metadataDetails.current[type];
      if (details) details.open = true;
      recordInputs.current[type]?.focus();
    } else fileInputs.current[type]?.focus();
  }

  function changeField(key: keyof CreateOwnerAdmissionRequest, value: string) {
    setForm(current => ({ ...current, [key]: value }));
    setFieldErrors(current => ({ ...current, [key]: '' }));
    setError('');
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (submitting.current) return;
    const nextFields: FieldErrors = {};
    for (const key of [...FIELDS.map(field => field.key), 'pedigreeNotes' as const]) {
      nextFields[key] = validateField(key, form[key] || '', fieldInputs.current[key]);
    }
    const nextDocuments = { ...documentErrors };
    const nextRecords: DocumentErrors = {};
    for (const section of DOCUMENTS) {
      if (section.required && !selections[section.type]?.file && !nextDocuments[section.type]) {
        nextDocuments[section.type] = `${section.label} is required.`;
      }
      nextRecords[section.type] = recordInputs.current[section.type]?.validity.badInput ? 'Enter a valid record date.' : '';
    }
    setFieldErrors(nextFields);
    setDocumentErrors(nextDocuments);
    setRecordErrors(nextRecords);
    const firstField = Object.keys(nextFields).find(key => nextFields[key as keyof FieldErrors]) as keyof FieldErrors | undefined;
    const firstDocument = DOCUMENTS.find(section => nextDocuments[section.type] || nextRecords[section.type]);
    if (firstField || firstDocument) {
      setError('Please correct the highlighted fields.');
      if (firstField) fieldInputs.current[firstField]?.focus();
      else if (firstDocument) focusDocument(firstDocument.type, !nextDocuments[firstDocument.type] && Boolean(nextRecords[firstDocument.type]));
      return;
    }
    const candidate: CreateOwnerAdmissionRequest = { name: form.name.trim() };
    for (const field of FIELDS) {
      const value = form[field.key]?.trim();
      if (value) candidate[field.key] = value;
    }
    if (form.pedigreeNotes?.trim()) candidate.pedigreeNotes = form.pedigreeNotes.trim();
    const uploads: AdmissionDocumentUpload[] = DOCUMENTS.flatMap(section => {
      const selection = selections[section.type];
      return selection?.file ? [{ file: selection.file, documentType: section.type, recordDate: selection.recordDate, note: selection.note }] : [];
    });
    submitting.current = true;
    setBusy(true);
    setError('');
    try {
      const created = await ownerAdmissionApi.submit(candidate, uploads);
      router.push(`/owner/admissions/${created.admissionId}?created=1`);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to submit admission.');
      submitting.current = false;
      setBusy(false);
    }
  }

  function renderDocument(section: typeof DOCUMENTS[number]) {
    const selection = selections[section.type];
    const message = documentErrors[section.type];
    return (
      <fieldset key={section.type} className="min-w-0 space-y-3 rounded-[var(--radius-md)] border border-[var(--color-border)] p-4">
        <legend className="px-1 text-sm font-semibold">
          {section.label}{' '}
          {section.required ? <span aria-hidden="true" className="text-[var(--color-danger)]">*</span> : <span className="font-normal text-[var(--color-text-muted)]">(Optional)</span>}
        </legend>
        <div className="max-w-xs space-y-1">
          <label className="block space-y-1 text-sm">
            <span>Select file</span>
            <input id={`file-${section.type}`} type="file"
              className={`block h-10 w-full min-w-0 rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] p-1.5 text-sm file:mr-3 file:h-6 file:cursor-pointer file:rounded file:border-0 file:bg-[var(--color-surface-muted)] file:px-2 file:text-xs file:font-semibold file:text-[var(--color-text-primary)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)] aria-invalid:border-[var(--color-danger)] aria-invalid:focus-visible:ring-[var(--color-danger)] ${selection?.file ? 'text-[var(--color-text-primary)]' : 'text-[var(--color-text-muted)]'}`}
              ref={element => { fileInputs.current[section.type] = element; }}
              accept={section.type === 'HORSE_PHOTO' ? '.jpg,.jpeg,.png,.webp' : '.pdf,.jpg,.jpeg,.png,.webp'}
              aria-required={section.required} aria-invalid={Boolean(message)}
              aria-describedby={`file-hint-${section.type}${message ? ` error-${section.type}` : ''}`}
              onChange={event => selectFile(section.type, event.target.files?.[0])} />
          </label>
          <p id={`file-hint-${section.type}`} className="text-xs leading-5 text-[var(--color-text-muted)]">
            {section.type === 'HORSE_PHOTO' ? 'JPEG, PNG or WebP' : 'PDF, JPEG, PNG or WebP'} · Up to 10 MB
          </p>
          {message && <p id={`error-${section.type}`} role="alert" className="text-sm text-[var(--color-danger)]">{message}</p>}
        </div>
        {selection?.file ? (
          <div className="flex items-center justify-between gap-2 text-sm">
            <span className="min-w-0 break-all">{selection.file.name}</span>
            <Button type="button" size="sm" aria-label={`Remove ${section.label.toLowerCase()}`} onClick={() => {
              selectFile(section.type);
              const input = fileInputs.current[section.type];
              if (input) input.value = '';
            }}>Remove</Button>
          </div>
        ) : message && !section.required ? (
          <Button type="button" size="sm" variant="tertiary" onClick={() => selectFile(section.type)}>Clear file error</Button>
        ) : null}
        <details ref={element => { metadataDetails.current[section.type] = element; }} className="text-sm">
          <summary className="cursor-pointer rounded text-[var(--color-text-secondary)] focus-visible:outline-2 focus-visible:outline-[var(--color-focus)]">
            {selection?.recordDate || selection?.note ? 'Edit date or note' : 'Add date or note (optional)'}
          </summary>
          <div className="mt-3 space-y-3">
            <label className="block space-y-1">
              <span>Record date (optional)</span>
              <input id={`record-${section.type}`} type="date" className={inputClass} value={selection?.recordDate || ''}
                ref={element => { recordInputs.current[section.type] = element; }}
                aria-invalid={Boolean(recordErrors[section.type])} aria-describedby={recordErrors[section.type] ? `record-error-${section.type}` : undefined}
                onBlur={event => setRecordErrors(current => ({ ...current, [section.type]: event.target.validity.badInput ? 'Enter a valid record date.' : '' }))}
                onChange={event => { updateSelection(section.type, { recordDate: event.target.value }); setRecordErrors(current => ({ ...current, [section.type]: '' })); setError(''); }} />
              {recordErrors[section.type] && <span id={`record-error-${section.type}`} role="alert" className="block text-sm text-[var(--color-danger)]">{recordErrors[section.type]}</span>}
            </label>
            <label className="block space-y-1">
              <span>Note (optional)</span>
              <textarea rows={2} maxLength={2000} className={inputClass} value={selection?.note || ''}
                onChange={event => updateSelection(section.type, { note: event.target.value })} />
            </label>
          </div>
        </details>
      </fieldset>
    );
  }

  return (
    <form noValidate onSubmit={submit} className="space-y-5" aria-busy={busy}>
      <p className="text-sm text-[var(--color-text-secondary)]"><span aria-hidden="true" className="font-semibold text-[var(--color-danger)]">*</span> Required fields</p>
      {error && (
        <div role="alert" className="rounded-md border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-3 text-sm text-[var(--color-danger)]">
          <p className="font-medium">{error}</p>
          <ul className="mt-1 list-inside list-disc">
            {[...FIELDS.map(field => ({ key: field.key, label: field.label })), { key: 'pedigreeNotes' as const, label: 'Pedigree notes' }].filter(field => fieldErrors[field.key]).map(field => (
              <li key={field.key}><a href={`#candidate-${field.key}`} className="underline" onClick={event => { event.preventDefault(); fieldInputs.current[field.key]?.focus(); }}>{field.label}: {fieldErrors[field.key]}</a></li>
            ))}
            {DOCUMENTS.filter(section => documentErrors[section.type]).map(section => (
              <li key={section.type}><a href={`#file-${section.type}`} className="underline" onClick={event => { event.preventDefault(); focusDocument(section.type); }}>{documentErrors[section.type]}</a></li>
            ))}
            {DOCUMENTS.filter(section => recordErrors[section.type]).map(section => (
              <li key={`record-${section.type}`}><a href={`#record-${section.type}`} className="underline" onClick={event => { event.preventDefault(); focusDocument(section.type, true); }}>{section.label}: {recordErrors[section.type]}</a></li>
            ))}
          </ul>
        </div>
      )}
      <fieldset disabled={busy} className="min-w-0 space-y-5">
        <AdmissionInfoSection title="Candidate horse & pedigree">
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            {FIELDS.map(field => (
              <label key={field.key} className="block space-y-1">
                <span className="block text-sm font-medium">{field.label}{field.required && <span aria-hidden="true" className="text-[var(--color-danger)]"> *</span>}</span>
                <input id={`candidate-${field.key}`} className={inputClass} type={field.type || 'text'} value={form[field.key] || ''}
                  ref={element => { fieldInputs.current[field.key] = element; }}
                  required={field.required}
                  maxLength={field.key.toLowerCase().includes('registration') ? 15 : 255}
                  pattern={field.key.toLowerCase().includes('registration') ? '[a-zA-Z0-9]{15}' : undefined}
                  max={field.type === 'date' ? today : undefined}
                  aria-invalid={Boolean(fieldErrors[field.key])}
                  aria-describedby={[field.hint ? `hint-${field.key}` : '', fieldErrors[field.key] ? `error-${field.key}` : ''].filter(Boolean).join(' ') || undefined}
                  onBlur={event => setFieldErrors(current => ({ ...current, [field.key]: validateField(field.key, event.target.value, event.target) }))}
                  onChange={event => changeField(field.key, event.target.value)} />
                {field.hint && <span id={`hint-${field.key}`} className="block text-xs leading-5 text-[var(--color-text-muted)]">{field.hint}</span>}
                {fieldErrors[field.key] && <span id={`error-${field.key}`} role="alert" className="block text-sm text-[var(--color-danger)]">{fieldErrors[field.key]}</span>}
              </label>
            ))}
          </div>
          <label className="block space-y-1">
            <span className="text-sm font-medium">Pedigree notes</span>
            <textarea id="candidate-pedigreeNotes" className={inputClass} rows={3} maxLength={2000} value={form.pedigreeNotes || ''}
              ref={element => { fieldInputs.current.pedigreeNotes = element; }}
              aria-invalid={Boolean(fieldErrors.pedigreeNotes)} aria-describedby={fieldErrors.pedigreeNotes ? 'error-pedigreeNotes' : undefined}
              onBlur={event => setFieldErrors(current => ({ ...current, pedigreeNotes: validateField('pedigreeNotes', event.target.value) }))}
              onChange={event => changeField('pedigreeNotes', event.target.value)} />
            {fieldErrors.pedigreeNotes && <span id="error-pedigreeNotes" role="alert" className="block text-sm text-[var(--color-danger)]">{fieldErrors.pedigreeNotes}</span>}
          </label>
        </AdmissionInfoSection>
        <AdmissionInfoSection title="Supporting documents">
          <p className="text-sm text-[var(--color-text-secondary)]">
            Choose one file per section. All files upload when you submit admission.
          </p>
          <p role="status" className="text-sm">Required documents selected: {completed}/4</p>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            {DOCUMENTS.filter(section => section.required).map(renderDocument)}
          </div>
          <details ref={optionalDocuments} className="rounded-[var(--radius-md)] border border-[var(--color-border)] p-4">
            <summary className="cursor-pointer rounded text-sm font-medium focus-visible:outline-2 focus-visible:outline-[var(--color-focus)]">
              Optional documents
              <span className="ml-2 font-normal text-[var(--color-text-muted)]">{optionalSelected ? `${optionalSelected} selected` : '4 additional document types'}</span>
            </summary>
            <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
              {DOCUMENTS.filter(section => !section.required).map(renderDocument)}
            </div>
          </details>
        </AdmissionInfoSection>
        <div className="flex flex-wrap justify-end gap-3">
          <Button type="button" onClick={() => router.push('/owner/admissions')}>Cancel</Button>
          <Button variant="primary" type="submit" loading={busy}>Submit admission</Button>
        </div>
      </fieldset>
    </form>
  );
}
