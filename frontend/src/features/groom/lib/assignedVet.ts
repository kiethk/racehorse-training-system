import type { IncidentReport } from '../types';

/**
 * "Vet: …" line shown on an incident report, so whoever reported it knows who took the case.
 * Null when there is nothing useful to say (e.g. dismissed before any vet was assigned).
 */
export function assignedVetLabel(report: Pick<IncidentReport, 'status' | 'handlerName'>): string | null {
  if (report.handlerName) return `Vet: ${report.handlerName}`;
  if (report.status === 'REPORTED') return 'Vet: awaiting assignment';
  return null;
}
