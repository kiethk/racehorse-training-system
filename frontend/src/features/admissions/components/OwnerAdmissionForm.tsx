'use client';

import { FileInput, Input, Select, Textarea } from '@/components/ui/Input';
import { FormField } from '@/components/ui/FormField';
import { useEffect, useRef, useState, type FormEvent, type DragEvent } from 'react';
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

const OCR_DOCUMENTS: { type: AdmissionDocumentType; label: string; description: string; required: boolean }[] = [
  {
    type: 'REGISTRATION_DOCUMENT',
    label: 'Giấy đăng ký / Hộ chiếu ngựa (Registration document)',
    description: 'Trích xuất tự động: Tên ngựa, UELN (15 ký tự), Giống, Ngày sinh, Giới tính, Sổ đăng bạ.',
    required: true,
  },
  {
    type: 'PEDIGREE_CERTIFICATE',
    label: 'Giấy phả hệ (Pedigree certificate)',
    description: 'Trích xuất tự động: Tên ngựa cha, UELN cha, Tên ngựa mẹ, UELN mẹ, Ghi chú phả hệ.',
    required: true,
  },
];

const REMAINING_REQUIRED_DOCUMENTS: { type: AdmissionDocumentType; label: string; required: boolean }[] = [
  { type: 'HORSE_PHOTO', label: 'Ảnh chụp ngựa (Horse photo)', required: true },
  { type: 'VACCINATION_RECORD', label: 'Sổ tiêm chủng vắc xin (Vaccination record)', required: true },
];

const OPTIONAL_DOCUMENTS: { type: AdmissionDocumentType; label: string; required: boolean }[] = [
  { type: 'DEWORMING_RECORD', label: 'Hồ sơ tẩy giun (Deworming record)', required: false },
  { type: 'HEALTH_CERTIFICATE', label: 'Chứng nhận sức khỏe (Health certificate)', required: false },
  { type: 'PREVIOUS_MEDICAL_RECORD', label: 'Bệnh án điều trị trước đây (Previous medical record)', required: false },
  { type: 'PREVIOUS_INJURY_RECORD', label: 'Hồ sơ chấn thương cũ (Previous injury record)', required: false },
];

const DOCUMENTS = [
  ...OCR_DOCUMENTS.map(d => ({ type: d.type, label: d.label, required: d.required })),
  ...REMAINING_REQUIRED_DOCUMENTS,
  ...OPTIONAL_DOCUMENTS,
];

const FIELDS: { key: Exclude<keyof CreateOwnerAdmissionRequest, 'sex'>; label: string; required?: boolean; type?: string; hint?: string }[] = [
  { key: 'name', label: 'Tên ngựa (Horse name)', required: true },
  { key: 'breed', label: 'Giống ngựa (Breed)' },
  { key: 'dateOfBirth', label: 'Ngày sinh (Date of birth)', type: 'date' },
  { key: 'registrationNumber', label: 'Mã số UELN / Số đăng bạ', hint: 'Gồm 15 ký tự (VD: 704001SC0000001 hoặc VNM22SC00000001).' },
  { key: 'registryName', label: 'Sổ đăng bạ (Registry name)' },
  { key: 'sireName', label: 'Tên ngựa cha (Sire name)' },
  { key: 'sireRegistrationNumber', label: 'UELN ngựa cha', hint: '15 ký tự theo chuẩn quốc tế, nếu có.' },
  { key: 'damName', label: 'Tên ngựa mẹ (Dam name)' },
  { key: 'damRegistrationNumber', label: 'UELN ngựa mẹ', hint: '15 ký tự theo chuẩn quốc tế, nếu có.' },
];
const SEX_FIELD = { key: 'sex', label: 'Giới tính (Sex)', required: true } as const;
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
  if (key === 'name' && !value) return 'Vui lòng nhập tên ngựa.';
  if (key === 'sex' && !isHorseSex(value)) return 'Vui lòng chọn giới tính (Male hoặc Female).';
  if (key.toLowerCase().includes('registration') && value) {
    const normalized = value.toUpperCase();
    if (!/^[0-9]{6}[A-Z0-9]{9}$/.test(normalized) && !/^[A-Z]{3}[A-Z0-9]{12}$/.test(normalized)) {
      return 'Mã UELN phải gồm 15 ký tự hợp lệ (VD: 704001SC0000001 hoặc VNM22SC00000001), hoặc để trống.';
    }
  }
  if (key === 'dateOfBirth' && input?.validity.badInput) return 'Vui lòng nhập ngày sinh hợp lệ.';
  if (key === 'dateOfBirth' && input?.validity.rangeOverflow) return 'Ngày sinh phải là hôm nay hoặc trong quá khứ.';
  const limit = key === 'pedigreeNotes' ? 2000 : 255;
  return value.length > limit ? `Vui lòng nhập tối đa ${limit} ký tự.` : '';
}

