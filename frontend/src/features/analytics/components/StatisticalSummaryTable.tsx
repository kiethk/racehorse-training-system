'use client';

import React from 'react';
import type { EdaDistributionStats } from '../types/eda';

interface StatisticalSummaryTableProps {
  title: string;
  unit: string;
  stats: EdaDistributionStats;
}

export function StatisticalSummaryTable({ title, unit, stats }: StatisticalSummaryTableProps) {
  return (
    <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-3 text-xs">
      <div className="flex items-center justify-between pb-2 border-b border-[var(--color-border)] mb-2 font-medium text-[var(--color-text-primary)]">
        <span>{title}</span>
        <span className="text-[var(--color-text-secondary)] font-normal text-[11px]">
          Mẫu: <b>{stats.sampleSize}</b> | Dị biệt (Outliers): <b className={stats.outlierCount > 0 ? 'text-[var(--color-warning)]' : ''}>{stats.outlierCount}</b>
        </span>
      </div>

      <div className="grid grid-cols-4 gap-2 text-center">
        <div className="bg-[var(--color-surface-muted)] rounded p-1.5">
          <div className="text-[10px] text-[var(--color-text-secondary)] uppercase">Trung bình (Mean)</div>
          <div className="font-semibold text-sm mt-0.5">{stats.mean.toFixed(1)} <span className="text-[10px] font-normal">{unit}</span></div>
        </div>
        <div className="bg-[var(--color-surface-muted)] rounded p-1.5">
          <div className="text-[10px] text-[var(--color-text-secondary)] uppercase">Trung vị (Median)</div>
          <div className="font-semibold text-sm mt-0.5">{stats.median.toFixed(1)} <span className="text-[10px] font-normal">{unit}</span></div>
        </div>
        <div className="bg-[var(--color-surface-muted)] rounded p-1.5">
          <div className="text-[10px] text-[var(--color-text-secondary)] uppercase">Khoảng IQR (Q3 - Q1)</div>
          <div className="font-semibold text-sm mt-0.5">{stats.iqr.toFixed(1)} <span className="text-[10px] font-normal">{unit}</span></div>
        </div>
        <div className="bg-[var(--color-surface-muted)] rounded p-1.5">
          <div className="text-[10px] text-[var(--color-text-secondary)] uppercase">Độ lệch chuẩn (StdDev)</div>
          <div className="font-semibold text-sm mt-0.5">±{stats.stdDev.toFixed(1)} <span className="text-[10px] font-normal">{unit}</span></div>
        </div>
      </div>

      <div className="mt-2 pt-2 border-t border-[var(--color-border)]/50 flex justify-between text-[11px] text-[var(--color-text-secondary)]">
        <span>Min: <b>{stats.min.toFixed(1)} {unit}</b></span>
        <span>Q1 (25%): <b>{stats.q1.toFixed(1)} {unit}</b></span>
        <span>Q3 (75%): <b>{stats.q3.toFixed(1)} {unit}</b></span>
        <span>Max: <b>{stats.max.toFixed(1)} {unit}</b></span>
      </div>
    </div>
  );
}
