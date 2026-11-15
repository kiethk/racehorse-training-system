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
} from 'recharts';
import { Panel, Pill, Icon, Skeleton, Notice } from '@/components/ui';
import { managerEdaService } from '@/features/manager-analytics/services/managerEdaService';
import type { AdmissionFunnelEdaDto } from '@/features/manager-analytics/types/managerEda';
import { formatAdmissionStage } from '@/features/manager-analytics/lib/edaFormatters';

const STAGE_ORDER = [
  'Submitted',
  'Groom Checked',
  'Arrived at Facility',
  'Vet Examined',
  'Trainer Evaluated',
  'Manager Cleared',
];

export const AdmissionFunnelExecutiveWidget: React.FC = () => {
  const [data, setData] = useState<AdmissionFunnelEdaDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    managerEdaService
      .getOverview(90)
      .then((res) => {
        if (active) {
          setData(res.admissionFunnel);
          setLoading(false);
        }
      })
      .catch((err) => {
        if (active) {
          setError(err?.message || 'Unable to load admission funnel.');
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
          Admission Pipeline Funnel
        </h3>
        <Notice tone="error">{error || 'Data unavailable'}</Notice>
      </Panel>
    );
  }

  const sortedStages = [...data.stageFunnel].sort((a, b) => {
    const idxA = STAGE_ORDER.indexOf(a.stage);
    const idxB = STAGE_ORDER.indexOf(b.stage);
    if (idxA !== -1 && idxB !== -1) return idxA - idxB;
    return 0;
  });

  const chartData = sortedStages.map((s) => ({
    stage: s.stage,
    count: s.count,
    conversion: s.conversionPercentage,
  }));

  return (
    <Panel padded className="space-y-3">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">
            Admission Pipeline Funnel
          </h3>
          {data.bottleneckStage !== 'NONE' && (
            <Pill tone="warning" size="sm">
              Bottleneck: {formatAdmissionStage(data.bottleneckStage)}
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
            <XAxis dataKey="stage" tick={{ fontSize: 10 }} interval={0} angle={-15} textAnchor="end" />
            <YAxis unit="%" domain={[0, 100]} width={40} tick={{ fontSize: 10 }} />
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

      <div className="flex items-center justify-between pt-1 border-t border-[var(--color-border)] text-xs text-[var(--color-text-secondary)]">
        <span>Total: <strong>{data.totalApplications}</strong> applications</span>
        <span>Clearance: <strong className="text-emerald-600 dark:text-emerald-400">{data.approvalRate.toFixed(1)}%</strong> ({data.approvedCount} admitted)</span>
      </div>
    </Panel>
  );
};
