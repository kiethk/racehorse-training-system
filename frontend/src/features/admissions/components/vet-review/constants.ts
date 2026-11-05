import type { IconName } from '@/components/ui/Icon';
import type { HydrationStatusValue } from '@/features/health/lib/hydrationStatus';

export type MetricField = keyof import('../../types').HorseHealthMetricRequest;
export type ExamMode = 'NORMAL' | 'ABNORMAL';
export type FindingLevel = 'NORMAL' | 'MILD' | 'MODERATE' | 'SEVERE' | 'NOT_EXAMINED';
export type BodySystemKey =
  | 'general'
  | 'respiratory'
  | 'cardiovascular'
  | 'musculoskeletal'
  | 'neurological'
  | 'skinCoat'
  | 'hooves'
  | 'digestive';

export const bodySystems: Array<{ key: BodySystemKey; label: string }> = [
  { key: 'general', label: 'General Condition' },
  { key: 'respiratory', label: 'Respiratory' },
  { key: 'cardiovascular', label: 'Cardiovascular' },
  { key: 'musculoskeletal', label: 'Musculoskeletal' },
  { key: 'neurological', label: 'Neurological' },
  { key: 'skinCoat', label: 'Skin & Coat' },
  { key: 'hooves', label: 'Hooves' },
  { key: 'digestive', label: 'Digestive' },
];

export const findingLevelLabels: Record<FindingLevel, string> = {
  NORMAL: 'Normal',
  MILD: 'Mild abnormality',
  MODERATE: 'Moderate abnormality',
  SEVERE: 'Severe abnormality',
  NOT_EXAMINED: 'Not examined',
};

export const normalSystemFindings = Object.fromEntries(
  bodySystems.map(({ key }) => [key, 'NORMAL']),
) as Record<BodySystemKey, FindingLevel>;

export const symptomOptions = [
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

export const diagnosisOptions = [
  'Clinically healthy',
  'Minor clinical condition',
  'Treatment required',
  'Follow-up examination required',
  'Infectious disease suspected',
  'Musculoskeletal condition suspected',
  'Medically unsuitable for admission',
];

export interface TrainingDecisionOption {
  value: import('../../types').TrainingDecision;
  title: string;
  icon: IconName;
  tone: 'success' | 'danger';
  activeBorder: string;
  activeBg: string;
}

export const trainingDecisions: TrainingDecisionOption[] = [
  {
    value: 'ALLOWED',
    title: 'Training Allowed',
    icon: 'check',
    tone: 'success',
    activeBorder: 'border-[var(--color-success)] ring-1 ring-[var(--color-success)]',
    activeBg: 'bg-[var(--color-success-soft)]',
  },
  {
    value: 'BLOCKED',
    title: 'Training Blocked',
    icon: 'lock',
    tone: 'danger',
    activeBorder: 'border-[var(--color-danger)] ring-1 ring-[var(--color-danger)]',
    activeBg: 'bg-[var(--color-danger-soft)]',
  },
];

export interface MetricConfig {
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

export const metricConfigs: MetricConfig[] = [
  { key: 'temperature', label: 'Temperature', unit: '°C', step: '0.1', placeholder: '38.0', normalMin: 37.0, normalMax: 38.5, criticalMin: 36.5, criticalMax: 38.8 },
  { key: 'heartRate', label: 'Heart Rate', unit: 'bpm', step: '1', placeholder: '36', normalMin: 28, normalMax: 44, criticalMin: 24, criticalMax: 55 },
  { key: 'respiratoryRate', label: 'Resp Rate', unit: 'rpm', step: '1', placeholder: '12', normalMin: 8, normalMax: 16, criticalMin: 6, criticalMax: 24 },
  { key: 'weight', label: 'Weight', unit: 'kg', step: '0.5', placeholder: '500', normalMin: 400, normalMax: 600, criticalMin: 350, criticalMax: 650 },
  { key: 'bodyConditionScore', label: 'BCS', unit: '/ 9', step: '0.5', placeholder: '5.0', normalMin: 4.0, normalMax: 6.0, criticalMin: 3.0, criticalMax: 7.0 },
];

export const INJURY_SEVERITY_OPTIONS = ['MILD', 'MODERATE', 'SEVERE', 'CRITICAL'] as const;

export const emptyMetrics: Record<MetricField, string> = {
  heartRate: '',
  temperature: '',
  weight: '',
  respiratoryRate: '',
  hydrationStatus: '',
  bodyConditionScore: '',
  notes: '',
};

export interface HydrationAssessment {
  status: HydrationStatusValue;
  reason: string;
}

const CHECKED_HYDRATION_METRICS: Array<{ key: 'heartRate' | 'temperature' | 'respiratoryRate'; label: string }> = [
  { key: 'heartRate', label: 'HR' },
  { key: 'temperature', label: 'temp' },
  { key: 'respiratoryRate', label: 'RR' },
];

/**
 * Screening aid: sustained tachycardia, heat and tachypnoea are the vitals-side signs of an
 * equine fluid deficit (a horse dehydrating pushes all three up before the mouth dries out).
 * Grades from how many metrics pass the app's own normal/critical limits above. Not a
 * diagnosis: the vet confirms against mucous membranes, CRT and skin turgor, then applies or overrides.
 */
export function assessHydrationFromVitals(vitals: {
  heartRate: string;
  temperature: string;
  respiratoryRate: string;
}): HydrationAssessment | null {
  let abnormal = 0;
  let critical = 0;
  let parsed = 0;
  const reasons: string[] = [];
  for (const { key, label } of CHECKED_HYDRATION_METRICS) {
    const raw = vitals[key]?.trim();
    if (!raw) continue;
    const num = Number(raw);
    if (Number.isNaN(num)) continue;
    parsed += 1;
    const cfg = metricConfigs.find((m) => m.key === key);
    if (!cfg) continue;
    if (num > cfg.criticalMax) {
      critical += 1;
      abnormal += 1;
      reasons.push(`${label} ${raw} > ${cfg.criticalMax}`);
    } else if (num > cfg.normalMax) {
      abnormal += 1;
      reasons.push(`${label} ${raw} > ${cfg.normalMax}`);
    }
  }
  if (!parsed) return null;
  if (critical >= 2) return { status: 'SEVERE_DEHYDRATION', reason: reasons.join(', ') };
  if (critical === 1) return { status: 'MODERATE_DEHYDRATION', reason: reasons.join(', ') };
  if (abnormal >= 1) return { status: 'MILD_DEHYDRATION', reason: reasons.join(', ') };
  return { status: 'NORMAL', reason: 'all recorded vitals inside the resting range' };
}