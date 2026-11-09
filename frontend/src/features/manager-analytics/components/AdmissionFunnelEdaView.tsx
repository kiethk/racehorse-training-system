'use client';

import React from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from 'recharts';
import { Panel, Notice, DataTable } from '@/components/ui';
import type { DataTableColumn } from '@/components/ui/DataTable';
import type { AdmissionFunnelEdaDto, StageFunnelPointDto } from '../types/managerEda';
import { formatAdmissionStage } from '../lib/edaFormatters';

interface AdmissionFunnelEdaViewProps {
  data: AdmissionFunnelEdaDto;
}

const STAGE_ORDER = [
  'Submitted',
  'Groom Checked',
  'Arrived at Facility',
  'Vet Examined',
  'Trainer Evaluated',
  'Manager Cleared',
];

export const AdmissionFunnelEdaView: React.FC<AdmissionFunnelEdaViewProps> = ({ data }) => {
  const sortedStages = [...data.stageFunnel].sort((a, b) => {
    const idxA = STAGE_ORDER.indexOf(a.stage);
    const idxB = STAGE_ORDER.indexOf(b.stage);
    if (idxA !== -1 && idxB !== -1) return idxA - idxB;
    return 0;
  });

  const funnelChartData = sortedStages.map((s) => ({
    stage: s.stage,
    count: s.count,
    conversion: s.conversionPercentage,
  }));

  const latencyData = [
    { stage: 'Groom Inspection', stageKey: 'GROOM_REVIEW', hours: data.medianLatenciesHours.groomReviewHours },
    { stage: 'Arrival Lead Time', stageKey: 'ARRIVAL', hours: data.medianLatenciesHours.arrivalLeadTimeHours },
    { stage: 'Veterinary Review', stageKey: 'VET_REVIEW', hours: data.medianLatenciesHours.vetReviewHours },
    { stage: 'Trainer Assessment', stageKey: 'TRAINER_REVIEW', hours: data.medianLatenciesHours.trainerReviewHours },
    { stage: 'Manager Approval', stageKey: 'MANAGER_REVIEW', hours: data.medianLatenciesHours.managerReviewHours },
  ];

  const columns: DataTableColumn<StageFunnelPointDto>[] = [
    {
      id: 'stage',
      header: 'Review Funnel Stage',
      render: (row: StageFunnelPointDto) => (
        <span className="font-semibold text-xs text-[var(--color-text-primary)]">
          {row.stage}
        </span>
      ),
    },
    {
      id: 'reached',
      header: 'Applications Reached',
      render: (row: StageFunnelPointDto) => <span className="text-xs">{row.count}</span>,
    },
    {
      id: 'conversion',
      header: 'Pass Rate',
      render: (row: StageFunnelPointDto) => (
        <div className="flex items-center gap-2">
          <div className="w-24 bg-[var(--color-surface-hover)] h-2 rounded-full overflow-hidden">
            <div
              className="bg-blue-600 h-full rounded-full"
              style={{ width: `${Math.min(100, Math.max(0, row.conversionPercentage))}%` }}
            />
          </div>
          <span className="text-xs font-semibold">{row.conversionPercentage.toFixed(1)}%</span>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      {/* Bottleneck Alert Banner */}
      {data.bottleneckStage !== 'NONE' && (
        <Notice tone="warning">
          <span className="font-semibold">
            Pipeline Bottleneck: {formatAdmissionStage(data.bottleneckStage)} is experiencing the highest turnaround latency.
          </span>
        </Notice>
      )}

      {/* KPI Overview Grid */}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-4">
        <Panel padded className="space-y-1">
          <span className="text-xs text-[var(--color-text-muted)]">Total Applications</span>
          <div className="text-2xl font-bold text-[var(--color-text-primary)]">
            {data.totalApplications}
          </div>
        </Panel>

        <Panel padded className="space-y-1">
          <span className="text-xs text-[var(--color-text-muted)]">Approval Clearance</span>
          <div className="text-2xl font-bold text-emerald-600 dark:text-emerald-400">
            {data.approvalRate.toFixed(1)}%
          </div>
          <span className="text-xs text-[var(--color-text-secondary)]">
            {data.approvedCount} admitted
          </span>
        </Panel>

        <Panel padded className="space-y-1">
          <span className="text-xs text-[var(--color-text-muted)]">Rejection & Drop-off</span>
          <div className="text-2xl font-bold text-rose-600 dark:text-rose-400">
            {data.rejectionRate.toFixed(1)}%
          </div>
          <span className="text-xs text-[var(--color-text-secondary)]">
            {data.rejectedCount} rejected · {data.expiredCount} expired
          </span>
        </Panel>

        <Panel padded className="space-y-1">
          <span className="text-xs text-[var(--color-text-muted)]">End-to-End Turnaround</span>
          <div className="text-2xl font-bold text-blue-600 dark:text-blue-400">
            {data.medianLatenciesHours.totalEndToEndHours > 0 ? (
              data.medianLatenciesHours.totalEndToEndHours > 48 ? (
                <span>
                  {(data.medianLatenciesHours.totalEndToEndHours / 24).toFixed(1)}{' '}
                  <span className="text-base font-semibold">days</span>{' '}
                  <span className="text-xs font-normal text-[var(--color-text-muted)]">
                    ({data.medianLatenciesHours.totalEndToEndHours.toFixed(0)}h)
                  </span>
                </span>
              ) : (
                `${data.medianLatenciesHours.totalEndToEndHours.toFixed(1)}h`
              )
            ) : (
              '—'
            )}
          </div>
        </Panel>
      </div>

      {/* Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Stage Conversion Chart */}
        <Panel padded className="space-y-3">
          <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">
            Admission Pipeline Funnel & Stage Drop-off
          </h3>

          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart
                data={funnelChartData}
                margin={{ top: 15, right: 20, left: 15, bottom: 35 }}
              >
                <CartesianGrid vertical={false} strokeDasharray="3 3" opacity={0.2} />
                <XAxis dataKey="stage" tick={{ fontSize: 11 }} interval={0} angle={-15} textAnchor="end" />
                <YAxis unit="%" domain={[0, 100]} width={45} tick={{ fontSize: 11 }} />
                <Tooltip
                  formatter={(val: unknown, _name: unknown, item: { payload?: { count?: number } }) => [
                    `${val}% (${item.payload?.count ?? 0} horses)`,
                    'Pass Rate',
                  ]}
                />
                <Bar dataKey="conversion" fill="#3b82f6" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Panel>

        {/* Stage Turnaround Latency */}
        <Panel padded className="space-y-3">
          <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">
            Average Review Time by Stage (Hours)
          </h3>

          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart
                data={latencyData}
                layout="vertical"
                margin={{ top: 10, right: 30, left: 10, bottom: 10 }}
              >
                <CartesianGrid vertical={false} strokeDasharray="3 3" opacity={0.2} />
                <XAxis type="number" unit="h" tick={{ fontSize: 11 }} />
                <YAxis type="category" dataKey="stage" tick={{ fontSize: 11 }} width={140} />
                <Tooltip
                  formatter={(val: unknown, _name: unknown, item: { payload?: { stageKey?: string } }) => [
                    `${val} hours`,
                    item?.payload?.stageKey === data.bottleneckStage ? 'Bottleneck Turnaround' : 'Average Review Time',
                  ]}
                />
                <Bar dataKey="hours" radius={[0, 4, 4, 0]}>
                  {latencyData.map((entry, index) => {
                    const isBottleneck =
                      data.bottleneckStage !== 'NONE' &&
                      entry.stageKey === data.bottleneckStage;
                    return (
                      <Cell
                        key={`cell-${index}`}
                        fill={isBottleneck ? '#ef4444' : '#8b5cf6'}
                      />
                    );
                  })}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Panel>
      </div>

      {/* Stage Conversion Table */}
      <Panel padded className="space-y-3">
        <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">
          Funnel Stage Progression Matrix
        </h3>
        <DataTable
          ariaLabel="Admission funnel table"
          rows={sortedStages}
          columns={columns}
          getRowKey={(row: StageFunnelPointDto) => row.stage}
          pageSize={6}
          emptyTitle="No admission stage data available"
        />
      </Panel>
    </div>
  );
};
