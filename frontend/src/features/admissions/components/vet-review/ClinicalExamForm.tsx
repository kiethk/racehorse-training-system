'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useImperativeHandle,
  useRef,
  useState,
  type FormEvent,
  type ReactNode,
} from 'react';
import { z } from 'zod';
import { VitalsSection } from './VitalsSection';
import { DiagnosisSection } from './DiagnosisSection';
import { TrainingDecisionSection } from './TrainingDecisionSection';
import { FollowUpMergeNotice, FollowUpSection } from './FollowUpSection';
import {
  PrescriptionSection,
  toPrescriptionRequests,
  computeSuggestedRecheckDate,
  type PrescriptionFormItem,
} from '@/features/medical';
import { ActiveConditionsSection } from './ActiveConditionsSection';
import { ApiError } from '@/services/api';
import { Button } from '@/components/ui/Button';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Notice } from '@/components/ui/Notice';
import { Icon } from '@/components/ui/Icon';
import { Input, Textarea, ChoiceInput } from '@/components/ui/Input';
import { FormField } from '@/components/ui/FormField';
import { formatDate, formatDateTime } from '@/lib/display';
import { admissionsApi } from '../../services/api';
import {
  ExamVaccinationSection,
  toVaccinationRequests,
  validateVaccinationDrafts,
  type VaccinationDraft,
} from '@/features/care/components/preventive/ExamVaccinationSection';
import { randomizeUrgentVitals, pickFindingScenario } from './urgentSimulation';
import {
  emptyMetrics,
  metricConfigs,
  normalSystemFindings,
  type MetricField,
  type ExamMode,
  type BodySystemKey,
} from './constants';
import { toast } from '@/lib/toast';
import {
  EXAM_VARIANT_CONFIG,
  sixVitalsRequired,
  validateSixVitals,
  tier1VerdictEnabled,
  type ExamVariant,
} from './examVariantConfig';
import { healthApi } from '@/features/health/services/healthApi';
import type { HorseBodyRegion, InjuryRecord } from '@/features/health/types';
import {
  InjuryMarkerPanel,
  buildInjuryMarkerRequests,
  type InjuryMarker,
} from './InjuryMarkerPanel';
import {
  resolveInjuryWorldPosition,
  resolveInjuryNormal,
} from '@/features/vet/lib/injuryZones';
import {
  admissionAsideClassName,
  admissionDetailGridClassName,
} from '../../shared/components/AdmissionDetailLayout';
import type {
  CareType,
  CareSchedule,
  CareScheduleStatus,
  CompleteCareScheduleRequest,
  CreateInjuryMarkerRequest,
  HorseHealthMetricRequest,
  TrainingDecision,
  VetReviewDecision,
  VetReviewRequest,
  VetReviewResponse,
} from '../../types';

const BUSINESS_TIME_ZONE = 'Asia/Ho_Chi_Minh';

