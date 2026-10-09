'use client';

import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { Icon } from '@/components/ui/Icon';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { incidentApi } from '../services/incidentService';
import { stableApi } from '@/features/stable/services/stableService';
import type { IncidentReport, IncidentSeverity, IncidentStatus } from '../types';
import type { Horse } from '@/features/stable/types';

const STATUS_MAP: Record<
  IncidentStatus,
  { tone: 'warning' | 'info' | 'success' | 'neutral'; label: string }
> = {
  REPORTED: { tone: 'warning', label: 'Reported - Awaiting Veterinary Review' },
  IN_REVIEW: { tone: 'info', label: 'Under Veterinary Review' },
  RESOLVED: { tone: 'success', label: 'Resolved' },
  DISMISSED: { tone: 'neutral', label: 'No Issue' },
};

const SEVERITY_MAP: Record<IncidentSeverity, { tone: 'neutral' | 'info' | 'warning' | 'danger'; label: string }> = {
  LOW: { tone: 'neutral', label: 'Minor' },
  MEDIUM: { tone: 'info', label: 'Moderate' },
  HIGH: { tone: 'warning', label: 'Major' },
  CRITICAL: { tone: 'danger', label: 'Critical' },
};

export function IncidentList() {
  const [reports, setReports] = useState<IncidentReport[]>([]);
  const [horses, setHorses] = useState<Horse[]>([]);
  const [statusFilter, setStatusFilter] = useState<'ALL' | IncidentStatus>('ALL');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [incidentList, horseList] = await Promise.all([
        incidentApi.list(statusFilter === 'ALL' ? undefined : statusFilter),
        stableApi.getHorses().catch(() => [] as Horse[]),
      ]);
      setReports(incidentList);
      setHorses(horseList);
    } catch (err) {
      console.error('Unable to load incident reports:', err);
      setError('Unable to load incident reports. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadData();
  }, [loadData]);

  const horseMap = new Map<number, Horse>();
  for (const h of horses) {
    horseMap.set(h.id, h);
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-[18px] font-semibold text-[var(--color-text-primary)]">
            Incident Reports
          </h1>
          <p className="text-[12px] text-[var(--color-text-secondary)]">
            Monitor unusual horse health conditions and veterinary follow-up.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button variant="secondary" size="sm" onClick={loadData} disabled={loading}>
            <Icon name="refresh" size={14} /> Refresh
          </Button>
          <Link href="/groom/incidents/new">
            <Button variant="primary" size="sm">
              <Icon name="plus" size={14} /> New Incident Report
            </Button>
          </Link>
        </div>
      </div>

      {/* Status filters */}
      <div className="flex flex-wrap items-center gap-2 border-b border-[var(--color-border)] pb-3">
        {(
          [
            { id: 'ALL', label: 'All' },
            { id: 'REPORTED', label: 'Awaiting Veterinary Review' },
            { id: 'IN_REVIEW', label: 'Under Review' },
            { id: 'RESOLVED', label: 'Resolved' },
            { id: 'DISMISSED', label: 'No Issue' },
          ] as const
        ).map((tab) => (
          <button
            key={tab.id}
            type="button"
            onClick={() => setStatusFilter(tab.id)}
            className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
              statusFilter === tab.id
                ? 'bg-[var(--color-primary)] text-white'
                : 'bg-[var(--color-surface-subtle)] text-[var(--color-text-secondary)] hover:bg-[var(--color-border)] hover:text-[var(--color-text-primary)]'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {loading ? (
        <ListSkeleton rows={4} />
      ) : error ? (
        <Panel padded>
          <EmptyState icon="alert-triangle" title="Unable to load data" description={error} />
        </Panel>
      ) : reports.length === 0 ? (
        <Panel padded>
          <EmptyState
            icon="shield"
            title="No incident reports found"
            description={
              statusFilter === 'ALL'
                ? 'No injury incidents have been recorded for your assigned horses.'
                : 'No incident reports match the current filter.'
            }
          />
        </Panel>
      ) : (
        <div className="space-y-4">
          {reports.map((report) => {
            const horse = horseMap.get(report.horseId);
            const statusCfg = STATUS_MAP[report.status] || { tone: 'neutral', label: report.status };
            const severityCfg = SEVERITY_MAP[report.severity] || { tone: 'neutral', label: report.severity };
            const imgSrc = incidentApi.imageSrc(report);

            return (
              <Panel key={report.id} padded>
                <div className="space-y-3">
                  <div className="flex flex-wrap items-start justify-between gap-2 border-b border-[var(--color-border)] pb-2.5">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-semibold text-[var(--color-text-muted)]">
                          #{report.id}
                        </span>
                        <h2 className="text-sm font-semibold text-[var(--color-text-primary)]">
                          {report.title}
                        </h2>
                      </div>
                      <div className="mt-1 flex flex-wrap items-center gap-3 text-xs text-[var(--color-text-secondary)]">
                        <span className="font-medium text-[var(--color-text-primary)]">
                          Horse: {horse ? horse.name : report.horseName || `ID #${report.horseId}`}
                        </span>
                        <span>•</span>
                        <span>
                          Reported: {report.reportedAt ? new Date(report.reportedAt).toLocaleString('en-US') : '—'}
                        </span>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      <Pill tone={severityCfg.tone} size="sm">
                        {severityCfg.label}
                      </Pill>
                      <Pill tone={statusCfg.tone} size="sm">
                        {statusCfg.label}
                      </Pill>
                    </div>
                  </div>

                  {/* Description */}
                  <div className="text-xs text-[var(--color-text-secondary)] whitespace-pre-wrap leading-relaxed">
                    {report.description}
                  </div>

                  {/* Incident image */}
                  {imgSrc && (
                    <div className="pt-1">
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img
                        src={imgSrc}
                        alt={`Incident image: ${report.title}`}
                        className="max-h-56 max-w-sm rounded-[var(--radius-md)] border border-[var(--color-border)] object-cover shadow-sm"
                        loading="lazy"
                      />
                    </div>
                  )}

                  {/* Veterinary assessment, when available */}
                  {report.handlerNote && (
                    <div className="rounded-[var(--radius-sm)] border border-emerald-200 bg-emerald-50/50 p-3 text-xs">
                      <div className="flex items-center gap-1.5 font-medium text-emerald-900">
                        <Icon name="stethoscope" size={14} />
                        <span>
                          Veterinary assessment & guidance{' '}
                          {report.handledAt ? `(${new Date(report.handledAt).toLocaleString('en-US')})` : ''}:
                        </span>
                      </div>
                      <p className="mt-1.5 text-emerald-800 whitespace-pre-wrap">
                        {report.handlerNote}
                      </p>
                    </div>
                  )}
                </div>
              </Panel>
            );
          })}
        </div>
      )}
    </div>
  );
}
