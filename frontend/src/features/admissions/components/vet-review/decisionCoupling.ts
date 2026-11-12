import type { CareType, TrainingDecision } from '../../types';
import type { ExamVariant } from './examVariantConfig';

/**
 * Marker → decision coupling (user-approved 2026-11-12):
 * placing an injury marker while training is ALLOWED forces BLOCKED, and removing
 * the marker that caused it reverts to ALLOWED. Vet's own manual decisions are
 * never overridden, and URGENT exams (already hard-blocked) are out of scope.
 */
export type MarkerCouplingResult = {
  decision: TrainingDecision;
  blockedByMarker: boolean;
  autoSwitch: 'TO_BLOCKED' | 'TO_ALLOWED' | null;
};

/** The coupling only applies to the standard admission and routine exam forms. */
export function markerCouplingEnabled(variant: ExamVariant, careType: CareType): boolean {
  return variant === 'STANDARD' && (careType === 'INITIAL' || careType === 'ROUTINE');
}

/**
 * Pure state-machine step for a marker list change. Counts are compared so the
 * same function covers placement (first/extra marker) and removal (last marker).
 */
export function applyMarkerCoupling(
  prevCount: number,
  nextCount: number,
  decision: TrainingDecision,
  blockedByMarker: boolean,
): MarkerCouplingResult {
  if (nextCount > prevCount && decision === 'ALLOWED') {
    return { decision: 'BLOCKED', blockedByMarker: true, autoSwitch: 'TO_BLOCKED' };
  }
  if (nextCount === 0 && prevCount > 0 && blockedByMarker && decision === 'BLOCKED') {
    return { decision: 'ALLOWED', blockedByMarker: false, autoSwitch: 'TO_ALLOWED' };
  }
  return { decision, blockedByMarker, autoSwitch: null };
}
