import type { MetricField } from './constants';
import type { HydrationStatusValue } from '@/features/health/lib/hydrationStatus';

/**
 * Mock data for the urgent examination demo. The vital ranges are drawn from measured
 * Thoroughbred racehorse physiology, NOT resting values:
 * - peak HR during a race/gallop: ~200-214 bpm (J. Equine Sci 2007; PMC11083884)
 * - HR immediately after hard work (1 min): 141-188 bpm, ~100 bpm by 15 min (PMC3427134)
 * - pain/excitement while handled: 73-114 bpm; slow recovery flags pain/exhaustion (Veterian Key)
 * - RR during and just after gallop: 49-77 rpm, settling <25 within 10-20 min if not exhausted
 * - rectal temperature after intense work / inflammation: 39 C and up, exhaustion >= 40 C
 */

export interface UrgentVitalProfile {
  key: string;
  heartRate: { min: number; max: number };
  temperature: { min: number; max: number };
  respiratoryRate: { min: number; max: number };
  hydration: HydrationStatusValue[];
  notes: string[];
}

export const URGENT_VITAL_PROFILES: UrgentVitalProfile[] = [
  {
    key: 'acute-pain-tachycardia',
    heartRate: { min: 52, max: 68 },
    temperature: { min: 37.8, max: 38.4 },
    respiratoryRate: { min: 12, max: 16 },
    hydration: ['NORMAL', 'MILD_DEHYDRATION'],
    notes: [
      'Guarding affected limb; acute localized pain on palpation; elevated heart rate consistent with pain response while temperature and respiration remain normal.',
      'Reluctant to bear weight; digital pulse pronounced; heart rate elevated due to discomfort; mucous membranes pink and moist.',
    ],
  },
  {
    key: 'localized-inflammation',
    heartRate: { min: 48, max: 60 },
    temperature: { min: 38.8, max: 39.3 },
    respiratoryRate: { min: 14, max: 18 },
    hydration: ['MILD_DEHYDRATION'],
    notes: [
      'Localized inflammatory reaction with heat over limb; mild systemic temperature elevation; pulse steady and strong.',
      'Warmth and mild swelling detected; mild pyrexia; capillary refill ~2s; drinking small amounts.',
    ],
  },
  {
    key: 'post-exertion-distress',
    heartRate: { min: 46, max: 58 },
    temperature: { min: 37.9, max: 38.5 },
    respiratoryRate: { min: 18, max: 24 },
    hydration: ['NORMAL', 'MILD_DEHYDRATION'],
    notes: [
      'Mild tachypnea following exercise trauma; heart rate recovering gradually; body temperature within resting parameters.',
      'Slight distress on examination; respiratory rate mildly elevated; clear lung sounds bilaterally.',
    ],
  },
];

function rand(min: number, max: number): number {
  return min + Math.random() * (max - min);
}

function pickOne<T>(list: T[]): T {
  return list[Math.floor(Math.random() * list.length)];
}

/** One simulated telemetry scan of a horse that just injured itself training. */
export function randomizeUrgentVitals(): Record<MetricField, string> {
  const profile = pickOne(URGENT_VITAL_PROFILES);
  return {
    temperature: rand(profile.temperature.min, profile.temperature.max).toFixed(1),
    heartRate: String(Math.round(rand(profile.heartRate.min, profile.heartRate.max))),
    respiratoryRate: String(Math.round(rand(profile.respiratoryRate.min, profile.respiratoryRate.max))),
    weight: rand(480, 525).toFixed(1),
    bodyConditionScore: Math.random() > 0.5 ? '5.5' : '5.0',
    hydrationStatus: pickOne(profile.hydration),
    notes: pickOne(profile.notes),
  };
}

export interface FindingScenario {
  name: string;
  symptoms: string;
  findings: string;
  diagnosis: string;
  treatment: string;
  notes: string;
}

/** Complete clinical narratives used by the "Sample Findings" button; only empty fields are filled. */
export const FINDING_SCENARIOS: FindingScenario[] = [
  {
    name: 'right-fore tendon strain',
    symptoms: 'Short, uneven stride on the right fore after galloping, reluctant to turn on the affected side.',
    findings:
      'Heat and mild swelling along the palmar aspect of the right fore cannon, strong digital pulse, pain on flexion of the proximal sesamoids, lameness 3/5 at walk, worse on firm ground.',
    diagnosis: 'Acute superficial digital flexor tendon strain, right fore',
    treatment: 'Cold hosing 20 minutes, pressure bandage with support wrap, phenylbutazone at body weight, strict stall rest.',
    notes: 'Ultrasound recheck before any work is reintroduced, icing twice daily and daily gait notes recorded by the groom.',
  },
  {
    name: 'mild spasmodic colic',
    symptoms: 'Pawing the bedding, looking at the flank and attempting to roll after the evening feed.',
    findings:
      'Mild right-side flank distension, reduced gut sounds on the right, heart rate raised for rest, dry faecal balls on rectal exam, no obstruction.',
    diagnosis: 'Mild spasmodic colic',
    treatment: 'Butylscopolamine IV, hand walking as tolerated, feed withheld two hours then small soaked feeds.',
    notes: 'Recheck pain score and gut sounds in two hours, night staff to call back immediately if rolling restarts.',
  },
  {
    name: 'fetlock laceration',
    symptoms: 'Bleeding wound over the left fore fetlock found at the morning feed.',
    findings:
      '5 cm laceration above the left fore coronet, haemorrhage controlled with pressure, wound bed clean, joint and tendon untouched, lameness 1/5, tetanus status uncertain.',
    diagnosis: 'Superficial laceration, left fore fetlock',
    treatment: 'Irrigation and debridement under sedation, pressure bandage, topical antiseptic, referral for tetanus booster.',
    notes: 'Bandage change at 48 hours, discharge and odour checked daily, stall rest until the walk is completely sound.',
  },
];

export function pickFindingScenario(): FindingScenario {
  return pickOne(FINDING_SCENARIOS);
}
