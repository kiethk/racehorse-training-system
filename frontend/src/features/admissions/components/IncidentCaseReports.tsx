'use client';

import { Icon, Panel, Pill, ProtectedImage } from '@/components/ui';
import { INCIDENT_TYPE_MAP } from '@/features/groom/lib/incidentDisplay';
import { formatDateTime } from '@/lib/display';
import type { UrgentCaseReport } from '../types';

/** The incident reports attached to a case. Renders nothing when there are none. */
export function IncidentCaseReports({ title, reports }: { title: string; reports: UrgentCaseReport[] }) {
  if (reports.length === 0) return null;

  return (
    <Panel padded className="space-y-3">
      <h2 className="text-sm font-bold text-[var(--color-text-primary)]">{title}</h2>
      <ul className="space-y-3">
        {reports.map((report) => {
          const severity = INCIDENT_TYPE_MAP[report.incidentType];
          return (
            <li
              key={report.id}
              className="flex flex-col sm:flex-row items-start gap-3.5 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3.5"
            >
              {report.imageUrl && (
                <div className="group relative h-20 w-20 sm:h-24 sm:w-24 shrink-0 overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-xs transition-all hover:border-[var(--color-primary)] hover:shadow-sm">
                  <ProtectedImage
                    src={report.imageUrl}
                    alt={`Report: ${report.title}`}
                    openOnClick
                    className="h-20 w-20 sm:h-24 sm:w-24 object-cover transition-transform duration-200 group-hover:scale-105"
                  />
                  <div className="pointer-events-none absolute bottom-1 right-1 flex items-center gap-0.5 rounded bg-black/60 px-1 py-0.5 text-[9px] font-medium text-white shadow-xs backdrop-blur-xs">
                    <Icon name="search" size={9} />
                    <span>Zoom</span>
                  </div>
                </div>
              )}

              <div className="min-w-0 flex-1 space-y-1.5">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <strong className="text-sm text-[var(--color-text-primary)]">{report.title}</strong>
                  <Pill tone={severity.tone}>{severity.label}</Pill>
                </div>
                <p className="whitespace-pre-wrap break-words text-xs leading-relaxed text-[var(--color-text-secondary)]">
                  {report.description}
                </p>
                <div className="flex items-center gap-2 text-xs text-[var(--color-text-muted)]">
                  <span>
                    {[report.reportedByRole, report.reportedByName].filter(Boolean).join(' ') ||
                      `#${report.reportedById}`}
                  </span>
                  <span>·</span>
                  <span>{formatDateTime(report.reportedAt)}</span>
                </div>
              </div>
            </li>
          );
        })}
      </ul>
    </Panel>
  );
}
