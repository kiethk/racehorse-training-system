'use client';

import { useCallback, useEffect, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { DataTable, type DataTableColumn } from '@/components/ui/DataTable';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { Icon } from '@/components/ui/Icon';
import { FilterChips } from '@/components/ui/SegmentedControl';
import { EmptyState } from '@/components/ui/states';
import { incidentApi } from '@/features/groom/services/incidentService';
import { IncidentDetailModal } from '@/features/groom/components/IncidentDetailModal';
import {
  INCIDENT_SEVERITY_MAP,
  INCIDENT_STATUS_MAP,
  formatIncidentReportedAt,
  incidentHorseName,
} from '@/features/groom/lib/incidentDisplay';
import type { IncidentReport, IncidentStatus } from '@/features/groom/types';

const PAGE_SIZE = 10;

const horseName = (report: IncidentReport) => incidentHorseName(report, undefined);

/** Every incident report on the Trainer's horses (sent by the Trainer or a Groom), with the vet's progress. */
export function TrainerIncidentList() {
  const [reports, setReports] = useState<IncidentReport[]>([]);
  const [statusFilter, setStatusFilter] = useState<'ALL' | IncidentStatus>('ALL');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<IncidentReport | null>(null);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      setReports(await incidentApi.list(statusFilter === 'ALL' ? undefined : statusFilter));
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
      sortValue: horseName,
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
      id: 'reportedBy',
      header: 'Reported by',
      render: (report) => report.reportedByName ?? '—',
      sortValue: (report) => report.reportedByName ?? '',
    },
    {
      id: 'veterinarian',
      header: 'Veterinarian',
      render: (report) =>
        report.handlerName && report.status !== 'DISMISSED' ? (
          report.handlerName
        ) : (
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
        <h1 className="text-xl font-semibold text-[var(--color-text-primary)]">Incident Reports</h1>
        <Button variant="secondary" size="sm" onClick={loadData} disabled={loading}>
          <Icon name="refresh" size={14} /> Refresh
        </Button>
      </div>

      <FilterChips
        label="Incident status"
        value={statusFilter}
        onChange={setStatusFilter}
        options={[
          { value: 'ALL', label: 'All' },
          { value: 'REPORTED', label: 'Awaiting veterinary review' },
          { value: 'IN_REVIEW', label: 'Under review' },
          { value: 'RESOLVED', label: 'Resolved' },
          { value: 'DISMISSED', label: 'Closed' },
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
          onRowClick={(report) => setSelected(report)}
          loading={loading}
          pageSize={PAGE_SIZE}
          getSearchText={horseName}
          searchPlaceholder="Search by horse name…"
          emptyTitle="No incident reports found"
          emptyDescription={
            statusFilter === 'ALL'
              ? 'No incidents have been reported for your horses. Report one from a horse’s Incidents tab.'
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
