'use client';

import { useId, useRef, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ownerAdmissionApi } from '@/features/admissions/services/ownerApi';
import type { AdmissionDocumentType } from '@/features/admissions/types/owner';

const DOCUMENT_SECTIONS: {
    type: AdmissionDocumentType;
    label: string;
    required: boolean;
}[] = [
    { type: 'HORSE_PHOTO', label: 'Horse photo', required: true },
    { type: 'REGISTRATION_DOCUMENT', label: 'Registration document', required: true },
    { type: 'PEDIGREE_CERTIFICATE', label: 'Pedigree certificate', required: true },
    { type: 'VACCINATION_RECORD', label: 'Vaccination record', required: true },
    { type: 'DEWORMING_RECORD', label: 'Deworming record', required: false },
    { type: 'HEALTH_CERTIFICATE', label: 'Health certificate', required: false },
    { type: 'PREVIOUS_MEDICAL_RECORD', label: 'Previous medical record', required: false },
    { type: 'PREVIOUS_INJURY_RECORD', label: 'Previous injury record', required: false },
];

type Document = {
    id: number;
    documentType: string;
};

type Props = {
    admissionId: number;
    documents: Document[];
    locked: boolean;
    onUploaded: () => Promise<void>;
};

const inputClass = 'block min-h-11 w-full rounded-md border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)] aria-invalid:border-[var(--color-danger)] aria-invalid:focus-visible:ring-[var(--color-danger)]';

export function AdmissionDocumentUploads({ admissionId, documents, locked, onUploaded }: Props) {
    const [open, setOpen] = useState(false);
    const [selectedType, setSelectedType] = useState<AdmissionDocumentType | ''>('');
    const [draftActive, setDraftActive] = useState(false);
    const completed = DOCUMENT_SECTIONS.filter(
        section => section.required && documents.some(document => document.documentType === section.type),
    ).length;
    const selectedSection = DOCUMENT_SECTIONS.find(section => section.type === selectedType);

    function cancel() {
        setOpen(false);
        if (!draftActive) setSelectedType('');
    }

    function discardDraft() {
        setDraftActive(false);
        setSelectedType('');
        setOpen(false);
    }

    return (
        <div className="space-y-3">
            <p role="status" className="text-sm text-[var(--color-text-secondary)]">
                Required documents: {completed}/4 uploaded.
            </p>
            <p className="text-sm text-[var(--color-text-secondary)]">
                Your admission is already submitted. Adding documents is optional.
            </p>
            {locked && <p className="text-sm">Documents are locked for review.</p>}

            <Button
                type="button"
                variant="secondary"
                className="min-h-11 whitespace-normal"
                disabled={locked}
                aria-expanded={open}
                aria-controls="admission-document-upload-panel"
                onClick={() => setOpen(current => !current)}
            >
                {open ? 'Hide upload form' : draftActive ? 'Continue document upload' : 'Add document'}
            </Button>

            <div id="admission-document-upload-panel" hidden={!open} className="max-w-xl space-y-4 rounded-[var(--radius-md)] border border-[var(--color-border)] p-4">
                {draftActive && (
                    <p className="text-sm text-[var(--color-text-secondary)]">
                        Your selected file and optional details are saved in this form. Hiding it will keep this draft.
                    </p>
                )}
                <label className="block space-y-1 text-sm font-medium">
                    <span>Document type</span>
                    <select
                        className={inputClass}
                        value={selectedType}
                        disabled={locked || draftActive}
                        onChange={event => setSelectedType(event.target.value as AdmissionDocumentType | '')}
                    >
                        <option value="">Choose a document type</option>
                        {DOCUMENT_SECTIONS.map(section => (
                            <option key={section.type} value={section.type}>
                                {section.label}{section.required ? ' (required at submission)' : ' (optional at submission)'}
                            </option>
                        ))}
                    </select>
                </label>

                {selectedSection && (
                    <DocumentUploadSection
                        key={selectedSection.type}
                        admissionId={admissionId}
                        section={selectedSection}
                        locked={locked}
                        onUploaded={onUploaded}
                        onDone={discardDraft}
                        onDraftChange={setDraftActive}
                        onCancel={cancel}
                        onDiscard={discardDraft}
                    />
                )}
                {!selectedSection && !draftActive && (
                    <div className="flex flex-wrap gap-2">
                        <Button type="button" className="min-h-11" onClick={cancel}>Cancel</Button>
                    </div>
                )}
            </div>
        </div>
    );
}