export function businessDateFromToday(dayOffset: number) {
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

function newIdempotencyKey() {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `follow-up-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

export interface ClinicalExamFormHandle {
  appendToFindings: (line: string) => void;
}

export interface ClinicalExamFormProps {
  variant: ExamVariant;
  admissionId: number;
  horseId?: number | null;
  candidateName?: string;
  careScheduleId?: number | null;
  careType?: CareType;
  scheduleStatus?: CareScheduleStatus;
  schedulePurpose?: 'FOLLOW_UP' | 'PREVENTIVE';
  /** The visit was opened by an incident report, so Follow-up rules do not apply. */
  fromIncident?: boolean;
  /** Set when another exam decides this horse's block, so this visit cannot lift it. */
  blockHeldUntil?: string | null;
  /** The horse's waiting Follow-up: a new date merges into it. */
  waitingFollowUpAt?: string | null;
  onStartExam?: () => Promise<void>;
  asideTop?: ReactNode;
  onSuccess: (
    result: VetReviewResponse | CareSchedule,
    completionKind: 'INITIAL' | 'CARE_SCHEDULE',
    request: VetReviewRequest | CompleteCareScheduleRequest,
  ) => void;
  onDirtyChange?: (dirty: boolean) => void;
  prefillFindings?: string | null;
  injuryMarkers?: CreateInjuryMarkerRequest[];
}

export const ClinicalExamForm = forwardRef<ClinicalExamFormHandle, ClinicalExamFormProps>(
  function ClinicalExamForm(props, ref) {
    const {
      variant,
      admissionId,
      horseId,
      candidateName = 'Candidate horse',
      careScheduleId,
      careType = 'INITIAL',
      scheduleStatus,
      schedulePurpose,
      fromIncident,
      blockHeldUntil,
      waitingFollowUpAt,
      onStartExam,
      asideTop,
      onSuccess,
      onDirtyChange,
      prefillFindings,
      injuryMarkers,
    } = props;

    const config = EXAM_VARIANT_CONFIG[variant];
    const isPrescriptionActive = config.prescription || careType === 'ROUTINE' || careType === 'URGENT';

    const draftStorageKey = careScheduleId
      ? `draft:care-schedule:${careScheduleId}`
      : `draft:admission:${admissionId}`;

    const showTier1 = tier1VerdictEnabled(variant, careType);
    const [verdict, setVerdict] = useState<VetReviewDecision>('ACCEPT');
    const [rejectReason, setRejectReason] = useState('');

    const [trainingDecision, setTrainingDecision] = useState<TrainingDecision>(careType === 'URGENT' ? 'BLOCKED' : config.defaultDecision);
    const [restrictionDetails, setRestrictionDetails] = useState('');
    const [scheduleFollowUp, setScheduleFollowUp] = useState(false);
    const defaultFollowUpDate = variant === 'URGENT' ? businessDateFromToday(3) : '';
    const defaultFollowUpDescription =
      variant === 'URGENT' ? 'Follow-up after the urgent examination' : 'Routine follow-up examination';
    const [followUpDate, setFollowUpDate] = useState(defaultFollowUpDate);
    const [followUpDescription, setFollowUpDescription] = useState(defaultFollowUpDescription);
    const [followUpIdempotencyKey, setFollowUpIdempotencyKey] = useState(newIdempotencyKey);

    const [markers, setMarkers] = useState<InjuryMarker[]>([]);
    const [bodyRegions, setBodyRegions] = useState<HorseBodyRegion[]>([]);
    const [anatomyTab, setAnatomyTab] = useState<'form' | 'anatomy3d'>('form');

    const show3DTab =
      (variant === 'STANDARD' && careType === 'INITIAL' && trainingDecision === 'BLOCKED') ||
      variant === 'URGENT';

    const [physicalExamConfirmed, setPhysicalExamConfirmed] = useState(true);
    const [examMode, setExamMode] = useState<ExamMode>('NORMAL');
    const [systemFindings, setSystemFindings] = useState<Record<BodySystemKey, string>>({
      ...normalSystemFindings,
    });
    const [structuredDiagnosis, setStructuredDiagnosis] = useState('Clinically healthy');
    const [abnormalFindings, setAbnormalFindings] = useState('');
    const [findings, setFindings] = useState('');
    const [diagnosis, setDiagnosis] = useState('');

    const [metrics, setMetrics] = useState<Record<MetricField, string>>(emptyMetrics);

    const [activeInjuries, setActiveInjuries] = useState<InjuryRecord[]>([]);
    const [resolvedConditionIds, setResolvedConditionIds] = useState<number[]>([]);
    const [prescriptions, setPrescriptions] = useState<PrescriptionFormItem[]>([]);
    const [generalCareInstructions, setGeneralCareInstructions] = useState('');
    const [vaccinationDrafts, setVaccinationDrafts] = useState<VaccinationDraft[]>([]);
    const [deferVaccinationDays, setDeferVaccinationDays] = useState<number | null>(null);
    const [vaccinationErrors, setVaccinationErrors] = useState<Record<string, string>>({});

    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
    const [formError, setFormError] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [startingExam, setStartingExam] = useState(false);
    const [showApproveConfirm, setShowApproveConfirm] = useState(false);
    const [draftSavedTime, setDraftSavedTime] = useState<string | null>(null);
    const [availableDraft, setAvailableDraft] = useState<{
      metrics?: Record<MetricField, string>;
      examMode?: ExamMode;
      abnormalFindings?: string;
      systemFindings?: Record<BodySystemKey, string>;
      structuredDiagnosis?: string;
      findings?: string;
      diagnosis?: string;
      trainingDecision?: TrainingDecision;
      restrictionDetails?: string;
      scheduleFollowUp?: boolean;
      followUpDate?: string;
      followUpDescription?: string;
      followUpIdempotencyKey?: string;
      markers?: InjuryMarker[];
      resolvedConditionIds?: number[];
      timestamp: string;
    } | null>(() => {
      if (!config.draft || typeof window === 'undefined') return null;
      try {
        const fallbackKey = careScheduleId ? null : `rtms_vet_draft_${admissionId}`;
        const saved =
          localStorage.getItem(draftStorageKey) ||
          (fallbackKey ? localStorage.getItem(fallbackKey) : null);
        if (saved) {
          const parsed = JSON.parse(saved);
          if (parsed && parsed.timestamp) {
            return parsed;
          }
        }
      } catch {
        // Ignore storage read errors
      }
      return null;
    });

    const inFlight = useRef(false);
    const prefillApplied = useRef(false);

    useImperativeHandle(ref, () => ({
      appendToFindings: (line: string) => {
        setFindings((prev) => (prev ? `${prev}\n${line}` : line));
      },
    }));

    useEffect(() => {
      if (prefillFindings && !prefillApplied.current) {
        prefillApplied.current = true;
        setFindings((prev) => prev || prefillFindings);
        setAbnormalFindings((prev) => prev || prefillFindings);
      }
    }, [prefillFindings]);

    useEffect(() => {
      let active = true;
      healthApi
        .getHorseBodyRegions()
        .then((regions) => {
          if (active) setBodyRegions(regions);
        })
        .catch(() => {});
      return () => {
        active = false;
      };
    }, []);

    useEffect(() => {
      if (!horseId) return;
      let active = true;
      healthApi
        .getActiveInjuries(horseId)
        .then((injuries) => {
          if (!active) return;
          setActiveInjuries(injuries);
          if (config.markersEnabled && injuries.length > 0) {
            setMarkers((prev) =>
              prev.length > 0
                ? prev
                : injuries.map((inj, idx) => {
                    const worldPos = resolveInjuryWorldPosition(inj);
                    const normal =
                      inj.normalX != null && inj.normalY != null && inj.normalZ != null
                        ? resolveInjuryNormal(
                            [Number(inj.normalX), Number(inj.normalY), Number(inj.normalZ)],
                            worldPos,
                          )
                        : resolveInjuryNormal(undefined, worldPos);
                    return {
                      id: String(inj.id),
                      position: worldPos,
                      normal,
                      zoneId: inj.zoneId ?? undefined,
                      injuryType: inj.injuryType || `Injury ${idx + 1}`,
                      bodyRegionId: inj.bodyRegionId ?? null,
                      severity: inj.severity || 'MODERATE',
                      description: inj.description ?? undefined,
                    };
                  }),
            );
          }
        })
        .catch(() => {});
      return () => {
        active = false;
      };
    }, [horseId, config.markersEnabled]);

    useEffect(() => {
      if (!show3DTab && anatomyTab === 'anatomy3d') {
        setAnatomyTab('form');
      }
    }, [show3DTab, anatomyTab]);

    // AD-022 / AD-026: DELAYED and OVERDUE exams are still open and must remain editable so the vet can submit findings.
    const isGated =
      scheduleStatus != null &&
      scheduleStatus !== 'IN_PROGRESS' &&
      scheduleStatus !== 'DELAYED' &&
      scheduleStatus !== 'OVERDUE';
    // A visit opened by an incident report needs a Follow-up only when the horse stays blocked.
    const followUpRequired = !fromIncident || trainingDecision === 'BLOCKED';
    const followUpActive = scheduleFollowUp || followUpRequired;
    const isFormDisabled = submitting || startingExam || isGated;

    const generatedFindings =
      examMode === 'NORMAL'
        ? 'Physical examination completed in person. General condition, respiratory, cardiovascular, musculoskeletal, neurological, skin and coat, hooves, and digestive systems were within normal limits.'
        : abnormalFindings.trim();
    const effectiveFindings =
      config.section2Mode === 'freeText' ? findings.trim() : generatedFindings;
    const effectiveDiagnosis =
      config.section2Mode === 'freeText' ? diagnosis.trim() : structuredDiagnosis;

    const isDirty =
      showTier1 && verdict === 'REJECT'
        ? rejectReason.trim().length > 0
        : metrics.heartRate !== '' ||
          metrics.temperature !== '' ||
          metrics.weight !== '' ||
          metrics.respiratoryRate !== '' ||
          metrics.bodyConditionScore !== '' ||
          metrics.hydrationStatus !== '' ||
          metrics.notes !== '' ||
          findings.trim() !== '' ||
          diagnosis.trim() !== '' ||
          examMode !== 'NORMAL' ||
          Object.values(systemFindings).some((f) => f !== 'NORMAL') ||
          structuredDiagnosis !== 'Clinically healthy' ||
          trainingDecision !== config.defaultDecision ||
          restrictionDetails.trim() !== '' ||
          scheduleFollowUp ||
          followUpDate !== '' ||
          markers.length > 0;

    useEffect(() => {
      onDirtyChange?.(isDirty);
    }, [isDirty, onDirtyChange]);

    const handleRestoreDraft = () => {
      if (!availableDraft) return;
      if (availableDraft.metrics) setMetrics(availableDraft.metrics);
      if (availableDraft.examMode) setExamMode(availableDraft.examMode);
      if (availableDraft.abnormalFindings !== undefined)
        setAbnormalFindings(availableDraft.abnormalFindings);
      if (availableDraft.systemFindings) setSystemFindings(availableDraft.systemFindings);
      if (availableDraft.structuredDiagnosis)
        setStructuredDiagnosis(availableDraft.structuredDiagnosis);
      if (availableDraft.findings !== undefined) setFindings(availableDraft.findings);
      if (availableDraft.diagnosis !== undefined) setDiagnosis(availableDraft.diagnosis);
      if (availableDraft.trainingDecision) {
        setTrainingDecision(availableDraft.trainingDecision === 'ALLOWED' ? 'ALLOWED' : 'BLOCKED');
      }
      if (availableDraft.restrictionDetails !== undefined)
        setRestrictionDetails(availableDraft.restrictionDetails);
      if (availableDraft.scheduleFollowUp !== undefined)
        setScheduleFollowUp(availableDraft.scheduleFollowUp);
      if (availableDraft.followUpDate !== undefined) setFollowUpDate(availableDraft.followUpDate);
      if (availableDraft.followUpDescription !== undefined)
        setFollowUpDescription(availableDraft.followUpDescription);
      if (availableDraft.followUpIdempotencyKey)
        setFollowUpIdempotencyKey(availableDraft.followUpIdempotencyKey);
      if (availableDraft.markers && Array.isArray(availableDraft.markers)) {
        setMarkers(availableDraft.markers);
      }
      if (availableDraft.resolvedConditionIds) {
        setResolvedConditionIds(availableDraft.resolvedConditionIds);
      }
      setDraftSavedTime(availableDraft.timestamp);
      setAvailableDraft(null);
    };

    const handleDiscardAvailableDraft = () => {
      if (!config.draft) return;
      try {
        localStorage.removeItem(draftStorageKey);
        localStorage.removeItem(`rtms_vet_draft_${admissionId}`);
      } catch {
        // Ignore storage errors in restricted environments
      }
      setAvailableDraft(null);
    };

    const handleSaveDraft = useCallback(() => {
      if (
        !config.draft ||
        typeof window === 'undefined' ||
        (showTier1 && verdict === 'REJECT')
      ) {
        return;
      }
      try {
        const timestamp = new Date().toISOString();
        const payload = {
          metrics,
          examMode,
          abnormalFindings,
          systemFindings,
          structuredDiagnosis,
          findings,
          diagnosis,
          trainingDecision,
          restrictionDetails,
          scheduleFollowUp,
          followUpDate,
          followUpDescription,
          followUpIdempotencyKey,
          markers,
          resolvedConditionIds,
          timestamp,
        };
        localStorage.setItem(draftStorageKey, JSON.stringify(payload));
        if (!careScheduleId) localStorage.setItem(`rtms_vet_draft_${admissionId}`, JSON.stringify(payload));
        setDraftSavedTime(timestamp);
      } catch {
        // Storage write error
      }
    }, [
      config.draft,
      showTier1,
      verdict,
      admissionId,
      careScheduleId,
      draftStorageKey,
      metrics,
      examMode,
      abnormalFindings,
      systemFindings,
      structuredDiagnosis,
      findings,
      diagnosis,
      trainingDecision,
      restrictionDetails,
      scheduleFollowUp,
      followUpDate,
      followUpDescription,
      followUpIdempotencyKey,
      markers,
      resolvedConditionIds,
    ]);

    const handleClearDraft = () => {
      if (!config.draft) return;
      try {
        localStorage.removeItem(draftStorageKey);
        localStorage.removeItem(`rtms_vet_draft_${admissionId}`);
      } catch {
        // Ignore storage errors in restricted environments
      }
      setMetrics(emptyMetrics);
      setExamMode('NORMAL');
      setAbnormalFindings('');
      setSystemFindings({ ...normalSystemFindings });
      setStructuredDiagnosis('Clinically healthy');
      setFindings('');
      setDiagnosis('');
      setTrainingDecision(config.defaultDecision);
      setRestrictionDetails('');
      setScheduleFollowUp(false);
      setFollowUpDate(defaultFollowUpDate);
      setFollowUpDescription(defaultFollowUpDescription);
      setMarkers([]);
      setResolvedConditionIds([]);
      setVerdict('ACCEPT');
      setRejectReason('');
      setPhysicalExamConfirmed(true);
      setFieldErrors({});
      setDraftSavedTime(null);
    };

    const handleRandomizeData = () => {
      if (careType === 'URGENT' || config.simulateMode === 'hot-profiles') {
        const urgentVitals = randomizeUrgentVitals();
        setMetrics(urgentVitals);
        setPhysicalExamConfirmed(true);
        setExamMode('ABNORMAL');
        setStructuredDiagnosis('Musculoskeletal condition suspected');
        setAbnormalFindings(
          urgentVitals.notes || 'Acute localized pain and mild vital elevation secondary to trauma.',
        );
        setTrainingDecision('BLOCKED');
        setFieldErrors((prev) => ({
          ...prev,
          metrics: '',
          abnormalFindings: '',
          diagnosis: '',
          trainingDecision: '',
        }));
        return;
      }

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
      const randomObservation =
        observationNotesPool[Math.floor(Math.random() * observationNotesPool.length)];

      setMetrics({
        temperature: randomTemp,
        heartRate: randomHr,
        respiratoryRate: randomRr,
        weight: randomWeight,
        bodyConditionScore: randomBcs,
        hydrationStatus: 'NORMAL',
        notes: randomObservation,
      });

      setPhysicalExamConfirmed(true);
      setExamMode('NORMAL');
      setAbnormalFindings('');
      setSystemFindings({ ...normalSystemFindings });
      setStructuredDiagnosis('Clinically healthy');

      if (config.section2Mode === 'freeText') {
        setFindings('Clear lungs, strong peripheral pulses, sound hooves, no heat or effusion.');
        setDiagnosis('Clinically healthy with normal post-transport adaptation.');
      }

      setTrainingDecision('ALLOWED');
      setRestrictionDetails('');
      setScheduleFollowUp(false);
      setFollowUpDate('');
      setVerdict('ACCEPT');
      setRejectReason('');

      setFieldErrors({});
      setFormError('');

      onDirtyChange?.(true);
    };

    useEffect(() => {
      if (!config.draft || !isDirty || isGated || availableDraft !== null) return;
      const timer = setTimeout(() => {
        handleSaveDraft();
      }, 15000);
      return () => clearTimeout(timer);
    }, [config.draft, isDirty, isGated, availableDraft, handleSaveDraft]);

    const handleSampleFindings = () => {
      if (!config.sampleFindings) return;
      const scenario = pickFindingScenario();
      const inserted: string[] = [];
      if (!findings.trim()) {
        setFindings(scenario.findings);
        inserted.push('findings');
      }
      if (!diagnosis.trim()) {
        setDiagnosis(scenario.diagnosis);
        inserted.push('diagnosis');
      }
      if (!inserted.length) {
        toast.info('Nothing to insert', 'Every findings field already contains text.');
        return;
      }
      setFieldErrors((prev) => {
        const next = { ...prev };
        for (const key of inserted) delete next[key];
        return next;
      });
      toast.success(
        'Sample findings inserted',
        `${inserted.length} empty field(s) filled from the ${scenario.name} scenario.`,
      );
    };

    function validate(): Record<string, string> {
      const errors: Record<string, string> = {};

      if (showTier1 && verdict === 'REJECT') {
        if (!rejectReason.trim()) {
          errors.rejectReason = 'Reason for rejection is required when rejecting admission.';
        }
        return errors;
      }

      const schema = z
        .object({
          physicalExamConfirmed: z.boolean(),
          effectiveFindings: z.string().min(1, config.findingsRequiredMessage),
          effectiveDiagnosis: z.string().min(1, config.diagnosisRequiredMessage),
          restrictionDetails: z.string(),
          followUpDate: z.string(),
          followUpDescription: z.string(),
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
                message:
                  config.followUpDateMinMessage ?? 'Follow-up date must be at least one day in the future.',
              });
            }
            if (!data.followUpDescription.trim()) {
              ctx.addIssue({
                code: z.ZodIssueCode.custom,
                path: ['followUpDescription'],
                message:
                  config.followUpDescriptionRequiredMessage ??
                  'Follow-up care description is required.',
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
        followUpDescription,
      });
      if (!parsed.success) {
        for (const issue of parsed.error.issues) {
          const field = String(issue.path[0] ?? '');
          if (field && !errors[field]) errors[field] = issue.message;
        }
      }

      if (isPrescriptionActive && prescriptions.length > 0) {
        for (const rx of prescriptions) {
          if (!rx.medicationName.trim()) {
            errors["rx." + rx.id + ".medicationName"] = "Medication name is required.";
          }
          if (!rx.dosage.trim()) {
            errors["rx." + rx.id + ".dosage"] = "Dosage is required.";
          }
          if (!rx.frequency.trim()) {
            errors["rx." + rx.id + ".frequency"] = "Frequency is required.";
          }
          if (!rx.duration || rx.duration < 1) {
            errors["rx." + rx.id + ".duration"] = "Duration must be at least 1 day.";
          }
        }
      }

      const isSixVitalsMandatory = sixVitalsRequired(variant, careType, schedulePurpose, fromIncident);
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

      if (
        variant === 'STANDARD' &&
        careType === 'INITIAL' &&
        trainingDecision === 'BLOCKED'
      ) {
        if (markers.length === 0) {
          errors.injuryMarkers =
            'An injury or condition record is required when blocking a newly admitted horse.';
        } else {
          for (const m of markers) {
            if (!m.injuryType.trim()) {
              errors[`injuryType:${m.id}`] = 'Injury type is required.';
            }
            if (m.bodyRegionId == null) {
              errors[`bodyRegionId:${m.id}`] = 'Body region is required.';
            }
          }
        }
      }

      if (config.vaccination && horseId != null) {
        const doseErrors = validateVaccinationDrafts(vaccinationDrafts);
        setVaccinationErrors(doseErrors);
        if (Object.keys(doseErrors).length > 0) {
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

      return errors;
    }

    function handleFormSubmit(event: FormEvent<HTMLFormElement>) {
      event.preventDefault();
      if (scheduleStatus && scheduleStatus !== 'IN_PROGRESS' && scheduleStatus !== 'DELAYED' && scheduleStatus !== 'OVERDUE') {
        setFormError(
          'Care schedule is SCHEDULED. Physical examination must be started before recording and submitting findings.',
        );
        return;
      }
      const errors = validate();
      setFieldErrors(errors);
      if (Object.keys(errors).length > 0) {
        const msg =
          errors.vaccinations && Object.keys(errors).length === 1
            ? errors.vaccinations
            : 'Please resolve all validation errors before submitting.';
        setFormError(msg);
        return;
      }
      setFormError('');
      if (config.confirmDialog) {
        setShowApproveConfirm(true);
      } else {
        void executeSubmit();
      }
    }

    async function executeSubmit() {
      if (inFlight.current) return;
      if (scheduleStatus && scheduleStatus !== 'IN_PROGRESS' && scheduleStatus !== 'DELAYED' && scheduleStatus !== 'OVERDUE') {
        setFormError(
          'Care schedule is SCHEDULED. Physical examination must be started before recording and submitting findings.',
        );
        setShowApproveConfirm(false);
        return;
      }

      const errors = validate();
      if (Object.keys(errors).length > 0) {
        setFieldErrors(errors);
        const msg =
          errors.vaccinations && Object.keys(errors).length === 1
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

      const nextScheduleRequest =
        followUpActive && followUpDate
          ? {
              horseId: horseId ?? undefined,
              admissionId: admissionId || undefined,
              careType: 'ROUTINE' as const,
              scheduledDate: followUpDate,
              description: followUpDescription.trim() || 'Follow-up veterinary examination',
              idempotencyKey: followUpIdempotencyKey,
            }
          : undefined;

      try {
        let result: VetReviewResponse | CareSchedule;
        let completionKind: 'INITIAL' | 'CARE_SCHEDULE';
        let request: VetReviewRequest | CompleteCareScheduleRequest;

        if (showTier1 && verdict === 'REJECT') {
          request = {
            careScheduleId: careScheduleId ?? undefined,
            decision: 'REJECT',
            rejectReason: rejectReason.trim(),
          };
          result = await admissionsApi.vetReview(admissionId, request as VetReviewRequest);
          completionKind = 'INITIAL';
        } else if (config.submitAdapter === 'admission-aware' && careType === 'INITIAL') {
          request = {
            careScheduleId: careScheduleId ?? undefined,
            decision: 'ACCEPT',
            trainingDecision,
            restrictionDetails: restrictionDetails.trim() || undefined,
            physicalExamConfirmed: true,
            findings: effectiveFindings,
            diagnosis: effectiveDiagnosis,
            nextSchedule: nextScheduleRequest,
            metrics: Object.keys(metricValues).length ? [metricValues] : undefined,
            vaccinations: vaccinationDrafts.length ? toVaccinationRequests(vaccinationDrafts) : undefined,
            injuryMarkers: markers.length ? buildInjuryMarkerRequests(markers) : undefined,
          };
          result = await admissionsApi.vetReview(admissionId, request as VetReviewRequest);
          completionKind = 'INITIAL';
        } else {
          if (!careScheduleId) {
            throw new Error('The care schedule identifier is missing. Reload this examination and try again.');
          }
          request = {
            findings: effectiveFindings,
            diagnosis: effectiveDiagnosis,
            trainingDecision,
            restrictionDetails: restrictionDetails.trim() || undefined,
            nextSchedule: nextScheduleRequest,
            metrics: Object.keys(metricValues).length ? [metricValues] : undefined,
            vaccinations: vaccinationDrafts.length ? toVaccinationRequests(vaccinationDrafts) : undefined,
            deferVaccinationDays: deferVaccinationDays ?? undefined,
            injuryMarkers: markers.length ? buildInjuryMarkerRequests(markers) : injuryMarkers,
            prescriptions:
              isPrescriptionActive && prescriptions.length > 0
                ? toPrescriptionRequests(prescriptions)
                : undefined,
            generalCareInstructions:
              isPrescriptionActive && generalCareInstructions.trim()
                ? generalCareInstructions.trim()
                : undefined,
            resolvedConditionIds:
              resolvedConditionIds.length > 0 ? resolvedConditionIds : undefined,
          };
          result = await admissionsApi.completeCareSchedule(careScheduleId, request as CompleteCareScheduleRequest);
          completionKind = 'CARE_SCHEDULE';
        }

        if (config.draft) {
          try {
            localStorage.removeItem(draftStorageKey);
            localStorage.removeItem(`rtms_vet_draft_${admissionId}`);
          } catch {
            // Ignore
          }
        }

        setShowApproveConfirm(false);
        onSuccess(result, completionKind, request);
      } catch (cause) {
        if (config.errorMapping === 'formatted' && cause instanceof ApiError) {
          const errorMsg = `${cause.status === 403 ? 'Permission denied' : cause.status === 409 ? 'Review state conflict' : 'Review failed'} (${cause.status}${cause.errorCode ? ` · ${cause.errorCode}` : ''}): ${cause.message}`;
          setFormError(errorMsg);
        } else if (config.errorMapping === 'formatted') {
          const errorMsg = cause instanceof Error ? cause.message : 'Failed to submit review.';
          setFormError(errorMsg);
        } else {
          const errorMsg =
            cause instanceof ApiError
              ? cause.message
              : 'Unable to complete the examination. Your entries have been kept. Please try again.';
          setFormError(errorMsg);
        }
        setShowApproveConfirm(false);
      } finally {
        inFlight.current = false;
        setSubmitting(false);
      }
    }

    const renderVitals = () => (
      <>
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
      </>
    );

    const renderActiveConditions = () => {
      if (activeInjuries.length === 0) return null;
      return (
        <ActiveConditionsSection
          activeInjuries={activeInjuries}
          resolvedConditionIds={resolvedConditionIds}
          setResolvedConditionIds={setResolvedConditionIds}
          isFormDisabled={isFormDisabled}
        />
      );
    };

    const renderPrescription = () => {
      if (!isPrescriptionActive) return null;
      return (
        <PrescriptionSection
          isFormDisabled={isFormDisabled}
          prescriptions={prescriptions}
          setPrescriptions={setPrescriptions}
          generalCareInstructions={generalCareInstructions}
          setGeneralCareInstructions={setGeneralCareInstructions}
          fieldErrors={fieldErrors}
          setFieldErrors={setFieldErrors}
        />
      );
    };

    const renderSection2 = () => {
      if (config.section2Mode === 'structured') {
        return (
          <div className="pt-2">
            <DiagnosisSection
              isUrgent={false}
              isFormDisabled={isFormDisabled}
              diagnosis={diagnosis}
              setDiagnosis={setDiagnosis}
              examMode={examMode}
              setExamMode={setExamMode}
              structuredDiagnosis={structuredDiagnosis}
              setStructuredDiagnosis={setStructuredDiagnosis}
              abnormalFindings={abnormalFindings}
              setAbnormalFindings={setAbnormalFindings}
              fieldErrors={fieldErrors}
              setFieldErrors={setFieldErrors}
            />
          </div>
        );
      }

      if (!config.showFindingsTextarea) {
        return (
          <div className="pt-2">
            <DiagnosisSection
              isUrgent={true}
              isFormDisabled={isFormDisabled}
              diagnosis={diagnosis}
              setDiagnosis={setDiagnosis}
              examMode={examMode}
              setExamMode={setExamMode}
              structuredDiagnosis={structuredDiagnosis}
              setStructuredDiagnosis={setStructuredDiagnosis}
              abnormalFindings={abnormalFindings}
              setAbnormalFindings={setAbnormalFindings}
              fieldErrors={fieldErrors}
              setFieldErrors={setFieldErrors}
            />
          </div>
        );
      }

      return (
        <div className="space-y-4 border-t border-[var(--color-border)] pt-5">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">
              <Icon name="stethoscope" size={14} className="text-[var(--color-medical)]" />
              <span>2. Examination Findings &amp; Clinical Diagnosis</span>
            </div>
            {config.sampleFindings && (
              <Button
                type="button"
                size="sm"
                variant="secondary"
                icon="sparkles"
                onClick={handleSampleFindings}
                className="transition-all active:scale-[0.97]"
                title="Insert a complete sample clinical narrative into the empty fields"
              >
                Sample Findings
              </Button>
            )}
          </div>

          <div>
            <FormField label="Physical examination findings" htmlFor="urgent-findings" required>
              <Textarea
                id="urgent-findings"
                rows={3}
                value={findings}
                onChange={(e) => {
                  setFindings(e.target.value);
                  if (fieldErrors.findings) setFieldErrors((prev) => ({ ...prev, findings: '' }));
                }}
                placeholder="Detail physical inspection: palpation, limb flexion, swellings, wounds, pain responses…"
              />
            </FormField>
            {fieldErrors.findings && (
              <p className="mt-1 text-xs text-[var(--color-danger)]">{fieldErrors.findings}</p>
            )}
          </div>

          <div>
            <FormField label="Clinical diagnosis" htmlFor="urgent-diagnosis" required>
              <Input
                id="urgent-diagnosis"
                type="text"
                value={diagnosis}
                onChange={(e) => {
                  setDiagnosis(e.target.value);
                  if (fieldErrors.diagnosis) setFieldErrors((prev) => ({ ...prev, diagnosis: '' }));
                }}
                placeholder="e.g. Acute superficial digital flexor tendon strain"
              />
            </FormField>
            {fieldErrors.diagnosis && (
              <p className="mt-1 text-xs text-[var(--color-danger)]">{fieldErrors.diagnosis}</p>
            )}
          </div>
        </div>
      );
    };

    const renderTrainingDecision = () => {
      if (config.decisionUi === 'shared-section') {
        return (
          <TrainingDecisionSection
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
        );
      }

      return (
        <div className="space-y-4 border-t border-[var(--color-border)] pt-5">
          <div className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">
            <Icon name="shield" size={14} className="text-[var(--color-primary)]" />
            <span>3. Veterinary Training Clearance Decision</span>
            <span className="text-[var(--color-danger)]">*</span>
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            {[
              {
                value: 'ALLOWED' as const,
                label: 'ALLOWED',
                desc: 'Fit for training. Horse may resume normal training schedule.',
              },
              {
                value: 'BLOCKED' as const,
                label: 'BLOCKED',
                desc: 'Training suspended. Workouts cancelled until follow-up clearance.',
              },
            ].map((option) => (
              <label
                key={option.value}
                className={`flex cursor-pointer flex-col rounded-[var(--radius-md)] border p-3.5 transition-all ${
                  trainingDecision === option.value
                    ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)]'
                    : 'border-[var(--color-border)] bg-[var(--color-surface)] hover:border-[var(--color-border-strong)]'
                }`}
              >
                <div className="flex items-center gap-2.5">
                  <ChoiceInput
                    type="radio"
                    name="trainingDecision"
                    value={option.value}
                    checked={trainingDecision === option.value}
                    onChange={() => {
                      setTrainingDecision(option.value);
                      if (option.value === 'ALLOWED') {
                        setFieldErrors((prev) =>
                          Object.fromEntries(
                            Object.entries(prev).filter(([field]) => !field.startsWith('followUp')),
                          ),
                        );
                      }
                    }}
                  />
                  <span className="text-xs font-bold tracking-wide">{option.label}</span>
                </div>
                <p className="mt-1.5 pl-6 text-xs text-[var(--color-text-secondary)]">
                  {option.desc}
                </p>
              </label>
            ))}
          </div>

          {trainingDecision === 'ALLOWED' && (
            <div className="mt-3 rounded-[var(--radius-sm)] border border-[var(--color-warning)]/40 bg-[var(--color-warning-soft)]/20 p-3 text-xs text-[var(--color-text-secondary)]">
              <span className="font-semibold text-[var(--color-warning)]">Notice:</span> Training remains on hold until confirmed safe during the follow-up recheck examination.
            </div>
          )}

          {trainingDecision === 'BLOCKED' && (
            <div className="mt-4 space-y-4 rounded-[var(--radius-md)] border border-[var(--color-warning)]/40 bg-[var(--color-warning-soft)]/20 p-4">
              <div>
                <FormField
                  label={config.restrictionLabel}
                  htmlFor="restriction-details"
                  required
                >
                  <Textarea
                    id="restriction-details"
                    rows={2}
                    value={restrictionDetails}
                    onChange={(e) => {
                      setRestrictionDetails(e.target.value);
                      if (fieldErrors.restrictionDetails) {
                        setFieldErrors((prev) => ({ ...prev, restrictionDetails: '' }));
                      }
                    }}
                    placeholder={config.restrictionPlaceholder}
                  />
                </FormField>
                {fieldErrors.restrictionDetails && (
                  <p className="mt-1 text-xs text-[var(--color-danger)]">
                    {fieldErrors.restrictionDetails}
                  </p>
                )}
              </div>
            </div>
          )}

          {variant === 'URGENT' && (
            <div className="mt-4">
              {renderFollowUps()}
            </div>
          )}
        </div>
      );
    };

    const renderFollowUps = () => {
      const activeRxDurations = prescriptions.map((p) => p.duration).filter((d): d is number => typeof d === 'number' && d > 0);
      const activeInjuryRecoveryDates = activeInjuries.map((inj) => inj.expectedRecoveryDate).filter(Boolean) as string[];
      const suggestedRecheckDate = computeSuggestedRecheckDate(activeRxDurations, activeInjuryRecoveryDates);

      return (
        <FollowUpSection
          isFormDisabled={isFormDisabled}
          careType={careType}
          schedulePurpose={schedulePurpose}
          fromIncident={fromIncident}
          waitingFollowUpAt={waitingFollowUpAt}
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
          suggestedRecheckDate={suggestedRecheckDate}
        />
      );
    };

    const renderInjuryMarkers = () => {
      const isVisible =
        careType === 'URGENT' ||
        config.markersEnabled ||
        trainingDecision === 'BLOCKED' ||
        examMode === 'ABNORMAL';
      if (!isVisible) return null;

      return (
        <fieldset
          disabled={isFormDisabled}
          className="min-w-0 rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-[var(--shadow-panel)] space-y-4"
        >
          <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-2">
            <h3 className="text-sm font-semibold tracking-tight text-[var(--color-text-primary)]">
              3. 3D Anatomy &amp; Injury Markers
            </h3>
            <span className="text-xs text-[var(--color-text-secondary)]">
              {markers.length} marker{markers.length === 1 ? '' : 's'} placed
            </span>
          </div>
          <p className="text-xs text-[var(--color-text-secondary)]">
            Click on the 3D model to pinpoint injury or pain locations.
          </p>
          <InjuryMarkerPanel
            markers={markers}
            setMarkers={setMarkers}
            bodyRegions={bodyRegions}
            readOnly={isFormDisabled}
            fieldErrors={fieldErrors}
            setFieldErrors={setFieldErrors}
            onAppendToFindings={
              config.markersAppendToFindings
                ? (line) => setFindings((prev) => (prev ? `${prev}\n${line}` : line))
                : undefined
            }
          />
        </fieldset>
      );
    };

    const renderVaccination = () => {
      if (!config.vaccination || horseId == null) return null;
      return (
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
            <p
              className="mt-1.5 text-xs font-medium text-[var(--color-danger)]"
              role="alert"
            >
              {fieldErrors.vaccinations}
            </p>
          )}
        </div>
      );
    };

    const renderFormError = () =>
      formError ? (
        <div
          role="alert"
          className="mb-4 flex items-start gap-2.5 rounded-[var(--radius-md)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-3 text-sm text-[var(--color-danger)]"
        >
          <Icon name="alert-triangle" size={16} className="mt-0.5 shrink-0" />
          <span>{formError}</span>
        </div>
      ) : null;

    const renderDraftBanner = () =>
      config.draft && availableDraft ? (
        <div
          role="status"
          className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-[var(--radius-sm)] border border-[var(--color-primary)] bg-[var(--color-primary-soft)] px-3 py-2 text-xs text-[var(--color-text-primary)]"
        >
          <div className="flex items-center gap-2">
            <Icon name="clipboard" size={14} className="shrink-0 text-[var(--color-primary)]" />
            <span>
              A saved draft from <strong>{formatDateTime(availableDraft.timestamp)}</strong> was
              found. Would you like to restore it?
            </span>
          </div>
          <div className="flex items-center gap-2">
            <Button type="button" size="sm" variant="primary" onClick={handleRestoreDraft}>
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
      ) : null;

    const renderDraftSaved = () =>
      config.draft && draftSavedTime && !availableDraft ? (
        <div className="mb-4 flex items-center justify-between rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] px-3 py-1.5 text-xs text-[var(--color-text-secondary)]">
          <span className="flex items-center gap-1.5 font-medium">
            <Icon name="check" size={12} className="text-[var(--color-success)]" />
            Draft saved at {formatDateTime(draftSavedTime)}
          </span>
          <Button type="button" variant="link" size="sm" onClick={handleClearDraft}>
            Delete draft
          </Button>
        </div>
      ) : null;

    const renderGates = () => (
      <>
        {scheduleStatus === 'SCHEDULED' && (
          <div
            role="status"
            className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-[var(--radius-md)] border border-[var(--color-warning)] bg-[var(--color-warning-soft)] p-3 text-sm text-[var(--color-warning)]"
          >
            <div className="flex items-center gap-2">
              <Icon
                name="alert-triangle"
                size={18}
                className="shrink-0 text-[var(--color-warning)]"
              />
              <strong className="block text-sm font-bold">Care schedule is SCHEDULED</strong>
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
        {scheduleStatus === 'REQUESTED' && (
          <div
            role="status"
            className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-[var(--radius-md)] border border-[var(--color-info)] bg-[var(--color-info-soft)] p-3 text-sm text-[var(--color-info)]"
          >
            <div className="flex items-center gap-2">
              <Icon name="clock" size={18} className="shrink-0 text-[var(--color-info)]" />
              <strong className="block text-sm font-bold">Care schedule is REQUESTED</strong>
            </div>
          </div>
        )}
      </>
    );

    const renderSplitStickyForm = () => (
      <form onSubmit={handleFormSubmit} noValidate className="pb-24 md:pb-0">
        {renderGates()}
        {renderFormError()}
        {renderDraftBanner()}
        {renderDraftSaved()}

        <div className={admissionDetailGridClassName}>
          <div className="min-w-0 space-y-5">
            {renderVitals()}
            {renderSection2()}
            {renderInjuryMarkers()}
            {verdict !== 'REJECT' && renderActiveConditions()}
            {verdict !== 'REJECT' && careType !== 'URGENT' && renderVaccination()}
            {verdict !== 'REJECT' && renderPrescription()}
          </div>

          <div className={admissionAsideClassName}>
            {asideTop}
            <div className="min-w-0 space-y-4 rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-[var(--shadow-panel)]">
              <div className="border-b border-[var(--color-border)] pb-3">
                <h3 className="text-sm font-semibold tracking-tight text-[var(--color-text-primary)]">
                  {config.decisionPanelTitle}
                </h3>
              </div>

              {showTier1 && (
                <div className="space-y-3 border-b border-[var(--color-border)] pb-4">
                  <div className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">
                    <Icon name="shield" size={14} className="text-[var(--color-primary)]" />
                    <span>Verdict</span>
                    <span className="text-[var(--color-danger)]">*</span>
                  </div>

                  <div className="grid gap-2 sm:grid-cols-2">
                    {[
                      {
                        value: 'ACCEPT' as const,
                        label: 'ACCEPT',
                        desc: 'Approve intake & record exam',
                      },
                      {
                        value: 'REJECT' as const,
                        label: 'REJECT',
                        desc: 'Close app & reject horse',
                      },
                    ].map((option) => (
                      <label
                        key={option.value}
                        className={`flex cursor-pointer flex-col rounded-[var(--radius-md)] border p-3 transition-all ${
                          verdict === option.value
                            ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)]'
                            : 'border-[var(--color-border)] bg-[var(--color-surface)] hover:border-[var(--color-border-strong)]'
                        }`}
                      >
                        <div className="flex items-center gap-2">
                          <ChoiceInput
                            type="radio"
                            name="admissionVerdict"
                            value={option.value}
                            checked={verdict === option.value}
                            onChange={() => {
                              setVerdict(option.value);
                              if (option.value === 'ACCEPT') {
                                setFieldErrors((prev) => {
                                  const next = { ...prev };
                                  delete next.rejectReason;
                                  return next;
                                });
                              }
                            }}
                          />
                          <span className="text-xs font-bold tracking-wide">{option.label}</span>
                        </div>
                        <p className="mt-1 pl-5 text-[11px] text-[var(--color-text-secondary)]">
                          {option.desc}
                        </p>
                      </label>
                    ))}
                  </div>

                  {verdict === 'REJECT' && (
                    <div className="mt-3 space-y-3">
                      <div>
                        <FormField label="Reason for rejection" htmlFor="reject-reason" required>
                          <Textarea
                            id="reject-reason"
                            rows={3}
                            value={rejectReason}
                            onChange={(e) => {
                              setRejectReason(e.target.value);
                              if (fieldErrors.rejectReason) {
                                setFieldErrors((prev) => ({ ...prev, rejectReason: '' }));
                              }
                            }}
                            placeholder="Detail clinical or behavioral grounds for refusing club admission…"
                          />
                        </FormField>
                        {fieldErrors.rejectReason && (
                          <p className="mt-1 text-xs text-[var(--color-danger)]">
                            {fieldErrors.rejectReason}
                          </p>
                        )}
                      </div>
                      <p className="text-xs font-medium text-[var(--color-danger)]">
                        Rejecting closes the application and rejects the horse.
                      </p>
                    </div>
                  )}
                </div>
              )}

              {verdict !== 'REJECT' && (
                <>
                  {renderTrainingDecision()}
                  {renderFollowUps()}
                </>
              )}

              <div className="hidden space-y-2 border-t border-[var(--color-border)] pt-3 md:block">
                {formError && <Notice tone="error">{formError}</Notice>}
                <Button
                  type="submit"
                  variant={showTier1 && verdict === 'REJECT' ? 'destructive' : 'primary'}
                  loading={submitting}
                  disabled={submitting || startingExam || isGated}
                  icon={showTier1 && verdict === 'REJECT' ? 'x' : 'check'}
                  className="w-full"
                >
                  {showTier1 && verdict === 'REJECT' ? 'Confirm Reject' : config.submitLabel}
                </Button>
                {config.draft && (!showTier1 || verdict !== 'REJECT') && (
                  <Button
                    type="button"
                    variant="tertiary"
                    size="sm"
                    className="w-full"
                    disabled={submitting}
                    onClick={handleSaveDraft}
                  >
                    Save draft locally
                  </Button>
                )}
              </div>
            </div>
          </div>
        </div>

        <div className="fixed bottom-0 left-0 right-0 z-40 border-t border-[var(--color-border)] bg-[var(--color-surface)]/95 p-3 shadow-[var(--shadow-popover)] backdrop-blur-md space-y-2 md:hidden">
          {formError && <Notice tone="error">{formError}</Notice>}
          <div className="flex items-center justify-between gap-2.5">
            <Button
              type="submit"
              variant={showTier1 && verdict === 'REJECT' ? 'destructive' : 'primary'}
              loading={submitting}
              disabled={submitting || startingExam || isGated}
              icon={showTier1 && verdict === 'REJECT' ? 'x' : 'check'}
              className="w-full"
            >
              {showTier1 && verdict === 'REJECT' ? 'Confirm Reject' : config.submitLabel}
            </Button>
          </div>
        </div>

        {config.confirmDialog && (
          <ConfirmDialog
            open={showApproveConfirm}
            title={
              showTier1 && verdict === 'REJECT'
                ? 'Reject Admission Application?'
                : config.confirmDialogTitle
            }
            description={
              showTier1 && verdict === 'REJECT' ? (
                <div className="space-y-2 text-sm">
                  <p>
                    You are rejecting admission for <strong>{candidateName || 'this horse'}</strong>.
                  </p>
                  <p className="text-xs font-medium text-[var(--color-danger)]">
                    Rejecting closes the application and rejects the horse.
                  </p>
                </div>
              ) : (
                <div className="space-y-2 text-sm">
                  <p>
                    You are certifying medical examination for <strong>{candidateName}</strong> with
                    training decision{' '}
                    <strong className="uppercase text-[var(--color-primary)]">
                      {trainingDecision}
                    </strong>
                    .
                  </p>
                  {blockHeldUntil && trainingDecision === 'ALLOWED' && (
                    <div className="rounded-[var(--radius-sm)] bg-[var(--color-warning-soft)] p-2 text-xs font-medium text-[var(--color-warning)]">
                      The horse stays blocked until its Follow-up on {formatDate(blockHeldUntil)}.
                    </div>
                  )}
                  {followUpActive && (waitingFollowUpAt ? (
                    <FollowUpMergeNotice chosenDate={followUpDate} waitingAt={waitingFollowUpAt} />
                  ) : (
                    <div className="rounded-[var(--radius-sm)] bg-[var(--color-warning-soft)] p-2 text-xs font-medium text-[var(--color-warning)]">
                      Follow-up scheduled for <strong>{followUpDate}</strong>: {followUpDescription}.
                    </div>
                  ))}
                  <p className="text-xs text-[var(--color-text-secondary)]">
                    {careType === 'INITIAL'
                      ? 'This action writes the official clinical record and advances the admission to trainer review.'
                      : 'This action completes only this care schedule and does not change the admission workflow.'}
                  </p>
                </div>
              )
            }
            confirmLabel={
              showTier1 && verdict === 'REJECT'
                ? 'Reject Admission'
                : config.confirmDialogConfirmLabel
            }
            cancelLabel={config.confirmDialogCancelLabel}
            tone={showTier1 && verdict === 'REJECT' ? 'danger' : 'primary'}
            loading={submitting}
            onConfirm={() => executeSubmit()}
            onCancel={() => setShowApproveConfirm(false)}
          />
        )}
      </form>
    );

    const renderStackedForm = () => (
      <form onSubmit={handleFormSubmit} noValidate className="space-y-6">
        {renderFormError()}
        {renderVitals()}
        {renderSection2()}
        {renderInjuryMarkers()}
        {renderTrainingDecision()}
        {renderActiveConditions()}
        {renderPrescription()}
        <div className="border-t border-[var(--color-border)] pt-5">
          <Button
            type="submit"
            variant="primary"
            loading={submitting}
            icon="check"
            className="w-full transition-all active:scale-[0.97] sm:w-auto"
          >
            {config.submitLabel}
          </Button>
        </div>
      </form>
    );

    return config.layout === 'split-sticky-decision-panel'
      ? renderSplitStickyForm()
      : renderStackedForm();
  },
);
