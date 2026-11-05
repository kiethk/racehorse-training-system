'use client';

import { Panel, Pill, ProtectedImage } from '@/components/ui';
import { INCIDENT_SEVERITY_MAP } from '@/features/groom/lib/incidentDisplay';
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
          const severity = INCIDENT_SEVERITY_MAP[report.severity];
          return (
            <li
              key={report.id}
              className="space-y-2 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3.5"
            >
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
              {report.imageUrl && (
                <ProtectedImage
                  src={report.imageUrl}
                  alt={`Report: ${report.title}`}
                  openOnClick
                  className="max-h-48 w-full rounded-[var(--radius-sm)] object-contain"
                />
              )}
            </li>
          );
        })}
      </ul>
    </Panel>
  );
}