function validatePedigreeRelationships(formData: Omit<CreateOwnerAdmissionRequest, 'sex'>): FieldErrors {
  const errors: FieldErrors = {};
  const childUeln = normalizeUeln(formData.registrationNumber);
  const sireUeln = normalizeUeln(formData.sireRegistrationNumber);
  const damUeln = normalizeUeln(formData.damRegistrationNumber);

  if (childUeln && sireUeln && childUeln === sireUeln) {
    errors.sireRegistrationNumber = 'Mã UELN của ngựa cha không được trùng với mã của ngựa con.';
  }
  if (childUeln && damUeln && childUeln === damUeln) {
    errors.damRegistrationNumber = 'Mã UELN của ngựa mẹ không được trùng với mã của ngựa con.';
  }
  if (sireUeln && damUeln && sireUeln === damUeln) {
    errors.damRegistrationNumber = 'Mã UELN của ngựa mẹ không được trùng với mã của ngựa cha.';
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
  const [dragActiveDocType, setDragActiveDocType] = useState<AdmissionDocumentType | null>(null);

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
      message: `AI đang quét và bóc tách dữ liệu từ ${docName}... Các ô tải tệp tạm thời được khóa để tránh xung đột dữ liệu.`,
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

      const isPedigree = type === 'PEDIGREE_CERTIFICATE';
      const updatedKeys: string[] = [];

      setForm(currentForm => {
        const nextForm = { ...currentForm };

        // Precedence: REGISTRATION_DOCUMENT là chân lý tối cao cho định danh ngựa; PEDIGREE chỉ điền nếu chưa có
        // 1. Name
        if (data.name) {
          if (!isPedigree || !nextForm.name?.trim()) {
            if (nextForm.name !== data.name) {
              nextForm.name = data.name;
              updatedKeys.push('name');
            }
          }
        }
        // 2. Sex (Sex Safeguard: chỉ set nếu OCR chắc chắn)
        if (data.sex) {
          if (!isPedigree || !nextForm.sex) {
            if (nextForm.sex !== data.sex) {
              nextForm.sex = data.sex;
              updatedKeys.push('sex');
            }
          }
        }
        // 3. Breed
        if (data.breed) {
          if (!isPedigree || !nextForm.breed?.trim()) {
            if (nextForm.breed !== data.breed) {
              nextForm.breed = data.breed;
              updatedKeys.push('breed');
            }
          }
        }
        // 4. Date of Birth
        if (data.dateOfBirth) {
          if (!isPedigree || !nextForm.dateOfBirth?.trim()) {
            if (nextForm.dateOfBirth !== data.dateOfBirth) {
              nextForm.dateOfBirth = data.dateOfBirth;
              updatedKeys.push('dateOfBirth');
            }
          }
        }
        // 5. UELN / Registration Number
        if (data.registrationNumber) {
          if (!isPedigree || !nextForm.registrationNumber?.trim()) {
            if (nextForm.registrationNumber !== data.registrationNumber) {
              nextForm.registrationNumber = data.registrationNumber;
              updatedKeys.push('registrationNumber');
              checkUeln(data.registrationNumber);
            }
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

        return nextForm;
      });

      // Clear any previous validation errors for auto-filled fields
      setFieldErrors(current => {
        const next = { ...current };
        updatedKeys.forEach(key => {
          delete next[key as keyof CreateOwnerAdmissionRequest];
        });
        return next;
      });

      if (updatedKeys.length > 0) {
        setHighlightedFields(new Set(updatedKeys));
        setTimeout(() => setHighlightedFields(new Set()), 6000);
        setOcrNotice({
          tone: 'success',
          message: `AI đã tự động trích xuất và điền ${updatedKeys.length} trường thông tin từ ${docName}. Vui lòng rà soát lại ở Bước 2 trước khi nộp.`,
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
        message: 'AI đang quét tài liệu khác. Vui lòng chờ quá trình quét hoàn tất trước khi tải lên tệp tiếp theo.',
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
      message = 'Dung lượng tệp phải từ 1 byte đến tối đa 10 MB.';
    } else if (file && !allowed.includes(file.type)) {
      message = imageOnly ? 'Chỉ chấp nhận ảnh JPEG, PNG hoặc WebP.' : 'Chỉ chấp nhận tệp PDF, JPEG, PNG hoặc WebP.';
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
    } else {
      const element = fileInputs.current[type];
      element?.focus();
      element?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
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

  function scrollToManualForm() {
    const firstField = fieldInputs.current.name;
    if (firstField) {
      firstField.focus();
      firstField.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting.current || isAnyScanning || uelnCheck.status === 'checking') return;
    setError('');

    const nextFieldErrors: FieldErrors = {};
    if (!form.name.trim()) nextFieldErrors.name = 'Vui lòng nhập tên ngựa.';
    if (!form.sex) nextFieldErrors.sex = 'Vui lòng chọn giới tính (Male hoặc Female).';
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
      fieldInputs.current[firstInvalidField.key]?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      return;
    }

    const nextDocErrors: DocumentErrors = {};
    DOCUMENTS.forEach(section => {
      const selection = selections[section.type];
      if (section.required && !selection?.file) {
        nextDocErrors[section.type] = `${section.label} là bắt buộc.`;
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
        setError(err.message || 'Không thể gửi hồ sơ tiếp nhận.');
      } else {
        setError('Không thể gửi hồ sơ tiếp nhận.');
      }
    } finally {
      setBusy(false);
      submitting.current = false;
    }
  }

  const handleOpenPreview = (type: AdmissionDocumentType) => {
    const selection = selections[type];
    if (!selection?.file) return;
    const isPdf = selection.file.type === 'application/pdf';
    const previewUrl = filePreviews[type];

    if (isPdf) {
      const url = rawFileUrls[type] || URL.createObjectURL(selection.file);
      setActivePdfPreview({ file: selection.file, url });
    } else if (previewUrl) {
      setLightboxImage({ src: previewUrl, alt: selection.file.name });
    }
  };

  const handleDragOver = (event: DragEvent, type: AdmissionDocumentType) => {
    event.preventDefault();
    if (!isAnyScanning && dragActiveDocType !== type) {
      setDragActiveDocType(type);
    }
  };

  const handleDragLeave = (event: DragEvent, type: AdmissionDocumentType) => {
    event.preventDefault();
    if (dragActiveDocType === type) {
      setDragActiveDocType(null);
    }
  };

  const handleDrop = (event: DragEvent, type: AdmissionDocumentType) => {
    event.preventDefault();
    setDragActiveDocType(null);
    if (isAnyScanning) return;
    const file = event.dataTransfer.files?.[0];
    if (file) selectFile(type, file);
  };

  // Render specialized Hero OCR Document card for Step 1
  const renderHeroOcrDocument = (doc: (typeof OCR_DOCUMENTS)[number]) => {
    const selection = selections[doc.type];
    const message = documentErrors[doc.type];
    const previewUrl = filePreviews[doc.type];
    const isPdf = selection?.file?.type === 'application/pdf';
    const isThisScanning = scanningDocType === doc.type;
    const isDragging = dragActiveDocType === doc.type;

    return (
      <fieldset
        key={doc.type}
        className={`flex flex-col justify-between rounded-[var(--radius-md)] border bg-[var(--color-surface)] p-4 transition-all duration-200 ${
          isDragging
            ? 'border-[var(--color-primary)] ring-2 ring-[var(--color-focus)] bg-[var(--color-primary-soft)]'
            : message
            ? 'border-[var(--color-danger)]'
            : 'border-[var(--color-border)] hover:border-[var(--color-primary)]'
        }`}
        onDragOver={e => handleDragOver(e, doc.type)}
        onDragLeave={e => handleDragLeave(e, doc.type)}
        onDrop={e => handleDrop(e, doc.type)}
      >
        <div className="space-y-2">
          <div className="flex items-start justify-between gap-2">
            <div>
              <legend className="text-sm font-semibold text-[var(--color-text-primary)]">
                {doc.label} <span aria-hidden="true" className="text-[var(--color-danger)]">*</span>
              </legend>
              <p className="mt-0.5 text-xs text-[var(--color-text-secondary)]">{doc.description}</p>
            </div>
            <span className="inline-flex shrink-0 items-center gap-1 rounded bg-[var(--color-primary-soft)] px-2 py-0.5 text-[11px] font-semibold text-[var(--color-primary)]">
              <Icon name="sparkles" size={12} />
              AI OCR
            </span>
          </div>

          {/* Active Scanning Indicator */}
          {isThisScanning && (
            <div className="flex items-center gap-2.5 rounded-[var(--radius-sm)] bg-[var(--color-primary-soft)] p-3 text-xs font-medium text-[var(--color-primary)]">
              <span className="h-4 w-4 shrink-0 animate-spin rounded-full border-2 border-[var(--color-primary)] border-t-transparent" />
              <span>AI đang quét và trích xuất dữ liệu từ tài liệu...</span>
            </div>
          )}

          {/* File Selected State */}
          {selection?.file ? (
            <div className="space-y-2.5 rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface-subtle)] p-3">
              <div className="flex flex-wrap items-center justify-between gap-2 text-sm">
                <div className="flex min-w-0 items-center gap-3">
                  {previewUrl ? (
                    <ImageThumbnail
                      src={previewUrl}
                      alt={selection.file.name}
                      onClick={() => handleOpenPreview(doc.type)}
                      className="h-14 w-14 shrink-0 cursor-pointer rounded border border-[var(--color-border)]"
                    />
                  ) : (
                    <button
                      type="button"
                      onClick={() => handleOpenPreview(doc.type)}
                      className="flex h-14 w-14 shrink-0 cursor-pointer items-center justify-center rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] text-xs font-semibold text-[var(--color-text-muted)] hover:border-[var(--color-primary)]"
                    >
                      {isPdf ? 'PDF' : 'FILE'}
                    </button>
                  )}
                  <div className="min-w-0">
                    <span className="block truncate font-medium text-[var(--color-text-primary)]" title={selection.file.name}>
                      {selection.file.name}
                    </span>
                    <div className="flex items-center gap-2 pt-0.5 text-xs text-[var(--color-text-muted)]">
                      <span>{(selection.file.size / (1024 * 1024)).toFixed(2)} MB</span>
                      <span>·</span>
                      <button
                        type="button"
                        onClick={() => handleOpenPreview(doc.type)}
                        className="text-[var(--color-primary)] hover:underline focus-visible:outline-none"
                      >
                        {isPdf ? 'Xem trước PDF' : 'Xem trước ảnh'}
                      </button>
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  {!isThisScanning && (
                    <Button
                      type="button"
                      size="sm"
                      variant="secondary"
                      disabled={isAnyScanning}
                      onClick={() => triggerOcrScan(selection.file!, doc.type)}
                      title="Quét lại tài liệu này để cập nhật form"
                    >
                      <Icon name="sparkles" size={13} className="mr-1" />
                      Quét lại
                    </Button>
                  )}
                  <Button
                    type="button"
                    size="sm"
                    variant="tertiary"
                    disabled={isAnyScanning}
                    aria-label={`Xóa ${doc.label}`}
                    onClick={() => {
                      selectFile(doc.type);
                      const input = fileInputs.current[doc.type];
                      if (input) input.value = '';
                    }}
                  >
                    Xóa
                  </Button>
                </div>
              </div>

              {!isThisScanning && (
                <div className="flex items-center gap-1.5 text-[11px] font-medium text-emerald-700">
                  <Icon name="check" size={13} className="text-emerald-600" />
                  <span>Đã trích xuất và bảo vệ dữ liệu</span>
                </div>
              )}
            </div>
          ) : (
            /* Empty Dropzone State */
            <div className="space-y-2">
              <label
                htmlFor={`file-${doc.type}`}
                className={`flex cursor-pointer flex-col items-center justify-center rounded-[var(--radius-sm)] border-2 border-dashed p-4 text-center transition-colors ${
                  isDragging
                    ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)]'
                    : 'border-[var(--color-border-strong)] bg-[var(--color-surface-subtle)] hover:border-[var(--color-primary)] hover:bg-[var(--color-surface-muted)]'
                } ${isAnyScanning ? 'cursor-not-allowed opacity-60' : ''}`}
              >
                <Icon name="upload" size={26} className="mb-1 text-[var(--color-primary)]" />
                <span className="text-xs font-semibold text-[var(--color-text-primary)]">
                  Kéo thả tệp vào đây, hoặc <span className="text-[var(--color-primary)] underline">chọn tệp từ máy</span>
                </span>
                <span className="mt-0.5 text-[11px] text-[var(--color-text-muted)]">
                  PDF, JPEG, PNG, WebP · Dung lượng tối đa 10 MB
                </span>
              </label>
              <FileInput
                id={`file-${doc.type}`}
                className="sr-only"
                ref={element => { fileInputs.current[doc.type] = element; }}
                accept=".pdf,.jpg,.jpeg,.png,.webp"
                disabled={isAnyScanning}
                aria-required={doc.required}
                aria-invalid={Boolean(message)}
                onChange={event => selectFile(doc.type, event.target.files?.[0])}
              />
              {message && <p role="alert" className="text-xs text-[var(--color-danger)]">{message}</p>}
            </div>
          )}
        </div>

        {/* Date / Note details */}
        <details ref={element => { metadataDetails.current[doc.type] = element; }} className="mt-3 text-xs">
          <summary className="cursor-pointer text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] focus-visible:outline-2 focus-visible:outline-[var(--color-focus)]">
            {selection?.recordDate || selection?.note ? 'Chỉnh sửa ngày cấp hoặc ghi chú' : '+ Thêm ngày cấp hoặc ghi chú (tùy chọn)'}
          </summary>
          <div className="mt-2.5 space-y-2.5 rounded bg-[var(--color-surface-muted)] p-2.5">
            <label className="block space-y-1">
              <span>Ngày cấp (tùy chọn)</span>
              <Input
                id={`record-${doc.type}`}
                type="date"
                value={selection?.recordDate || ''}
                ref={element => { recordInputs.current[doc.type] = element; }}
                disabled={isAnyScanning}
                aria-invalid={Boolean(recordErrors[doc.type])}
                onBlur={event => setRecordErrors(current => ({ ...current, [doc.type]: event.target.validity.badInput ? 'Ngày cấp không hợp lệ.' : '' }))}
                onChange={event => {
                  updateSelection(doc.type, { recordDate: event.target.value });
                  setRecordErrors(current => ({ ...current, [doc.type]: '' }));
                }}
              />
              {recordErrors[doc.type] && <span role="alert" className="block text-xs text-[var(--color-danger)]">{recordErrors[doc.type]}</span>}
            </label>
            <FormField label="Ghi chú (tùy chọn)">
              <Textarea
                rows={2}
                maxLength={2000}
                value={selection?.note || ''}
                disabled={isAnyScanning}
                onChange={event => updateSelection(doc.type, { note: event.target.value })}
              />
            </FormField>
          </div>
        </details>
      </fieldset>
    );
  };

  // Render standard document card for Step 3
  const renderStandardDocument = (section: (typeof DOCUMENTS)[number]) => {
    const selection = selections[section.type];
    const message = documentErrors[section.type];
    const previewUrl = filePreviews[section.type];
    const isPdf = selection?.file?.type === 'application/pdf';

    return (
      <fieldset key={section.type} className="min-w-0 space-y-3 rounded-[var(--radius-md)] border border-[var(--color-border)] p-4">
        <div className="flex items-center justify-between">
          <legend className="px-1 text-sm font-semibold text-[var(--color-text-primary)]">
            {section.label}{' '}
            {section.required ? <span aria-hidden="true" className="text-[var(--color-danger)]">*</span> : <span className="font-normal text-[var(--color-text-muted)]">(Tùy chọn)</span>}
          </legend>
        </div>
        <div className="max-w-xs space-y-1">
          <label className="block space-y-1 text-sm">
            <span>Chọn tệp</span>
            <FileInput
              id={`file-${section.type}`}
              className={`block h-10 w-full min-w-0 rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] p-1.5 text-sm file:mr-3 file:h-6 file:cursor-pointer file:rounded file:border-0 file:bg-[var(--color-surface-muted)] file:px-2 file:text-xs file:font-semibold file:text-[var(--color-text-primary)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)] aria-invalid:border-[var(--color-danger)] ${isAnyScanning ? 'opacity-60 cursor-not-allowed' : ''} ${selection?.file ? 'text-[var(--color-text-primary)]' : 'text-[var(--color-text-muted)]'}`}
              ref={element => { fileInputs.current[section.type] = element; }}
              accept={section.type === 'HORSE_PHOTO' ? '.jpg,.jpeg,.png,.webp' : '.pdf,.jpg,.jpeg,.png,.webp'}
              disabled={isAnyScanning}
              aria-required={section.required}
              aria-invalid={Boolean(message)}
              onChange={event => selectFile(section.type, event.target.files?.[0])}
            />
          </label>
          <p id={`file-hint-${section.type}`} className="text-xs leading-5 text-[var(--color-text-muted)]">
            {section.type === 'HORSE_PHOTO' ? 'Ảnh JPEG, PNG hoặc WebP' : 'Tệp PDF, JPEG, PNG hoặc WebP'} · Tối đa 10 MB
          </p>
          {message && <p id={`error-${section.type}`} role="alert" className="text-sm text-[var(--color-danger)]">{message}</p>}
        </div>
        {selection?.file ? (
          <div className="flex min-h-12 flex-wrap items-center justify-between gap-2 text-sm">
            <span className="flex min-w-0 items-center gap-3">
              {previewUrl ? (
                <ImageThumbnail
                  src={previewUrl}
                  alt={selection.file.name}
                  onClick={() => handleOpenPreview(section.type)}
                  className="h-14 w-14 shrink-0 rounded border border-[var(--color-border)]"
                />
              ) : (
                <button
                  type="button"
                  onClick={() => handleOpenPreview(section.type)}
                  className="flex h-14 w-14 shrink-0 cursor-pointer items-center justify-center rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] text-xs font-semibold text-[var(--color-text-muted)] hover:border-[var(--color-primary)]"
                >
                  {isPdf ? 'PDF' : 'FILE'}
                </button>
              )}
              <div className="min-w-0">
                <span className="block truncate font-medium text-[var(--color-text-primary)]">{selection.file.name}</span>
                <button
                  type="button"
                  onClick={() => handleOpenPreview(section.type)}
                  className="text-xs text-[var(--color-primary)] hover:underline focus-visible:outline-none"
                >
                  {isPdf ? 'Xem trước PDF' : 'Xem trước ảnh'}
                </button>
              </div>
            </span>
            <Button
              type="button"
              size="sm"
              disabled={isAnyScanning}
              aria-label={`Xóa ${section.label.toLowerCase()}`}
              onClick={() => {
                selectFile(section.type);
                const input = fileInputs.current[section.type];
                if (input) input.value = '';
              }}
            >
              Xóa
            </Button>
          </div>
        ) : message && !section.required ? (
          <Button type="button" size="sm" variant="tertiary" disabled={isAnyScanning} onClick={() => selectFile(section.type)}>
            Xóa lỗi tệp
          </Button>
        ) : null}
        <details ref={element => { metadataDetails.current[section.type] = element; }} className="text-sm">
          <summary className="cursor-pointer rounded text-[var(--color-text-secondary)] focus-visible:outline-2 focus-visible:outline-[var(--color-focus)]">
            {selection?.recordDate || selection?.note ? 'Chỉnh sửa ngày hoặc ghi chú' : 'Thêm ngày hoặc ghi chú (tùy chọn)'}
          </summary>
          <div className="mt-3 space-y-3">
            <label className="block space-y-1">
              <span>Ngày cấp (tùy chọn)</span>
              <Input
                id={`record-${section.type}`}
                type="date"
                value={selection?.recordDate || ''}
                ref={element => { recordInputs.current[section.type] = element; }}
                disabled={isAnyScanning}
                aria-invalid={Boolean(recordErrors[section.type])}
                onBlur={event => setRecordErrors(current => ({ ...current, [section.type]: event.target.validity.badInput ? 'Vui lòng nhập ngày hợp lệ.' : '' }))}
                onChange={event => {
                  updateSelection(section.type, { recordDate: event.target.value });
                  setRecordErrors(current => ({ ...current, [section.type]: '' }));
                }}
              />
              {recordErrors[section.type] && <span role="alert" className="block text-sm text-[var(--color-danger)]">{recordErrors[section.type]}</span>}
            </label>
            <FormField label="Ghi chú (tùy chọn)">
              <Textarea
                rows={2}
                maxLength={2000}
                value={selection?.note || ''}
                disabled={isAnyScanning}
                onChange={event => updateSelection(section.type, { note: event.target.value })}
              />
            </FormField>
          </div>
        </details>
      </fieldset>
    );
  };

  const filledCount = FORM_FIELDS.filter(field => {
    const val = form[field.key];
    return typeof val === 'string' && val.trim().length > 0;
  }).length;

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-6">
      <fieldset disabled={busy} className="space-y-6">
        {error && <Notice tone="error">{error}</Notice>}
        {ocrNotice && (
          <Notice tone={ocrNotice.tone} onDismiss={() => setOcrNotice(null)}>
            {ocrNotice.message}
          </Notice>
        )}

        {/* STEP 1: SMART OCR HERO SECTION */}
        <section aria-labelledby="step-1-ocr-title" className="space-y-4 rounded-[var(--radius-lg)] border-2 border-[var(--color-primary)] bg-[var(--color-primary-subtle)] p-5 sm:p-6">
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between border-b border-[var(--color-border)] pb-3">
            <div className="flex items-center gap-2.5">
              <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-[var(--color-primary)] text-white">
                <Icon name="sparkles" size={18} />
              </span>
              <div>
                <h2 id="step-1-ocr-title" className="text-base font-bold text-[var(--color-text-primary)]">
                  Bước 1: Quét nhanh tài liệu bằng AI (Smart OCR)
                </h2>
                <p className="text-xs text-[var(--color-text-secondary)]">
                  Tải lên Giấy đăng ký hoặc Giấy phả hệ để AI tự động trích xuất và điền sẵn thông tin ngựa bên dưới.
                </p>
              </div>
            </div>
            <span className="inline-flex self-start sm:self-auto items-center gap-1.5 rounded-full bg-[var(--color-primary-soft)] px-3 py-1 text-xs font-semibold text-[var(--color-primary)]">
              <Icon name="sparkles" size={13} />
              Tự động điền 10+ trường
            </span>
          </div>

          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            {OCR_DOCUMENTS.map(renderHeroOcrDocument)}
          </div>

          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pt-1 text-xs text-[var(--color-text-secondary)]">
            <div className="flex items-center gap-1.5">
              <span>Chưa có bản scan sẵn?</span>
              <button
                type="button"
                onClick={scrollToManualForm}
                className="font-semibold text-[var(--color-primary)] hover:underline focus:outline-none"
              >
                Bỏ qua quét và tự nhập tay vào biểu mẫu bên dưới &darr;
              </button>
            </div>
            <span className="font-medium text-[var(--color-text-muted)]">
              Độ ưu tiên: Giấy đăng ký là căn cứ chính cho định danh ngựa
            </span>
          </div>
        </section>

        {/* STEP 2: CANDIDATE HORSE IDENTITY */}
        <AdmissionInfoSection
          title="Bước 2: Rà soát & Bổ sung thông tin ngựa (Candidate horse identity)"
        >
          <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-2 text-xs">
            <p className="text-[var(--color-text-secondary)]">
              Kiểm tra lại dữ liệu do AI bóc tách và bổ sung các trường còn thiếu trước khi nộp.
            </p>
            <span className="rounded bg-[var(--color-surface-muted)] px-2.5 py-1 font-semibold text-[var(--color-text-primary)]">
              {filledCount}/10 trường đã hoàn thành
            </span>
          </div>

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            {FORM_FIELDS.map(field => {
              const isHighlighted = highlightedFields.has(field.key);
              return field.key === 'sex' ? (
                <FormField
                  key="sex"
                  htmlFor="candidate-sex"
                  label={field.label}
                  required
                  error={fieldErrors.sex}
                  className={isHighlighted ? 'ring-2 ring-[var(--color-primary)] rounded-[var(--radius-sm)] p-1 transition-all duration-500' : ''}
                >
                  <Select
                    value={form.sex}
                    ref={element => { fieldInputs.current.sex = element; }}
                    onChange={event => changeField('sex', event.target.value)}
                    onBlur={event => setFieldErrors(current => ({ ...current, sex: validateField('sex', event.target.value) }))}
                  >
                    <option value="">Chọn giới tính</option>
                    {HORSE_SEX_OPTIONS.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
                  </Select>
                </FormField>
              ) : (
                <label
                  key={field.key}
                  className={`block space-y-1 ${isHighlighted ? 'ring-2 ring-[var(--color-primary)] rounded-[var(--radius-sm)] p-1 transition-all duration-500' : ''}`}
                >
                  <div className="flex items-center justify-between">
                    <span className="block text-sm font-medium text-[var(--color-text-primary)]">
                      {field.label}{field.required && <span aria-hidden="true" className="text-[var(--color-danger)]"> *</span>}
                    </span>
                    {isHighlighted && (
                      <span className="rounded bg-[var(--color-primary-soft)] px-1.5 py-0.5 text-[10px] font-semibold text-[var(--color-primary)] uppercase">
                        AI Filled
                      </span>
                    )}
                  </div>
                  <Input
                    id={`candidate-${field.key}`}
                    type={field.type || 'text'}
                    value={form[field.key] || ''}
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
                    onChange={event => changeField(field.key, event.target.value)}
                  />
                  {field.hint && <span id={`hint-${field.key}`} className="block text-xs leading-5 text-[var(--color-text-muted)]">{field.hint}</span>}
                  {fieldError(field.key) && <span id={`error-${field.key}`} role="alert" className="block text-sm text-[var(--color-danger)]">{fieldError(field.key)}</span>}
                  {field.key === 'registrationNumber' && (
                    <span id="ueln-check-status" role="status" className="block text-xs text-[var(--color-text-muted)]">
                      {uelnCheck.status === 'checking' ? 'Đang kiểm tra UELN…' : uelnCheck.status === 'error' ? uelnCheck.message : ''}
                    </span>
                  )}
                </label>
              );
            })}
          </div>

          <label className="block space-y-1">
            <span className="text-sm font-medium text-[var(--color-text-primary)]">Ghi chú phả hệ (Pedigree notes)</span>
            <Textarea
              id="candidate-pedigreeNotes"
              rows={3}
              maxLength={2000}
              value={form.pedigreeNotes || ''}
              ref={element => { fieldInputs.current.pedigreeNotes = element; }}
              aria-invalid={Boolean(fieldErrors.pedigreeNotes)}
              aria-describedby={fieldErrors.pedigreeNotes ? 'error-pedigreeNotes' : undefined}
              onBlur={event => setFieldErrors(current => ({ ...current, pedigreeNotes: validateField('pedigreeNotes', event.target.value) }))}
              onChange={event => changeField('pedigreeNotes', event.target.value)}
            />
            {fieldErrors.pedigreeNotes && <span id="error-pedigreeNotes" role="alert" className="block text-sm text-[var(--color-danger)]">{fieldErrors.pedigreeNotes}</span>}
          </label>
        </AdmissionInfoSection>

        {/* STEP 3: REMAINING SUPPORTING DOCUMENTS */}
        <AdmissionInfoSection
          title="Bước 3: Hồ sơ bắt buộc còn lại & Giấy tờ y tế (Supporting documents)"
        >
          <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-2 text-xs">
            <p className="text-[var(--color-text-secondary)]">
              Tải ảnh chụp thể trạng ngựa và sổ tiêm chủng kiểm dịch trước khi nhập chuồng.
            </p>
            <span role="status" className="rounded bg-[var(--color-surface-muted)] px-2.5 py-1 font-semibold text-[var(--color-text-primary)]">
              Hồ sơ bắt buộc đã tải: {completed}/4
            </span>
          </div>

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            {REMAINING_REQUIRED_DOCUMENTS.map(renderStandardDocument)}
          </div>

          <details ref={optionalDocuments} className="rounded-[var(--radius-md)] border border-[var(--color-border)] p-4">
            <summary className="cursor-pointer rounded text-sm font-medium text-[var(--color-text-primary)] focus-visible:outline-2 focus-visible:outline-[var(--color-focus)]">
              Tài liệu y tế bổ sung (Tùy chọn)
              <span className="ml-2 font-normal text-[var(--color-text-muted)]">
                {optionalSelected ? `${optionalSelected} tệp đã chọn` : '4 loại giấy tờ y tế bổ sung'}
              </span>
            </summary>
            <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
              {OPTIONAL_DOCUMENTS.map(renderStandardDocument)}
            </div>
          </details>
        </AdmissionInfoSection>

        {/* ACTIONS */}
        <div className="flex flex-wrap justify-end gap-3 pt-2">
          <Button type="button" disabled={isAnyScanning} onClick={() => router.push('/owner/admissions')}>
            Hủy bỏ
          </Button>
          <Button
            variant="primary"
            type="submit"
            loading={busy}
            disabled={isAnyScanning || uelnCheck.status === 'checking' || uelnCheck.status === 'unavailable'}
          >
            Nộp hồ sơ tiếp nhận
          </Button>
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
