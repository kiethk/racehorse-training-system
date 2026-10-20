'use client';

import { useCallback, useEffect, useState } from 'react';
import { Button, EmptyState, Icon, ListSkeleton, Notice, Panel, Pill } from '@/components/ui';
import { ProtectedImage } from '@/components/ui/ProtectedImage';
import { formatDateTime } from '@/lib/display';
import { incidentApi } from '@/features/groom/services/incidentService';
import type { IncidentReport, IncidentSeverity, IncidentStatus } from '@/features/groom/types';
import { ReportIncidentDialog } from './ReportIncidentDialog';

const STATUS_MAP: Record<
  IncidentStatus,
  { tone: 'warning' | 'info' | 'success' | 'neutral'; label: string }
> = {
  REPORTED: { tone: 'warning', label: 'Reported — Awaiting Vet Review' },
  IN_REVIEW: { tone: 'info', label: 'Under Vet Review' },
  RESOLVED: { tone: 'success', label: 'Resolved' },
  DISMISSED: { tone: 'neutral', label: 'No Issue' },
};

const SEVERITY_MAP: Record<IncidentSeverity, { tone: 'neutral' | 'info' | 'warning' | 'danger'; label: string }> = {
  LOW: { tone: 'neutral', label: 'Minor' },
  MEDIUM: { tone: 'info', label: 'Moderate' },
  HIGH: { tone: 'warning', label: 'Major' },
  CRITICAL: { tone: 'danger', label: 'Critical' },
};

export function TrainerHorseIncidentsTab({ horseId, horseName }: { horseId: number; horseName?: string }) {
  const [reports, setReports] = useState<IncidentReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isReportOpen, setIsReportOpen] = useState(false);

  const fetchReports = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const list = await incidentApi.list({ horseId });
      setReports(list);
    } catch (err) {
      console.error('Unable to load horse incidents:', err);
      setError('Unable to load incident reports for this horse.');
    } finally {
      setLoading(false);
    }
  }, [horseId]);

  useEffect(() => {
    fetchReports();
  }, [fetchReports]);

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 className="text-base font-semibold text-[var(--color-text-primary)]">Incident & Safety Reports</h3>
          <p className="text-xs text-[var(--color-text-secondary)]">
            Reported incidents and current veterinary review statuses for this horse.
          </p>
        </div>
        <Button
          variant="destructive"
          size="sm"
          icon="alert-triangle"
          onClick={() => setIsReportOpen(true)}
        >
          + Report Incident to Vet
        </Button>
      </div>

      {error && <Notice tone="error">{error}</Notice>}

      {loading ? (
        <Panel padded>
          <ListSkeleton rows={3} />
        </Panel>
      ) : reports.length === 0 ? (
        <Panel padded>
          <EmptyState
            icon="check"
            title="No incident reports"
            description="No health, training or physical incidents have been recorded for this horse."
          />
        </Panel>
      ) : (
        <div className="space-y-3">
          {reports.map((report) => {
            const statusConfig = STATUS_MAP[report.status] ?? { tone: 'neutral', label: report.status };
            const severityConfig = SEVERITY_MAP[report.severity] ?? { tone: 'neutral', label: report.severity };
            const imgPath = incidentApi.imageSrc(report);

            return (
              <Panel key={report.id} padded className="space-y-3">
                <div className="flex flex-wrap items-start justify-between gap-2 border-b border-[var(--color-border-subtle)] pb-2.5">
                  <div>
                    <div className="flex items-center gap-2">
                      <h4 className="text-sm font-semibold text-[var(--color-text-primary)]">{report.title}</h4>
                      <Pill tone={severityConfig.tone} size="sm">{severityConfig.label}</Pill>
                    </div>
                    <p className="text-xs text-[var(--color-text-muted)]">
                      Reported on {formatDateTime(report.reportedAt)}
                      {report.reportedByName && (
                        <span> • Reported by: <strong>{report.reportedByName}</strong></span>
                      )}
                    </p>
                  </div>
                  <Pill tone={statusConfig.tone} size="sm">{statusConfig.label}</Pill>
                </div>

                <p className="text-xs leading-relaxed text-[var(--color-text-secondary)] whitespace-pre-wrap">
                  {report.description}
                </p>

                {imgPath && (
                  <div className="pt-2">
                    <p className="mb-1 text-xs font-medium text-[var(--color-text-secondary)]">Attached photo:</p>
                    <div className="relative h-40 w-56 overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)]">
                      <ProtectedImage
                        src={imgPath}
                        alt="Incident attachment"
                        className="h-full w-full object-cover"
                      />
                    </div>
                  </div>
                )}

                {report.handlerNote && (
                  <div className="rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] p-3 text-xs">
                    <span className="font-semibold text-[var(--color-text-primary)]">Veterinary Note: </span>
                    <span className="text-[var(--color-text-secondary)]">{report.handlerNote}</span>
                  </div>
                )}
              </Panel>
            );
          })}
        </div>
      )}

      <ReportIncidentDialog
        horseId={horseId}
        horseName={horseName}
        open={isReportOpen}
        onClose={() => setIsReportOpen(false)}
        onSuccess={fetchReports}
      />
    </div>
  );
}
