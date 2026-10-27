'use client';

import { useId, useState } from 'react';
import clsx from 'clsx';
import { formatNumber } from '@/lib/display';

export interface DonutSegment {
  id: string;
  label: string;
  value: number;
  color: string;
  hint?: string;
}

export interface DonutChartProps {
  segments: DonutSegment[];
  totalLabel?: string;
  size?: number; // Size in px on desktop, default 170
  strokeWidth?: number; // default 14
  className?: string;
  showLegend?: boolean;
}

export function DonutChart({
  segments,
  totalLabel = 'Total',
  size = 170,
  strokeWidth = 14,
  className,
  showLegend = true,
}: DonutChartProps) {
  const [activeSegmentId, setActiveSegmentId] = useState<string | null>(null);
  const chartId = useId();

  const total = segments.reduce((sum, s) => sum + Math.max(0, s.value), 0);

  // SVG dimensions & circle metrics
  const radius = 38;
  const circumference = 2 * Math.PI * radius; // ~238.761

  let accumulatedLength = 0;
  const renderedSlices = segments.map((segment) => {
    const val = Math.max(0, segment.value);
    const fraction = total > 0 ? val / total : 0;
    const strokeDash = fraction * circumference;
    const offset = -accumulatedLength;
    accumulatedLength += strokeDash;

    return {
      ...segment,
      fraction,
      percent: fraction * 100,
      strokeDasharray: `${strokeDash} ${circumference - strokeDash}`,
      strokeDashoffset: offset,
    };
  });

  const activeSegment = segments.find((s) => s.id === activeSegmentId);

  return (
    <div
      className={clsx(
        'flex flex-col sm:flex-row items-center justify-center gap-6 w-full',
        className
      )}
    >
      {/* Donut SVG Viewport */}
      <div
        className="relative shrink-0 flex items-center justify-center"
        style={{ width: size, height: size }}
      >
        <svg
          viewBox="0 0 100 100"
          className="w-full h-full transform transition-transform duration-300"
          aria-label={`Donut chart showing ${totalLabel}`}
          role="img"
        >
          {/* Base Background Track (empty circle) */}
          <circle
            cx="50"
            cy="50"
            r={radius}
            fill="transparent"
            stroke="var(--color-neutral-subtle)"
            strokeWidth={strokeWidth}
          />

          {/* Slices using stroke-dasharray and stroke-dashoffset */}
          {total > 0 &&
            renderedSlices.map((slice) => {
              const isHovered = activeSegmentId === slice.id;
              return (
                <circle
                  key={`${chartId}-${slice.id}`}
                  cx="50"
                  cy="50"
                  r={radius}
                  fill="transparent"
                  stroke={slice.color}
                  strokeWidth={isHovered ? strokeWidth + 2.5 : strokeWidth}
                  strokeDasharray={slice.strokeDasharray}
                  strokeDashoffset={slice.strokeDashoffset}
                  transform="rotate(-90 50 50)"
                  className="transition-all duration-300 cursor-pointer"
                  style={{
                    opacity: activeSegmentId && !isHovered ? 0.45 : 1,
                  }}
                  onMouseEnter={() => setActiveSegmentId(slice.id)}
                  onMouseLeave={() => setActiveSegmentId(null)}
                />
              );
            })}
        </svg>

        {/* Center Summary Text (Zero hydration mismatch) */}
        <div className="absolute inset-0 flex flex-col items-center justify-center text-center pointer-events-none select-none px-3">
          <span className="font-metric text-2xl font-bold leading-tight text-[var(--color-text-primary)] transition-all">
            {activeSegment ? activeSegment.value : total}
          </span>
          <span className="text-[11px] font-medium uppercase tracking-wider text-[var(--color-text-muted)] line-clamp-1">
            {activeSegment ? activeSegment.label : totalLabel}
          </span>
        </div>
      </div>

      {/* Legend & Breakdown - Responsive: vertical stack beside or below */}
      {showLegend && (
        <div className="w-full sm:flex-1 space-y-2">
          {renderedSlices.map((slice) => {
            const isHovered = activeSegmentId === slice.id;
            return (
              <div
                key={`legend-${slice.id}`}
                className={clsx(
                  'flex items-center justify-between p-2 rounded-md transition-colors cursor-pointer text-xs border border-transparent',
                  isHovered
                    ? 'bg-[var(--color-surface-hover)] border-[var(--color-border)] shadow-2xs'
                    : 'hover:bg-[var(--color-surface-hover)]/50'
                )}
                onMouseEnter={() => setActiveSegmentId(slice.id)}
                onMouseLeave={() => setActiveSegmentId(null)}
              >
                <div className="flex items-center gap-2.5 min-w-0">
                  <span
                    className="inline-block h-3 w-3 rounded-full shrink-0 shadow-2xs"
                    style={{ backgroundColor: slice.color }}
                  />
                  <span className="font-medium text-[var(--color-text-primary)] truncate">
                    {slice.label}
                  </span>
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  <span className="font-semibold text-[var(--color-text-primary)]">
                    {slice.value}
                  </span>
                  <span className="text-[11px] text-[var(--color-text-muted)] w-11 text-right">
                    {formatNumber(slice.percent, 0)}%
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
