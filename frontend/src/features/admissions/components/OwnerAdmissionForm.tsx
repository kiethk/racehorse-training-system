'use client';

import { FileInput, Input, Select, Textarea } from '@/components/ui/Input';
import { FormField } from '@/components/ui/FormField';
import { useEffect, useRef, useState, type FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { Notice } from '@/components/ui/Notice';
import { Icon } from '@/components/ui/Icon';
import { ImageLightbox, ImageThumbnail } from '@/components/ui/ImageLightbox';
import { AdmissionInfoSection } from '../shared/components/AdmissionInfoSection';
import { ownerAdmissionApi, type OcrExtractionData } from '../services/ownerApi';
import { useAdmissionUelnCheck } from '../hooks/useAdmissionUelnCheck';
import { LocalFilePreviewModal } from '../shared/components/LocalFilePreviewModal';
import { renderPdfFirstPage } from '@/lib/pdfThumbnail';
import { ApiError } from '@/services/api';
import type { CreateOwnerAdmissionRequest, AdmissionDocumentType, AdmissionDocumentUpload } from '../types/owner';
import type { HorseSex } from '@/types/horse';
import { HORSE_SEX_OPTIONS, isHorseSex } from '@/features/horses/lib/sex';

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
const FIELDS: { key: Exclude<keyof CreateOwnerAdmissionRequest, 'sex'>; label: string; required?: boolean; type?: string; hint?: string }[] = [
  { key: 'name', label: 'Horse name', required: true },
  { key: 'breed', label: 'Breed' },
  { key: 'dateOfBirth', label: 'Date of birth', type: 'date' },
  { key: 'registrationNumber', label: 'UELN / registration number', hint: '15 characters (e.g. 704001SC0000001 or VNM22SC00000001), if known.' },
  { key: 'registryName', label: 'Registry name' },
  { key: 'sireName', label: 'Sire name' },
  { key: 'sireRegistrationNumber', label: 'Sire UELN', hint: '15 characters (e.g. 704001RT0000001 or VNM18RT00000001), if known.' },
  { key: 'damName', label: 'Dam name' },
  { key: 'damRegistrationNumber', label: 'Dam UELN', hint: '15 characters (e.g. 704001GB0000001 or VNM19GB00000001), if known.' },
];
const SEX_FIELD = { key: 'sex', label: 'Sex', required: true } as const;
const FORM_FIELDS = [...FIELDS.slice(0, 2), SEX_FIELD, ...FIELDS.slice(2)];
type AdmissionFormState = Omit<CreateOwnerAdmissionRequest, 'sex'> & { sex: HorseSex | '' };
type Selection = { file?: File; recordDate: string; note: string };
type DocumentErrors = Partial<Record<AdmissionDocumentType, string>>;
type FieldErrors = Partial<Record<keyof CreateOwnerAdmissionRequest, string>>;

function normalizeUeln(raw?: string | null): string | null {
  if (!raw) return null;
  const trimmed = raw.trim();
  return trimmed ? trimmed.toUpperCase() : null;
}

function validateField(key: keyof CreateOwnerAdmissionRequest, raw: string, input?: HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement | null) {
  const value = raw.trim();
  if (key === 'name' && !value) return 'Enter the horse name.';
  if (key === 'sex' && !isHorseSex(value)) return 'Select Male or Female.';
  if (key.toLowerCase().includes('registration') && value) {
    const normalized = value.toUpperCase();
    if (!/^[0-9]{6}[A-Z0-9]{9}$/.test(normalized) && !/^[A-Z]{3}[A-Z0-9]{12}$/.test(normalized)) {
      return 'Enter a valid 15-character UELN (e.g. 704001SC0000001 or VNM22SC00000001), or leave this field empty.';
    }
  }
  if (key === 'dateOfBirth' && input?.validity.badInput) return 'Enter a valid date of birth.';
  if (key === 'dateOfBirth' && input?.validity.rangeOverflow) return 'Date of birth must be today or earlier.';
  const limit = key === 'pedigreeNotes' ? 2000 : 255;
  return value.length > limit ? `Use ${limit} characters or fewer.` : '';
}

function validatePedigreeRelationships(formData: Omit<CreateOwnerAdmissionRequest, 'sex'>): FieldErrors {
  const errors: FieldErrors = {};
  const childUeln = normalizeUeln(formData.registrationNumber);
  const sireUeln = normalizeUeln(formData.sireRegistrationNumber);
  const damUeln = normalizeUeln(formData.damRegistrationNumber);

  if (childUeln && sireUeln && childUeln === sireUeln) {
    errors.sireRegistrationNumber = 'Sire UELN cannot be the same as horse UELN.';
  }
  if (childUeln && damUeln && childUeln === damUeln) {
    errors.damRegistrationNumber = 'Dam UELN cannot be the same as horse UELN.';
  }
  if (sireUeln && damUeln && sireUeln === damUeln) {
    errors.damRegistrationNumber = 'Dam UELN cannot be the same as sire UELN.';
  }
  return errors;
}

export function OwnerAdmissionForm() {
  const router = useRouter();
  const { result: uelnCheck, check: checkUeln } = useAdmissionUelnCheck();
  const submitting = useRef(false);
  const fileInputs = useRef<Partial<Record<AdmissionDocumentType, HTMLInputElement | null>>>({});
  const fieldInputs = useRef<Partial<Record<keyof CreateOwnerAdmissionRequest, HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement | null>>>({});
  const optionalDocuments = useRef<HTMLDetailsElement>(null);
  const recordInputs = useRef<Partial<Record<AdmissionDocumentType, HTMLInputElement | null>>>({});
  const metadataDetails = useRef<Partial<Record<AdmissionDocumentType, HTMLDetailsElement | null>>>({});
  const [form, setForm] = useState<AdmissionFormState>({ name: '', sex: '' });
  const [selections, setSelections] = useState<Partial<Record<AdmissionDocumentType, Selection>>>({});
  const [filePreviews, setFilePreviews] = useState<Partial<Record<AdmissionDocumentType, string>>>({});
  const [rawFileUrls, setRawFileUrls] = useState<Partial<Record<AdmissionDocumentType, string>>>({});
  const [activePdfPreview, setActivePdfPreview] = useState<{ file: File; url: string } | null>(null);
  const [lightboxImage, setLightboxImage] = useState<{ src: string; alt: string } | null>(null);
  const [documentErrors, setDocumentErrors] = useState<DocumentErrors>({});
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [recordErrors, setRecordErrors] = useState<DocumentErrors>({});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  // OCR state
  const [scanningDocType, setScanningDocType] = useState<AdmissionDocumentType | null>(null);
  const isAnyScanning = scanningDocType !== null;
  const [ocrNotice, setOcrNotice] = useState<{ tone: 'success' | 'warning' | 'info'; message: string } | null>(null);
  const [highlightedFields, setHighlightedFields] = useState<Set<string>>(new Set());

  const completed = DOCUMENTS.filter(section => section.required && selections[section.type]?.file).length;
  const optionalSelected = DOCUMENTS.filter(section => !section.required && selections[section.type]?.file).length;
  const now = new Date();
  const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;

  const previewsRef = useRef(filePreviews);
  previewsRef.current = filePreviews;
  const rawUrlsRef = useRef(rawFileUrls);
  rawUrlsRef.current = rawFileUrls;

  useEffect(() => () => {
    Object.values(previewsRef.current).forEach(url => {
      if (url) URL.revokeObjectURL(url);
    });
    Object.values(rawUrlsRef.current).forEach(url => {
      if (url) URL.revokeObjectURL(url);
    });
  }, []);

  function updateSelection(type: AdmissionDocumentType, update: Partial<Selection>) {
    setSelections(current => ({ ...current, [type]: { recordDate: '', note: '', ...current[type], ...update } }));
  }

  async function triggerOcrScan(file: File, type: AdmissionDocumentType) {
    if (type !== 'REGISTRATION_DOCUMENT' && type !== 'PEDIGREE_CERTIFICATE') return;
    setScanningDocType(type);
    const docName = type === 'REGISTRATION_DOCUMENT' ? 'Giấy đăng ký' : 'Giấy phả hệ';
    setOcrNotice({
      tone: 'info',
      message: `AI đang quét và bóc tách dữ liệu từ ${docName}... Các ô tải tệp tạm thời bị khóa để tránh xung đột dữ liệu.`,
    });

    try {
      const res = await ownerAdmissionApi.scanOcr(file, type);
      const data: OcrExtractionData = res.data;
      if (!data) {
        setOcrNotice({
          tone: 'warning',
          message: 'Không thể trích xuất tự động từ tài liệu này. Vui lòng rà soát và điền tay.',
        });
        return;
      }

      let countUpdated = 0;
      setForm(currentForm => {
        const nextForm = { ...currentForm };
        const updatedKeys: string[] = [];

        // Precedence: REGISTRATION_DOCUMENT là chân lý tối cao cho định danh ngựa
        const isPedigree = type === 'PEDIGREE_CERTIFICATE';

        // 1. Name
        if (data.name && (!isPedigree || !nextForm.name?.trim())) {
          if (!nextForm.name?.trim()) {
            nextForm.name = data.name;
            updatedKeys.push('name');
          }
        }
        // 2. Sex (Sex Safeguard: chỉ set nếu OCR chắc chắn và form chưa có)
        if (data.sex && !nextForm.sex) {
          nextForm.sex = data.sex;
          updatedKeys.push('sex');
        }
        // 3. Breed
        if (data.breed && (!isPedigree || !nextForm.breed?.trim())) {
          if (!nextForm.breed?.trim()) {
            nextForm.breed = data.breed;
            updatedKeys.push('breed');
          }
        }
        // 4. Date of Birth
        if (data.dateOfBirth && (!isPedigree || !nextForm.dateOfBirth?.trim())) {
          if (!nextForm.dateOfBirth?.trim()) {
            nextForm.dateOfBirth = data.dateOfBirth;
            updatedKeys.push('dateOfBirth');
          }
        }
        // 5. UELN / Registration Number
        if (data.registrationNumber && (!isPedigree || !nextForm.registrationNumber?.trim())) {
          if (!nextForm.registrationNumber?.trim()) {
            nextForm.registrationNumber = data.registrationNumber;
            updatedKeys.push('registrationNumber');
            checkUeln(data.registrationNumber);
          }
        }
        // 6. Registry Name
        if (data.registryName && !nextForm.registryName?.trim()) {
          nextForm.registryName = data.registryName;
          updatedKeys.push('registryName');
        }
        // 7. Sire Name & UELN
        if (data.sireName && !nextForm.sireName?.trim()) {
          nextForm.sireName = data.sireName;
          updatedKeys.push('sireName');
        }
        if (data.sireRegistrationNumber && !nextForm.sireRegistrationNumber?.trim()) {
          nextForm.sireRegistrationNumber = data.sireRegistrationNumber;
          updatedKeys.push('sireRegistrationNumber');
        }
        // 8. Dam Name & UELN
        if (data.damName && !nextForm.damName?.trim()) {
          nextForm.damName = data.damName;
          updatedKeys.push('damName');
        }
        if (data.damRegistrationNumber && !nextForm.damRegistrationNumber?.trim()) {
          nextForm.damRegistrationNumber = data.damRegistrationNumber;
          updatedKeys.push('damRegistrationNumber');
        }

        countUpdated = updatedKeys.length;
        if (updatedKeys.length > 0) {
          setHighlightedFields(new Set(updatedKeys));
          setTimeout(() => setHighlightedFields(new Set()), 5000);
        }
        return nextForm;
      });

      if (countUpdated > 0) {
        setOcrNotice({
          tone: 'success',
          message: `AI đã tự động trích xuất và điền ${countUpdated} trường thông tin từ ${docName}. Vui lòng rà soát lại trước khi nộp đơn.`,
        });
      } else {
        setOcrNotice({
          tone: 'info',
          message: `Quét ${docName} hoàn tất. Các trường thông tin đã có sẵn được bảo vệ và giữ nguyên.`,
        });
      }
    } catch {
      setOcrNotice({
        tone: 'warning',
        message: 'Dịch vụ AI OCR tạm thời gián đoạn. Tệp đã được lưu, bạn có thể tự kiểm tra và nhập thông tin.',
      });
    } finally {
      setScanningDocType(null);
    }
  }

  function selectFile(type: AdmissionDocumentType, file?: File) {
    if (isAnyScanning) {
      setOcrNotice({
        tone: 'warning',
        message: 'AI đang bận quét tài liệu khác. Vui lòng chờ quá trình quét hoàn tất trước khi tải lên tệp tiếp theo.',
      });
      return;
    }

    const oldThumbUrl = filePreviews[type];
    if (oldThumbUrl) {
      URL.revokeObjectURL(oldThumbUrl);
      setFilePreviews(curr => {
        const next = { ...curr };
        delete next[type];
        return next;
      });
    }
    const oldRawUrl = rawFileUrls[type];
    if (oldRawUrl && oldRawUrl !== oldThumbUrl) {
      URL.revokeObjectURL(oldRawUrl);
      setRawFileUrls(curr => {
        const next = { ...curr };
        delete next[type];
        return next;
      });
    }

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
    if (message && input) {
      input.value = '';
    } else if (file) {
      if (file.type.startsWith('image/')) {
        const previewUrl = URL.createObjectURL(file);
        setFilePreviews(curr => ({ ...curr, [type]: previewUrl }));
        setRawFileUrls(curr => ({ ...curr, [type]: previewUrl }));
      } else if (file.type === 'application/pdf') {
        const rawUrl = URL.createObjectURL(file);
        setRawFileUrls(curr => ({ ...curr, [type]: rawUrl }));
        renderPdfFirstPage(file, 240)
          .then(png => {
            const thumbUrl = URL.createObjectURL(png);
            setFilePreviews(curr => ({ ...curr, [type]: thumbUrl }));
          })
          .catch(() => undefined);
      }

      // Automatically trigger smart OCR scan for registration and pedigree certificates
      if (type === 'REGISTRATION_DOCUMENT' || type === 'PEDIGREE_CERTIFICATE') {
        triggerOcrScan(file, type);
      }
    }
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
    if (key === 'registrationNumber') checkUeln(value);
    setForm(current => ({ ...current, [key]: value }));
    setFieldErrors(current => ({ ...current, [key]: '' }));
    setError('');
  }

  function fieldError(key: keyof CreateOwnerAdmissionRequest) {
    return fieldErrors[key] || (key === 'registrationNumber' && uelnCheck.status === 'unavailable'
      ? uelnCheck.message : '');
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');

    const nextFieldErrors: FieldErrors = {};
    if (!form.name.trim()) nextFieldErrors.name = 'Enter the horse name.';
    if (!form.sex) nextFieldErrors.sex = 'Select Male or Female.';
    FIELDS.forEach(field => {
      const value = (form[field.key] || '').trim();
      const err = validateField(field.key, value, fieldInputs.current[field.key]);
      if (err) nextFieldErrors[field.key] = err;
    });

    const pedErrors = validatePedigreeRelationships(form);
    Object.assign(nextFieldErrors, pedErrors);

    setFieldErrors(nextFieldErrors);
    const firstInvalidField = FORM_FIELDS.find(field => nextFieldErrors[field.key]);
    if (firstInvalidField) {
      fieldInputs.current[firstInvalidField.key]?.focus();
      return;
    }

    const nextDocErrors: DocumentErrors = {};
    DOCUMENTS.forEach(section => {
      const selection = selections[section.type];
      if (section.required && !selection?.file) {
        nextDocErrors[section.type] = `${section.label} is required.`;
      }
    });

    setDocumentErrors(nextDocErrors);
    const firstInvalidDoc = DOCUMENTS.find(section => nextDocErrors[section.type]);
    if (firstInvalidDoc) {
      focusDocument(firstInvalidDoc.type);
      return;
    }

    const payloadCandidate: CreateOwnerAdmissionRequest = {
      name: form.name.trim(),
      sex: form.sex as HorseSex,
      breed: form.breed?.trim() || undefined,
      dateOfBirth: form.dateOfBirth?.trim() || undefined,
      registrationNumber: normalizeUeln(form.registrationNumber) || undefined,
      registryName: form.registryName?.trim() || undefined,
      sireName: form.sireName?.trim() || undefined,
      sireRegistrationNumber: normalizeUeln(form.sireRegistrationNumber) || undefined,
      damName: form.damName?.trim() || undefined,
      damRegistrationNumber: normalizeUeln(form.damRegistrationNumber) || undefined,
      pedigreeNotes: form.pedigreeNotes?.trim() || undefined,
    };

    const documentsToUpload: AdmissionDocumentUpload[] = DOCUMENTS
      .filter(section => selections[section.type]?.file)
      .map(section => ({
        documentType: section.type,
        file: selections[section.type]!.file!,
        recordDate: selections[section.type]?.recordDate || undefined,
        note: selections[section.type]?.note?.trim() || undefined,
      }));

    setBusy(true);
    submitting.current = true;
    try {
      const result = await ownerAdmissionApi.submit(payloadCandidate, documentsToUpload);
      router.push(`/owner/admissions/${result.admissionId}?created=true`);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message || 'Unable to submit admission application.');
      } else {
        setError('Unable to submit admission application.');
      }
    } finally {
      setBusy(false);
      submitting.current = false;
    }
  }

  const renderDocument = (section: (typeof DOCUMENTS)[number]) => {
    const selection = selections[section.type];
    const message = documentErrors[section.type];
    const previewUrl = filePreviews[section.type];
    const isPdf = selection?.file?.type === 'application/pdf';
    const isThisScanning = scanningDocType === section.type;
    const isOcrEligible = section.type === 'REGISTRATION_DOCUMENT' || section.type === 'PEDIGREE_CERTIFICATE';

    const handleOpenPreview = () => {
      if (!selection?.file) return;
      if (isPdf) {
        const url = rawFileUrls[section.type] || URL.createObjectURL(selection.file);
        setActivePdfPreview({ file: selection.file, url });
      } else if (previewUrl) {
        setLightboxImage({ src: previewUrl, alt: selection.file.name });
      }
    };

    return (
      <fieldset key={section.type} className="min-w-0 space-y-3 rounded-[var(--radius-md)] border border-[var(--color-border)] p-4">
        <div className="flex items-center justify-between">
          <legend className="px-1 text-sm font-semibold">
            {section.label}{' '}
            {section.required ? <span aria-hidden="true" className="text-[var(--color-danger)]">*</span> : <span className="font-normal text-[var(--color-text-muted)]">(Optional)</span>}
          </legend>
          {isOcrEligible && (
            <span className="inline-flex items-center gap-1 rounded bg-[var(--color-primary-soft)] px-2 py-0.5 text-xs font-semibold text-[var(--color-primary)]">
              <Icon name="sparkles" size={12} />
              AI OCR
            </span>
          )}
        </div>
        <div className="max-w-xs space-y-1">
          <label className="block space-y-1 text-sm">
            <span>Select file</span>
            <FileInput id={`file-${section.type}`}
              className={`block h-10 w-full min-w-0 rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] p-1.5 text-sm file:mr-3 file:h-6 file:cursor-pointer file:rounded file:border-0 file:bg-[var(--color-surface-muted)] file:px-2 file:text-xs file:font-semibold file:text-[var(--color-text-primary)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)] aria-invalid:border-[var(--color-danger)] aria-invalid:focus-visible:ring-[var(--color-danger)] ${isAnyScanning ? 'opacity-60 cursor-not-allowed' : ''} ${selection?.file ? 'text-[var(--color-text-primary)]' : 'text-[var(--color-text-muted)]'}`}
              ref={element => { fileInputs.current[section.type] = element; }}
              accept={section.type === 'HORSE_PHOTO' ? '.jpg,.jpeg,.png,.webp' : '.pdf,.jpg,.jpeg,.png,.webp'}
              disabled={isAnyScanning}
              aria-required={section.required} aria-invalid={Boolean(message)}
              aria-describedby={`file-hint-${section.type}${message ? ` error-${section.type}` : ''}`}
              onChange={event => selectFile(section.type, event.target.files?.[0])} />
          </label>
          <p id={`file-hint-${section.type}`} className="text-xs leading-5 text-[var(--color-text-muted)]">
            {section.type === 'HORSE_PHOTO' ? 'JPEG, PNG or WebP' : 'PDF, JPEG, PNG or WebP'} · Up to 10 MB
          </p>
          {message && <p id={`error-${section.type}`} role="alert" className="text-sm text-[var(--color-danger)]">{message}</p>}
        </div>
        {isThisScanning && (
          <div className="flex items-center gap-2 rounded bg-[var(--color-primary-soft)] p-2.5 text-xs font-medium text-[var(--color-primary)]">
            <span className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-[var(--color-primary)] border-t-transparent" />
            <span>AI đang quét và trích xuất dữ liệu từ tài liệu này...</span>
          </div>
        )}
        {selection?.file ? (
          <div className="flex min-h-12 flex-wrap items-center justify-between gap-2 text-sm">
            <span className="flex min-w-0 items-center gap-3">
              {previewUrl ? (
                <ImageThumbnail
                  src={previewUrl}
                  alt={selection.file.name}
                  onClick={handleOpenPreview}
                  className="h-14 w-14 shrink-0"
                />
              ) : (
                <button
                  type="button"
                  onClick={handleOpenPreview}
                  className="flex h-14 w-14 shrink-0 cursor-pointer items-center justify-center rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] text-xs font-semibold text-[var(--color-text-muted)] hover:border-[var(--color-primary)]"
                >
                  {isPdf ? 'PDF' : 'FILE'}
                </button>
              )}
              <div className="min-w-0">
                <span className="block truncate font-medium text-[var(--color-text-primary)]">{selection.file.name}</span>
                <button
                  type="button"
                  onClick={handleOpenPreview}
                  className="text-xs text-[var(--color-primary)] hover:underline focus-visible:outline-none"
                >
                  {isPdf ? 'Preview full PDF' : 'Preview image'}
                </button>
              </div>
            </span>
            <div className="flex items-center gap-2">
              {isOcrEligible && !isThisScanning && (
                <Button
                  type="button"
                  size="sm"
                  variant="secondary"
                  disabled={isAnyScanning}
                  onClick={() => triggerOcrScan(selection.file!, section.type)}
                >
                  Re-scan
                </Button>
              )}
              <Button type="button" size="sm" disabled={isAnyScanning} aria-label={`Remove ${section.label.toLowerCase()}`} onClick={() => {
                selectFile(section.type);
                const input = fileInputs.current[section.type];
                if (input) input.value = '';
              }}>Remove</Button>
            </div>
          </div>
        ) : message && !section.required ? (
          <Button type="button" size="sm" variant="tertiary" disabled={isAnyScanning} onClick={() => selectFile(section.type)}>Clear file error</Button>
        ) : null}
        <details ref={element => { metadataDetails.current[section.type] = element; }} className="text-sm">
          <summary className="cursor-pointer rounded text-[var(--color-text-secondary)] focus-visible:outline-2 focus-visible:outline-[var(--color-focus)]">
            {selection?.recordDate || selection?.note ? 'Edit date or note' : 'Add date or note (optional)'}
          </summary>
          <div className="mt-3 space-y-3">
            <label className="block space-y-1">
              <span>Record date (optional)</span>
              <Input id={`record-${section.type}`} type="date" value={selection?.recordDate || ''}
                ref={element => { recordInputs.current[section.type] = element; }}
                disabled={isAnyScanning}
                aria-invalid={Boolean(recordErrors[section.type])} aria-describedby={recordErrors[section.type] ? `record-error-${section.type}` : undefined}
                onBlur={event => setRecordErrors(current => ({ ...current, [section.type]: event.target.validity.badInput ? 'Enter a valid record date.' : '' }))}
                onChange={event => { updateSelection(section.type, { recordDate: event.target.value }); setRecordErrors(current => ({ ...current, [section.type]: '' })); setError(''); }} />
              {recordErrors[section.type] && <span id={`record-error-${section.type}`} role="alert" className="block text-sm text-[var(--color-danger)]">{recordErrors[section.type]}</span>}
            </label>
            <FormField label="Note (optional)">
              <Textarea rows={2} maxLength={2000} value={selection?.note || ''}
                disabled={isAnyScanning}
                onChange={event => updateSelection(section.type, { note: event.target.value })} />
            </FormField>
          </div>
        </details>
      </fieldset>
    );
  };

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-6">
      <fieldset disabled={busy} className="space-y-6">
        {error && <Notice tone="error">{error}</Notice>}
        {ocrNotice && (
          <Notice tone={ocrNotice.tone} onDismiss={() => setOcrNotice(null)}>
            {ocrNotice.message}
          </Notice>
        )}
        <AdmissionInfoSection title="Candidate horse identity">
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            {FORM_FIELDS.map(field => {
              const isHighlighted = highlightedFields.has(field.key);
              return field.key === 'sex' ? (
                <FormField
                  key="sex"
                  htmlFor="candidate-sex"
                  label="Sex"
                  required
                  error={fieldErrors.sex}
                  className={isHighlighted ? 'ring-2 ring-[var(--color-primary)] rounded p-1 transition-all duration-500' : ''}
                >
                  <Select value={form.sex} ref={element => { fieldInputs.current.sex = element; }}
                    onChange={event => changeField('sex', event.target.value)}
                    onBlur={event => setFieldErrors(current => ({ ...current, sex: validateField('sex', event.target.value) }))}>
                    <option value="">Select sex</option>
                    {HORSE_SEX_OPTIONS.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
                  </Select>
                </FormField>
              ) : (
                <label
                  key={field.key}
                  className={`block space-y-1 ${isHighlighted ? 'ring-2 ring-[var(--color-primary)] rounded p-1 transition-all duration-500' : ''}`}
                >
                  <div className="flex items-center justify-between">
                    <span className="block text-sm font-medium">{field.label}{field.required && <span aria-hidden="true" className="text-[var(--color-danger)]"> *</span>}</span>
                    {isHighlighted && (
                      <span className="text-[10px] font-semibold text-[var(--color-primary)] uppercase">
                        AI Filled
                      </span>
                    )}
                  </div>
                  <Input id={`candidate-${field.key}`} type={field.type || 'text'} value={form[field.key] || ''}
                    ref={element => { fieldInputs.current[field.key] = element; }}
                    required={field.required}
                    maxLength={field.key.toLowerCase().includes('registration') ? 15 : 255}
                    pattern={field.key.toLowerCase().includes('registration') ? '([0-9]{6}[a-zA-Z0-9]{9}|[a-zA-Z]{3}[a-zA-Z0-9]{12})' : undefined}
                    max={field.type === 'date' ? today : undefined}
                    aria-invalid={Boolean(fieldError(field.key))}
                    aria-describedby={[field.hint ? `hint-${field.key}` : '', fieldError(field.key) ? `error-${field.key}` : '', field.key === 'registrationNumber' ? 'ueln-check-status' : ''].filter(Boolean).join(' ') || undefined}
                    onBlur={event => {
                      let val = event.target.value;
                      if (field.key.toLowerCase().includes('registration')) {
                        val = val.trim().toUpperCase();
                        setForm(curr => ({ ...curr, [field.key]: val }));
                      }
                      const fieldErr = validateField(field.key, val, event.target);
                      const updatedForm = { ...form, [field.key]: val };
                      const pedErrors = validatePedigreeRelationships(updatedForm);
                      setFieldErrors(current => ({
                        ...current,
                        [field.key]: fieldErr || pedErrors[field.key] || '',
                        ...(pedErrors.sireRegistrationNumber ? { sireRegistrationNumber: pedErrors.sireRegistrationNumber } : {}),
                        ...(pedErrors.damRegistrationNumber ? { damRegistrationNumber: pedErrors.damRegistrationNumber } : {}),
                      }));
                    }}
                    onChange={event => changeField(field.key, event.target.value)} />
                  {field.hint && <span id={`hint-${field.key}`} className="block text-xs leading-5 text-[var(--color-text-muted)]">{field.hint}</span>}
                  {fieldError(field.key) && <span id={`error-${field.key}`} role="alert" className="block text-sm text-[var(--color-danger)]">{fieldError(field.key)}</span>}
                  {field.key === 'registrationNumber' && (
                    <span id="ueln-check-status" role="status" className="block text-xs text-[var(--color-text-muted)]">
                      {uelnCheck.status === 'checking' ? 'Checking UELN…' : uelnCheck.status === 'error' ? uelnCheck.message : ''}
                    </span>
                  )}
                </label>
              );
            })}
          </div>
          <label className="block space-y-1">
            <span className="text-sm font-medium">Pedigree notes</span>
            <Textarea id="candidate-pedigreeNotes" rows={3} maxLength={2000} value={form.pedigreeNotes || ''}
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
          {isAnyScanning && (
            <div className="flex items-center gap-3 rounded-[var(--radius-md)] border border-[var(--color-primary)] bg-[var(--color-primary-soft)] p-3 text-sm text-[var(--color-primary)]">
              <span className="h-4 w-4 shrink-0 animate-spin rounded-full border-2 border-[var(--color-primary)] border-t-transparent" />
              <span>
                Hệ thống AI đang trích xuất dữ liệu từ <strong>{scanningDocType === 'REGISTRATION_DOCUMENT' ? 'Giấy đăng ký' : 'Giấy phả hệ'}</strong>. Các ô tải tài liệu tạm thời bị khóa để đảm bảo an toàn dữ liệu và tránh xung đột.
              </span>
            </div>
          )}
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
          <Button type="button" disabled={isAnyScanning} onClick={() => router.push('/owner/admissions')}>Cancel</Button>
          <Button variant="primary" type="submit" loading={busy}
            disabled={isAnyScanning || uelnCheck.status === 'checking' || uelnCheck.status === 'unavailable'}>Submit admission</Button>
        </div>
        {activePdfPreview && (
          <LocalFilePreviewModal
            file={activePdfPreview.file}
            previewUrl={activePdfPreview.url}
            onClose={() => setActivePdfPreview(null)}
          />
        )}
        {lightboxImage && (
          <ImageLightbox
            images={[lightboxImage]}
            index={0}
            onIndexChange={() => undefined}
            onClose={() => setLightboxImage(null)}
          />
        )}
      </fieldset>
    </form>
  );
}
