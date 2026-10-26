'use client';

import { FileInput, Select } from '@/components/ui/Input';
import { FormField } from '@/components/ui/FormField';
import { useEffect, useId, useRef, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ImageLightbox, ImageThumbnail } from '@/components/ui/ImageLightbox';
import { isImageFile } from '@/features/admissions/shared/lib/documentKind';
import { ownerAdmissionApi } from '@/features/admissions/services/ownerApi';
import { LocalFilePreviewModal } from '@/features/admissions/shared/components/LocalFilePreviewModal';
import { renderPdfFirstPage } from '@/lib/pdfThumbnail';
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

export function AdmissionDocumentUploads({ admissionId, documents, locked, onUploaded }: Props) {
    const [open, setOpen] = useState(false);
    const [selectedType, setSelectedType] = useState<AdmissionDocumentType | ''>('');
    const [draftActive, setDraftActive] = useState(false);

    const uploadedTypes = new Set(documents.map(d => d.documentType));
    const completed = DOCUMENT_SECTIONS.filter(
        section => section.required && uploadedTypes.has(section.type),
    ).length;
    const missingRequired = DOCUMENT_SECTIONS.filter(
        section => section.required && !uploadedTypes.has(section.type),
    );
    const selectedSection = DOCUMENT_SECTIONS.find(section => section.type === selectedType);
    const isSelectedUploaded = selectedSection ? uploadedTypes.has(selectedSection.type) : false;

    function cancel() {
        setOpen(false);
        if (!draftActive) setSelectedType('');
    }

    function discardDraft() {
        setDraftActive(false);
        setSelectedType('');
        setOpen(false);
    }

    function startUpload(type: AdmissionDocumentType) {
        setSelectedType(type);
        setOpen(true);
    }

    return (
        <div className="space-y-3">
            <div className="space-y-1 text-sm text-[var(--color-text-secondary)]">
                <p role="status" className="font-medium text-[var(--color-text-primary)]">
                    Required documents: {completed}/4 uploaded
                </p>
                <p className="text-xs">
                    {locked
                        ? 'Documents are locked after Groom completes review.'
                        : 'You can upload missing files or re-upload to overwrite existing files until Groom approves.'}
                </p>
            </div>

            {/* Quick list of missing required documents if any */}
            {!locked && missingRequired.length > 0 && !open && (
                <div className="space-y-1.5 rounded-[var(--radius-sm)] border border-amber-300 bg-amber-50 dark:border-amber-700/50 dark:bg-amber-950/20 p-2.5 text-xs">
                    <p className="font-semibold text-amber-900 dark:text-amber-200">
                        Missing required documents:
                    </p>
                    <div className="flex flex-wrap gap-1.5 pt-1">
                        {missingRequired.map(section => (
                            <Button
                                key={section.type}
                                size="sm"
                                variant="secondary"
                                className="text-xs h-7 px-2"
                                onClick={() => startUpload(section.type)}
                            >
                                + Upload {section.label}
                            </Button>
                        ))}
                    </div>
                </div>
            )}

            <Button
                type="button"
                variant="secondary"
                className="min-h-11 whitespace-normal"
                disabled={locked}
                aria-expanded={open}
                aria-controls="admission-document-upload-panel"
                onClick={() => setOpen(current => !current)}
            >
                {open ? 'Hide upload form' : draftActive ? 'Continue document upload' : 'Upload / Replace document'}
            </Button>

            <div id="admission-document-upload-panel" hidden={!open} className="max-w-xl space-y-4 rounded-[var(--radius-md)] border border-[var(--color-border)] p-4">
                {draftActive && (
                    <p className="text-sm text-[var(--color-text-secondary)]">
                        Your selected file is saved in this form. Hiding it will keep this draft.
                    </p>
                )}
                <FormField label="Document type">
                  <Select
                        value={selectedType}
                        disabled={locked || draftActive}
                        onChange={event => setSelectedType(event.target.value as AdmissionDocumentType | '')}
                    >
                        <option value="">Choose a document type</option>
                        {DOCUMENT_SECTIONS.map(section => {
                            const uploaded = uploadedTypes.has(section.type);
                            return (
                                <option key={section.type} value={section.type}>
                                    {section.label} — {uploaded ? 'Uploaded (re-upload to overwrite)' : section.required ? 'Not uploaded (Required)' : 'Not uploaded (Optional)'}
                                </option>
                            );
                        })}
                    </Select>
                </FormField>

                {selectedSection && (
                    <DocumentUploadSection
                        key={selectedSection.type}
                        admissionId={admissionId}
                        section={selectedSection}
                        isUploaded={isSelectedUploaded}
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
    isUploaded,
    locked,
    onUploaded,
    onDone,
    onDraftChange,
    onCancel,
    onDiscard,
}: {
    admissionId: number;
    section: (typeof DOCUMENT_SECTIONS)[number];
    isUploaded: boolean;
    locked: boolean;
    onUploaded: () => Promise<void>;
    onDone: () => void;
    onDraftChange: (active: boolean) => void;
    onCancel: () => void;
    onDiscard: () => void;
}) {
    const [file, setFile] = useState<File | null>(null);
    const [previewUrl, setPreviewUrl] = useState<string | null>(null);
    const [rawPdfUrl, setRawPdfUrl] = useState<string | null>(null);
    const [activePdfPreview, setActivePdfPreview] = useState<{ file: File; url: string } | null>(null);
    const [viewingPreview, setViewingPreview] = useState(false);
    const previewRef = useRef<string | null>(null);
    const rawPdfRef = useRef<string | null>(null);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState('');
    const [fileValidationError, setFileValidationError] = useState('');
    const [inputKey, setInputKey] = useState(0);
    const id = useId();
    const fileHintId = `${id}-file-hint`;
    const fileErrorId = `${id}-file-error`;
    const imageOnly = section.type === 'HORSE_PHOTO';
    const acceptedTypes = imageOnly
        ? ['image/jpeg', 'image/png', 'image/webp']
        : ['application/pdf', 'image/jpeg', 'image/png', 'image/webp'];

    useEffect(() => () => {
        if (previewRef.current) URL.revokeObjectURL(previewRef.current);
        if (rawPdfRef.current) URL.revokeObjectURL(rawPdfRef.current);
    }, []);

    function choose(nextFile: File | null) {
        if (previewRef.current) {
            URL.revokeObjectURL(previewRef.current);
            previewRef.current = null;
        }
        if (rawPdfRef.current) {
            URL.revokeObjectURL(rawPdfRef.current);
            rawPdfRef.current = null;
        }
        setPreviewUrl(null);
        setRawPdfUrl(null);
        setViewingPreview(false);
        setActivePdfPreview(null);
        setFile(nextFile);

        if (!nextFile) return;

        if (isImageFile(nextFile)) {
            const url = URL.createObjectURL(nextFile);
            previewRef.current = url;
            setPreviewUrl(url);
        } else if (nextFile.type === 'application/pdf' || nextFile.name.toLowerCase().endsWith('.pdf')) {
            const rawUrl = URL.createObjectURL(nextFile);
            rawPdfRef.current = rawUrl;
            setRawPdfUrl(rawUrl);
            renderPdfFirstPage(nextFile, 240)
                .then(png => {
                    const thumbUrl = URL.createObjectURL(png);
                    previewRef.current = thumbUrl;
                    setPreviewUrl(thumbUrl);
                })
                .catch(() => undefined);
        }
    }

    function updateDraftState(nextFile: File | null) {
        onDraftChange(Boolean(nextFile));
    }

    function selectFile(candidate?: File) {
        setError('');
        setFileValidationError('');
        if (!candidate) {
            choose(null);
            updateDraftState(null);
            return;
        }

        if (candidate.size === 0 || candidate.size > 10 * 1024 * 1024) {
            choose(null);
            updateDraftState(null);
            setFileValidationError('File must be between 1 byte and 10 MB.');
            setInputKey(current => current + 1);
            return;
        }

        if (!acceptedTypes.includes(candidate.type)) {
            choose(null);
            updateDraftState(null);
            setFileValidationError(
                imageOnly
                    ? 'Select a JPEG, PNG or WebP image.'
                    : 'Select a PDF, JPEG, PNG or WebP file.',
            );
            setInputKey(current => current + 1);
            return;
        }

        choose(candidate);
        updateDraftState(candidate);
    }

    async function upload() {
        if (!file || busy || locked) return;
        setBusy(true);
        setError('');
        let uploaded = false;

        try {
            await ownerAdmissionApi.upload(
                admissionId,
                file,
                section.type,
            );
            uploaded = true;
        } catch (cause) {
            setError(cause instanceof Error ? cause.message : 'Upload failed.');
        }

        if (!uploaded) {
            setBusy(false);
            return;
        }

        choose(null);
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

    const isPdf = file ? (file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf')) : false;

    return (
        <fieldset disabled={busy} className="space-y-4">
            <legend className="text-base font-semibold text-[var(--color-text-primary)]">
                {isUploaded ? `Replace ${section.label}` : `Upload ${section.label}`}{' '}
                {section.required ? (
                    <span aria-hidden="true" className="text-[var(--color-danger)]">*</span>
                ) : (
                    <span className="text-sm font-normal text-[var(--color-text-secondary)]">(Optional)</span>
                )}
            </legend>

            {isUploaded && (
                <div className="rounded-[var(--radius-sm)] border border-[var(--color-primary)] bg-[var(--color-primary-soft)] p-2.5 text-xs text-[var(--color-primary)]">
                    This document was previously uploaded. Uploading a new file will overwrite the existing version upon approval.
                </div>
            )}

            {error && (
                <p role="alert" className="text-sm text-[var(--color-danger)]">{error}</p>
            )}

            <div className="space-y-1">
                <label htmlFor={`file-${section.type}`} className="block space-y-1 text-sm">
                <span>Select file <span aria-hidden="true" className="text-[var(--color-danger)]">*</span></span>
                <FileInput
                    key={inputKey}
                    id={`file-${section.type}`}
                    accept={imageOnly ? '.jpg,.jpeg,.png,.webp' : '.pdf,.jpg,.jpeg,.png,.webp'}
                    disabled={locked || busy}
                    aria-describedby={[fileHintId, fileValidationError ? fileErrorId : ''].filter(Boolean).join(' ') || undefined}
                    aria-invalid={Boolean(fileValidationError)}
                    className={file ? 'text-[var(--color-text-primary)]' : 'text-[var(--color-text-muted)]'}
                    onChange={event => selectFile(event.target.files?.[0])}
                />
                </label>
                <p id={fileHintId} className="text-xs text-[var(--color-text-secondary)]">
                    {imageOnly ? 'JPEG, PNG or WebP' : 'PDF, JPEG, PNG or WebP'} up to 10 MB.
                </p>
                {fileValidationError && (
                    <p id={fileErrorId} role="alert" className="text-sm text-[var(--color-danger)]">{fileValidationError}</p>
                )}
            </div>

            {file && (
                <div className="flex flex-wrap items-center justify-between gap-3 rounded-[var(--radius-sm)] border border-[var(--color-border)] p-3 text-sm">
                    <span className="flex min-w-0 items-center gap-3">
                        {previewUrl ? (
                            <ImageThumbnail
                                src={previewUrl}
                                alt={file.name}
                                onClick={() => {
                                    if (isPdf) {
                                        setActivePdfPreview({ file, url: rawPdfUrl || previewUrl });
                                    } else {
                                        setViewingPreview(true);
                                    }
                                }}
                                className="h-14 w-14 shrink-0 rounded border border-[var(--color-border)] cursor-pointer"
                            />
                        ) : (
                            <Button
                                type="button"
                                variant="secondary"
                                onClick={() => {
                                    if (isPdf) {
                                        setActivePdfPreview({ file, url: rawPdfUrl || '' });
                                    } else {
                                        setViewingPreview(true);
                                    }
                                }}
                                className="flex h-14 w-14 shrink-0 items-center justify-center rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] text-xs font-semibold text-[var(--color-text-muted)] hover:border-[var(--color-primary)] cursor-pointer"
                            >
                                {isPdf ? 'PDF' : 'FILE'}
                            </Button>
                        )}
                        <span className="min-w-0">
                            <span className="block truncate font-medium text-[var(--color-text-primary)]">{file.name}</span>
                            <span className="block text-xs text-[var(--color-text-secondary)]">
                                {(file.size / (1024 * 1024)).toFixed(2)} MB ·{' '}
                                <Button
                                    type="button"
                                    variant="link"
                                    size="sm"
                                    onClick={() => {
                                        if (isPdf) {
                                            setActivePdfPreview({ file, url: rawPdfUrl || previewUrl || '' });
                                        } else {
                                            setViewingPreview(true);
                                        }
                                    }}
                                    className="p-0 text-xs text-[var(--color-primary)]"
                                >
                                    {isPdf ? 'Preview full PDF' : 'Preview image'}
                                </Button>
                            </span>
                        </span>
                    </span>
                    <Button
                        type="button"
                        size="sm"
                        disabled={busy}
                        aria-label={`Remove ${section.label}`}
                        onClick={() => {
                            choose(null);
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

            <div className="flex flex-wrap gap-2">
                <Button
                    type="button"
                    variant="primary"
                    className="min-h-11 whitespace-normal"
                    disabled={!file || locked || busy}
                    loading={busy}
                    onClick={upload}
                >
                    {isUploaded ? `Upload & replace ${section.label.toLowerCase()}` : `Upload ${section.label.toLowerCase()}`}
                </Button>
                <Button
                    type="button"
                    className="min-h-11 whitespace-normal"
                    disabled={busy}
                    onClick={onCancel}
                >
                    {draftActiveLabel(file) ? 'Hide and keep draft' : 'Cancel'}
                </Button>
                {draftActiveLabel(file) && (
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
            {activePdfPreview && (
                <LocalFilePreviewModal
                    file={activePdfPreview.file}
                    previewUrl={activePdfPreview.url}
                    onClose={() => setActivePdfPreview(null)}
                />
            )}
            {previewUrl && file && !isPdf && (
                <ImageLightbox
                    images={[{ src: previewUrl, alt: file.name }]}
                    index={viewingPreview ? 0 : null}
                    onIndexChange={() => undefined}
                    onClose={() => setViewingPreview(false)}
                />
            )}
        </fieldset>
    );
}

function draftActiveLabel(file: File | null): boolean {
    return Boolean(file);
}
