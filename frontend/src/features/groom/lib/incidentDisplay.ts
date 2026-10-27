import type { Horse } from '@/features/stable/types';
import type { IncidentReport, IncidentSeverity, IncidentStatus } from '../types';

export const INCIDENT_STATUS_MAP: Record<
  IncidentStatus,
  { tone: 'warning' | 'info' | 'success' | 'neutral'; label: string }
> = {
  REPORTED: { tone: 'warning', label: 'Reported - Awaiting Veterinary Review' },
  IN_REVIEW: { tone: 'info', label: 'Under Veterinary Review' },
  RESOLVED: { tone: 'success', label: 'Resolved' },
  DISMISSED: { tone: 'neutral', label: 'No Issue' },
};

export const INCIDENT_SEVERITY_MAP: Record<
  IncidentSeverity,
  { tone: 'neutral' | 'info' | 'warning' | 'danger'; label: string }
> = {
  LOW: { tone: 'neutral', label: 'Minor' },
  MEDIUM: { tone: 'info', label: 'Moderate' },
  HIGH: { tone: 'warning', label: 'Major' },
  CRITICAL: { tone: 'danger', label: 'Critical' },
};

/**
 * Horse name for an incident. A Groom only sees the horses they currently look after, so an older
 * report may name a horse that is no longer in `/api/horses`; fall back to the name stored on the report.
 */
export function incidentHorseName(report: IncidentReport, horse: Horse | undefined): string {
  return horse?.name ?? report.horseName ?? `ID #${report.horseId}`;
}

export function formatIncidentReportedAt(reportedAt: string | null | undefined): string {
  return reportedAt ? new Date(reportedAt).toLocaleString('en-US') : '—';
}
