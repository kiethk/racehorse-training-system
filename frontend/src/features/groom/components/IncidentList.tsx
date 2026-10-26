'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/Button';
import { DataTable, type DataTableColumn } from '@/components/ui/DataTable';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { Icon } from '@/components/ui/Icon';
import { FilterChips } from '@/components/ui/SegmentedControl';
import { EmptyState } from '@/components/ui/states';
import { incidentApi } from '../services/incidentService';
import {
  INCIDENT_SEVERITY_MAP,
  INCIDENT_STATUS_MAP,
  formatIncidentReportedAt,
  incidentHorseName,
} from '../lib/incidentDisplay';
import { IncidentDetailModal } from './IncidentDetailModal';
import { stableApi } from '@/features/stable/services/stableService';
import type { IncidentReport, IncidentStatus } from '../types';
import type { Horse } from '@/features/stable/types';

const PAGE_SIZE = 10;

export function IncidentList() {
  const [reports, setReports] = useState<IncidentReport[]>([]);
  const [horses, setHorses] = useState<Horse[]>([]);
  const [statusFilter, setStatusFilter] = useState<'ALL' | IncidentStatus>('ALL');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<IncidentReport | null>(null);

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

  const horseMap = useMemo(() => new Map<number, Horse>(horses.map((h) => [h.id, h])), [horses]);
  const horseName = useCallback(
    (report: IncidentReport) => incidentHorseName(report, horseMap.get(report.horseId)),
    [horseMap],
  );

  const columns: DataTableColumn<IncidentReport>[] = [
    {
      id: 'reportedAt',
      header: 'Reported',
      render: (report) => formatIncidentReportedAt(report.reportedAt),
      sortValue: (report) => report.reportedAt,
      className: 'whitespace-nowrap',
    },
    {
      id: 'horse',
      header: 'Horse',
      render: (report) => (
        <span className="font-medium text-[var(--color-text-primary)]">{horseName(report)}</span>
      ),
      sortValue: (report) => horseName(report),
    },
    {
      id: 'title',
      header: 'Incident',
      render: (report) => (
        <span>
          <span className="text-[var(--color-text-muted)]">#{report.id}</span> {report.title}
        </span>
      ),
      sortValue: (report) => report.title,
    },
    {
      id: 'veterinarian',
      header: 'Veterinarian',
      render: (report) => report.handlerName
        ? report.handlerName
        : (
          <span className="italic text-[var(--color-text-muted)]">
            {report.status === 'REPORTED' ? 'Awaiting assignment' : '—'}
          </span>
        ),
      sortValue: (report) => report.handlerName ?? '',
    },
    {
      id: 'severity',
      header: 'Severity',
      render: (report) => {
        const cfg = INCIDENT_SEVERITY_MAP[report.severity] || { tone: 'neutral', label: report.severity };
        return <Pill tone={cfg.tone} size="sm">{cfg.label}</Pill>;
      },
    },
    {
      id: 'status',
      header: 'Status',
      render: (report) => {
        const cfg = INCIDENT_STATUS_MAP[report.status] || { tone: 'neutral', label: report.status };
        return <Pill tone={cfg.tone} size="sm">{cfg.label}</Pill>;
      },
    },
    {
      id: 'actions',
      header: <span className="sr-only">Actions</span>,
      align: 'right',
      render: (report) => (
        <Button variant="secondary" size="sm" onClick={() => setSelected(report)}>
          Details
        </Button>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold text-[var(--color-text-primary)]">
            Incident Reports
          </h1>
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
      <FilterChips
        label="Incident status"
        value={statusFilter}
        onChange={setStatusFilter}
        options={[
          { value: 'ALL', label: 'All' },
          { value: 'REPORTED', label: 'Awaiting veterinary review' },
          { value: 'IN_REVIEW', label: 'Under review' },
          { value: 'RESOLVED', label: 'Resolved' },
          { value: 'DISMISSED', label: 'No issue' },
        ]}
      />

      {error ? (
        <Panel padded>
          <EmptyState icon="alert-triangle" title="Unable to load data" description={error} />
        </Panel>
      ) : (
        <DataTable
          ariaLabel="Incident reports"
          rows={reports}
          columns={columns}
          getRowKey={(report) => report.id}
          loading={loading}
          pageSize={PAGE_SIZE}
          getSearchText={horseName}
          searchPlaceholder="Search by horse name…"
          emptyTitle="No incident reports found"
          emptyDescription={
            statusFilter === 'ALL'
              ? 'No injury incidents have been recorded for your assigned horses.'
              : 'No incident reports match the current filter.'
          }
        />
      )}

      <IncidentDetailModal
        report={selected}
        horseName={selected ? horseName(selected) : ''}
        onClose={() => setSelected(null)}
      />
    </div>
  );
}
