'use client';

import { useMemo } from 'react';
import Link from 'next/link';
import { Panel, SectionTitle, DonutChart, Icon, type DonutSegment } from '@/components/ui';
import type { DashboardHorse, FleetSummary } from '../../types';

export interface FleetDistributionWidgetProps {
  summary: FleetSummary;
  horses?: DashboardHorse[];
}

export function FleetDistributionWidget({ summary, horses = [] }: FleetDistributionWidgetProps) {
  const total = summary.totalHorses;

  // Compute ready/idle athletes without double-counting those in training or on medical hold
  const readyHorses = Math.max(0, summary.activeEligible - summary.inTraining - summary.medicalBlocked);

  const readinessSegments: DonutSegment[] = [
    {
      id: 'training',
      label: 'In Training',
      value: summary.inTraining,
      color: '#0284c7', // Sky 600
    },
    {
      id: 'ready',
      label: 'Ready / Available',
      value: readyHorses,
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

  // Group horses by distance specialty
  const specialtyCounts = useMemo(() => {
    const counts = {
      SPRINT: 0,
      MILE: 0,
      MIDDLE: 0,
      STAYER: 0,
      DEVELOPING: 0,
    };

    if (!horses || horses.length === 0) {
      return counts;
    }

    for (const horse of horses) {
      const strong = horse.specialty?.strongDistances ?? [];
      if (strong.length === 0) {
        counts.DEVELOPING += 1;
      } else if (strong.length === 1) {
        const band = strong[0];
        if (band in counts) {
          counts[band as keyof typeof counts] += 1;
        } else {
          counts.DEVELOPING += 1;
        }
      } else {
        // Pick primary distance band with highest averageRating from distanceStats
        const stats = horse.specialty?.distanceStats ?? [];
        const sorted = [...strong].sort((a, b) => {
          const statA = stats.find((s) => s.band === a)?.averageRating ?? 0;
          const statB = stats.find((s) => s.band === b)?.averageRating ?? 0;
          return statB - statA;
        });
        const bestBand = sorted[0];
        if (bestBand in counts) {
          counts[bestBand as keyof typeof counts] += 1;
        } else {
          counts.DEVELOPING += 1;
        }
      }
    }

    return counts;
  }, [horses]);

  const specialtySegments: DonutSegment[] = [
    {
      id: 'sprint',
      label: 'Sprint (< 1,400m)',
      value: specialtyCounts.SPRINT,
      color: '#f59e0b', // Amber 500
    },
    {
      id: 'mile',
      label: 'Mile (1,400–1,800m)',
      value: specialtyCounts.MILE,
      color: '#0284c7', // Sky 600
    },
    {
      id: 'middle',
      label: 'Middle (1,800–2,200m)',
      value: specialtyCounts.MIDDLE,
      color: '#8b5cf6', // Purple 500
    },
    {
      id: 'stayer',
      label: 'Stayer (> 2,200m)',
      value: specialtyCounts.STAYER,
      color: '#10b981', // Emerald 500
    },
    {
      id: 'developing',
      label: 'Developing / Undecided',
      value: specialtyCounts.DEVELOPING,
      color: '#94a3b8', // Slate 400
    },
  ];

  const specialistTotal = total - specialtyCounts.DEVELOPING;

  return (
    <Panel padded className="space-y-4 flex flex-col justify-between">
      <div>
        <div className="flex items-center justify-between">
          <div>
            <SectionTitle>Fleet Readiness & Distance Specialty</SectionTitle>
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

        {/* 1. Fleet Readiness Donut Chart */}
        <div className="pt-3 pb-1">
          <div className="text-[11px] font-semibold text-[var(--color-text-muted)] uppercase tracking-wider mb-1.5">
            Fleet Readiness by Status
          </div>
          <DonutChart
            segments={readinessSegments}
            totalLabel="Horses"
            size={130}
            strokeWidth={12}
            showLegend={true}
          />
        </div>

        <div className="border-t border-[var(--color-border)] my-2" />

        {/* 2. Distance Specialty Profile Donut Chart */}
        <div className="pt-1 pb-1">
          <div className="text-[11px] font-semibold text-[var(--color-text-muted)] uppercase tracking-wider mb-1.5">
            Distance Specialty Distribution
          </div>
          <DonutChart
            segments={specialtySegments}
            totalLabel="Horses"
            size={130}
            strokeWidth={12}
            showLegend={true}
          />
        </div>
      </div>

      {/* Quick Status Context Summary Footer */}
      <div className="pt-2 border-t border-[var(--color-border)] flex items-center justify-between text-xs text-[var(--color-text-muted)]">
        <span>Active Training Rate: <strong>{total > 0 ? Math.round((summary.inTraining / total) * 100) : 0}%</strong></span>
        <span>Specialists In Fleet: <strong>{Math.max(0, specialistTotal)}/{total}</strong></span>
      </div>
    </Panel>
  );
}
