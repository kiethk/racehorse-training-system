'use client';

import Link from 'next/link';
import { Panel, SectionTitle, DonutChart, Icon, type DonutSegment } from '@/components/ui';
import type { FleetSummary } from '../../types';

export interface FleetDistributionWidgetProps {
  summary: FleetSummary;
}

export function FleetDistributionWidget({ summary }: FleetDistributionWidgetProps) {
  const total = summary.totalHorses;

  const segments: DonutSegment[] = [
    {
      id: 'training',
      label: 'In Training',
      value: summary.inTraining,
      color: '#0284c7', // Sky 600
    },
    {
      id: 'eligible',
      label: 'Eligible (Ready)',
      value: summary.activeEligible,
      color: '#10b981', // Emerald 500
    },
    {
      id: 'medical',
      label: 'Medical Hold',
      value: summary.medicalBlocked,
      color: '#f59e0b', // Amber 500
    },
    {
      id: 'inactive',
      label: 'Inactive',
      value: summary.inactive,
      color: '#94a3b8', // Slate 400
    },
  ];

  return (
    <Panel padded className="space-y-4 flex flex-col justify-between">
      <div>
        <div className="flex items-center justify-between">
          <div>
            <SectionTitle>Fleet Readiness & Training Distribution</SectionTitle>
            <p className="text-xs text-[var(--color-text-secondary)] mt-0.5">
              Total fleet: <strong>{total}</strong> registered equine athlete(s)
            </p>
          </div>

          <Link
            href="/manager/horses"
            className="text-xs font-semibold text-[var(--color-primary)] hover:underline inline-flex items-center gap-1"
          >
            Horse profiles <Icon name="chevron-right" size={12} />
          </Link>
        </div>

        {/* Pure SVG Donut Chart with Responsive Flex (sm:flex-row, flex-col on mobile) */}
        <div className="pt-4 pb-2">
          <DonutChart
            segments={segments}
            totalLabel="Horses"
            size={160}
            strokeWidth={14}
            showLegend={true}
          />
        </div>
      </div>

      {/* Quick Status Context Summary Footer */}
      <div className="pt-2 border-t border-[var(--color-border)] flex items-center justify-between text-xs text-[var(--color-text-muted)]">
        <span>Active Training Rate: <strong>{total > 0 ? Math.round((summary.inTraining / total) * 100) : 0}%</strong></span>
        <span>Biosecurity Clear: <strong>{total - summary.medicalBlocked}/{total}</strong></span>
      </div>
    </Panel>
  );
}
