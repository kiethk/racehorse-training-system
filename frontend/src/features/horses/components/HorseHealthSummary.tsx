'use client';
import { useCallback, useMemo } from 'react';
import Link from 'next/link';
import { hydrationStatusLabel } from '@/features/health/lib/hydrationStatus';
import { useAuth } from '@/context/AuthContext';

import { EmptyState, Icon, MetricCard, Notice, Panel, Pill } from '@/components/ui';
import { DataTable, type DataTableColumn } from '@/components/ui/DataTable';
import { HorseAlertList } from '@/features/training/components/horses/HorseAlertList';
import type { HorseAlert } from '@/features/training/types';
import type { HorseHealthMetric } from '../types';
import { formatDate } from '@/lib/display';

export function HorseHealthSummary({
  alerts,
  metrics,
  preventiveCare,
}: {
  alerts: HorseAlert[];
  metrics: HorseHealthMetric[];
  preventiveCare: any[];
}) {
  const { user } = useAuth();
  const isManager = user?.role === 'CLUB_MANAGER';
  const latestMetric = metrics[0] || null;

  const defaultOrder = useCallback((left: any, right: any) => {
    const timeA = new Date(left.scheduledDate || left.scheduledAt || left.requestedAt || left.createdAt || 0).getTime();
    const timeB = new Date(right.scheduledDate || right.scheduledAt || right.requestedAt || right.createdAt || 0).getTime();
    return timeB - timeA;
  }, []);

  const columns: DataTableColumn<any>[] = useMemo(
    () => [
      {
        id: 'date',
        header: 'Scheduled Date',
        sortValue: (row) =>
          new Date(row.scheduledDate || row.scheduledAt || row.requestedAt || row.createdAt || 0).getTime(),
        render: (row) => {
          const dateStr = formatDate(row.scheduledDate || row.scheduledAt || row.requestedAt);
          return <span className="font-medium text-[var(--color-text-primary)] block">{dateStr}</span>;
        },
      },
      {
        id: 'careType',
        header: 'Care Type',
        sortValue: (row) => (row.purpose === 'PREVENTIVE' ? 'PREVENTIVE' : row.careType || ''),
        render: (row) => {
          const typeLabel =
            row.purpose === 'PREVENTIVE'
              ? 'Preventive'
              : row.careType === 'INITIAL'
                ? 'Initial'
                : row.careType === 'ROUTINE'
                  ? 'Routine'
                  : row.careType || 'CARE';
          return (
            <div>
              <div className="flex items-center gap-1.5 font-semibold text-[var(--color-text-primary)]">

                <span>{typeLabel}</span>
              </div>
              {row.description && (
                <p className="text-[12px] text-[var(--color-text-muted)] truncate max-w-[280px]" title={row.description}>
                 
                </p>
              )}
            </div>
          );
        },
      },
      {
        id: 'veterinarian',
        header: 'Assigned Veterinarian',
        sortValue: (row) => row.veterinarianName || '',
        render: (row) => {
          if (!row.veterinarianName) {
            return <span className="text-[var(--color-text-muted)] italic">Unassigned</span>;
          }
          if (isManager && row.veterinarianId) {
            return (
              <Link
                href={`/manager/accounts?userId=${row.veterinarianId}`}
                className="inline-flex items-center gap-1.5 text-[var(--color-primary)] hover:underline font-medium"
                title={`View account of ${row.veterinarianName}`}
              >
                <Icon name="user" size={14} className="text-[var(--color-primary)]" />
                <span>{row.veterinarianName}</span>
              </Link>
            );
          }
          return (
            <span className="inline-flex items-center gap-1.5 text-[var(--color-text-secondary)] font-medium">
              <Icon name="user" size={14} className="text-[var(--color-primary)]" />
              <span>{row.veterinarianName}</span>
            </span>
          );
        },
      },
      {
        id: 'status',
        header: 'Status',
        sortValue: (row) => row.status || '',
        render: (row) => {
          const status = row.status || 'SCHEDULED';
          const tone =
            status === 'COMPLETED'
              ? 'success'
              : status === 'IN_PROGRESS'
                ? 'warning'
                : status === 'CANCELLED'
                  ? 'neutral'
                  : 'warning';
          return (
            <div className="flex items-center gap-2">
              <Pill tone={tone} size="sm">
                {status}
              </Pill>
              {row.rolloverCount > 0 && (
                <span className="text-[11px] font-medium text-[var(--color-warning-text)] bg-[var(--color-warning-muted)] px-1.5 py-0.5 rounded">
                  Rolled {row.rolloverCount}x
                </span>
              )}
            </div>
          );
        },
      },
    ],
    [isManager]
  );

  return (
    <div className="space-y-6">
      {/* Risk and Alerts */}
      <HorseAlertList alerts={alerts} />

      {/* Latest Vitals / Examination Metrics */}
      <div>
        <h3 className="text-[14px] font-semibold text-[var(--color-text-primary)] mb-3 flex items-center gap-1.5">
          <Icon name="activity" size={16} className="text-[var(--color-primary)]" />
          Latest Veterinary Examination Vitals
        </h3>

        {latestMetric ? (
          <div className="space-y-3">
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              <MetricCard
                label="Heart Rate"
                value={latestMetric.heartRate != null ? `${latestMetric.heartRate} bpm` : 'N/A'}
                icon="heart-pulse"
              />
              <MetricCard
                label="Temperature"
                value={latestMetric.temperature != null ? `${latestMetric.temperature} °C` : 'N/A'}
                icon="activity"
              />
              <MetricCard
                label="Weight"
                value={latestMetric.weight != null ? `${latestMetric.weight} kg` : 'N/A'}
                icon="gauge"
              />
              <MetricCard
                label="Respiratory Rate"
                value={latestMetric.respiratoryRate != null ? `${latestMetric.respiratoryRate} bpm` : 'N/A'}
                icon="trending-up"
              />
            </div>

            <Panel padded className="text-[12px] space-y-2">
              <div className="flex flex-wrap items-center justify-between gap-2 text-[var(--color-text-muted)] border-b border-[var(--color-border-subtle)] pb-2">
                <span>Recorded: <strong>{formatDate(latestMetric.recordedAt)}</strong></span>
                {latestMetric.hydrationStatus && (
                  <span>Hydration: <strong className="text-[var(--color-text-primary)]">{hydrationStatusLabel(latestMetric.hydrationStatus)}</strong></span>
                )}
                {latestMetric.bodyConditionScore != null && (
                  <span>BCS: <strong className="text-[var(--color-text-primary)]">{latestMetric.bodyConditionScore}/9</strong></span>
                )}
              </div>
              {latestMetric.notes && (
                <p className="text-[var(--color-text-secondary)] italic">
                  &ldquo;{latestMetric.notes}&rdquo;
                </p>
              )}
            </Panel>
          </div>
        ) : (
          <Panel padded>
            <Notice tone="info" title="No vital measurements recorded">
             
            </Notice>
          </Panel>
        )}
      </div>

      {/* Upcoming Preventive Care Schedules */}
      <div>
        <h3 className="text-[14px] font-semibold text-[var(--color-text-primary)] mb-3 flex items-center gap-1.5">
          <Icon name="calendar" size={16} className="text-[var(--color-info)]" />
          Care & Preventive Schedules
        </h3>

        <DataTable
          rows={preventiveCare || []}
          columns={columns}
          getRowKey={(row) => row.id || `${row.careType}-${row.scheduledDate || row.scheduledAt}`}
          pageSize={5}
          defaultOrder={defaultOrder}
          emptyTitle="No care schedules found"
          emptyDescription="Routine checks, vaccinations, and preventive treatments will appear here."
          ariaLabel="Care schedules"
        />
      </div>
    </div>
  );
}
