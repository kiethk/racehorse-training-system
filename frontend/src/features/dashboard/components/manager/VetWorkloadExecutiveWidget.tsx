'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
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
import { Panel, Pill, Icon, Skeleton, Notice } from '@/components/ui';
import { managerEdaService } from '@/features/manager-analytics/services/managerEdaService';
import type { VetDelaysEdaDto } from '@/features/manager-analytics/types/managerEda';

export const VetWorkloadExecutiveWidget: React.FC = () => {
  const [data, setData] = useState<VetDelaysEdaDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    managerEdaService
      .getOverview(90)
      .then((res) => {
        if (active) {
          setData(res.vetDelays);
          setLoading(false);
        }
      })
      .catch((err) => {
        if (active) {
          setError(err?.message || 'Unable to load veterinary delay data.');
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  if (loading) {
    return (
      <Panel padded className="space-y-3">
        <div className="flex items-center justify-between">
          <Skeleton className="h-5 w-40" />
          <Skeleton className="h-4 w-24" />
        </div>
        <Skeleton className="h-56 w-full" />
      </Panel>
    );
  }

  if (error || !data) {
    return (
      <Panel padded className="space-y-2">
        <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">
          Veterinary Delays & Rollovers
        </h3>
        <Notice tone="error">{error || 'Data unavailable'}</Notice>
      </Panel>
    );
  }

  const chartData = data.veterinarians.map((v) => ({
    name: v.veterinarianName,
    ratePercent: Math.round(v.rolloverRate * 1000) / 10,
    totalSchedules: v.totalSchedules,
    totalRollovers: v.totalRollovers,
    alert: v.individualDelinquencyAlert,
  }));

  const clinicMeanPercent = Math.round(data.clinicRolloverRate * 1000) / 10;

  return (
    <Panel padded className="space-y-3">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">
            Veterinary Delays & Rollovers
          </h3>
          {data.systemicUnderstaffingAlert ? (
            <Pill tone="danger" size="sm">
              Staffing Strain
            </Pill>
          ) : data.veterinarians.some((v) => v.individualDelinquencyAlert) ? (
            <Pill tone="warning" size="sm">
              Workload Outlier
            </Pill>
          ) : (
            <Pill tone="success" size="sm">
              Normal Workload
            </Pill>
          )}
        </div>

        <Link
          href="/manager/analytics"
          className="text-xs font-semibold text-[var(--color-primary)] hover:underline inline-flex items-center gap-1 shrink-0"
        >
          View Analytics <Icon name="chevron-right" size={12} />
        </Link>
      </div>

      <div className="h-56 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={chartData} margin={{ top: 10, right: 10, left: 0, bottom: 25 }}>
            <CartesianGrid vertical={false} strokeDasharray="3 3" opacity={0.2} />
            <XAxis dataKey="name" tick={{ fontSize: 10 }} interval={0} angle={-15} textAnchor="end" />
            <YAxis
              unit="%"
              width={40}
              tick={{ fontSize: 10 }}
              domain={[0, (dataMax: number) => Math.max(20, Math.ceil(dataMax * 1.2))]}
            />
            <Tooltip
              formatter={(val: unknown) => [`${val}%`, 'Rollover Rate']}
              labelFormatter={(name) => `Dr. ${name}`}
            />
            <Bar dataKey="ratePercent" radius={[4, 4, 0, 0]}>
              {chartData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.alert ? '#ef4444' : '#3b82f6'} />
              ))}
            </Bar>
            <ReferenceLine
              y={clinicMeanPercent}
              stroke="#ef4444"
              strokeDasharray="4 4"
              strokeWidth={1.5}
              label={{
                value: `Mean (${clinicMeanPercent}%)`,
                position: 'insideTopLeft',
                fill: '#ef4444',
                fontSize: 10,
                fontWeight: 600,
              }}
            />
          </BarChart>
        </ResponsiveContainer>
      </div>

      <div className="flex items-center justify-between pt-1 border-t border-[var(--color-border)] text-xs text-[var(--color-text-secondary)]">
        <span>Clinic Rollover Mean: <strong>{clinicMeanPercent}%</strong></span>
        <span>Rollovers: <strong>{data.totalRolloverEvents}</strong> / {data.totalCareSchedules} exams</span>
      </div>
    </Panel>
  );
};