function DocumentUploadSection({
    admissionId,
    section,
    locked,
    onUploaded,
    onDone,
    onDraftChange,
    onCancel,
    onDiscard,
}: {
    admissionId: number;
    section: (typeof DOCUMENT_SECTIONS)[number];
    locked: boolean;
    onUploaded: () => Promise<void>;
    onDone: () => void;
    onDraftChange: (active: boolean) => void;
    onCancel: () => void;
    onDiscard: () => void;
}) {
    const [file, setFile] = useState<File | null>(null);
    const [recordDate, setRecordDate] = useState('');
    const [note, setNote] = useState('');
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState('');
    const [fileValidationError, setFileValidationError] = useState('');
    const [dateError, setDateError] = useState('');
    const [inputKey, setInputKey] = useState(0);
    const recordDateInput = useRef<HTMLInputElement>(null);
    const metadataDetails = useRef<HTMLDetailsElement>(null);
    const id = useId();
    const fileHintId = `${id}-file-hint`;
    const fileErrorId = `${id}-file-error`;
    const dateErrorId = `${id}-date-error`;
    const imageOnly = section.type === 'HORSE_PHOTO';
    const acceptedTypes = imageOnly
        ? ['image/jpeg', 'image/png', 'image/webp']
        : ['application/pdf', 'image/jpeg', 'image/png', 'image/webp'];

    function updateDraftState(nextFile: File | null, nextDate = recordDate, nextNote = note, nextDateError = Boolean(dateError)) {
        onDraftChange(Boolean(nextFile || nextDate || nextNote || nextDateError));
    }

    function selectFile(candidate?: File) {
        setError('');
        setFileValidationError('');
        if (!candidate) {
            setFile(null);
            updateDraftState(null);
            return;
        }

        if (candidate.size === 0 || candidate.size > 10 * 1024 * 1024) {
            setFile(null);
            updateDraftState(null);
            setFileValidationError('File must be between 1 byte and 10 MB.');
            setInputKey(current => current + 1);
            return;
        }

        if (!acceptedTypes.includes(candidate.type)) {
            setFile(null);
            updateDraftState(null);
            setFileValidationError(
                imageOnly
                    ? 'Select a JPEG, PNG or WebP image.'
                    : 'Select a PDF, JPEG, PNG or WebP file.',
            );
            setInputKey(current => current + 1);
            return;
        }

        setFile(candidate);
        updateDraftState(candidate);
    }

    function changeRecordDate(value: string) {
        setRecordDate(value);
        updateDraftState(file, value, note, false);
    }

    function changeNote(value: string) {
        setNote(value);
        updateDraftState(file, recordDate, value);
    }

    async function upload() {
        if (!file || busy || locked) return;
        const dateInput = recordDateInput.current;
        if (dateError || dateInput?.validity.badInput) {
            if (metadataDetails.current) metadataDetails.current.open = true;
            setDateError('Enter a valid record date.');
            dateInput?.focus();
            return;
        }

        setBusy(true);
        setError('');
        let uploaded = false;

        try {
            await ownerAdmissionApi.upload(
                admissionId,
                file,
                section.type,
                recordDate || undefined,
                note.trim() || undefined,
            );
            uploaded = true;
        } catch (cause) {
            setError(cause instanceof Error ? cause.message : 'Upload failed.');
        }

        if (!uploaded) {
            setBusy(false);
            return;
        }

        setFile(null);
        setRecordDate('');
        setNote('');
        onDraftChange(false);
        setInputKey(current => current + 1);

        let refreshed = false;
        try {
            await onUploaded();
            refreshed = true;
        } catch {
            setError('File uploaded. Reload the page to refresh the document list.');
        }

        setBusy(false);
        if (refreshed) onDone();
    }

    return (
        <fieldset
            disabled={locked || busy}
            className="min-w-0 space-y-4 rounded-[var(--radius-md)] border border-[var(--color-border)] p-4"
        >
            <legend className="px-1 text-sm font-semibold">
                {section.label}
            </legend>

            <label className="block max-w-xs space-y-1 text-sm">
                <span>Select file <span aria-hidden="true" className="text-[var(--color-danger)]">*</span></span>
                <input
                    key={inputKey}
                    type="file"
                    accept={imageOnly ? '.jpg,.jpeg,.png,.webp' : '.pdf,.jpg,.jpeg,.png,.webp'}
                    className="block min-h-11 w-full min-w-0 rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] p-1.5 text-sm text-[var(--color-text-muted)] file:mr-3 file:min-h-8 file:cursor-pointer file:rounded file:border-0 file:bg-[var(--color-surface-muted)] file:px-3 file:text-xs file:font-semibold file:text-[var(--color-text-primary)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)] aria-invalid:border-[var(--color-danger)] aria-invalid:focus-visible:ring-[var(--color-danger)]"
                    aria-required="true"
                    aria-invalid={Boolean(fileValidationError)}
                    aria-describedby={`${fileHintId}${fileValidationError ? ` ${fileErrorId}` : ''}${error ? ` ${fileErrorId}-server` : ''}`}
                    onChange={event => selectFile(event.target.files?.[0])}
                />
                <span id={fileHintId} className="block text-xs leading-5 text-[var(--color-text-muted)]">
                    {imageOnly ? 'JPEG, PNG or WebP' : 'PDF, JPEG, PNG or WebP'} · Up to 10 MB
                </span>
                {fileValidationError && <span id={fileErrorId} role="alert" className="block text-sm text-[var(--color-danger)]">{fileValidationError}</span>}
                {error && <span id={`${fileErrorId}-server`} role="alert" className="block text-sm text-[var(--color-danger)]">{error}</span>}
            </label>

            {file && (
                <div className="flex min-h-11 flex-wrap items-center justify-between gap-2 text-sm">
                    <span className="min-w-0 break-all text-[var(--color-text-muted)]">{file.name}</span>
                    <Button
                        type="button"
                        className="min-h-11"
                        aria-label={`Remove selected ${section.label.toLowerCase()}`}
                        onClick={() => {
                            setFile(null);
                            setError('');
                            setFileValidationError('');
                            setInputKey(current => current + 1);
                            updateDraftState(null);
                        }}
                    >
                        Remove file
                    </Button>
                </div>
            )}

            <details ref={metadataDetails} className="text-sm">
                <summary className="block min-h-11 cursor-pointer rounded py-2 text-[var(--color-text-secondary)] focus-visible:outline-2 focus-visible:outline-[var(--color-focus)]">
                    {recordDate || note ? 'Edit date or note' : 'Add date or note (optional)'}
                </summary>
                <div className="mt-3 space-y-3">
                    <label className="block space-y-1">
                        <span>Record date (optional)</span>
                        <input
                            type="date"
                            value={recordDate}
                            ref={recordDateInput}
                            className={inputClass}
                            aria-invalid={Boolean(dateError)}
                            aria-describedby={dateError ? dateErrorId : undefined}
                            onBlur={event => {
                                if (event.target.validity.badInput) {
                                    setDateError('Enter a valid record date.');
                                    onDraftChange(true);
                                } else setDateError('');
                            }}
                            onChange={event => {
                                changeRecordDate(event.target.value);
                                setDateError('');
                            }}
                        />
                        {dateError && (
                            <span id={dateErrorId} role="alert" className="block text-sm text-[var(--color-danger)]">{dateError}</span>
                        )}
                    </label>
                    <label className="block space-y-1">
                        <span>Note (optional)</span>
                        <textarea
                            rows={2}
                            maxLength={2000}
                            value={note}
                            className={inputClass}
                            onChange={event => changeNote(event.target.value)}
                        />
                    </label>
                </div>
            </details>

            <div className="flex flex-wrap gap-2">
                <Button
                    type="button"
                    variant="primary"
                    className="min-h-11 whitespace-normal"
                    disabled={!file || locked || busy}
                    loading={busy}
                    onClick={upload}
                >
                    Upload {section.label.toLowerCase()}
                </Button>
                <Button
                    type="button"
                    className="min-h-11 whitespace-normal"
                    disabled={busy}
                    onClick={onCancel}
                >
                    {draftActiveLabel(file, recordDate, note, Boolean(dateError)) ? 'Hide and keep draft' : 'Cancel'}
                </Button>
                {draftActiveLabel(file, recordDate, note, Boolean(dateError)) && (
                    <Button
                        type="button"
                        variant="destructive"
                        className="min-h-11 whitespace-normal"
                        disabled={busy}
                        onClick={onDiscard}
                    >
                        Discard draft
                    </Button>
                )}
            </div>
        </fieldset>
    );
}

function draftActiveLabel(file: File | null, recordDate: string, note: string, hasDateError = false) {
    return Boolean(file || recordDate || note || hasDateError);
}
