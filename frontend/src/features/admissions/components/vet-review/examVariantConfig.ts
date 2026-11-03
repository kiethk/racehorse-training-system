import type { CareType, TrainingDecision } from '../../types/index.ts';

export type Section2Mode = 'structured' | 'freeText';
export type FollowUpMode = 'single-required-select' | 'multi-optional-freetext';
export type DecisionUiMode = 'shared-section' | 'inline-cards';
export type LayoutMode = 'split-sticky-decision-panel' | 'stacked';
export type SimulateMode = 'resting-inline' | 'hot-profiles';
export type SubmitAdapterMode = 'admission-aware' | 'care-schedule';
export type ErrorMappingMode = 'formatted' | 'raw';

export interface ExamVariantConfig {
  section2Mode: Section2Mode;
  /** Show a dedicated "Physical examination findings" textarea in freeText mode. */
  showFindingsTextarea: boolean;
  /** Show a [Sample Findings] button that fills empty clinical-text fields. */
  sampleFindings: boolean;
  /** Append 3D injury marker lines to the findings textarea. */
  markersAppendToFindings: boolean;
  /** Enable the 3D anatomy marker panel (display may still be gated by training decision for STANDARD). */
  markersEnabled: boolean;
  followUpMode: FollowUpMode;
  decisionUi: DecisionUiMode;
  defaultDecision: TrainingDecision;
  layout: LayoutMode;
  draft: boolean;
  confirmDialog: boolean;
  dirtyGuard: boolean;
  vaccination: boolean;
  prescription: boolean;
  simulateMode: SimulateMode;
  submitAdapter: SubmitAdapterMode;
  errorMapping: ErrorMappingMode;
  submitLabel: string;
  confirmDialogTitle: string;
  confirmDialogConfirmLabel: string;
  confirmDialogCancelLabel: string;
  restrictionLabel: string;
  restrictionPlaceholder: string;
  restrictionZodMessage: string;
  followUpDateRequiredMessage?: string;
  followUpDateMinMessage?: string;
  followUpDescriptionRequiredMessage?: string;
  followUpDateZodMessage?: string;
  followUpDateMinZodMessage?: string;
  followUpDescriptionZodMessage?: string;
  followUpsRequiredZodMessage?: string;
  findingsRequiredMessage: string;
  diagnosisRequiredMessage: string;
  abnormalFindingsRequiredMessage?: string;
  decisionPanelTitle?: string;
}

export type ExamVariant = 'STANDARD' | 'URGENT_LEGACY' | 'URGENT';

export const EXAM_VARIANT_CONFIG: Record<ExamVariant, ExamVariantConfig> = {
  STANDARD: {
    section2Mode: 'structured',
    showFindingsTextarea: false,
    sampleFindings: false,
    markersAppendToFindings: false,
    markersEnabled: true,
    followUpMode: 'single-required-select',
    decisionUi: 'shared-section',
    defaultDecision: 'ALLOWED',
    layout: 'split-sticky-decision-panel',
    draft: true,
    confirmDialog: true,
    dirtyGuard: true,
    vaccination: true,
    prescription: false,
    simulateMode: 'resting-inline',
    submitAdapter: 'admission-aware',
    errorMapping: 'formatted',
    submitLabel: 'Complete Examination',
    confirmDialogTitle: 'Submit Veterinary Examination?',
    confirmDialogConfirmLabel: 'Confirm & Submit',
    confirmDialogCancelLabel: 'Back to Review',
    restrictionLabel: 'Assessment note',
    restrictionPlaceholder:
      'e.g. Tendon strain: box rest with hand-walking twice daily; recheck before trotting…',
    restrictionZodMessage: 'An assessment note is required when training is blocked.',
    followUpDateRequiredMessage: 'Follow-up date is required when scheduling follow-up care.',
    followUpDateMinMessage: 'Follow-up date must be at least one day in the future.',
    followUpDescriptionRequiredMessage: 'Follow-up care description is required.',
    findingsRequiredMessage: 'Clinical examination findings are required.',
    diagnosisRequiredMessage: 'Clinical diagnosis is required.',
    abnormalFindingsRequiredMessage: 'Describe the abnormal examination findings.',
    decisionPanelTitle: 'Veterinary Decision Panel',
  },
  URGENT_LEGACY: {
    section2Mode: 'freeText',
    showFindingsTextarea: false,
    sampleFindings: false,
    markersAppendToFindings: false,
    markersEnabled: false,
    followUpMode: 'single-required-select',
    decisionUi: 'shared-section',
    defaultDecision: 'ALLOWED',
    layout: 'split-sticky-decision-panel',
    draft: true,
    confirmDialog: true,
    dirtyGuard: true,
    vaccination: true,
    prescription: false,
    simulateMode: 'resting-inline',
    submitAdapter: 'admission-aware',
    errorMapping: 'formatted',
    submitLabel: 'Complete Examination',
    confirmDialogTitle: 'Submit Veterinary Examination?',
    confirmDialogConfirmLabel: 'Confirm & Submit',
    confirmDialogCancelLabel: 'Back to Review',
    restrictionLabel: 'Assessment note',
    restrictionPlaceholder:
      'e.g. Tendon strain: box rest with hand-walking twice daily; recheck before trotting…',
    restrictionZodMessage: 'An assessment note is required when training is blocked.',
    followUpDateRequiredMessage: 'Follow-up date is required when scheduling follow-up care.',
    followUpDateMinMessage: 'Follow-up date must be at least one day in the future.',
    followUpDescriptionRequiredMessage: 'Follow-up care description is required.',
    findingsRequiredMessage: 'Clinical examination findings are required.',
    diagnosisRequiredMessage: 'Clinical diagnosis is required.',
    decisionPanelTitle: 'Veterinary Decision Panel',
  },
  URGENT: {
    section2Mode: 'freeText',
    showFindingsTextarea: true,
    sampleFindings: true,
    markersAppendToFindings: true,
    markersEnabled: true,
    followUpMode: 'multi-optional-freetext',
    decisionUi: 'inline-cards',
    defaultDecision: 'BLOCKED',
    layout: 'stacked',
    draft: false,
    confirmDialog: false,
    dirtyGuard: false,
    vaccination: false,
    prescription: true,
    simulateMode: 'hot-profiles',
    submitAdapter: 'care-schedule',
    errorMapping: 'raw',
    submitLabel: 'Complete Urgent Examination',
    confirmDialogTitle: 'Submit Urgent Examination?',
    confirmDialogConfirmLabel: 'Confirm & Submit',
    confirmDialogCancelLabel: 'Back to Review',
    restrictionLabel: 'Training restriction & rest details',
    restrictionPlaceholder:
      'e.g. Strict box rest for 3 days, followed by 15-minute hand walking twice daily…',
    restrictionZodMessage: 'Restriction details are required when training is blocked.',
    followUpDateZodMessage: 'Select a follow-up date for this urgent case.',
    followUpDateMinZodMessage: 'The follow-up date must be tomorrow or later.',
    followUpDescriptionZodMessage: 'A follow-up description is required.',
    followUpsRequiredZodMessage: 'Schedule at least one follow-up examination for this urgent case.',
    findingsRequiredMessage: 'Examination findings are required.',
    diagnosisRequiredMessage: 'A clinical diagnosis is required.',
  },
};

