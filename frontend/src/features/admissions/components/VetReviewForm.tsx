'use client';

import { useCallback, useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react';
import { z } from 'zod';
import { VitalsSection } from './vet-review/VitalsSection';
// FindingsSection removed: section 2 clinical body-system exam is no longer collected.
import { DiagnosisSection } from './vet-review/DiagnosisSection';
import { TreatmentSection } from './vet-review/TreatmentSection';
import { TrainingDecisionSection } from './vet-review/TrainingDecisionSection';
import { FollowUpSection } from './vet-review/FollowUpSection';
import { ApiError } from '@/services/api';
import { Button } from '@/components/ui/Button';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Notice } from '@/components/ui/Notice';
import { admissionAsideClassName, admissionDetailGridClassName } from '../shared/components/AdmissionDetailLayout';
import { Icon } from '@/components/ui/Icon';

import { admissionsApi } from '../services/api';
import {
  ExamVaccinationSection,
  toVaccinationRequests,
  validateVaccinationDrafts,
  type VaccinationDraft,
} from '@/features/care/components/preventive/ExamVaccinationSection';
import type {
  CareType,
  CareSchedule,
  CareScheduleStatus,
  CompleteCareScheduleRequest,
  HorseHealthMetricRequest,
  TrainingDecision,
  VetReviewRequest,
  VetReviewResponse,
} from '../types';

type MetricField = keyof HorseHealthMetricRequest;
type ExamMode = 'NORMAL' | 'ABNORMAL';
type FindingLevel = 'NORMAL' | 'MILD' | 'MODERATE' | 'SEVERE' | 'NOT_EXAMINED';
type BodySystemKey =
  | 'general'
  | 'respiratory'
  | 'cardiovascular'
  | 'musculoskeletal'
  | 'neurological'
  | 'skinCoat'
  | 'hooves'
  | 'digestive';

const bodySystems: Array<{ key: BodySystemKey; label: string }> = [
  { key: 'general', label: 'General Condition' },
  { key: 'respiratory', label: 'Respiratory' },
  { key: 'cardiovascular', label: 'Cardiovascular' },
  { key: 'musculoskeletal', label: 'Musculoskeletal' },
  { key: 'neurological', label: 'Neurological' },
  { key: 'skinCoat', label: 'Skin & Coat' },
  { key: 'hooves', label: 'Hooves' },
  { key: 'digestive', label: 'Digestive' },
];

const findingLevelLabels: Record<FindingLevel, string> = {
  NORMAL: 'Normal',
  MILD: 'Mild abnormality',
  MODERATE: 'Moderate abnormality',
  SEVERE: 'Severe abnormality',
  NOT_EXAMINED: 'Not examined',
};

const normalSystemFindings = Object.fromEntries(
  bodySystems.map(({ key }) => [key, 'NORMAL']),
) as Record<BodySystemKey, FindingLevel>;

interface MetricConfig {
  key: MetricField;
  label: string;
  unit: string;
  step: string;
  placeholder: string;
  normalMin: number;
  normalMax: number;
  criticalMin: number;
  criticalMax: number;
}

const metricConfigs: MetricConfig[] = [
  {
    key: 'temperature',
    label: 'Temperature',
    unit: '°C',
    step: '0.1',
    placeholder: '38.0',
    normalMin: 37.2,
    normalMax: 38.3,
    criticalMin: 36.5,
    criticalMax: 38.8,
  },
  {
    key: 'heartRate',
    label: 'Heart Rate',
    unit: 'bpm',
    step: '1',
    placeholder: '36',
    normalMin: 28,
    normalMax: 44,
    criticalMin: 24,
    criticalMax: 55,
  },
  {
    key: 'respiratoryRate',
    label: 'Resp Rate',
    unit: 'rpm',
    step: '1',
    placeholder: '12',
    normalMin: 8,
    normalMax: 16,
    criticalMin: 6,
    criticalMax: 24,
  },
  {
    key: 'weight',
    label: 'Weight',
    unit: 'kg',
    step: '0.5',
    placeholder: '500',
    normalMin: 400,
    normalMax: 600,
    criticalMin: 350,
    criticalMax: 650,
  },
  {
    key: 'bodyConditionScore',
    label: 'BCS',
    unit: '/ 9',
    step: '0.5',
    placeholder: '5.0',
    normalMin: 4.0,
    normalMax: 6.0,
    criticalMin: 3.0,
    criticalMax: 7.0,
  },
];

const emptyMetrics: Record<MetricField, string> = {
  heartRate: '',
  temperature: '',
  weight: '',
  respiratoryRate: '',
  hydrationStatus: '',
  bodyConditionScore: '',
  notes: '',
};

const BUSINESS_TIME_ZONE = 'Asia/Ho_Chi_Minh';

