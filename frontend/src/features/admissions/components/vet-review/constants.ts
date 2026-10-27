import type { IconName } from '@/components/ui/Icon';

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

export const restrictionOptions = [
  'Walking only; no trotting, cantering, or galloping',
  'Light exercise only; maximum 20 minutes per session',
  'No strenuous exercise until veterinary re-examination',
  'Stall rest with hand-walking only',
  'Complete stall rest; no training activity',
  'Isolation protocol; no contact with other horses',
];

export const followUpDescriptionOptions = [
  'Routine follow-up recheck examination',
  'Post-treatment clinical reassessment',
  'Lameness and gait reassessment',
  'Respiratory and infectious disease recheck',
  'Training restriction clearance examination',
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
  { key: 'temperature', label: 'Temperature', unit: '°C', step: '0.1', placeholder: '38.0', normalMin: 37.2, normalMax: 38.3, criticalMin: 36.5, criticalMax: 38.8 },
  { key: 'heartRate', label: 'Heart Rate', unit: 'bpm', step: '1', placeholder: '36', normalMin: 28, normalMax: 44, criticalMin: 24, criticalMax: 55 },
  { key: 'respiratoryRate', label: 'Resp Rate', unit: 'rpm', step: '1', placeholder: '12', normalMin: 8, normalMax: 16, criticalMin: 6, criticalMax: 24 },
  { key: 'weight', label: 'Weight', unit: 'kg', step: '0.5', placeholder: '500', normalMin: 400, normalMax: 600, criticalMin: 350, criticalMax: 650 },
  { key: 'bodyConditionScore', label: 'BCS', unit: '/ 9', step: '0.5', placeholder: '5.0', normalMin: 4.0, normalMax: 6.0, criticalMin: 3.0, criticalMax: 7.0 },
];

export const emptyMetrics: Record<MetricField, string> = {
  heartRate: '',
  temperature: '',
  weight: '',
  respiratoryRate: '',
  hydrationStatus: '',
  bodyConditionScore: '',
  notes: '',
};