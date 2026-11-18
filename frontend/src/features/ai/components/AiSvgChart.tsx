'use client';

import React, { useState } from 'react';
import { AiPieChartContent } from './AiPieChartContent';
import { formatNumber } from '@/lib/display';
import {
  ChartConfig,
  describeArc,
  parseChartConfig,
  polarToCartesian,
  CHART_PALETTE,
} from '../lib/chartUtils';

export type { ChartDataItem, ChartConfig } from '../lib/chartUtils';
export { parseChartConfig, describeArc, polarToCartesian, CHART_PALETTE };

interface AiSvgChartProps {
  config: ChartConfig;
}

export function AiSvgChart({ config }: AiSvgChartProps) {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);

  const isDonut = config.type === 'donut';
  const isPie = config.type === 'pie';
  const isBar = config.type === 'bar';

  const activeItem = hoveredIndex !== null ? config.data[hoveredIndex] : null;

  return (
    <div className="my-2.5 overflow-hidden rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] shadow-xs transition-shadow">
      {/* Clear chart title */}
      <div className="flex items-center justify-between border-b border-[var(--color-border)] bg-[var(--color-surface-muted)]/70 px-3.5 py-2">
        <div className="flex items-center gap-2 min-w-0">
          <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-md bg-[var(--color-primary)]/10 text-[var(--color-primary)]">
            {isBar ? (
              <svg
                width="13"
                height="13"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <line x1="18" y1="20" x2="18" y2="10" />
                <line x1="12" y1="20" x2="12" y2="4" />
                <line x1="6" y1="20" x2="6" y2="14" />
              </svg>
            ) : (
              <svg
                width="13"
                height="13"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M21.21 15.89A10 10 0 1 1 8 2.83" />
                <path d="M22 12A10 10 0 0 0 12 2v10z" />
              </svg>
            )}
          </span>
          <span className="truncate text-xs font-semibold text-[var(--color-text-primary)]">
            {config.title ||
              (isBar
                ? 'Bar chart of counts'
                : isDonut
                ? 'Donut chart proportions'
                : 'Pie chart proportions')}
          </span>
        </div>
        <span className="shrink-0 rounded bg-neutral-200/60 px-1.5 py-0.5 text-[9px] font-semibold uppercase tracking-wider text-[var(--color-text-secondary)] dark:bg-neutral-800">
          {config.type}
        </span>
      </div>

      {config.subtitle && (
        <div className="border-b border-[var(--color-border)]/50 px-3.5 py-1 text-[11px] text-[var(--color-text-secondary)]">
          {config.subtitle}
        </div>
      )}

      {/* Chart contents */}
      <div className="p-3">
        {(isDonut || isPie) && (
          <AiPieChartContent
            config={config}
            hoveredIndex={hoveredIndex}
            setHoveredIndex={setHoveredIndex}
            activeItem={activeItem}
          />
        )}

        {/* Bar chart */}
        {isBar && (
          <div>
            {config.orientation === 'vertical' ? (
              /* Vertical bars */
              <div className="space-y-2">
                <svg
                  viewBox="0 0 340 180"
                  className="w-full h-auto overflow-visible"
                  aria-label="Vertical bar chart"
                >
                  {(() => {
                    const maxVal = Math.max(...config.data.map((d) => d.value), 1);
                    const count = config.data.length;
                    const step = (340 - 30) / count;
                    const colWidth = Math.min(step * 0.65, 34);

                    return (
                      <>
                        {/* Baseline */}
                        <line
                          x1="10"
                          y1="145"
                          x2="330"
                          y2="145"
                          stroke="currentColor"
                          strokeWidth="1"
                          className="text-[var(--color-border)]"
                        />
                        {config.data.map((item, idx) => {
                          const isHovered = hoveredIndex === idx;
                          const h = (item.value / maxVal) * 115;
                          const x = 15 + idx * step + (step - colWidth) / 2;
                          const y = 145 - h;

                          return (
                            <g
                              key={idx}
                              className="cursor-pointer"
                              onMouseEnter={() => setHoveredIndex(idx)}
                              onMouseLeave={() => setHoveredIndex(null)}
                            >
                              <rect
                                x={x}
                                y={y}
                                width={colWidth}
                                height={Math.max(h, 2)}
                                rx="4"
                                fill={item.color}
                                opacity={
                                  hoveredIndex === null || isHovered ? 1 : 0.45
                                }
                                className="transition-all duration-200"
                              >
                                <title>{`${item.label}: ${formatNumber(item.value)} (${item.percentage.toFixed(1)}%)`}</title>
                              </rect>
                              <text
                                x={x + colWidth / 2}
                                y={y - 5}
                                textAnchor="middle"
                                fontSize="10"
                                fontWeight="600"
                                fill="currentColor"
                                className="text-[var(--color-text-secondary)] font-mono"
                              >
                                {formatNumber(item.value)}
                              </text>
                              <text
                                x={x + colWidth / 2}
                                y="162"
                                textAnchor="middle"
                                fontSize="10"
                                fontWeight="500"
                                fill="currentColor"
                                className="text-[var(--color-text-primary)]"
                              >
                                {item.label.length > 8
                                  ? item.label.slice(0, 7) + '…'
                                  : item.label}
                              </text>
                            </g>
                          );
                        })}
                      </>
                    );
                  })()}
                </svg>
              </div>
            ) : (
              /* Horizontal bars, best suited to chat */
              <div className="space-y-2">
                <svg
                  viewBox={`0 0 340 ${config.data.length * 36 + 10}`}
                  className="w-full h-auto overflow-visible"
                  aria-label="Horizontal bar chart"
                >
                  {(() => {
                    const maxVal = Math.max(...config.data.map((d) => d.value), 1);
                    const labelWidth = 100;
                    const trackWidth = 185;

                    return config.data.map((item, idx) => {
                      const isHovered = hoveredIndex === idx;
                      const y = 8 + idx * 36;
                      const barWidth = (item.value / maxVal) * trackWidth;

                      return (
                        <g
                          key={idx}
                          className="cursor-pointer"
                          onMouseEnter={() => setHoveredIndex(idx)}
                          onMouseLeave={() => setHoveredIndex(null)}
                        >
                          {/* Row label */}
                          <text
                            x={labelWidth - 8}
                            y={y + 13}
                            textAnchor="end"
                            fontSize="11"
                            fontWeight="500"
                            fill="currentColor"
                            className={
                              isHovered
                                ? 'text-[var(--color-primary)] font-semibold'
                                : 'text-[var(--color-text-primary)]'
                            }
                          >
                            {item.label.length > 14
                              ? item.label.slice(0, 13) + '…'
                              : item.label}
                          </text>

                          {/* Background track */}
                          <rect
                            x={labelWidth}
                            y={y}
                            width={trackWidth}
                            height="18"
                            rx="4"
                            fill="currentColor"
                            className="text-neutral-200/70 dark:text-neutral-800/80"
                          />

                          {/* Value bar */}
                          <rect
                            x={labelWidth}
                            y={y}
                            width={Math.max(barWidth, 3)}
                            height="18"
                            rx="4"
                            fill={item.color}
                            opacity={
                              hoveredIndex === null || isHovered ? 1 : 0.45
                            }
                            className="transition-all duration-200"
                          >
                            <title>{`${item.label}: ${formatNumber(item.value)} (${item.percentage.toFixed(1)}%)`}</title>
                          </rect>

                          {/* Count displayed to the right of the bar */}
                          <text
                            x={labelWidth + barWidth + 8}
                            y={y + 13}
                            fontSize="10"
                            fontWeight="600"
                            fill="currentColor"
                            className="text-[var(--color-text-secondary)] font-mono"
                          >
                            {formatNumber(item.value)}
                            {config.unit ? ` ${config.unit}` : ''}
                          </text>
                        </g>
                      );
                    });
                  })()}
                </svg>
              </div>
            )}
          </div>
        )}

        {/* Tooltip / hover details */}
        {activeItem && (
          <div className="mt-2.5 flex items-center justify-between rounded-lg border border-[var(--color-border)]/60 bg-[var(--color-surface-muted)] px-3 py-1.5 text-xs text-[var(--color-text-secondary)] transition-all">
            <span className="flex items-center gap-1.5">
              <span
                className="h-2 w-2 rounded-full"
                style={{ backgroundColor: activeItem.color }}
              />
              <span className="font-medium text-[var(--color-text-primary)]">
                {activeItem.label}
              </span>
            </span>
            <span className="font-mono font-semibold text-[var(--color-primary)]">
              {activeItem.value.toLocaleString()}{' '}
              <span className="text-[10px] text-[var(--color-text-secondary)]">
                ({activeItem.percentage.toFixed(1)}%)
              </span>
            </span>
          </div>
        )}
      </div>
    </div>
  );
}
