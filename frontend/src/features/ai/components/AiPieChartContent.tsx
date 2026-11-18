'use client';

import { formatNumber } from '@/lib/display';
import { describeArc } from '../lib/chartUtils';
import type { ChartConfig, ChartDataItem } from '../lib/chartUtils';

type AiPieChartContentProps = {
  config: ChartConfig;
  hoveredIndex: number | null;
  setHoveredIndex: (index: number | null) => void;
  activeItem: ChartDataItem | null;
};

export function AiPieChartContent({ config, hoveredIndex, setHoveredIndex, activeItem }: AiPieChartContentProps) {
  const isDonut = config.type === 'donut';
  const isPie = config.type === 'pie';
  // Calculate slices for Donut / Pie charts
  let currentAngle = 0;
  const slices = (isDonut || isPie)
    ? config.data.map((item) => {
        const sliceAngle =
          config.total > 0
            ? (item.value / config.total) * 2 * Math.PI
            : (1 / config.data.length) * 2 * Math.PI;
        const startAngle = currentAngle;
        const endAngle = currentAngle + sliceAngle;
        currentAngle = endAngle;

        const outerR = 80;
        const innerR = isDonut ? 50 : 0;
        const path = describeArc(100, 100, outerR, innerR, startAngle, endAngle);

        return {
          ...item,
          startAngle,
          endAngle,
          path,
        };
      })
    : [];
  return (
    <div className="flex flex-col sm:flex-row items-center gap-3.5">
            {/* SVG Canvas Donut/Pie */}
            <div className="relative flex shrink-0 items-center justify-center">
              <svg
                viewBox="0 0 200 200"
                className="h-44 w-44 overflow-visible"
                aria-label={`${config.type} chart`}
              >
                {config.total === 0 ? (
                  <circle
                    cx="100"
                    cy="100"
                    r="80"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="12"
                    className="text-neutral-200 dark:text-neutral-800"
                  />
                ) : (
                  slices.map((slice, idx) => {
                    const isHovered = hoveredIndex === idx;
                    return (
                      <path
                        key={idx}
                        d={slice.path}
                        fill={slice.color}
                        stroke="var(--color-surface, #ffffff)"
                        strokeWidth="1.5"
                        opacity={
                          hoveredIndex === null || isHovered ? 1 : 0.45
                        }
                        className="cursor-pointer transition-all duration-200"
                        onMouseEnter={() => setHoveredIndex(idx)}
                        onMouseLeave={() => setHoveredIndex(null)}
                      >
                        <title>{`${slice.label}: ${formatNumber(slice.value)} (${slice.percentage.toFixed(1)}%)`}</title>
                      </path>
                    );
                  })
                )}

                {/* Donut center displays the total or hovered item */}
                {isDonut && (
                  <g pointerEvents="none" className="transition-all">
                    {activeItem ? (
                      <>
                        <text
                          x="100"
                          y="92"
                          textAnchor="middle"
                          fontSize="10"
                          fontWeight="500"
                          fill="currentColor"
                          className="text-[var(--color-text-secondary)]"
                        >
                          {activeItem.label.length > 13
                            ? activeItem.label.slice(0, 12) + '…'
                            : activeItem.label}
                        </text>
                        <text
                          x="100"
                          y="112"
                          textAnchor="middle"
                          fontSize="13"
                          fontWeight="700"
                          fill="currentColor"
                          className="text-[var(--color-primary)] font-mono"
                        >
                          {formatNumber(activeItem.value)}{' '}
                          <tspan fontSize="10" fontWeight="500">
                            ({activeItem.percentage.toFixed(0)}%)
                          </tspan>
                        </text>
                      </>
                    ) : (
                      <>
                        <text
                          x="100"
                          y="92"
                          textAnchor="middle"
                          fontSize="9"
                          fontWeight="600"
                          letterSpacing="0.05em"
                          fill="currentColor"
                          className="text-[var(--color-text-secondary)] uppercase"
                        >
                          TOTAL
                        </text>
                        <text
                          x="100"
                          y="113"
                          textAnchor="middle"
                          fontSize="14"
                          fontWeight="700"
                          fill="currentColor"
                          className="text-[var(--color-text-primary)] font-mono"
                        >
                          {formatNumber(config.total)}
                        </text>
                      </>
                    )}
                  </g>
                )}
              </svg>
            </div>

            {/* Color legend */}
            <div className="flex-1 w-full min-w-0 space-y-1">
              {config.data.map((item, idx) => {
                const isHovered = hoveredIndex === idx;
                return (
                  <div
                    key={idx}
                    onMouseEnter={() => setHoveredIndex(idx)}
                    onMouseLeave={() => setHoveredIndex(null)}
                    className={`flex items-center justify-between gap-2 rounded-lg px-2.5 py-1 text-xs transition-colors cursor-pointer ${
                      isHovered
                        ? 'bg-[var(--color-primary)]/10 text-[var(--color-primary)]'
                        : 'hover:bg-[var(--color-surface-muted)]'
                    }`}
                  >
                    <div className="flex items-center gap-2 min-w-0">
                      <span
                        className="h-2.5 w-2.5 rounded-full shrink-0 transition-transform"
                        style={{
                          backgroundColor: item.color,
                          transform: isHovered ? 'scale(1.25)' : 'scale(1)',
                        }}
                      />
                      <span
                        className={`truncate text-[11px] ${
                          isHovered
                            ? 'font-semibold text-[var(--color-primary)]'
                            : 'font-medium text-[var(--color-text-primary)]'
                        }`}
                      >
                        {item.label}
                      </span>
                    </div>
                    <div className="shrink-0 text-right font-mono text-[11px]">
                      <span className="font-semibold text-[var(--color-text-primary)]">
                        {formatNumber(item.value)}
                      </span>
                      <span className="ml-1 text-[10px] text-[var(--color-text-secondary)]">
                        ({item.percentage.toFixed(1)}%)
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
    </div>
  );
}
