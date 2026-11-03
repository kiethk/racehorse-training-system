'use client';

import React from 'react';
import type { EdaDistributionStats } from '../types/eda';

export interface StatisticalSummaryTableProps {
  /** Optional single stat title (backwards compatibility) */
  title?: string;
  /** Optional single stat unit */
  unit?: string;
  /** Optional single stat object */
  stats?: EdaDistributionStats;
  /** Fleet or individual speed distribution stats */
  speedStats?: EdaDistributionStats;
  /** Fleet or individual recovery heart rate distribution stats */
  recoveryHeartRateStats?: EdaDistributionStats;
  /** Optional custom header label for speed column (defaults to "Average Speed (km/h)") */
  speedHeader?: string;
  /** Optional custom header label for recovery HR column (defaults to "Recovery HR (bpm)") */
  recoveryHrHeader?: string;
}

interface MetricRow {
  metric: string;
  speedVal?: string | number;
  hrVal?: string | number;
  singleVal?: string | number;
}

export function StatisticalSummaryTable({
  title,
  unit,
  stats,
  speedStats,
  recoveryHeartRateStats,
  speedHeader = 'Average Speed (km/h)',
  recoveryHrHeader = 'Recovery HR (bpm)',
}: StatisticalSummaryTableProps) {
  // Dual-metric mode (speed + recovery HR)
  if (speedStats && recoveryHeartRateStats) {
    const rows: MetricRow[] = [
      {
        metric: 'Count',
        speedVal: speedStats.sampleSize,
        hrVal: recoveryHeartRateStats.sampleSize,
      },
      {
        metric: 'Min',
        speedVal: `${speedStats.min.toFixed(1)} km/h`,
        hrVal: `${recoveryHeartRateStats.min.toFixed(1)} bpm`,
      },
      {
        metric: 'Q1 (25th %)',
        speedVal: `${speedStats.q1.toFixed(1)} km/h`,
        hrVal: `${recoveryHeartRateStats.q1.toFixed(1)} bpm`,
      },
      {
        metric: 'Median',
        speedVal: `${speedStats.median.toFixed(1)} km/h`,
        hrVal: `${recoveryHeartRateStats.median.toFixed(1)} bpm`,
      },
      {
        metric: 'Mean',
        speedVal: `${speedStats.mean.toFixed(1)} km/h`,
        hrVal: `${recoveryHeartRateStats.mean.toFixed(1)} bpm`,
      },
      {
        metric: 'Q3 (75th %)',
        speedVal: `${speedStats.q3.toFixed(1)} km/h`,
        hrVal: `${recoveryHeartRateStats.q3.toFixed(1)} bpm`,
      },
      {
        metric: 'Max',
        speedVal: `${speedStats.max.toFixed(1)} km/h`,
        hrVal: `${recoveryHeartRateStats.max.toFixed(1)} bpm`,
      },
      {
        metric: 'Std Dev',
        speedVal: `±${speedStats.stdDev.toFixed(1)} km/h`,
        hrVal: `±${recoveryHeartRateStats.stdDev.toFixed(1)} bpm`,
      },
      {
        metric: 'Outliers count',
        speedVal: speedStats.outlierCount,
        hrVal: recoveryHeartRateStats.outlierCount,
      },
    ];

    return (
      <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] overflow-hidden shadow-xs text-xs">
        <div className="px-3.5 py-2.5 border-b border-[var(--color-border)] bg-[var(--color-surface-muted)]/40 flex items-center justify-between">
          <span className="font-semibold text-[var(--color-text-primary)]">
            Descriptive Statistics Summary
          </span>
          <span className="text-[11px] text-[var(--color-text-secondary)]">
            Tukey 1.5×IQR Distribution Analysis
          </span>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-left">
            <thead>
              <tr className="border-b border-[var(--color-border)] bg-[var(--color-surface-muted)]/20 text-[11px] text-[var(--color-text-secondary)] uppercase tracking-wider">
                <th scope="col" className="px-3.5 py-2 font-semibold">
                  Metric
                </th>
                <th scope="col" className="px-3.5 py-2 font-semibold text-right">
                  {speedHeader}
                </th>
                <th scope="col" className="px-3.5 py-2 font-semibold text-right">
                  {recoveryHrHeader}
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--color-border)]/50">
              {rows.map((r) => {
                const isOutlierRow = r.metric === 'Outliers count';
                const hasOutlier =
                  isOutlierRow &&
                  ((Number(r.speedVal) > 0) || (Number(r.hrVal) > 0));

                return (
                  <tr
                    key={r.metric}
                    className={`hover:bg-[var(--color-surface-hover)]/30 transition-colors ${
                      isOutlierRow ? 'bg-[var(--color-surface-muted)]/30 font-medium' : ''
                    }`}
                  >
                    <td className="px-3.5 py-1.5 font-medium text-[var(--color-text-primary)]">
                      {r.metric}
                    </td>
                    <td
                      className={`px-3.5 py-1.5 text-right font-mono ${
                        isOutlierRow && Number(r.speedVal) > 0
                          ? 'text-[var(--color-warning)] font-semibold'
                          : 'text-[var(--color-text-primary)]'
                      }`}
                    >
                      {r.speedVal}
                    </td>
                    <td
                      className={`px-3.5 py-1.5 text-right font-mono ${
                        isOutlierRow && Number(r.hrVal) > 0
                          ? 'text-[var(--color-warning)] font-semibold'
                          : 'text-[var(--color-text-primary)]'
                      }`}
                    >
                      {r.hrVal}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    );
  }

  // Single metric mode fallback
  if (stats) {
    const singleRows: MetricRow[] = [
      { metric: 'Count', singleVal: stats.sampleSize },
      { metric: 'Min', singleVal: `${stats.min.toFixed(1)} ${unit || ''}` },
      { metric: 'Q1 (25th %)', singleVal: `${stats.q1.toFixed(1)} ${unit || ''}` },
      { metric: 'Median', singleVal: `${stats.median.toFixed(1)} ${unit || ''}` },
      { metric: 'Mean', singleVal: `${stats.mean.toFixed(1)} ${unit || ''}` },
      { metric: 'Q3 (75th %)', singleVal: `${stats.q3.toFixed(1)} ${unit || ''}` },
      { metric: 'Max', singleVal: `${stats.max.toFixed(1)} ${unit || ''}` },
      { metric: 'Std Dev', singleVal: `±${stats.stdDev.toFixed(1)} ${unit || ''}` },
      { metric: 'Outliers count', singleVal: stats.outlierCount },
    ];

    return (
      <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] overflow-hidden shadow-xs text-xs">
        <div className="px-3.5 py-2.5 border-b border-[var(--color-border)] bg-[var(--color-surface-muted)]/40 flex items-center justify-between">
          <span className="font-semibold text-[var(--color-text-primary)]">
            {title || 'Statistical Summary'}
          </span>
          <span className="text-[11px] text-[var(--color-text-secondary)]">
            Sample: <b>{stats.sampleSize}</b> | Outliers: <b className={stats.outlierCount > 0 ? 'text-[var(--color-warning)]' : ''}>{stats.outlierCount}</b>
          </span>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-left">
            <thead>
              <tr className="border-b border-[var(--color-border)] bg-[var(--color-surface-muted)]/20 text-[11px] text-[var(--color-text-secondary)] uppercase tracking-wider">
                <th scope="col" className="px-3.5 py-2 font-semibold">
                  Metric
                </th>
                <th scope="col" className="px-3.5 py-2 font-semibold text-right">
                  {title ? `${title} (${unit || ''})` : 'Value'}
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--color-border)]/50">
              {singleRows.map((r) => (
                <tr key={r.metric} className="hover:bg-[var(--color-surface-hover)]/30 transition-colors">
                  <td className="px-3.5 py-1.5 font-medium text-[var(--color-text-primary)]">
                    {r.metric}
                  </td>
                  <td className="px-3.5 py-1.5 text-right font-mono text-[var(--color-text-primary)]">
                    {r.singleVal}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    );
  }

  return null;
}
