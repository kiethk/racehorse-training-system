'use client';

import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import { ApiError } from '@/services/api';
import { Button } from '@/components/ui/Button';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Icon, type IconName } from '@/components/ui/Icon';
import { Pill } from '@/components/ui/StatusBadge';
import { admissionsApi } from '../services/api';
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

const symptomOptions = [
  'No symptoms observed',
  'Cough',
  'Nasal discharge',
  'Elevated temperature',
  'Lameness',
  'Joint swelling',
  'Reduced flexion',
  'Hoof pain',
  'Skin lesion',
  'Poor appetite',
  'Digestive discomfort',
  'Neurological signs',
];

const diagnosisOptions = [
  'Clinically healthy',
  'Minor clinical condition',
  'Treatment required',
  'Follow-up examination required',
  'Infectious disease suspected',
  'Musculoskeletal condition suspected',
  'Medically unsuitable for admission',
];

const restrictionOptions = [
  'Walking only; no trotting, cantering, or galloping',
  'Light exercise only; maximum 20 minutes per session',
  'No strenuous exercise until veterinary re-examination',
  'Stall rest with hand-walking only',
  'Complete stall rest; no training activity',
  'Isolation protocol; no contact with other horses',
];



const followUpDescriptionOptions = [
  'Routine follow-up recheck examination',
  'Post-treatment clinical reassessment',
  'Lameness and gait reassessment',
  'Respiratory and infectious disease recheck',
  'Training restriction clearance examination',
];

interface TrainingDecisionOption {
  value: TrainingDecision;
  title: string;
  subtitle: string;
  icon: IconName;
  tone: 'success' | 'warning' | 'danger';
  activeBorder: string;
  activeBg: string;
}

const trainingDecisions: TrainingDecisionOption[] = [
  {
    value: 'ALLOWED',
    title: 'Training Allowed',
    subtitle: 'Medically cleared for athletic conditioning',
    icon: 'check',
    tone: 'success',
    activeBorder: 'border-[var(--color-success)] ring-1 ring-[var(--color-success)]',
    activeBg: 'bg-[var(--color-success-soft)]',
  },
  {
    value: 'RESTRICTED',
    title: 'Training Restricted',
    subtitle: 'Limited or modified conditioning only',
    icon: 'alert-triangle',
    tone: 'warning',
    activeBorder: 'border-[var(--color-warning)] ring-1 ring-[var(--color-warning)]',
    activeBg: 'bg-[var(--color-warning-soft)]',
  },
  {
    value: 'BLOCKED',
    title: 'Training Blocked',
    subtitle: 'Zero training permitted; stall rest',
    icon: 'lock',
    tone: 'danger',
    activeBorder: 'border-[var(--color-danger)] ring-1 ring-[var(--color-danger)]',
    activeBg: 'bg-[var(--color-danger-soft)]',
  },
];

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
  normalLabel: string;
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
    normalLabel: '37.2 – 38.3 °C',
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
    normalLabel: '28 – 44 bpm',
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
    normalLabel: '8 – 16 rpm',
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
    normalLabel: '400 – 600 kg',
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
    normalLabel: '4.0 – 6.0',
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

function getMetricBorderTone(
  valStr: string,
  normalMin: number,
  normalMax: number,
  criticalMin: number,
  criticalMax: number,
): { borderClass: string; statusLabel: string | null; tone: 'warning' | 'danger' | 'success' | null } {
  if (!valStr.trim()) {
    return {
      borderClass: 'border-[var(--color-border)] focus:border-[var(--color-primary)]',
      statusLabel: null,
      tone: null,
    };
  }
  const num = Number(valStr);
  if (Number.isNaN(num)) {
    return {
      borderClass: 'border-[var(--color-danger)] focus:border-[var(--color-danger)] ring-1 ring-[var(--color-danger)]',
      statusLabel: 'Invalid',
      tone: 'danger',
    };
  }
  if (num < criticalMin) {
    return {
      borderClass: 'border-[var(--color-danger)] focus:border-[var(--color-danger)] ring-1 ring-[var(--color-danger)]',
      statusLabel: 'Critical Low',
      tone: 'danger',
    };
  }
  if (num > criticalMax) {
    return {
      borderClass: 'border-[var(--color-danger)] focus:border-[var(--color-danger)] ring-1 ring-[var(--color-danger)]',
      statusLabel: 'Critical High',
      tone: 'danger',
    };
  }
  if (num < normalMin) {
    return {
      borderClass: 'border-[var(--color-warning)] focus:border-[var(--color-warning)] ring-1 ring-[var(--color-warning)]',
      statusLabel: 'Low',
      tone: 'warning',
    };
  }
  if (num > normalMax) {
    return {
      borderClass: 'border-[var(--color-warning)] focus:border-[var(--color-warning)] ring-1 ring-[var(--color-warning)]',
      statusLabel: 'Elevated',
      tone: 'warning',
    };
  }
  return {
    borderClass: 'border-[var(--color-success)] focus:border-[var(--color-success)]',
    statusLabel: 'Normal',
    tone: 'success',
  };
}

