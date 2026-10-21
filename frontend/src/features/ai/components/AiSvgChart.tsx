'use client';

import React, { useState } from 'react';
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

  // Tính toán các lát cắt cho Donut / Pie
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

  const activeItem = hoveredIndex !== null ? config.data[hoveredIndex] : null;

  return (
    <div className="my-2.5 overflow-hidden rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] shadow-xs transition-shadow">
      {/* Tiêu đề biểu đồ rõ ràng */}
      <div className="flex items-center justify-between border-b border-[var(--color-border)] bg-[var(--color-surface-muted)]/70 px-3.5 py-2">
        <div className="flex items-center gap-2 min-w-0">
          <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-md bg-emerald-500/10 text-emerald-600 dark:text-emerald-400">
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
                ? 'Biểu đồ thanh số lượng'
                : isDonut
                ? 'Biểu đồ tỷ lệ Donut'
                : 'Biểu đồ tỷ lệ tròn')}
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

      {/* Nội dung vẽ biểu đồ */}
      <div className="p-3">
        {(isDonut || isPie) && (
          <div className="flex flex-col sm:flex-row items-center gap-3.5">
            {/* SVG Canvas Donut/Pie */}
            <div className="relative flex shrink-0 items-center justify-center">
              <svg
                viewBox="0 0 200 200"
                className="h-44 w-44 overflow-visible"
                aria-label={`Biểu đồ ${config.type}`}
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
                        <title>{`${slice.label}: ${slice.value.toLocaleString()} (${slice.percentage.toFixed(1)}%)`}</title>
                      </path>
                    );
                  })
                )}

                {/* Vùng tâm Donut hiển thị Tổng hoặc Item đang hover */}
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
                          className="text-emerald-600 dark:text-emerald-400 font-mono"
                        >
                          {activeItem.value.toLocaleString()}{' '}
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
                          TỔNG SỐ
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
                          {config.total.toLocaleString()}
                        </text>
                      </>
                    )}
                  </g>
                )}
              </svg>
            </div>

            {/* Chú giải màu sắc (Legends) bên cạnh */}
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
                        ? 'bg-emerald-500/10 dark:bg-emerald-400/15'
                        : 'hover:bg-neutral-100 dark:hover:bg-neutral-800/60'
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
                            ? 'font-semibold text-emerald-700 dark:text-emerald-300'
                            : 'font-medium text-[var(--color-text-primary)]'
                        }`}
                      >
                        {item.label}
                      </span>
                    </div>
                    <div className="shrink-0 text-right font-mono text-[11px]">
                      <span className="font-semibold text-[var(--color-text-primary)]">
                        {item.value.toLocaleString()}
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
        )}

        {/* Biểu đồ dạng Bar */}
        {isBar && (
          <div>
            {config.orientation === 'vertical' ? (
              /* Dạng cột đứng (Vertical Bars) */
              <div className="space-y-2">
                <svg
                  viewBox="0 0 340 180"
                  className="w-full h-auto overflow-visible"
                  aria-label="Biểu đồ cột đứng"
                >
                  {(() => {
                    const maxVal = Math.max(...config.data.map((d) => d.value), 1);
                    const count = config.data.length;
                    const step = (340 - 30) / count;
                    const colWidth = Math.min(step * 0.65, 34);

                    return (
                      <>
                        {/* Đường baseline */}
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
                                <title>{`${item.label}: ${item.value.toLocaleString()} (${item.percentage.toFixed(1)}%)`}</title>
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
                                {item.value.toLocaleString()}
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
              /* Dạng thanh ngang (Horizontal Bars) - Tối ưu nhất cho chat */
              <div className="space-y-2">
                <svg
                  viewBox={`0 0 340 ${config.data.length * 36 + 10}`}
                  className="w-full h-auto overflow-visible"
                  aria-label="Biểu đồ thanh ngang"
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
                          {/* Nhãn hàng */}
                          <text
                            x={labelWidth - 8}
                            y={y + 13}
                            textAnchor="end"
                            fontSize="11"
                            fontWeight="500"
                            fill="currentColor"
                            className={
                              isHovered
                                ? 'text-emerald-700 dark:text-emerald-300 font-semibold'
                                : 'text-[var(--color-text-primary)]'
                            }
                          >
                            {item.label.length > 14
                              ? item.label.slice(0, 13) + '…'
                              : item.label}
                          </text>

                          {/* Track nền */}
                          <rect
                            x={labelWidth}
                            y={y}
                            width={trackWidth}
                            height="18"
                            rx="4"
                            fill="currentColor"
                            className="text-neutral-200/70 dark:text-neutral-800/80"
                          />

                          {/* Thanh giá trị */}
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
                            <title>{`${item.label}: ${item.value.toLocaleString()} (${item.percentage.toFixed(1)}%)`}</title>
                          </rect>

                          {/* Giá trị số lượng hiển thị bên phải thanh */}
                          <text
                            x={labelWidth + barWidth + 8}
                            y={y + 13}
                            fontSize="10"
                            fontWeight="600"
                            fill="currentColor"
                            className="text-[var(--color-text-secondary)] font-mono"
                          >
                            {item.value.toLocaleString()}
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

        {/* Tooltip / Chi tiết khi hover */}
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
            <span className="font-mono font-semibold text-emerald-600 dark:text-emerald-400">
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
