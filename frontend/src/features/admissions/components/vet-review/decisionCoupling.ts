import type { CareType, TrainingDecision } from '../../types';

/**
 * Marker → decision coupling (user-approved 2026-11-12, extended 2026-11-20):
 * a new marker, or an active condition the Vet has not marked Recovered, forces BLOCKED.
 * Once no marker source is left, a marker-caused block reverts to ALLOWED. The Vet's own
 * manual BLOCKED is never released, and a vaccination visit never changes the decision.
 */
export type MarkerCouplingResult = {
  decision: TrainingDecision;
  blockedByMarker: boolean;
};

/** The coupling only applies to the admission and routine exam forms, not to vaccination visits. */
export function markerCouplingEnabled(
  careType: CareType,
  purpose?: 'FOLLOW_UP' | 'PREVENTIVE',
): boolean {
  if (purpose === 'PREVENTIVE') return false;
  return careType === 'INITIAL' || careType === 'ROUTINE';
}

/**
 * Markers still in force: active conditions not yet marked Recovered, plus markers placed in
 * this session. Compared by count so placement, removal and resolution share one state machine.
 */
export function effectiveMarkerCount(
  activeConditions: ReadonlyArray<{ id: number }>,
  resolvedConditionIds: ReadonlyArray<number>,
  newMarkers: ReadonlyArray<unknown>,
): number {
  return (
    activeConditions.filter((condition) => !resolvedConditionIds.includes(condition.id)).length +
    newMarkers.length
  );
}

/** The decision implied by the markers still in force. */
export function couplingDecision(
  markersInForce: number,
  decision: TrainingDecision,
  blockedByMarker: boolean,
): MarkerCouplingResult {
  if (markersInForce > 0 && decision === 'ALLOWED') {
    return { decision: 'BLOCKED', blockedByMarker: true };
  }
  if (markersInForce === 0 && blockedByMarker && decision === 'BLOCKED') {
    return { decision: 'ALLOWED', blockedByMarker: false };
  }
  return { decision, blockedByMarker };
}
