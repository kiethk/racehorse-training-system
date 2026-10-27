import type { HorseDocumentSlot, HorseDocumentType, HorseEditFields } from '../types';

export const HORSE_DOCUMENT_TYPES: { type: HorseDocumentType; label: string }[] = [
  { type: 'HORSE_PHOTO', label: 'Horse photo' },
  { type: 'REGISTRATION_DOCUMENT', label: 'Registration document' },
  { type: 'PEDIGREE_CERTIFICATE', label: 'Pedigree certificate' },
  { type: 'VACCINATION_RECORD', label: 'Vaccination record' },
  { type: 'DEWORMING_RECORD', label: 'Deworming record' },
  { type: 'HEALTH_CERTIFICATE', label: 'Health certificate' },
  { type: 'PREVIOUS_MEDICAL_RECORD', label: 'Previous medical record' },
  { type: 'PREVIOUS_INJURY_RECORD', label: 'Previous injury record' },
];

export function documentTypeLabel(type: HorseDocumentType): string {
  return HORSE_DOCUMENT_TYPES.find((d) => d.type === type)?.label ?? type;
}

export const HORSE_EDIT_FIELD_LABELS: Record<keyof HorseEditFields, string> = {
  name: 'Name',
  breed: 'Breed',
  sireName: 'Sire name',
  sireRegistrationNumber: 'Sire UELN',
  damName: 'Dam name',
  damRegistrationNumber: 'Dam UELN',
};

/** Mirrors the server: PDF/JPEG/PNG/WebP up to 10 MB; a horse photo must be an image. */
export const MAX_DOCUMENT_BYTES = 10 * 1024 * 1024;
const ACCEPTED = ['application/pdf', 'image/jpeg', 'image/png', 'image/webp'];

export function validateDocumentFile(type: HorseDocumentType, file: { type: string; size: number }): string | null {
  if (!ACCEPTED.includes(file.type)) return 'Use a PDF, JPEG, PNG or WebP file.';
  if (type === 'HORSE_PHOTO' && !file.type.startsWith('image/')) return 'The horse photo must be an image.';
  if (file.size > MAX_DOCUMENT_BYTES) return 'The file is too large. The limit is 10 MB.';
  return null;
}

export type SlotState =
  | { kind: 'current'; slot: HorseDocumentSlot }
  | { kind: 'empty'; type: HorseDocumentType }
  | { kind: 'pending'; type: HorseDocumentType; fileName: string; replaces: HorseDocumentSlot | null };

/** One state per document type, in display order; a newly chosen file overrides what is there. */
export function buildSlotStates(
  slots: HorseDocumentSlot[],
  chosen: Partial<Record<HorseDocumentType, { name: string }>>,
): SlotState[] {
  return HORSE_DOCUMENT_TYPES.map(({ type }) => {
    const existing = slots.find((s) => s.documentType === type && s.fileUrl) ?? null;
    const file = chosen[type];
    if (file) return { kind: 'pending', type, fileName: file.name, replaces: existing };
    return existing ? { kind: 'current', slot: existing } : { kind: 'empty', type };
  });
}

function clean(value: string | null | undefined): string | null {
  const trimmed = (value ?? '').trim();
  return trimmed ? trimmed : null;
}

/** Trims every field and folds blanks to null, the way the server compares them. */
export function normalizeFields(fields: HorseEditFields): HorseEditFields {
  return {
    name: clean(fields.name),
    breed: clean(fields.breed),
    sireName: clean(fields.sireName),
    sireRegistrationNumber: clean(fields.sireRegistrationNumber)?.toUpperCase() ?? null,
    damName: clean(fields.damName),
    damRegistrationNumber: clean(fields.damRegistrationNumber)?.toUpperCase() ?? null,
  };
}

export interface FieldChange {
  field: keyof HorseEditFields;
  label: string;
  from: string | null;
  to: string | null;
}

/** Only the fields that differ, for the review diff and the "nothing changed" check. */
export function diffFields(original: HorseEditFields, requested: HorseEditFields): FieldChange[] {
  const a = normalizeFields(original);
  const b = normalizeFields(requested);
  return (Object.keys(HORSE_EDIT_FIELD_LABELS) as (keyof HorseEditFields)[])
    .filter((field) => a[field] !== b[field])
    .map((field) => ({ field, label: HORSE_EDIT_FIELD_LABELS[field], from: a[field], to: b[field] }));
}

/** UELN: 15 characters, six digits then nine letters/digits, or ISO alpha-3 + 12 (checked again on the server). */
export function uelnFormatError(value: string | null): string | null {
  const v = clean(value)?.toUpperCase();
  if (!v) return null;
  return /^[0-9]{6}[A-Z0-9]{9}$/.test(v) || /^[A-Z]{3}[A-Z0-9]{12}$/.test(v)
    ? null
    : 'A UELN has 15 letters or digits.';
}