function businessDateFromToday(dayOffset: number) {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: BUSINESS_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date());
  const year = Number(parts.find((part) => part.type === 'year')?.value);
  const month = Number(parts.find((part) => part.type === 'month')?.value);
  const day = Number(parts.find((part) => part.type === 'day')?.value);
  const target = new Date(Date.UTC(year, month - 1, day + dayOffset));

  return [
    target.getUTCFullYear(),
    String(target.getUTCMonth() + 1).padStart(2, '0'),
    String(target.getUTCDate()).padStart(2, '0'),
  ].join('-');
}

function tomorrowBusinessDate() {
  return businessDateFromToday(1);
}


export interface VetReviewFormProps {
  admissionId: number;
  horseId?: number | null;
  candidateName?: string;
  quarantineStallCode?: string | null;
  careScheduleId?: number | null;
  careType?: CareType;
  scheduleStatus?: CareScheduleStatus;
  /** PREVENTIVE: the system-booked vaccination visit; it can defer vaccination instead of giving it. */
  schedulePurpose?: 'FOLLOW_UP' | 'PREVENTIVE';
  onStartExam?: () => Promise<void>;
  /** Rendered above the decision panel in the side column (e.g. the examination details card). */
  asideTop?: ReactNode;
  onSuccess: (result: VetReviewResponse | CareSchedule, completionKind: 'INITIAL' | 'CARE_SCHEDULE') => void;
  onDirtyChange?: (dirty: boolean) => void;
}

