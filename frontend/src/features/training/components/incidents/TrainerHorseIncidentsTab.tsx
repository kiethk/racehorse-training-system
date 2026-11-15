'use client';

import { useEffect, useState } from 'react';
import { Button, EmptyState, ListSkeleton, Notice, Panel, Pill } from '@/components/ui';
import { ProtectedImage } from '@/components/ui/ProtectedImage';
import { formatDateTime } from '@/lib/display';
import { incidentApi } from '@/features/groom/services/incidentService';
import type { IncidentReport } from '@/features/groom/types';
import {
  INCIDENT_STATUS_MAP,
  INCIDENT_TYPE_MAP,
  incidentVetText,
} from '@/features/groom/lib/incidentDisplay';
import { ReportIncidentDialog } from './ReportIncidentDialog';

export function TrainerHorseIncidentsTab({
  horseId,
  horseName,
  onReported,
}: {
  horseId: number;
  horseName?: string;
  onReported?: () => void;
}) {
  const [reports, setReports] = useState<IncidentReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isReportOpen, setIsReportOpen] = useState(false);
  // Bumped after a new report is sent, to load the list again.
  const [reloadKey, setReloadKey] = useState(0);

  // State only changes once the request answers. `loading` starts true for the first load; a reload keeps the
  // current list on screen until the new one arrives.
  useEffect(() => {
    let active = true;
    incidentApi
      .list({ horseId })
      .then((list) => {
        if (!active) return;
        setReports(list);
        setError(null);
      })
      .catch((err) => {
        if (!active) return;
        console.error('Unable to load horse incidents:', err);
        setError('Unable to load incident reports for this horse.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [horseId, reloadKey]);

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
            const statusConfig = INCIDENT_STATUS_MAP[report.status] ?? { tone: 'neutral', label: report.status };
            const vetText = incidentVetText(report);
            const severityConfig = INCIDENT_TYPE_MAP[report.incidentType] ?? { tone: 'neutral', label: report.incidentType };
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
                      {vetText && <span> • Vet: {vetText}</span>}
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
                    <span className="font-semibold text-[var(--color-text-primary)]">
                      {report.status === 'DISMISSED' ? 'Closed: ' : 'Veterinary Note: '}
                    </span>
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
        onSuccess={() => {
          setReloadKey((key) => key + 1);
          onReported?.();
        }}
      />
    </div>
  );
}
