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
    key: 'still-hot',
    heartRate: { min: 95, max: 140 },
    temperature: { min: 39.2, max: 40.3 },
    respiratoryRate: { min: 28, max: 48 },
    hydration: ['MODERATE_DEHYDRATION', 'MILD_DEHYDRATION'],
    notes: [
      'Sweating heavily over the neck and shoulders, capillary refill about 3 seconds, membranes tacky.',
      'Coat marked with dried sweat, pulse strong and rapid, horse markedly restless and watchful at the limb.',
    ],
  },
  {
    key: 'pain-after-effort',
    heartRate: { min: 55, max: 95 },
    temperature: { min: 38.4, max: 39.5 },
    respiratoryRate: { min: 18, max: 30 },
    hydration: ['MILD_DEHYDRATION', 'NORMAL'],
    notes: [
      'Heart rate stays above the expected recovery band for the time since the gallop, membranes pink, refill under 2 seconds.',
      'Guarding the right fore, high muscle tension, offers only small drinks when water is presented.',
    ],
  },
  {
    key: 'recovered-with-pain',
    heartRate: { min: 42, max: 58 },
    temperature: { min: 37.9, max: 38.6 },
    respiratoryRate: { min: 14, max: 22 },
    hydration: ['NORMAL', 'MILD_DEHYDRATION'],
    notes: [
      'Vitals settling toward the resting band, still reluctant to stand fully weight-bearing on the affected limb.',
      'Calmer on examination, heart rate recovery is slow but progressive, consistent with pain-driven tachycardia.',
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
