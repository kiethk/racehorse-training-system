'use client';

import Link from 'next/link';
import { Panel, SectionTitle, CapacityBar, Pill, Icon } from '@/components/ui';
import type { BarnOccupancyDto, StallSummary } from '../../types';

export interface BarnOccupancyWidgetProps {
  summary: StallSummary;
  barns: BarnOccupancyDto[];
}

export function BarnOccupancyWidget({ summary, barns }: BarnOccupancyWidgetProps) {
  return (
    <Panel padded className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <SectionTitle>Barn & Quarantine Capacity</SectionTitle>
          <p className="text-xs text-[var(--color-text-secondary)] mt-0.5">
            Total {summary.totalStalls} stalls · {summary.occupiedStalls} occupied · {summary.availableStalls} available
          </p>
        </div>

        <Link
          href="/manager/stable"
          className="text-xs font-semibold text-[var(--color-primary)] hover:underline inline-flex items-center gap-1"
        >
          Stable layout <Icon name="chevron-right" size={12} />
        </Link>
      </div>

      {/* Overall Facility Bar */}
      <div className="p-3 rounded-lg bg-[var(--color-surface-hover)]/40 border border-[var(--color-border)]">
        <CapacityBar
          label="Overall Facility Capacity"
          total={summary.totalStalls}
          occupied={summary.occupiedStalls}
          reserved={summary.reservedStalls}
          available={summary.availableStalls}
          size="md"
        />
      </div>

      {/* Barn by Barn Breakdown */}
      <div className="space-y-3 pt-1">
        {barns.map((barn) => {
          const isQuarantine = barn.areaType.toUpperCase() === 'QUARANTINE';
          return (
            <div
              key={barn.areaId}
              className="p-2.5 rounded-md border border-[var(--color-border)] bg-[var(--color-surface)] space-y-1.5"
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="font-semibold text-xs text-[var(--color-text-primary)]">
                    Barn {barn.areaCode}: {barn.areaName}
                  </span>
                  {isQuarantine && (
                    <Pill tone="warning" size="sm">
                      Quarantine
                    </Pill>
                  )}
                </div>
              </div>

              <CapacityBar
                label={`Barn ${barn.areaCode} Capacity`}
                total={barn.totalStalls}
                occupied={barn.occupiedStalls}
                reserved={barn.reservedStalls}
                available={barn.availableStalls}
                size="sm"
                showBreakdown={false}
              />
            </div>
          );
        })}
      </div>
    </Panel>
  );
}