export interface VetReviewFormProps {
  admissionId: number;
  horseId?: number | null;
  candidateName?: string;
  quarantineStallCode?: string | null;
  careScheduleId?: number | null;
  careType?: CareType;
  scheduleStatus?: CareScheduleStatus;
  onStartExam?: () => Promise<void>;
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
  onStartExam,
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
    treatment.trim() !== '' ||
    notes.trim() !== '' ||
    trainingDecision !== 'ALLOWED' ||
    restrictionDetails.trim() !== '' ||
    scheduleFollowUp;

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
    if (availableDraft.trainingDecision) setTrainingDecision(availableDraft.trainingDecision);
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
    setDraftSavedTime(null);
  };

  // 15-second debounce auto-save when dirty (FR-CHUNG-93)
  useEffect(() => {
    if (!isDirty || isGated) return;
    const timer = setTimeout(() => {
      handleSaveDraft();
    }, 15000);
    return () => clearTimeout(timer);
  }, [isDirty, isGated, handleSaveDraft]);

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

    if (!physicalExamConfirmed) {
      errors.physicalExamConfirmed = 'In-person physical examination confirmation is required.';
    }
    if (isUrgent && !symptoms.trim()) {
      errors.symptoms = 'Detailed presenting symptoms are required for urgent care.';
    }
    if (!effectiveFindings) {
      errors.findings = 'Clinical examination findings are required.';
    }
    if (!effectiveDiagnosis) {
      errors.diagnosis = 'Clinical diagnosis is required.';
    }
    if (
      !isUrgent &&
      examMode === 'ABNORMAL' &&
      bodySystems.every(({ key }) => systemFindings[key] === 'NORMAL')
    ) {
      errors.systemFindings = 'Select at least one abnormal or not-examined body system.';
    }
    if (trainingDecision === 'RESTRICTED' || trainingDecision === 'BLOCKED') {
      if (!restrictionDetails.trim()) {
        errors.restrictionDetails = 'Restriction details are mandatory when training is restricted or blocked.';
      }
    }
    if (scheduleFollowUp) {
      if (!followUpDate) {
        errors.followUpDate = 'Follow-up date is required when scheduling follow-up care.';
      } else if (followUpDate < tomorrowBusinessDate()) {
        errors.followUpDate = 'Follow-up date must be at least one day in the future.';
      }
      if (!followUpDescription.trim()) {
        errors.followUpDescription = 'Follow-up care description is required.';
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
      setFormError('Please resolve all validation errors before submitting.');
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
      const msg = 'Please resolve all validation errors before submitting.';
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

    const nextScheduleRequest = scheduleFollowUp && followUpDate ? {
      horseId: horseId ?? undefined,
      admissionId,
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
          decision: 'APPROVED',
          trainingDecision,
          restrictionDetails: restrictionDetails.trim() || undefined,
          rejectAdmission: false,
          physicalExamConfirmed: true,
          findings: effectiveFindings,
          diagnosis: effectiveDiagnosis,
          treatment: treatment.trim() || undefined,
          symptoms: effectiveSymptoms || undefined,
          notes: notes.trim() || undefined,
          feedback: notes.trim() || undefined,
          nextSchedule: nextScheduleRequest,
          metrics: Object.keys(metricValues).length ? [metricValues] : undefined,
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
      <form onSubmit={handleFormSubmit} className="pb-24 md:pb-0">
        {/* Gate Warning when SCHEDULED */}
        {scheduleStatus === 'SCHEDULED' && (
          <div
            role="status"
            className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-[var(--radius-md)] border border-[var(--color-warning)] bg-[var(--color-warning-soft)] p-3 text-[13px] text-[var(--color-warning)]"
          >
            <div className="flex items-center gap-2">
              <Icon name="alert-triangle" size={18} className="shrink-0 text-[var(--color-warning)]" />
              <div>
                <strong className="block text-[13px] font-bold">
                  Care schedule is SCHEDULED
                </strong>
                <p className="text-[12px] opacity-90">
                  Physical examination must be started before clinical findings and decisions can be recorded.
                </p>
              </div>
            </div>
            {onStartExam && (
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

        {/* Global Form Error */}
        {formError && (
          <div
            role="alert"
            className="mb-4 flex items-start gap-2.5 rounded-[var(--radius-md)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-3 text-[13px] text-[var(--color-danger)]"
          >
            <Icon name="alert-triangle" size={16} className="mt-0.5 shrink-0" />
            <span>{formError}</span>
          </div>
        )}

        {/* Local Draft Available Prompt (FR-CHUNG-94) */}
        {availableDraft && (
          <div
            role="status"
            className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-[var(--radius-sm)] border border-[var(--color-primary)] bg-[var(--color-primary-soft)] px-3 py-2 text-[12px] text-[var(--color-text-primary)]"
          >
            <div className="flex items-center gap-2">
              <Icon name="clipboard" size={14} className="text-[var(--color-primary)] shrink-0" />
              <span>
                Tìm thấy bản nháp từ <strong>{availableDraft.timestamp}</strong>. Bạn có muốn khôi phục dữ liệu đã lưu không?
              </span>
            </div>
            <div className="flex items-center gap-2">
              <Button
                type="button"
                size="sm"
                variant="primary"
                onClick={handleRestoreDraft}
              >
                Khôi phục
              </Button>
              <Button
                type="button"
                size="sm"
                variant="secondary"
                onClick={handleDiscardAvailableDraft}
              >
                Xóa nháp
              </Button>
            </div>
          </div>
        )}

        {/* Draft Saved Status (FR-CHUNG-92) */}
        {draftSavedTime && !availableDraft && (
          <div className="mb-4 flex items-center justify-between rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] px-3 py-1.5 text-[11px] text-[var(--color-text-secondary)]">
            <span className="flex items-center gap-1.5 font-medium">
              <Icon name="check" size={12} className="text-[var(--color-success)]" />
              Đã lưu nháp lúc {draftSavedTime}
            </span>
            <button
              type="button"
              onClick={handleClearDraft}
              className="text-[var(--color-text-muted)] hover:text-[var(--color-danger)] font-medium transition-colors"
            >
              Xóa nháp
            </button>
          </div>
        )}

        {/* Main Two-Column Body: Form (~65%) & Sticky Decision Panel (~35%) */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          {/* LEFT COLUMN: Clinical Examination (~65% width = lg:col-span-8) */}
          <div className="lg:col-span-8 space-y-6">
            {/* Section 1: Patient Vitals & Telemetry */}
            <fieldset disabled={isFormDisabled} className="space-y-3">
              <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-2">
                <legend className="text-[12px] font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">
                  1. Patient Vitals &amp; Telemetry
                </legend>
                <span className="text-[11px] text-[var(--color-text-muted)]">
                  Standard equine reference ranges
                </span>
              </div>

              {/* Vitals Grid without heavy card wrappers */}
              <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-3 xl:grid-cols-5">
                {metricConfigs.map((cfg) => {
                  const borderInfo = getMetricBorderTone(
                    metrics[cfg.key],
                    cfg.normalMin,
                    cfg.normalMax,
                    cfg.criticalMin,
                    cfg.criticalMax,
                  );
                  return (
                    <div key={cfg.key} className="space-y-1">
                      <div className="flex items-center justify-between text-[11px]">
                        <label
                          htmlFor={`metric-${cfg.key}`}
                          className="font-medium text-[var(--color-text-primary)]"
                        >
                          {cfg.label}
                        </label>
                        {borderInfo.statusLabel && (
                          <span
                            className={`text-[10px] font-bold ${
                              borderInfo.tone === 'danger'
                                ? 'text-[var(--color-danger)]'
                                : borderInfo.tone === 'warning'
                                  ? 'text-[var(--color-warning)]'
                                  : 'text-[var(--color-success)]'
                            }`}
                          >
                            {borderInfo.statusLabel}
                          </span>
                        )}
                      </div>

                      <div className="relative">
                        <input
                          id={`metric-${cfg.key}`}
                          type="number"
                          min="0"
                          step={cfg.step}
                          placeholder={cfg.placeholder}
                          value={metrics[cfg.key]}
                          onChange={(e) =>
                            setMetrics((curr) => ({ ...curr, [cfg.key]: e.target.value }))
                          }
                          className={`w-full rounded-[var(--radius-sm)] border bg-[var(--color-surface)] px-2.5 py-1.5 pr-8 font-mono text-[13px] font-semibold text-[var(--color-text-primary)] outline-none transition-all ${borderInfo.borderClass}`}
                        />
                        <span className="pointer-events-none absolute right-2 top-1.5 text-[11px] text-[var(--color-text-muted)]">
                          {cfg.unit}
                        </span>
                      </div>

                      <span className="block text-[10px] text-[var(--color-text-muted)]">
                        {cfg.normalLabel}
                      </span>
                      {fieldErrors[cfg.key] && (
                        <span className="block text-[10px] font-medium text-[var(--color-danger)]">
                          {fieldErrors[cfg.key]}
                        </span>
                      )}
                    </div>
                  );
                })}
              </div>

              {/* Hydration & Notes inline */}
              <div className="grid gap-3 sm:grid-cols-2 pt-1">
                <div>
                  <label
                    htmlFor="metric-hydration"
                    className="mb-1 block text-[11px] font-medium text-[var(--color-text-secondary)]"
                  >
                    Hydration &amp; Mucous Membranes
                  </label>
                  <select
                    id="metric-hydration"
                    className="w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-1.5 text-[12px] text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]"
                    value={metrics.hydrationStatus}
                    onChange={(e) => setMetrics((curr) => ({ ...curr, hydrationStatus: e.target.value }))}
                  >
                    <option value="">Not recorded</option>
                    <option value="Normal (CRT < 2s, pink)">
                      Normal (CRT &lt; 2s, pink)
                    </option>
                    <option value="Mild dehydration">Mild dehydration</option>
                    <option value="Moderate dehydration">Moderate dehydration</option>
                    <option value="Severe dehydration">Severe dehydration</option>
                    <option value="Abnormal CRT / mucous membranes">
                      Abnormal mucous membranes / CRT
                    </option>
                  </select>
                </div>

                <div>
                  <label
                    htmlFor="metric-notes"
                    className="mb-1 block text-[11px] font-medium text-[var(--color-text-secondary)]"
                  >
                    Telemetry &amp; Observation Context
                  </label>
                  <input
                    id="metric-notes"
                    type="text"
                    maxLength={2000}
                    className="w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-1.5 text-[12px] text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]"
                    value={metrics.notes}
                    onChange={(e) => setMetrics((curr) => ({ ...curr, notes: e.target.value }))}
                    placeholder="e.g. Calm disposition, post-transport check"
                  />
                </div>
              </div>
            </fieldset>

            {/* Section 2: Clinical Findings & Diagnostics */}
            <fieldset disabled={isFormDisabled} className="space-y-4 border-t border-[var(--color-border)] pt-4">
              <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-2">
                <legend className="text-[12px] font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">
                  2. Clinical Findings &amp; Examination
                </legend>
                <span className="text-[11px] font-medium text-[var(--color-danger)]">
                  * Required
                </span>
              </div>

              {/* Compact Physical Exam Conducted In-Person Toggle */}
              <div className="flex items-center justify-between rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] px-3 py-2">
                <label className="flex cursor-pointer items-center gap-2">
                  <input
                    type="checkbox"
                    checked={physicalExamConfirmed}
                    onChange={(e) => {
                      setPhysicalExamConfirmed(e.target.checked);
                      if (fieldErrors.physicalExamConfirmed) {
                        setFieldErrors((errs) => ({ ...errs, physicalExamConfirmed: '' }));
                      }
                    }}
                    className="h-4 w-4 rounded accent-[var(--color-primary)]"
                  />
                  <span className="text-[12px] font-semibold text-[var(--color-text-primary)]">
                    Physical Exam Conducted In-Person
                  </span>
                </label>
                <span className="text-[11px] text-[var(--color-text-muted)]">
                  Quarantine facility: {quarantineStallCode || 'Stall verified'}
                </span>
              </div>
              {fieldErrors.physicalExamConfirmed && (
                <p className="text-[11px] font-medium text-[var(--color-danger)]">
                  {fieldErrors.physicalExamConfirmed}
                </p>
              )}

              {/* Urgent Mode vs Standard Progressive Disclosure */}
              {isUrgent ? (
                <div className="grid gap-3 sm:grid-cols-2">
                  <div>
                    <label
                      htmlFor="field-symptoms"
                      className="mb-1 block text-[11px] font-medium text-[var(--color-text-primary)]"
                    >
                      Urgent Presenting Symptoms <span className="text-[var(--color-danger)]">*</span>
                    </label>
                    <textarea
                      id="field-symptoms"
                      className="w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] p-2 text-[12px] outline-none focus:border-[var(--color-primary)]"
                      rows={3}
                      value={symptoms}
                      onChange={(e) => setSymptoms(e.target.value)}
                      placeholder="Describe onset, symptoms, and urgency..."
                    />
                    {fieldErrors.symptoms && (
                      <p className="text-[11px] font-medium text-[var(--color-danger)]">
                        {fieldErrors.symptoms}
                      </p>
                    )}
                  </div>
                  <div>
                    <label
                      htmlFor="field-findings"
                      className="mb-1 block text-[11px] font-medium text-[var(--color-text-primary)]"
                    >
                      Urgent Physical Findings <span className="text-[var(--color-danger)]">*</span>
                    </label>
                    <textarea
                      id="field-findings"
                      className="w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] p-2 text-[12px] outline-none focus:border-[var(--color-primary)]"
                      rows={3}
                      value={findings}
                      onChange={(e) => setFindings(e.target.value)}
                      placeholder="Document trauma, distress, or acute signs..."
                    />
                    {fieldErrors.findings && (
                      <p className="text-[11px] font-medium text-[var(--color-danger)]">
                        {fieldErrors.findings}
                      </p>
                    )}
                  </div>
                </div>
              ) : (
                <div className="space-y-3">
                  {/* Segmented Control: Normal | Abnormal */}
                  <div className="space-y-1.5">
                    <label className="block text-[11px] font-semibold text-[var(--color-text-secondary)]">
                      Clinical Status Assessment
                    </label>
                    <div className="inline-flex rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-0.5">
                      <button
                        type="button"
                        onClick={() => {
                          setExamMode('NORMAL');
                          setSystemFindings({ ...normalSystemFindings });
                          setSelectedSymptoms(['No symptoms observed']);
                          setStructuredDiagnosis('Clinically healthy');
                          setFieldErrors((errs) => ({ ...errs, systemFindings: '' }));
                        }}
                        className={`flex items-center gap-1.5 rounded-[var(--radius-sm)] px-4 py-1.5 text-[12px] font-bold transition-all ${
                          examMode === 'NORMAL'
                            ? 'bg-[var(--color-success)] text-white shadow-xs'
                            : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]'
                        }`}
                      >
                        <Icon name="check" size={14} />
                        Normal (Bình thường)
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          setExamMode('ABNORMAL');
                          setSelectedSymptoms([]);
                          setStructuredDiagnosis('Minor clinical condition');
                        }}
                        className={`flex items-center gap-1.5 rounded-[var(--radius-sm)] px-4 py-1.5 text-[12px] font-bold transition-all ${
                          examMode === 'ABNORMAL'
                            ? 'bg-[var(--color-warning)] text-white shadow-xs'
                            : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]'
                        }`}
                      >
                        <Icon name="alert-triangle" size={14} />
                        Abnormal (Bất thường)
                      </button>
                    </div>

                    {/* Inline Helper Text */}
                    <p className="text-[11px] text-[var(--color-text-muted)]">
                      {examMode === 'NORMAL'
                        ? 'All 8 body systems are recorded as normal. Findings and symptoms will be generated automatically.'
                        : 'Abnormal findings detected. Specify affected body systems and observed symptoms below.'}
                    </p>
                  </div>

                  {/* Progressive Disclosure Checklist for the 8 Body Systems (Only when Abnormal) */}
                  {examMode === 'ABNORMAL' && (
                    <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3.5 space-y-4">
                      <div>
                        <div className="flex items-center justify-between">
                          <h4 className="text-[12px] font-bold text-[var(--color-text-primary)]">
                            8 Body Systems Checklist
                          </h4>
                          <button
                            type="button"
                            onClick={() => setSystemFindings({ ...normalSystemFindings })}
                            className="text-[11px] font-semibold text-[var(--color-primary)] hover:underline"
                          >
                            Reset All to Normal
                          </button>
                        </div>
                        <div className="mt-2 grid grid-cols-2 gap-2 sm:grid-cols-4">
                          {bodySystems.map(({ key, label }) => {
                            const isAbnormal = systemFindings[key] !== 'NORMAL';
                            return (
                              <label key={key} className="space-y-1">
                                <span className="block text-[10px] font-medium text-[var(--color-text-secondary)]">
                                  {label}
                                </span>
                                <select
                                  value={systemFindings[key]}
                                  onChange={(e) => {
                                    setSystemFindings((curr) => ({
                                      ...curr,
                                      [key]: e.target.value as FindingLevel,
                                    }));
                                    setFieldErrors((errs) => ({ ...errs, systemFindings: '' }));
                                  }}
                                  className={`w-full rounded-[var(--radius-sm)] px-2 py-1 text-[11px] outline-none border transition-colors ${
                                    isAbnormal
                                      ? 'border-[var(--color-warning)] bg-[var(--color-warning-soft)] text-[var(--color-warning)] font-semibold'
                                      : 'border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-text-primary)]'
                                  }`}
                                >
                                  {(Object.entries(findingLevelLabels) as Array<[FindingLevel, string]>).map(
                                    ([val, optLabel]) => (
                                      <option key={val} value={val}>
                                        {optLabel}
                                      </option>
                                    ),
                                  )}
                                </select>
                              </label>
                            );
                          })}
                        </div>
                        {fieldErrors.systemFindings && (
                          <p className="mt-1.5 text-[11px] font-medium text-[var(--color-danger)]">
                            {fieldErrors.systemFindings}
                          </p>
                        )}
                      </div>

                      {/* Symptoms Tags */}
                      <div className="border-t border-[var(--color-border)] pt-3">
                        <span className="block text-[11px] font-bold text-[var(--color-text-primary)] mb-2">
                          Observed Symptoms Checklist
                        </span>
                        <div className="flex flex-wrap gap-1.5">
                          {symptomOptions.map((symptom) => {
                            const selected = selectedSymptoms.includes(symptom);
                            return (
                              <button
                                key={symptom}
                                type="button"
                                onClick={() =>
                                  setSelectedSymptoms((curr) => {
                                    if (symptom === 'No symptoms observed') {
                                      return ['No symptoms observed'];
                                    }
                                    const withoutNone = curr.filter((s) => s !== 'No symptoms observed');
                                    return selected
                                      ? withoutNone.filter((s) => s !== symptom)
                                      : [...withoutNone, symptom];
                                  })
                                }
                                className={`rounded-full border px-2.5 py-1 text-[11px] font-medium transition-colors ${
                                  selected
                                    ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)] text-[var(--color-primary)] font-semibold'
                                    : 'border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-text-secondary)] hover:border-[var(--color-border-strong)]'
                                }`}
                              >
                                {selected ? '✓ ' : ''}
                                {symptom}
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* Side-by-Side Assessment & Treatment Inputs */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3 pt-2">
                <div>
                  <label
                    htmlFor="field-assessment-diag"
                    className="mb-1 block text-[11px] font-semibold text-[var(--color-text-primary)]"
                  >
                    Clinical Assessment &amp; Diagnosis <span className="text-[var(--color-danger)]">*</span>
                  </label>
                  {isUrgent ? (
                    <input
                      id="field-assessment-diag"
                      type="text"
                      className="w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-1.5 text-[12px] outline-none focus:border-[var(--color-primary)]"
                      value={diagnosis}
                      onChange={(e) => setDiagnosis(e.target.value)}
                      placeholder="Working diagnosis..."
                    />
                  ) : examMode === 'NORMAL' ? (
                    <div className="flex h-9 items-center gap-2 rounded-[var(--radius-sm)] border border-[var(--color-success)] bg-[var(--color-success-soft)] px-3 text-[12px] font-semibold text-[var(--color-success)]">
                      <Icon name="check" size={14} />
                      Clinically Healthy
                    </div>
                  ) : (
                    <select
                      id="field-assessment-diag"
                      className="w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-2 text-[12px] outline-none focus:border-[var(--color-primary)]"
                      value={structuredDiagnosis}
                      onChange={(e) => setStructuredDiagnosis(e.target.value)}
                    >
                      {diagnosisOptions
                        .filter((d) => d !== 'Clinically healthy')
                        .map((opt) => (
                          <option key={opt} value={opt}>
                            {opt}
                          </option>
                        ))}
                    </select>
                  )}
                  {fieldErrors.diagnosis && (
                    <p className="mt-1 text-[11px] font-medium text-[var(--color-danger)]">
                      {fieldErrors.diagnosis}
                    </p>
                  )}
                </div>

                <div>
                  <label
                    htmlFor="field-treatment"
                    className="mb-1 block text-[11px] font-semibold text-[var(--color-text-primary)]"
                  >
                    Treatment &amp; Care Protocol (Optional)
                  </label>
                  <textarea
                    id="field-treatment"
                    rows={2}
                    className="w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] p-2 text-[12px] outline-none focus:border-[var(--color-primary)] resize-y"
                    value={treatment}
                    onChange={(e) => setTreatment(e.target.value)}
                    placeholder="Enter prescribed medications, bandages, or therapy..."
                  />
                </div>
              </div>

              {/* Internal Clinical Notes */}
              <div>
                <label
                  htmlFor="field-notes"
                  className="mb-1 block text-[11px] font-semibold text-[var(--color-text-primary)]"
                >
                  Veterinarian Notes &amp; Stable Directives (Optional)
                </label>
                <textarea
                  id="field-notes"
                  rows={2}
                  className="w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] p-2 text-[12px] outline-none focus:border-[var(--color-primary)] resize-y"
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Internal notes, groom handling directives, or dietary recommendations..."
                />
              </div>
            </fieldset>
          </div>

          {/* RIGHT COLUMN: Sticky Decision Panel (~35% width = lg:col-span-4) */}
          <div className="lg:col-span-4 lg:sticky lg:top-0 lg:max-h-[calc(100vh-140px)] lg:overflow-y-auto scroll-slim space-y-4 rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-sm">
            <div className="border-b border-[var(--color-border)] pb-2.5">
              <h3 className="text-[13px] font-bold text-[var(--color-text-primary)]">
                Veterinary Decision Panel
              </h3>
              <p className="text-[11px] text-[var(--color-text-muted)]">
                Authorizes or restricts athletic activity for {candidateName}.
              </p>
            </div>

            {/* Streamlined Training Decision Options */}
            <div className="space-y-2">
              <label className="block text-[11px] font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">
                Training Clearance
              </label>

              <div className="space-y-2">
                {trainingDecisions.map((opt) => {
                  const isSelected = trainingDecision === opt.value;
                  return (
                    <label
                      key={opt.value}
                      className={`flex cursor-pointer items-start gap-2.5 rounded-[var(--radius-md)] border p-2.5 transition-all ${
                        isSelected
                          ? `${opt.activeBorder} ${opt.activeBg} shadow-2xs`
                          : 'border-[var(--color-border)] bg-[var(--color-surface)] hover:bg-[var(--color-surface-subtle)]'
                      }`}
                    >
                      <input
                        type="radio"
                        name="trainingDecision"
                        value={opt.value}
                        checked={isSelected}
                        onChange={() => {
                          setTrainingDecision(opt.value);
                          if (fieldErrors.restrictionDetails) {
                            setFieldErrors((errs) => ({ ...errs, restrictionDetails: '' }));
                          }
                        }}
                        className="mt-0.5 h-4 w-4 accent-[var(--color-primary)]"
                      />
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center justify-between gap-1">
                          <span className="text-[12px] font-bold text-[var(--color-text-primary)]">
                            {opt.title}
                          </span>
                          <Pill tone={opt.tone} size="sm" icon={opt.icon}>
                            {opt.value}
                          </Pill>
                        </div>
                        <p className="mt-0.5 text-[10px] text-[var(--color-text-secondary)]">
                          {opt.subtitle}
                        </p>
                      </div>
                    </label>
                  );
                })}
              </div>
            </div>

            {/* Expanding Restriction Details ONLY when RESTRICTED or BLOCKED */}
            {(trainingDecision === 'RESTRICTED' || trainingDecision === 'BLOCKED') && (
              <div className="rounded-[var(--radius-md)] border border-[var(--color-warning)] bg-[var(--color-warning-soft)] p-3 space-y-1.5 animate-in fade-in duration-200">
                <label
                  htmlFor="field-restriction-details"
                  className="block text-[11px] font-bold text-[var(--color-warning)]"
                >
                  Mandatory Restriction Protocol <span className="text-[var(--color-danger)]">*</span>
                </label>
                <p className="text-[10px] text-[var(--color-text-secondary)]">
                  Specify allowable gait limits or strict stall rest conditions.
                </p>
                {isUrgent ? (
                  <textarea
                    id="field-restriction-details"
                    rows={2}
                    className="w-full rounded-[var(--radius-sm)] border border-[var(--color-warning)] bg-[var(--color-surface)] p-2 text-[12px] outline-none focus:border-[var(--color-primary)]"
                    value={restrictionDetails}
                    onChange={(e) => setRestrictionDetails(e.target.value)}
                    placeholder="Describe restriction rules..."
                  />
                ) : (
                  <select
                    id="field-restriction-details"
                    className="w-full rounded-[var(--radius-sm)] border border-[var(--color-warning)] bg-[var(--color-surface)] px-2 py-1.5 text-[11px] outline-none"
                    value={restrictionDetails}
                    onChange={(e) => {
                      setRestrictionDetails(e.target.value);
                      if (fieldErrors.restrictionDetails) {
                        setFieldErrors((errs) => ({ ...errs, restrictionDetails: '' }));
                      }
                    }}
                  >
                    <option value="">Select restriction protocol</option>
                    {restrictionOptions.map((opt) => (
                      <option key={opt} value={opt}>
                        {opt}
                      </option>
                    ))}
                  </select>
                )}
                {fieldErrors.restrictionDetails && (
                  <p className="text-[10px] font-medium text-[var(--color-danger)]">
                    {fieldErrors.restrictionDetails}
                  </p>
                )}
              </div>
            )}

            {/* Inline Schedule Follow-up Toggle */}
            <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3 space-y-2">
              <label className="flex cursor-pointer items-center justify-between">
                <span className="text-[12px] font-semibold text-[var(--color-text-primary)]">
                  Schedule Follow-up Exam
                </span>
                <input
                  type="checkbox"
                  checked={scheduleFollowUp}
                  onChange={(e) => {
                    const checked = e.target.checked;
                    setScheduleFollowUp(checked);
                    if (checked && !followUpDate) {
                      setFollowUpDate(businessDateFromToday(7));
                    }
                  }}
                  className="h-4 w-4 rounded accent-[var(--color-primary)]"
                />
              </label>

              {scheduleFollowUp && (
                <div className="space-y-2 border-t border-[var(--color-border)] pt-2 animate-in fade-in duration-200">
                  <div>
                    <label
                      htmlFor="field-followup-date"
                      className="mb-1 block text-[10px] font-medium text-[var(--color-text-secondary)]"
                    >
                      Target Date <span className="text-[var(--color-danger)]">*</span>
                    </label>
                    <input
                      id="field-followup-date"
                      type="date"
                      min={tomorrowBusinessDate()}
                      value={followUpDate}
                      onChange={(e) => {
                        setFollowUpDate(e.target.value);
                        if (fieldErrors.followUpDate) {
                          setFieldErrors((errs) => ({ ...errs, followUpDate: '' }));
                        }
                      }}
                      className="w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-2 py-1 text-[11px] font-mono outline-none"
                    />
                    <div className="mt-1 flex gap-1">
                      {[3, 7, 14].map((days) => {
                        const iso = businessDateFromToday(days);
                        return (
                          <button
                            key={days}
                            type="button"
                            onClick={() => setFollowUpDate(iso)}
                            className="rounded border border-[var(--color-border)] bg-[var(--color-surface)] px-1.5 py-0.5 text-[9px] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]"
                          >
                            +{days}d
                          </button>
                        );
                      })}
                    </div>
                    {fieldErrors.followUpDate && (
                      <p className="mt-0.5 text-[10px] font-medium text-[var(--color-danger)]">
                        {fieldErrors.followUpDate}
                      </p>
                    )}
                  </div>

                  <div>
                    <label
                      htmlFor="field-followup-desc"
                      className="mb-1 block text-[10px] font-medium text-[var(--color-text-secondary)]"
                    >
                      Care Procedure <span className="text-[var(--color-danger)]">*</span>
                    </label>
                    <select
                      id="field-followup-desc"
                      value={followUpDescription}
                      onChange={(e) => {
                        setFollowUpDescription(e.target.value);
                        if (fieldErrors.followUpDescription) {
                          setFieldErrors((errs) => ({ ...errs, followUpDescription: '' }));
                        }
                      }}
                      className="w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-2 py-1 text-[11px] outline-none"
                    >
                      {followUpDescriptionOptions.map((opt) => (
                        <option key={opt} value={opt}>
                          {opt}
                        </option>
                      ))}
                    </select>
                    {fieldErrors.followUpDescription && (
                      <p className="mt-0.5 text-[10px] font-medium text-[var(--color-danger)]">
                        {fieldErrors.followUpDescription}
                      </p>
                    )}
                  </div>
                </div>
              )}
            </div>

            {/* Action Buttons for Desktop / Tablet (hidden on mobile, visible on >= 768px) */}
            <div className="space-y-2 border-t border-[var(--color-border)] pt-3 hidden md:block">
              {formError && (
                <div
                  role="alert"
                  className="flex items-start gap-2 rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-2 text-[11px] text-[var(--color-danger)]"
                >
                  <Icon name="alert-triangle" size={14} className="mt-0.5 shrink-0" />
                  <span className="leading-tight">{formError}</span>
                </div>
              )}

              {/* Primary Purple Action Button */}
              <Button
                type="submit"
                variant="primary"
                loading={submitting}
                disabled={submitting || startingExam || isGated}
                icon="check"
                className="w-full justify-center bg-[var(--color-primary)] text-white hover:bg-[var(--color-primary-hover)]"
              >
                Complete Examination
              </Button>

              {/* Save Draft Action */}
              <button
                type="button"
                disabled={submitting}
                onClick={handleSaveDraft}
                className="w-full text-center text-[11px] font-medium text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] hover:underline pt-1"
              >
                Save Draft Locally
              </button>
            </div>

            {/* Summary Notice */}
            <div className="rounded-[var(--radius-sm)] bg-[var(--color-surface-subtle)] p-2.5 text-[11px] text-[var(--color-text-muted)] leading-relaxed">
              <span className="font-semibold text-[var(--color-text-secondary)]">Outcome:</span>
              <ul className="mt-1 space-y-0.5 list-disc pl-3.5">
                <li>Permanently links health examination record.</li>
                <li>Advances admission to Head Trainer review stage.</li>
                <li>Quarantine stall stays locked until final manager clearance.</li>
              </ul>
            </div>
          </div>
        </div>

        {/* Mobile Fixed Footer Actions (< 768px per R2) */}
        <div className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-[var(--color-surface)]/95 backdrop-blur-md border-t border-[var(--color-border)] p-3 shadow-lg space-y-2">
          {formError && (
            <div
              role="alert"
              className="flex items-start gap-2 rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-2 text-[11px] text-[var(--color-danger)]"
            >
              <Icon name="alert-triangle" size={14} className="mt-0.5 shrink-0" />
              <span className="leading-tight">{formError}</span>
            </div>
          )}
          <div className="flex items-center justify-between gap-2.5">
            <Button
              type="submit"
              variant="primary"
              loading={submitting}
              disabled={submitting || startingExam || isGated}
              icon="check"
              className="w-full justify-center bg-[var(--color-primary)] text-white hover:bg-[var(--color-primary-hover)] text-[12px] py-2"
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
          <div className="space-y-2 text-[13px]">
            <p>
              You are certifying medical examination for <strong>{candidateName}</strong> with training decision{' '}
              <strong className="uppercase text-[var(--color-primary)]">{trainingDecision}</strong>.
            </p>
            {scheduleFollowUp && (
              <div className="rounded-[var(--radius-sm)] bg-[var(--color-warning-soft)] p-2 text-[12px] text-[var(--color-warning)] font-medium">
                📅 Follow-up scheduled for <strong>{followUpDate}</strong>: {followUpDescription}.
              </div>
            )}
            <p className="text-[12px] text-[var(--color-text-secondary)]">
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