export const SIX_VITAL_FIELDS: Array<
  | { key: 'temperature' | 'heartRate' | 'respiratoryRate' | 'weight' | 'bodyConditionScore'; label: string; min: number; max: number }
  | { key: 'hydrationStatus'; label: string; enum: true }
> = [
  { key: 'temperature', label: 'Temperature', min: 37.0, max: 38.5 },
  { key: 'heartRate', label: 'Heart Rate', min: 28, max: 44 },
  { key: 'respiratoryRate', label: 'Respiratory Rate', min: 8, max: 16 },
  { key: 'weight', label: 'Weight', min: 0, max: 1500 },
  { key: 'bodyConditionScore', label: 'BCS', min: 1, max: 9 },
  { key: 'hydrationStatus', label: 'Hydration', enum: true },
];

/**
 * Tracks which flows require all six vitals (5 measurements + hydration).
 * Scope agreed with the user: INITIAL admissions and ROUTINE FOLLOW_UP exams only.
 */
export function sixVitalsRequired(
  variant: ExamVariant,
  careType: CareType,
  purpose?: 'FOLLOW_UP' | 'PREVENTIVE',
): boolean {
  if (variant !== 'STANDARD') return false;
  if (careType === 'INITIAL') return true;
  if (careType === 'ROUTINE' && purpose === 'FOLLOW_UP') return true;
  return false;
}

/**
 * Pure validation helper for the six-vitals mandate. Returns per-field error messages.
 * Does not depend on React or the component tree so it can be unit-tested.
 */
export function validateSixVitals(metrics: Record<string, string>): Record<string, string> {
  const errors: Record<string, string> = {};
  for (const field of SIX_VITAL_FIELDS) {
    const raw = metrics[field.key]?.trim() ?? '';
    if (!raw) {
      errors[field.key] = `${field.label} is required.`;
      continue;
    }
    if ('enum' in field) continue;
    const num = Number(raw);
    if (Number.isNaN(num)) {
      errors[field.key] = `${field.label} must be a number.`;
    } else if (field.key === 'weight') {
      if (num <= 0 || num >= 1500) {
        errors[field.key] = `${field.label} must be greater than 0 and less than 1500.`;
      }
    } else if (num < field.min || num > field.max) {
      errors[field.key] = `${field.label} must be between ${field.min} and ${field.max}.`;
    }
  }
  return errors;
}

/** Whether the Tier-1 ACCEPT/REJECT verdict is shown for this exam context. */
export function tier1VerdictEnabled(variant: ExamVariant, careType: CareType): boolean {
  return variant === 'STANDARD' && careType === 'INITIAL';
}
