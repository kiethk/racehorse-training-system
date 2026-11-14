import type { Horse } from '@/features/stable/types';
import type { IncidentReport, IncidentType, IncidentStatus } from '../types';
import { formatDateTime } from '@/lib/display';

export const INCIDENT_STATUS_MAP: Record<
  IncidentStatus,
  { tone: 'warning' | 'info' | 'success' | 'neutral'; label: string }
> = {
  REPORTED: { tone: 'warning', label: 'Reported - Awaiting Veterinary Review' },
  IN_REVIEW: { tone: 'info', label: 'Under Veterinary Review' },
  RESOLVED: { tone: 'success', label: 'Resolved' },
  DISMISSED: { tone: 'neutral', label: 'Closed' },
};

export const INCIDENT_TYPE_MAP: Record<
  IncidentType,
  { tone: 'info' | 'danger'; label: string }
> = {
  PROBLEM: { tone: 'info', label: 'Problem' },
  TRAUMA: { tone: 'danger', label: 'Trauma' },
};

/**
 * Who took the case: the vet's name, "Awaiting assignment" while a reported case has no vet yet,
 * or null when there is nothing useful to say (e.g. dismissed before any vet was assigned).
 */
export function incidentVetText(report: Pick<IncidentReport, 'status' | 'handlerName'>): string | null {
  if (report.status === 'DISMISSED') return null;
  if (report.handlerName) return report.handlerName;
  return report.status === 'REPORTED' ? 'Awaiting assignment' : null;
}

/**
 * Horse name for an incident. A Groom only sees the horses they currently look after, so an older
 * report may name a horse that is no longer in `/api/horses`; fall back to the name stored on the report.
 */
export function incidentHorseName(report: IncidentReport, horse: Horse | undefined): string {
  return horse?.name ?? report.horseName ?? `ID #${report.horseId}`;
}

export function formatIncidentReportedAt(reportedAt: string | null | undefined): string {
  return formatDateTime(reportedAt);
}
