'use client';

import React from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip as RechartsTooltip,
  ResponsiveContainer,
  ReferenceLine,
  Cell,
} from 'recharts';
import { Panel, Notice, Pill, DataTable, Tooltip } from '@/components/ui';
import type { DataTableColumn } from '@/components/ui/DataTable';
import type { VetDelaysEdaDto, VeterinarianDelayDto } from '../types/managerEda';
import { formatRescheduleReason } from '../lib/edaFormatters';

interface VetDelaysEdaViewProps {
  data: VetDelaysEdaDto;
}

export const VetDelaysEdaView: React.FC<VetDelaysEdaViewProps> = ({ data }) => {
  const chartData = data.veterinarians.map((v) => ({
    name: v.veterinarianName,
    ratePercent: Math.round(v.rolloverRate * 1000) / 10,
    zScore: v.zScore,
    totalSchedules: v.totalSchedules,
    totalRollovers: v.totalRollovers,
    alert: v.individualDelinquencyAlert,
  }));

  const clinicMeanPercent = Math.round(data.clinicRolloverRate * 1000) / 10;

  const reasonChartData = data.reasonBreakdown.map((r) => ({
    reason: formatRescheduleReason(r.reason),
    count: r.count,
    percentage: r.percentage,
  }));

  const columns: DataTableColumn<VeterinarianDelayDto>[] = [
    {
      id: 'vet',
      header: 'Veterinarian',
      render: (row: VeterinarianDelayDto) => (
        <div>
          <span className="font-semibold text-xs text-[var(--color-text-primary)]">
            {row.veterinarianName}
          </span>
          <span className="block text-[11px] text-[var(--color-text-muted)]">
            ID: #{row.veterinarianId}
          </span>
        </div>
      ),
    },
    {
      id: 'schedules',
      header: 'Schedules',
      render: (row: VeterinarianDelayDto) => <span className="text-xs">{row.totalSchedules}</span>,
    },
    {
      id: 'rollovers',
      header: 'Rollovers',
      render: (row: VeterinarianDelayDto) => (
        <span className="text-xs font-medium text-[var(--color-text-primary)]">
          {row.totalRollovers}
        </span>
      ),
    },
    {
      id: 'rate',
      header: 'Rollover Rate',
      render: (row: VeterinarianDelayDto) => (
        <span className="text-xs font-semibold text-[var(--color-text-primary)]">
          {(row.rolloverRate * 100).toFixed(1)}%
        </span>
      ),
    },
    {
      id: 'workloadStatus',
      header: 'Workload Status',
      render: (row: VeterinarianDelayDto) => {
        const zFormatted = row.zScore > 0 ? `+${row.zScore.toFixed(2)}` : row.zScore.toFixed(2);
        return (
          <Tooltip content={`Statistical variation: ${zFormatted}σ vs clinic peers`}>
            <span className="inline-block cursor-help">
              <Pill tone={row.individualDelinquencyAlert ? 'danger' : 'success'} size="sm">
                {row.individualDelinquencyAlert ? 'Schedule Overload' : 'Normal Schedule'}
              </Pill>
            </span>
          </Tooltip>
        );
      },
    },
    {
      id: 'status',
      header: 'Review Status',
      render: (row: VeterinarianDelayDto) =>
        row.individualDelinquencyAlert ? (
          <span className="text-xs text-[var(--color-danger)] font-medium">Action Required</span>
        ) : (
          <span className="text-xs text-[var(--color-text-secondary)]">Standard</span>
        ),
    },
  ];

  return (
    <div className="space-y-4">
      {/* Diagnostic Alert Banner */}
      <Notice
        tone={
          data.systemicUnderstaffingAlert
            ? 'error'
            : data.veterinarians.some((v) => v.individualDelinquencyAlert)
            ? 'warning'
            : 'success'
        }
      >
        <span className="font-semibold">{data.diagnosticSummary}</span>
      </Notice>

      {/* KPI Overview Grid */}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Panel padded className="space-y-1">
          <span className="text-xs text-[var(--color-text-muted)]">Clinic Rollover Rate</span>
          <div className="text-2xl font-bold text-[var(--color-text-primary)]">
            {(data.clinicRolloverRate * 100).toFixed(1)}%
          </div>
          <span className="text-xs text-[var(--color-text-secondary)]">
            Mean rate across all active veterinarians
          </span>
        </Panel>

        <Panel padded className="space-y-1">
          <span className="text-xs text-[var(--color-text-muted)]">Total Rollover Events</span>
          <div className="text-2xl font-bold text-[var(--color-text-primary)]">
            {data.totalRolloverEvents}{' '}
            <span className="text-sm font-normal text-[var(--color-text-muted)]">
              / {data.totalCareSchedules} exams
            </span>
          </div>
          <span className="text-xs text-[var(--color-text-secondary)]">
            Rescheduled appointments in selected period
          </span>
        </Panel>

        <Panel padded className="space-y-1">
          <span className="text-xs text-[var(--color-text-muted)]">Staffing Risk Classification</span>
          <div className="mt-1">
            {data.systemicUnderstaffingAlert ? (
              <Pill tone="danger" size="md">
                Systemic Understaffing
              </Pill>
            ) : data.veterinarians.some((v) => v.individualDelinquencyAlert) ? (
              <Pill tone="warning" size="md">
                Individual Bottleneck
              </Pill>
            ) : (
              <Pill tone="success" size="md">
                Balanced Staffing
              </Pill>
            )}
          </div>
          <span className="block text-xs text-[var(--color-text-secondary)] mt-1">
            Evaluated via workload monitoring and appointment overtime
          </span>
        </Panel>
      </div>

      {/* Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Veterinarian Rollover Rate Bar Chart */}
        <Panel padded className="space-y-3">
          <div>
            <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">
              Veterinarian Rollover Rates vs. Clinic Average
            </h3>
            <p className="text-xs text-[var(--color-text-muted)]">
              Dotted red line indicates clinic mean ({clinicMeanPercent}%). Outliers are highlighted.
            </p>
          </div>

          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData} margin={{ top: 15, right: 20, left: 15, bottom: 35 }}>
                <CartesianGrid vertical={false} strokeDasharray="3 3" opacity={0.2} />
                <XAxis
                  dataKey="name"
                  tick={{ fontSize: 11 }}
                  interval={0}
                  angle={-15}
                  textAnchor="end"
                />
                <YAxis
                  unit="%"
                  width={45}
                  tick={{ fontSize: 11 }}
                  domain={[0, (dataMax: number) => Math.max(20, Math.ceil(dataMax * 1.2))]}
                />
                <RechartsTooltip
                  formatter={(val: unknown) => [`${val}%`, 'Rollover Rate']}
                  labelFormatter={(name) => `Dr. ${name}`}
                />
                <Bar dataKey="ratePercent" radius={[4, 4, 0, 0]}>
                  {chartData.map((entry, index) => (
                    <Cell
                      key={`cell-${index}`}
                      fill={entry.alert ? '#ef4444' : '#3b82f6'}
                    />
                  ))}
                </Bar>
                <ReferenceLine
                  y={clinicMeanPercent}
                  stroke="#ef4444"
                  strokeDasharray="4 4"
                  strokeWidth={2}
                                    label={{
                    value: `Clinic Avg (${clinicMeanPercent}%)`,
                    position: 'insideTopLeft',
                    fill: '#ef4444',
                    fontSize: 11,
                    fontWeight: 700,
                  }}
                />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Panel>

        {/* Reschedule Reasons Distribution */}
        <Panel padded className="space-y-3">
          <div>
            <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">
              Reschedule Reason Breakdown
            </h3>
            <p className="text-xs text-[var(--color-text-muted)]">
              High appointment overtime indicates capacity strain; emergency care preemption reflects urgent escalations.
            </p>
          </div>

          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart
                data={reasonChartData}
                layout="vertical"
                margin={{ top: 10, right: 30, left: 10, bottom: 10 }}
              >
                <CartesianGrid vertical={false} strokeDasharray="3 3" opacity={0.2} />
                <XAxis type="number" unit="%" tick={{ fontSize: 11 }} />
                <YAxis type="category" dataKey="reason" tick={{ fontSize: 11 }} width={160} />
                <RechartsTooltip
                  formatter={(val: unknown, _name: unknown, item: any) => [
                    `${val}% (${item.payload.count} events)`,
                    'Share',
                  ]}
                />
                <Bar dataKey="percentage" fill="#8b5cf6" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Panel>
      </div>

      {/* Detailed Veterinarian Roster Table */}
      <Panel padded className="space-y-3">
        <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">
          Veterinarian Delay & Delinquency Matrix
        </h3>
        <DataTable
          ariaLabel="Veterinarian delay statistics"
          rows={data.veterinarians}
          columns={columns}
          getRowKey={(row) => row.veterinarianId}
          pageSize={10}
          emptyTitle="No veterinarian schedule records found"
        />
      </Panel>
    </div>
  );
};