export function VetReviewForm({
  admissionId,
  horseId,
  candidateName = 'Candidate horse',
  quarantineStallCode,
  careScheduleId,
  careType = 'INITIAL',
  scheduleStatus,
  schedulePurpose,
  onStartExam,
  asideTop,
  onSuccess,
  onDirtyChange,
}: VetReviewFormProps) {
  const draftStorageKey = careScheduleId
    ? `draft:care-schedule:${careScheduleId}`
    : `draft:admission:${admissionId}`;
  // Decision Panel State
  const [trainingDecision, setTrainingDecision] = useState<TrainingDecision>('ALLOWED');
  const [restrictionDetails, setRestrictionDetails] = useState('');
  const [scheduleFollowUp, setScheduleFollowUp] = useState(false);
  const [followUpDate, setFollowUpDate] = useState('');
  const [followUpDescription, setFollowUpDescription] = useState('Routine follow-up recheck examination');
  const [followUpIdempotencyKey, setFollowUpIdempotencyKey] = useState(() =>
    typeof crypto !== 'undefined' && 'randomUUID' in crypto
      ? crypto.randomUUID()
      : `follow-up-${Date.now()}-${Math.random().toString(36).slice(2)}`,
  );



  // Clinical Findings Form State
  const [physicalExamConfirmed, setPhysicalExamConfirmed] = useState(true);
  const [examMode, setExamMode] = useState<ExamMode>('NORMAL');
  const [systemFindings, setSystemFindings] = useState<Record<BodySystemKey, FindingLevel>>({
    ...normalSystemFindings,
  });
  const [selectedSymptoms, setSelectedSymptoms] = useState<string[]>(['No symptoms observed']);
  const [structuredDiagnosis, setStructuredDiagnosis] = useState('Clinically healthy');
  const [treatment, setTreatment] = useState('');
  const [notes, setNotes] = useState('');

  // Urgent form fields override
  const [symptoms, setSymptoms] = useState('');
  const [findings, setFindings] = useState('');
  const [diagnosis, setDiagnosis] = useState('');

  // Vitals State
  const [metrics, setMetrics] = useState<Record<MetricField, string>>(emptyMetrics);

  // Vaccinations recorded in this exam (not kept in the local draft)
  const [vaccinationDrafts, setVaccinationDrafts] = useState<VaccinationDraft[]>([]);
  const [deferVaccinationDays, setDeferVaccinationDays] = useState<number | null>(null);
  const [vaccinationErrors, setVaccinationErrors] = useState<Record<string, string>>({});

  // Status & Validation
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [startingExam, setStartingExam] = useState(false);
  const [showApproveConfirm, setShowApproveConfirm] = useState(false);
  const [availableDraft, setAvailableDraft] = useState<{
    metrics?: Record<MetricField, string>;
    examMode?: ExamMode;
    systemFindings?: Record<BodySystemKey, FindingLevel>;
    selectedSymptoms?: string[];
    structuredDiagnosis?: string;
    treatment?: string;
    notes?: string;
    symptoms?: string;
    findings?: string;
    diagnosis?: string;
    trainingDecision?: TrainingDecision;
    restrictionDetails?: string;
    scheduleFollowUp?: boolean;
    followUpDate?: string;
    followUpDescription?: string;
    followUpIdempotencyKey?: string;
    timestamp: string;
  } | null>(() => {
    if (typeof window === 'undefined') return null;
    try {
      const storageKey = draftStorageKey;
      const fallbackKey = careScheduleId ? null : `rtms_vet_draft_${admissionId}`;
      const saved = localStorage.getItem(storageKey) || (fallbackKey ? localStorage.getItem(fallbackKey) : null);
      if (saved) {
        const parsed = JSON.parse(saved);
        if (parsed && parsed.timestamp) {
          return parsed;
        }
      }
    } catch {
      // Ignore storage read errors (e.g. private browsing)
    }
    return null;
  });
  const [draftSavedTime, setDraftSavedTime] = useState<string | null>(null);

  const inFlight = useRef(false);

  const isGated = scheduleStatus != null && scheduleStatus !== 'IN_PROGRESS';
  // Blocking training always needs a follow-up exam: "rest until" is that exam's date.
  // Routine/preventive visits must always produce a follow-up schedule.
  // Follow-up schedule is mandatory for every examination (user requirement).
  const followUpRequired = true;
  const followUpActive = scheduleFollowUp || followUpRequired;
  const isFormDisabled = submitting || startingExam || isGated;
  const isUrgent = careType === 'URGENT';

  // Compute dirty state
  const isDirty =
    metrics.heartRate !== '' ||
    metrics.temperature !== '' ||
    metrics.weight !== '' ||
    metrics.respiratoryRate !== '' ||
    metrics.bodyConditionScore !== '' ||
    metrics.hydrationStatus !== '' ||
    metrics.notes !== '' ||
    symptoms.trim() !== '' ||
    findings.trim() !== '' ||
    diagnosis.trim() !== '' ||
    examMode !== 'NORMAL' ||
    Object.values(systemFindings).some((f) => f !== 'NORMAL') ||
    selectedSymptoms.length > 1 ||
    (selectedSymptoms.length === 1 && selectedSymptoms[0] !== 'No symptoms observed') ||
    structuredDiagnosis !== 'Clinically healthy' ||
    treatment.trim() !== '' ||
    notes.trim() !== '' ||
    trainingDecision !== 'ALLOWED' ||
    restrictionDetails.trim() !== '' ||
    scheduleFollowUp ||
    followUpDate !== '';

  useEffect(() => {
    onDirtyChange?.(isDirty);
  }, [isDirty, onDirtyChange]);

  const handleRestoreDraft = () => {
    if (!availableDraft) return;
    if (availableDraft.metrics) setMetrics(availableDraft.metrics);
    if (availableDraft.examMode) setExamMode(availableDraft.examMode);
    if (availableDraft.systemFindings) setSystemFindings(availableDraft.systemFindings);
    if (availableDraft.selectedSymptoms) setSelectedSymptoms(availableDraft.selectedSymptoms);
    if (availableDraft.structuredDiagnosis) setStructuredDiagnosis(availableDraft.structuredDiagnosis);
    if (availableDraft.treatment !== undefined) setTreatment(availableDraft.treatment);
    if (availableDraft.notes !== undefined) setNotes(availableDraft.notes);
    if (availableDraft.symptoms !== undefined) setSymptoms(availableDraft.symptoms);
    if (availableDraft.findings !== undefined) setFindings(availableDraft.findings);
    if (availableDraft.diagnosis !== undefined) setDiagnosis(availableDraft.diagnosis);
    // Old drafts may still hold RESTRICTED -> it is now merged into BLOCKED.
    if (availableDraft.trainingDecision) {
      setTrainingDecision(availableDraft.trainingDecision === 'ALLOWED' ? 'ALLOWED' : 'BLOCKED');
    }
    if (availableDraft.restrictionDetails !== undefined) setRestrictionDetails(availableDraft.restrictionDetails);
    if (availableDraft.scheduleFollowUp !== undefined) setScheduleFollowUp(availableDraft.scheduleFollowUp);
    if (availableDraft.followUpDate !== undefined) setFollowUpDate(availableDraft.followUpDate);
    if (availableDraft.followUpDescription !== undefined) setFollowUpDescription(availableDraft.followUpDescription);
    if (availableDraft.followUpIdempotencyKey) setFollowUpIdempotencyKey(availableDraft.followUpIdempotencyKey);
    setDraftSavedTime(availableDraft.timestamp);
    setAvailableDraft(null);
  };

  const handleDiscardAvailableDraft = () => {
    try {
      localStorage.removeItem(draftStorageKey);
      localStorage.removeItem(`rtms_vet_draft_${admissionId}`);
    } catch {
      // Ignore storage errors in restricted environments
    }
    setAvailableDraft(null);
  };

  const handleSaveDraft = useCallback(() => {
    try {
      const storageKey = draftStorageKey;
      const timestamp = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
      const payload = {
        metrics,
        examMode,
        systemFindings,
        selectedSymptoms,
        structuredDiagnosis,
        treatment,
        notes,
        symptoms,
        findings,
        diagnosis,
        trainingDecision,
        restrictionDetails,
        scheduleFollowUp,
        followUpDate,
        followUpDescription,
        followUpIdempotencyKey,
        timestamp,
      };
      localStorage.setItem(storageKey, JSON.stringify(payload));
      if (!careScheduleId) localStorage.setItem(`rtms_vet_draft_${admissionId}`, JSON.stringify(payload));
      setDraftSavedTime(timestamp);
    } catch {
      // Storage write error
    }
  }, [
    admissionId,
    careScheduleId,
    draftStorageKey,
    metrics,
    examMode,
    systemFindings,
    selectedSymptoms,
    structuredDiagnosis,
    treatment,
    notes,
    symptoms,
    findings,
    diagnosis,
    trainingDecision,
    restrictionDetails,
    scheduleFollowUp,
    followUpDate,
    followUpDescription,
    followUpIdempotencyKey,
  ]);

  const handleClearDraft = () => {
    try {
      localStorage.removeItem(draftStorageKey);
      localStorage.removeItem(`rtms_vet_draft_${admissionId}`);
    } catch {
      // Ignore storage errors in restricted environments
    }
    setMetrics(emptyMetrics);
    setExamMode('NORMAL');
    setSystemFindings({ ...normalSystemFindings });
    setSelectedSymptoms(['No symptoms observed']);
    setStructuredDiagnosis('Clinically healthy');
    setTreatment('');
    setNotes('');
    setSymptoms('');
    setFindings('');
    setDiagnosis('');
    setTrainingDecision('ALLOWED');
    setRestrictionDetails('');
    setScheduleFollowUp(false);
    setFollowUpDate('');
    setFollowUpDescription('Routine follow-up recheck examination');
    setPhysicalExamConfirmed(true);
    setFieldErrors({});
    setDraftSavedTime(null);
  };

  const handleRandomizeData = () => {
    // 1. Generate realistic vital metrics within normal physiological limits for a Thoroughbred/Arabian racehorse
    const randomTemp = (37.5 + Math.random() * 0.7).toFixed(1);
    const randomHr = Math.floor(32 + Math.random() * 9).toString();
    const randomRr = Math.floor(10 + Math.random() * 5).toString();
    const randomWeight = (480 + Math.floor(Math.random() * 45 * 2) / 2).toFixed(1);
    const randomBcs = (5.0 + (Math.random() > 0.5 ? 0.5 : 0)).toFixed(1);

    const observationNotesPool = [
      'Calm disposition, alert and responsive, post-transport check normal.',
      'Clear respiratory sounds bilaterally, capillary refill time < 2s, gut motility active.',
      'Good body condition, responsive to stimuli, mucous membranes pink and moist.',
      'Resting comfortably in stall, vital telemetry within standard resting range.',
      'Normal heart sounds without murmurs, digital pulses equal and normal.',
    ];
    const randomObservation = observationNotesPool[Math.floor(Math.random() * observationNotesPool.length)];

    setMetrics({
      temperature: randomTemp,
      heartRate: randomHr,
      respiratoryRate: randomRr,
      weight: randomWeight,
      bodyConditionScore: randomBcs,
      hydrationStatus: 'NORMAL',
      notes: randomObservation,
    });

    // 2. Clinical Findings & Examination
    setPhysicalExamConfirmed(true);
    setExamMode('NORMAL');
    setSystemFindings({ ...normalSystemFindings });
    setSelectedSymptoms(['No symptoms observed']);
    setStructuredDiagnosis('Clinically healthy');
    setTreatment('No medical treatment required. Cleared for routine care protocol.');
    setNotes('General physical examination reveals no clinical abnormalities. Cardiovascular, respiratory, and musculoskeletal systems intact.');

    if (isUrgent) {
      setSymptoms('Mild fatigue following transport, no acute distress.');
      setFindings('Clear lungs, strong peripheral pulses, sound hooves, no heat or effusion.');
      setDiagnosis('Clinically healthy with normal post-transport adaptation.');
    }

    // 3. Training Decision
    setTrainingDecision('ALLOWED');
    setRestrictionDetails('');
    setScheduleFollowUp(false);
    setFollowUpDate('');

    // Clear validation errors
    setFieldErrors({});
    setFormError('');

    onDirtyChange?.(true);
  };

  // 15-second debounce auto-save when dirty (FR-CHUNG-93)
  // Do NOT autosave while an unrestored draft banner is pending user action to prevent overwriting
  useEffect(() => {
    if (!isDirty || isGated || availableDraft !== null) return;
    const timer = setTimeout(() => {
      handleSaveDraft();
    }, 15000);
    return () => clearTimeout(timer);
  }, [isDirty, isGated, availableDraft, handleSaveDraft]);

  const generatedSymptoms =
    examMode === 'NORMAL'
      ? 'No symptoms observed'
      : selectedSymptoms.join('; ') || 'No presenting symptoms selected';

  const generatedFindings =
    examMode === 'NORMAL'
      ? 'Physical examination completed in person. General condition, respiratory, cardiovascular, musculoskeletal, neurological, skin and coat, hooves, and digestive systems were within normal limits.'
      : `Physical examination completed in person. ${bodySystems
          .map(({ key, label }) => `${label}: ${findingLevelLabels[systemFindings[key]].toLowerCase()}`)
          .join('; ')}.`;

  const effectiveSymptoms = isUrgent ? symptoms.trim() : generatedSymptoms;
  const effectiveFindings = isUrgent ? findings.trim() : generatedFindings;
  const effectiveDiagnosis = isUrgent ? diagnosis.trim() : structuredDiagnosis;

  function validate(): Record<string, string> {
    const errors: Record<string, string> = {};

    // Zod schema for the core, conditional text/date fields (T6).
    const schema = z.object({
      physicalExamConfirmed: z.boolean(),
      symptoms: z.string(),
      effectiveFindings: z.string().min(1, 'Clinical examination findings are required.'),
      effectiveDiagnosis: z.string().min(1, 'Clinical diagnosis is required.'),
      restrictionDetails: z.string(),
      followUpDate: z.string(),
      followUpDescription: z.string(),
    }).superRefine((data, ctx) => {
      if (!data.physicalExamConfirmed) {
        ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['physicalExamConfirmed'], message: 'In-person physical examination confirmation is required.' });
      }
      if (isUrgent && !data.symptoms.trim()) {
        ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['symptoms'], message: 'Detailed presenting symptoms are required for urgent care.' });
      }
      if (
        !isUrgent &&
        examMode === 'ABNORMAL' &&
        bodySystems.every(({ key }) => systemFindings[key] === 'NORMAL')
      ) {
        ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['systemFindings'], message: 'Select at least one abnormal or not-examined body system.' });
      }
      if (trainingDecision === 'BLOCKED' && !data.restrictionDetails.trim()) {
        ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['restrictionDetails'], message: 'Restriction details are mandatory when training is blocked.' });
      }
      if (followUpActive) {
        if (!data.followUpDate) {
          ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['followUpDate'], message: 'Follow-up date is required when scheduling follow-up care.' });
        } else if (data.followUpDate < tomorrowBusinessDate()) {
          ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['followUpDate'], message: 'Follow-up date must be at least one day in the future.' });
        }
        if (!data.followUpDescription.trim()) {
          ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['followUpDescription'], message: 'Follow-up care description is required.' });
        }
      }
    });

    const parsed = schema.safeParse({
      physicalExamConfirmed,
      symptoms,
      effectiveFindings,
      effectiveDiagnosis,
      restrictionDetails,
      followUpDate,
      followUpDescription,
    });
    if (!parsed.success) {
      for (const issue of parsed.error.issues) {
        const field = String(issue.path[0] ?? '');
        if (field && !errors[field]) errors[field] = issue.message;
      }
    }
    for (const cfg of metricConfigs) {
      const val = metrics[cfg.key];
      if (val && val.trim()) {
        const num = Number(val);
        if (Number.isNaN(num) || num <= 0) {
          errors[cfg.key] = `${cfg.label} must be a valid positive number.`;
        }
      }
    }

    const hasVital = metricConfigs.some((cfg) => metrics[cfg.key]?.trim()) || metrics.hydrationStatus?.trim();
    if (!hasVital) {
      errors.metrics = 'At least one vital sign is required before submitting.';
    }

    const doseErrors = validateVaccinationDrafts(vaccinationDrafts);
    setVaccinationErrors(doseErrors);
    if (Object.keys(doseErrors).length > 0) {
      errors.vaccinations = 'Complete or remove the vaccinations before submitting.';
    } else if (schedulePurpose === 'PREVENTIVE' && vaccinationDrafts.length === 0 && deferVaccinationDays == null) {
      errors.vaccinations = 'Please record at least one vaccination or select deferral days for preventive care.';
    }

    return errors;
  }

  function handleFormSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (scheduleStatus && scheduleStatus !== 'IN_PROGRESS') {
      setFormError('Care schedule is SCHEDULED. Physical examination must be started before recording and submitting findings.');
      return;
    }
    const errors = validate();
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      const msg = errors.vaccinations && Object.keys(errors).length === 1
        ? errors.vaccinations
        : 'Please resolve all validation errors before submitting.';
      setFormError(msg);
      return;
    }
    setFormError('');
    setShowApproveConfirm(true);
  }

  async function executeSubmit() {
    if (inFlight.current) return;
    if (scheduleStatus && scheduleStatus !== 'IN_PROGRESS') {
      setFormError('Care schedule is SCHEDULED. Physical examination must be started before recording and submitting findings.');
      setShowApproveConfirm(false);
      return;
    }

    const errors = validate();
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      const msg = errors.vaccinations && Object.keys(errors).length === 1
        ? errors.vaccinations
        : 'Please resolve all validation errors before submitting.';
      setFormError(msg);
      setShowApproveConfirm(false);
      return;
    }

    inFlight.current = true;
    setSubmitting(true);
    setFormError('');

    const metricValues = Object.fromEntries(
      Object.entries(metrics)
        .filter(([, value]) => value.trim())
        .map(([key, value]) => [
          key,
          ['hydrationStatus', 'notes'].includes(key) ? value.trim() : Number(value),
        ]),
    ) as HorseHealthMetricRequest;

    const nextScheduleRequest = followUpActive && followUpDate ? {
      horseId: horseId ?? undefined,
      // Horses without an admission have admissionId 0; omit it so the backend
      // compares null with null instead of rejecting with 409 ADMISSION_MISMATCH.
      admissionId: admissionId || undefined,
      careType: 'ROUTINE' as const,
      scheduledDate: followUpDate,
      description: followUpDescription.trim() || 'Follow-up veterinary examination',
      idempotencyKey: followUpIdempotencyKey,
    } : undefined;

    try {
      let result: VetReviewResponse | CareSchedule;
      let completionKind: 'INITIAL' | 'CARE_SCHEDULE';
      if (careType === 'INITIAL') {
        const request: VetReviewRequest = {
          careScheduleId: careScheduleId ?? undefined,
          trainingDecision,
          restrictionDetails: restrictionDetails.trim() || undefined,
          physicalExamConfirmed: true,
          findings: effectiveFindings,
          diagnosis: effectiveDiagnosis,
          treatment: treatment.trim() || undefined,
          symptoms: effectiveSymptoms || undefined,
          notes: notes.trim() || undefined,
          feedback: notes.trim() || undefined,
          nextSchedule: nextScheduleRequest,
          metrics: Object.keys(metricValues).length ? [metricValues] : undefined,
          vaccinations: vaccinationDrafts.length ? toVaccinationRequests(vaccinationDrafts) : undefined,
        };
        result = await admissionsApi.vetReview(admissionId, request);
        completionKind = 'INITIAL';
      } else {
        if (!careScheduleId) throw new Error('The care schedule identifier is missing. Reload this examination and try again.');
        const request: CompleteCareScheduleRequest = {
          findings: effectiveFindings,
          diagnosis: effectiveDiagnosis,
          symptoms: effectiveSymptoms || undefined,
          treatment: treatment.trim() || undefined,
          trainingDecision,
          restrictionDetails: restrictionDetails.trim() || undefined,
          notes: notes.trim() || undefined,
          nextSchedule: nextScheduleRequest,
          metrics: Object.keys(metricValues).length ? [metricValues] : undefined,
          vaccinations: vaccinationDrafts.length ? toVaccinationRequests(vaccinationDrafts) : undefined,
          deferVaccinationDays: deferVaccinationDays ?? undefined,
        };
        result = await admissionsApi.completeCareSchedule(careScheduleId, request);
        completionKind = 'CARE_SCHEDULE';
      }

      // Clear draft on successful submission
      try {
        localStorage.removeItem(draftStorageKey);
        localStorage.removeItem(`rtms_vet_draft_${admissionId}`);
      } catch {
        // Ignore
      }

      setShowApproveConfirm(false);
      onSuccess(result, completionKind);
    } catch (cause) {
      if (cause instanceof ApiError) {
        const errorMsg = `${cause.status === 403 ? 'Permission denied' : cause.status === 409 ? 'Review state conflict' : 'Review failed'} (${cause.status}${cause.errorCode ? ` · ${cause.errorCode}` : ''}): ${cause.message}`;
        setFormError(errorMsg);
      } else {
        const errorMsg = cause instanceof Error ? cause.message : 'Failed to submit review.';
        setFormError(errorMsg);
      }
      setShowApproveConfirm(false);
    } finally {
      inFlight.current = false;
      setSubmitting(false);
    }
  }

  return (
    <>
      <form onSubmit={handleFormSubmit} noValidate className="pb-24 md:pb-0">
        {/* Gate Warning when SCHEDULED */}
        {scheduleStatus === 'SCHEDULED' && (
          <div
            role="status"
            className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-[var(--radius-md)] border border-[var(--color-warning)] bg-[var(--color-warning-soft)] p-3 text-sm text-[var(--color-warning)]"
          >
            <div className="flex items-center gap-2">
              <Icon name="alert-triangle" size={18} className="shrink-0 text-[var(--color-warning)]" />
              <strong className="block text-sm font-bold">
                Care schedule is SCHEDULED
              </strong>
            </div>
            {onStartExam && !asideTop && (
              <Button
                type="button"
                size="sm"
                variant="primary"
                loading={startingExam}
                onClick={async () => {
                  try {
                    setStartingExam(true);
                    await onStartExam();
                  } finally {
                    setStartingExam(false);
                  }
                }}
                icon="activity"
              >
                Start Examination
              </Button>
            )}
          </div>
        )}
        {/* Gate Warning when REQUESTED */}
        {scheduleStatus === 'REQUESTED' && (
          <div
            role="status"
            className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-[var(--radius-md)] border border-[var(--color-info)] bg-[var(--color-info-soft)] p-3 text-sm text-[var(--color-info)]"
          >
            <div className="flex items-center gap-2">
              <Icon name="clock" size={18} className="shrink-0 text-[var(--color-info)]" />
              <strong className="block text-sm font-bold">
                Care schedule is REQUESTED
              </strong>
            </div>
          </div>
        )}

        {/* Global Form Error */}
        {formError && (
          <div
            role="alert"
            className="mb-4 flex items-start gap-2.5 rounded-[var(--radius-md)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-3 text-sm text-[var(--color-danger)]"
          >
            <Icon name="alert-triangle" size={16} className="mt-0.5 shrink-0" />
            <span>{formError}</span>
          </div>
        )}

        {/* Local Draft Available Prompt (FR-CHUNG-94) */}
        {availableDraft && (
          <div
            role="status"
            className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-[var(--radius-sm)] border border-[var(--color-primary)] bg-[var(--color-primary-soft)] px-3 py-2 text-xs text-[var(--color-text-primary)]"
          >
            <div className="flex items-center gap-2">
              <Icon name="clipboard" size={14} className="text-[var(--color-primary)] shrink-0" />
              <span>
                A saved draft from <strong>{availableDraft.timestamp}</strong> was found. Would you like to restore it?
              </span>
            </div>
            <div className="flex items-center gap-2">
              <Button
                type="button"
                size="sm"
                variant="primary"
                onClick={handleRestoreDraft}
              >
                Restore
              </Button>
              <Button
                type="button"
                size="sm"
                variant="secondary"
                onClick={handleDiscardAvailableDraft}
              >
                Delete draft
              </Button>
            </div>
          </div>
        )}

        {/* Draft Saved Status (FR-CHUNG-92) */}
        {draftSavedTime && !availableDraft && (
          <div className="mb-4 flex items-center justify-between rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] px-3 py-1.5 text-xs text-[var(--color-text-secondary)]">
            <span className="flex items-center gap-1.5 font-medium">
              <Icon name="check" size={12} className="text-[var(--color-success)]" />
              Draft saved at {draftSavedTime}
            </span>
            <Button type="button" variant="link" size="sm" onClick={handleClearDraft}>
              Delete draft
            </Button>
          </div>
        )}

        {/* Main Two-Column Body: Form (~65%) & Sticky Decision Panel (~35%) */}
        <div className={admissionDetailGridClassName}>
          {/* LEFT COLUMN: Clinical Examination (~65% width = lg:col-span-8) */}
          <div className="min-w-0 space-y-5">
            {/* Section 1: Patient Vitals & Telemetry */}
            <VitalsSection
              metrics={metrics}
              setMetrics={setMetrics}
              fieldErrors={fieldErrors}
              isFormDisabled={isFormDisabled}
              onRandomize={handleRandomizeData}
            />
            {fieldErrors.metrics && (
              <p className="text-xs font-medium text-[var(--color-danger)]" role="alert">
                {fieldErrors.metrics}
              </p>
            )}

            {/* Section 2 removed: body-system exam no longer collected; findings are auto-generated. */}

            {/* Side-by-Side Assessment & Treatment Inputs */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3 pt-2">
                <DiagnosisSection
                  isUrgent={isUrgent}
                  isFormDisabled={isFormDisabled}
                  diagnosis={diagnosis}
                  setDiagnosis={setDiagnosis}
                  examMode={examMode}
                  structuredDiagnosis={structuredDiagnosis}
                  setStructuredDiagnosis={setStructuredDiagnosis}
                  fieldErrors={fieldErrors}
                />
                <TreatmentSection
                  isFormDisabled={isFormDisabled}
                  treatment={treatment}
                  setTreatment={setTreatment}
                  notes={notes}
                  setNotes={setNotes}
                />
              </div>


            {/* Section 3: Vaccination & deworming given in this exam */}
            {horseId != null && (
              <div>
                <ExamVaccinationSection
                  horseId={horseId}
                  careType={careType}
                  purpose={schedulePurpose}
                  temperature={metrics.temperature}
                  trainingBlocked={trainingDecision === 'BLOCKED'}
                  disabled={isFormDisabled}
                  value={vaccinationDrafts}
                  onChange={(next) => {
                    setVaccinationDrafts(next);
                    if (fieldErrors.vaccinations) {
                      setFieldErrors((errs) => ({ ...errs, vaccinations: '' }));
                    }
                  }}
                  deferDays={deferVaccinationDays}
                  onDeferChange={(days) => {
                    setDeferVaccinationDays(days);
                    if (fieldErrors.vaccinations) {
                      setFieldErrors((errs) => ({ ...errs, vaccinations: '' }));
                    }
                  }}
                  errors={vaccinationErrors}
                />
                {fieldErrors.vaccinations && (
                  <p className="mt-1.5 text-xs font-medium text-[var(--color-danger)]" role="alert">
                    {fieldErrors.vaccinations}
                  </p>
                )}
              </div>
            )}
          </div>

          {/* RIGHT COLUMN: Sticky Decision Panel (~35% width = lg:col-span-4) */}
          <div className={admissionAsideClassName}>
          {asideTop}
          <div className="space-y-4 min-w-0 rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-[var(--shadow-panel)]">
            <div className="border-b border-[var(--color-border)] pb-3">
              <h3 className="text-sm font-semibold tracking-tight text-[var(--color-text-primary)]">
                Veterinary Decision Panel
              </h3>
            </div>

            {/* Training Decision */}
            <TrainingDecisionSection
              isUrgent={isUrgent}
              isFormDisabled={isFormDisabled}
              trainingDecision={trainingDecision}
              setTrainingDecision={setTrainingDecision}
              restrictionDetails={restrictionDetails}
              setRestrictionDetails={setRestrictionDetails}
              fieldErrors={fieldErrors}
              setFieldErrors={setFieldErrors}
              followUpDate={followUpDate}
              setFollowUpDate={setFollowUpDate}
              businessDateFromToday={businessDateFromToday}
            />

            {/* Follow-up */}
            <FollowUpSection
              isFormDisabled={isFormDisabled}
              careType={careType}
              schedulePurpose={schedulePurpose}
              followUpRequired={followUpRequired}
              followUpActive={followUpActive}
              scheduleFollowUp={scheduleFollowUp}
              setScheduleFollowUp={setScheduleFollowUp}
              followUpDate={followUpDate}
              setFollowUpDate={setFollowUpDate}
              followUpDescription={followUpDescription}
              setFollowUpDescription={setFollowUpDescription}
              fieldErrors={fieldErrors}
              setFieldErrors={setFieldErrors}
              businessDateFromToday={businessDateFromToday}
              tomorrowBusinessDate={tomorrowBusinessDate}
            />

            {/* Action Buttons for Desktop / Tablet (hidden on mobile, visible on >= 768px) */}
            <div className="space-y-2 border-t border-[var(--color-border)] pt-3 hidden md:block">
              {formError && <Notice tone="error">{formError}</Notice>}

              {/* Primary Purple Action Button */}
              <Button
                type="submit"
                variant="primary"
                loading={submitting}
                disabled={submitting || startingExam || isGated}
                icon="check"
                className="w-full"
              >
                Complete Examination
              </Button>

              {/* Save Draft Action */}
              <Button type="button" variant="tertiary" size="sm" className="w-full" disabled={submitting} onClick={handleSaveDraft}>
                Save draft locally
              </Button>
            </div>
          </div>
          </div>
        </div>

        {/* Mobile Fixed Footer Actions (< 768px per R2) */}
        <div className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-[var(--color-surface)]/95 backdrop-blur-md border-t border-[var(--color-border)] p-3 shadow-[var(--shadow-popover)] space-y-2">
          {formError && <Notice tone="error">{formError}</Notice>}
          <div className="flex items-center justify-between gap-2.5">
            <Button
              type="submit"
              variant="primary"
              loading={submitting}
              disabled={submitting || startingExam || isGated}
              icon="check"
              className="w-full"
            >
              Complete Examination
            </Button>
          </div>
        </div>
      </form>

      {/* Complete Examination Confirmation Dialog */}
      <ConfirmDialog
        open={showApproveConfirm}
        title="Submit Veterinary Examination?"
        description={
          <div className="space-y-2 text-sm">
            <p>
              You are certifying medical examination for <strong>{candidateName}</strong> with training decision{' '}
              <strong className="uppercase text-[var(--color-primary)]">{trainingDecision}</strong>.
            </p>
            {followUpActive && (
              <div className="rounded-[var(--radius-sm)] bg-[var(--color-warning-soft)] p-2 text-xs text-[var(--color-warning)] font-medium">
                Follow-up scheduled for <strong>{followUpDate}</strong>: {followUpDescription}.
              </div>
            )}
            <p className="text-xs text-[var(--color-text-secondary)]">
              {careType === 'INITIAL'
                ? 'This action writes the official clinical record and advances the admission to trainer review.'
                : 'This action completes only this care schedule and does not change the admission workflow.'}
            </p>
          </div>
        }
        confirmLabel="Confirm & Submit"
        cancelLabel="Back to Review"
        tone="primary"
        loading={submitting}
        onConfirm={() => executeSubmit()}
        onCancel={() => setShowApproveConfirm(false)}
      />
    </>
  );
}
