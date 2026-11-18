import { z } from 'zod';
import { metricConfigs, type ExamMode } from './constants.ts';
import { sixVitalsRequired, validateSixVitals, type ExamConfig } from './examVariantConfig.ts';
import { tomorrowBusinessDate } from './businessDate.ts';
import { validateVaccinationDrafts, type VaccinationDraft } from '../../../care/components/preventive/vaccinationDrafts.ts';
import type { CareType, TrainingDecision, VetReviewDecision } from '../../types';

export interface ExamValidationInput {
  config: ExamConfig;
  careType: CareType;
  schedulePurpose?: 'FOLLOW_UP' | 'PREVENTIVE';
  fromIncident?: boolean;
  incidentType?: 'PROBLEM' | 'TRAUMA';
  showTier1: boolean;
  verdict: VetReviewDecision;
  rejectReason: string;
  physicalExamConfirmed: boolean;
  examMode: ExamMode;
  abnormalFindings: string;
  effectiveFindings: string;
  effectiveDiagnosis: string;
  trainingDecision: TrainingDecision;
  restrictionDetails: string;
  followUpActive: boolean;
  followUpDate: string;
  metrics: Record<string, string>;
  markerCount: number;
  horseId?: number | null;
  vaccinationDrafts: VaccinationDraft[];
  deferVaccinationDays: number | null;
}

export interface ExamValidationResult {
  errors: Record<string, string>;
  /** Per-dose errors, or null when vaccination does not apply (caller keeps the previous state). */
  vaccinationErrors: Record<string, string> | null;
}

export function validateExam(input: ExamValidationInput): ExamValidationResult {
  const {
    config,
    careType,
    schedulePurpose,
    fromIncident,
    incidentType,
    showTier1,
    verdict,
    rejectReason,
    physicalExamConfirmed,
    examMode,
    abnormalFindings,
    effectiveFindings,
    effectiveDiagnosis,
    trainingDecision,
    restrictionDetails,
    followUpActive,
    followUpDate,
    metrics,
    markerCount,
    horseId,
    vaccinationDrafts,
    deferVaccinationDays,
  } = input;

  const errors: Record<string, string> = {};

  if (showTier1 && verdict === 'REJECT') {
    if (!rejectReason.trim()) {
      errors.rejectReason = 'Reason for rejection is required when rejecting admission.';
    }
    return { errors, vaccinationErrors: null };
  }

  const schema = z
    .object({
      physicalExamConfirmed: z.boolean(),
      effectiveFindings: z.string().min(1, config.findingsRequiredMessage),
      effectiveDiagnosis: z.string().min(1, config.diagnosisRequiredMessage),
      restrictionDetails: z.string(),
      followUpDate: z.string(),
    })
    .superRefine((data, ctx) => {
      if (config.section2Mode === 'structured' && !physicalExamConfirmed) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['physicalExamConfirmed'],
          message: 'In-person physical examination confirmation is required.',
        });
      }
      if (
        config.section2Mode === 'structured' &&
        examMode === 'ABNORMAL' &&
        config.abnormalFindingsRequiredMessage &&
        !abnormalFindings.trim()
      ) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['abnormalFindings'],
          message: config.abnormalFindingsRequiredMessage,
        });
      }
      if (trainingDecision === 'BLOCKED' && !data.restrictionDetails.trim()) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['restrictionDetails'],
          message: config.restrictionZodMessage,
        });
      }
      if (followUpActive) {
        if (!data.followUpDate) {
          ctx.addIssue({
            code: z.ZodIssueCode.custom,
            path: ['followUpDate'],
            message: config.followUpDateRequiredMessage ?? 'Follow-up date is required.',
          });
        } else if (data.followUpDate < tomorrowBusinessDate()) {
          ctx.addIssue({
            code: z.ZodIssueCode.custom,
            path: ['followUpDate'],
            message: config.followUpDateMinMessage ?? 'Follow-up date must be at least one day in the future.',
          });
        }
      }
    });

  const parsed = schema.safeParse({
    physicalExamConfirmed,
    effectiveFindings,
    effectiveDiagnosis,
    restrictionDetails,
    followUpDate,
  });
  if (!parsed.success) {
    for (const issue of parsed.error.issues) {
      const field = String(issue.path[0] ?? '');
      if (field && !errors[field]) errors[field] = issue.message;
    }
  }

  const isSixVitalsMandatory = sixVitalsRequired(careType, schedulePurpose, fromIncident);
  if (isSixVitalsMandatory) {
    const vitalErrors = validateSixVitals(metrics);
    if (Object.keys(vitalErrors).length > 0) {
      Object.assign(errors, vitalErrors);
      errors.metrics = 'All six vital signs are required before completing this examination.';
    }
  } else {
    for (const cfg of metricConfigs) {
      const val = metrics[cfg.key];
      if (val && val.trim()) {
        const num = Number(val);
        if (Number.isNaN(num) || num <= 0) {
          errors[cfg.key] = `${cfg.label} must be a valid positive number.`;
        }
      }
    }

    const hasVital =
      metricConfigs.some((cfg) => metrics[cfg.key]?.trim()) || metrics.hydrationStatus?.trim();
    if (!hasVital) {
      errors.metrics = 'At least one vital sign is required before submitting.';
    }
  }

  if (careType === 'INITIAL' && trainingDecision === 'BLOCKED' && markerCount === 0) {
    errors.injuryMarkers =
      'An injury or condition record is required when blocking a newly admitted horse.';
  }
  if (incidentType === 'TRAUMA' && markerCount === 0) {
    errors.injuryMarkers =
      'A trauma case must record at least one injury marker before it can be completed.';
  }

  let vaccinationErrors: Record<string, string> | null = null;
  if (config.vaccination && horseId != null) {
    vaccinationErrors = validateVaccinationDrafts(vaccinationDrafts);
    if (Object.keys(vaccinationErrors).length > 0) {
      errors.vaccinations = 'Complete or remove the vaccinations before submitting.';
    } else if (
      schedulePurpose === 'PREVENTIVE' &&
      vaccinationDrafts.length === 0 &&
      deferVaccinationDays == null
    ) {
      errors.vaccinations =
        'Please record at least one vaccination or select deferral days for preventive care.';
    }
  }

  return { errors, vaccinationErrors };
}
