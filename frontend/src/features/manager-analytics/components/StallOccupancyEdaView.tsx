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
  ReferenceLine,
  Cell,
} from 'recharts';
import { Panel, Notice, Pill, DataTable } from '@/components/ui';
import type { DataTableColumn } from '@/components/ui/DataTable';
import type { StallOccupancyEdaDto, BarnOccupancyBreakdownDto } from '../types/managerEda';
import { formatFacilityAreaType } from '../lib/edaFormatters';

interface StallOccupancyEdaViewProps {
  data: StallOccupancyEdaDto;
}

export const StallOccupancyEdaView: React.FC<StallOccupancyEdaViewProps> = ({ data }) => {
  const chartData = data.barnBreakdown.map((b) => ({
    name: `${b.areaCode}: ${b.areaName}`,
    occupancyRate: b.occupancyRate,
    occupied: b.occupiedStalls,
    total: b.totalStalls,
    isQuarantine: b.areaType.toUpperCase() === 'QUARANTINE',
  }));

  const columns: DataTableColumn<BarnOccupancyBreakdownDto>[] = [
    {
      id: 'barn',
      header: 'Barn / Facility Area',
      render: (row: BarnOccupancyBreakdownDto) => (
        <div className="flex items-center gap-2">
          <span className="font-semibold text-xs text-[var(--color-text-primary)]">
            Barn {row.areaCode}: {row.areaName}
          </span>
          <Pill tone={row.areaType.toUpperCase() === 'QUARANTINE' ? 'warning' : 'neutral'} size="sm">
            {formatFacilityAreaType(row.areaType)}
          </Pill>
        </div>
      ),
    },
    {
      id: 'capacity',
      header: 'Capacity (Used / Total)',
      render: (row: BarnOccupancyBreakdownDto) => (
        <span className="text-xs">
          <strong>{row.occupiedStalls}</strong> / {row.totalStalls} stalls
        </span>
      ),
    },
    {
      id: 'available',
      header: 'Available',
      render: (row: BarnOccupancyBreakdownDto) => (
        <span className="text-xs font-medium text-[var(--color-text-secondary)]">
          {row.availableStalls} stalls
        </span>
      ),
    },
    {
      id: 'occupancy',
      header: 'Occupancy Rate',
      render: (row: BarnOccupancyBreakdownDto) => (
        <div className="flex items-center gap-2">
          <span className="text-xs font-semibold text-[var(--color-text-primary)]">
            {row.occupancyRate.toFixed(1)}%
          </span>
          <Pill
            tone={row.occupancyRate >= 90 ? 'danger' : row.occupancyRate >= 75 ? 'warning' : 'success'}
            size="sm"
          >
            {row.occupancyRate >= 90 ? 'High' : row.occupancyRate >= 75 ? 'Optimal' : 'Low'}
          </Pill>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      {/* Buffer Safety Alert Banner */}
      {data.quarantineStressAlert && (
        <Notice tone="error">
          <span className="font-semibold">
            Quarantine Buffer Warning: Buffer margin is {data.inflowBufferMargin} stall(s) ({data.quarantineAvailable} available vs. {data.awaitingArrivalAdmissions} admissions awaiting arrival). High risk of intake bottleneck.
          </span>
        </Notice>
      )}

      {/* KPI Overview Grid */}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-4">
        <Panel padded className="space-y-1">
          <span className="text-xs text-[var(--color-text-muted)]">Overall Occupancy</span>
          <div className="text-2xl font-bold text-[var(--color-text-primary)]">
            {data.overallRate.toFixed(1)}%
          </div>
          <span className="text-xs text-[var(--color-text-secondary)]">
            {data.occupiedStalls} / {data.totalStalls} total stalls used
          </span>
        </Panel>

        <Panel padded className="space-y-1">
          <span className="text-xs text-[var(--color-text-muted)]">Isolation Stall Occupancy</span>
          <div className="text-2xl font-bold text-amber-600 dark:text-amber-400">
            {data.quarantineRate.toFixed(1)}%
          </div>
          <span className="text-xs text-[var(--color-text-secondary)]">
            {data.quarantineOccupied} / {data.quarantineTotal} isolation stalls
          </span>
        </Panel>

        <Panel padded className="space-y-1">
          <span className="text-xs text-[var(--color-text-muted)]">Regular Stalls Rate</span>
          <div className="text-2xl font-bold text-emerald-600 dark:text-emerald-400">
            {data.regularRate.toFixed(1)}%
          </div>
          <span className="text-xs text-[var(--color-text-secondary)]">
            {data.regularOccupied} / {data.regularTotal} training stalls
          </span>
        </Panel>

        <Panel padded className="space-y-1">
          <span className="text-xs text-[var(--color-text-muted)]">Available Intake Stalls</span>
          <div
            className={`text-2xl font-bold ${
              data.inflowBufferMargin <= 0 ? 'text-rose-600 dark:text-rose-400' : 'text-blue-600 dark:text-blue-400'
            }`}
          >
            {data.inflowBufferMargin > 0 ? `+${data.inflowBufferMargin}` : data.inflowBufferMargin}
          </div>
          <span className="text-xs text-[var(--color-text-secondary)]">
            {data.awaitingArrivalAdmissions} horses pending arrival
          </span>
        </Panel>
      </div>

      {/* Barn Occupancy Chart */}
      <Panel padded className="space-y-3">
        <div>
          <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">
            Facility Capacity Breakdown by Barn & Area
          </h3>
          <p className="text-xs text-[var(--color-text-muted)]">
            Dotted line marks 85% optimal capacity threshold. Amber bars denote Quarantine blocks.
          </p>
        </div>

        <div className="h-64 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData} margin={{ top: 15, right: 20, left: 15, bottom: 35 }}>
              <CartesianGrid vertical={false} strokeDasharray="3 3" opacity={0.2} />
              <XAxis dataKey="name" tick={{ fontSize: 11 }} interval={0} angle={-15} textAnchor="end" />
              <YAxis unit="%" domain={[0, 100]} width={45} tick={{ fontSize: 11 }} />
              <Tooltip
                formatter={(val: unknown, _name: unknown, item: any) => [
                  `${val}% (${item.payload.occupied}/${item.payload.total} stalls)`,
                  'Occupancy',
                ]}
              />
              <Bar dataKey="occupancyRate" radius={[4, 4, 0, 0]}>
                {chartData.map((entry, index) => (
                  <Cell
                    key={`cell-${index}`}
                    fill={entry.isQuarantine ? '#f59e0b' : entry.occupancyRate >= 85 ? '#ef4444' : '#3b82f6'}
                  />
                ))}
              </Bar>
              <ReferenceLine
                y={85}
                stroke="#ef4444"
                strokeDasharray="4 4"
                strokeWidth={2}
                                label={{ value: 'Warning (85%)', fill: '#ef4444', fontSize: 11, fontWeight: 700, position: 'insideTopRight' }}
              />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </Panel>

      {/* Barn Breakdown Table */}
      <Panel padded className="space-y-3">
        <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">
          Barn & Area Utilization Details
        </h3>
        <DataTable
          ariaLabel="Barn occupancy table"
          rows={data.barnBreakdown}
          columns={columns}
          getRowKey={(row: BarnOccupancyBreakdownDto) => row.areaId}
          pageSize={8}
          emptyTitle="No facility areas registered"
        />
      </Panel>
    </div>
  );
};
