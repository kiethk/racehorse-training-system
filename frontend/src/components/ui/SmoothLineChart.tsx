'use client';

import { useId, useMemo, useState, type ReactNode } from 'react';
import clsx from 'clsx';
import { formatNumber } from '@/lib/display';

export interface DataPoint {
  date: string; // YYYY-MM-DD or label
  label: string; // Display date/label
  value: number | null;
  secondaryValue?: number | null;
}

export interface SmoothLineChartProps {
  data: DataPoint[];
  min?: number;
  max?: number;
  ticks?: number[];
  lineColor?: string; // default var(--color-primary)
  areaColor?: string;
  axisLabel?: string;
  ariaLabel: string;
  renderTooltip?: (point: DataPoint) => ReactNode;
  height?: number; // default 220
  className?: string;
}

const WIDTH = 800;
const PADDING = { top: 20, right: 25, bottom: 35, left: 45 };

/**
 * Calculates cubic bezier curve SVG path from array of coordinates.
 */
function getSmoothPath(points: Array<{ x: number; y: number }>): string {
  if (points.length === 0) return '';
  if (points.length === 1) return `M ${points[0].x} ${points[0].y}`;

  let path = `M ${points[0].x} ${points[0].y}`;

  for (let i = 0; i < points.length - 1; i++) {
    const current = points[i];
    const next = points[i + 1];
    const prev = points[i - 1] || current;
    const nextNext = points[i + 2] || next;

    // Control point calculations for natural tension (smoothing factor 0.2)
    const cp1x = current.x + (next.x - prev.x) * 0.2;
    const cp1y = current.y + (next.y - prev.y) * 0.2;
    const cp2x = next.x - (nextNext.x - current.x) * 0.2;
    const cp2y = next.y - (nextNext.y - current.y) * 0.2;

    path += ` C ${cp1x} ${cp1y}, ${cp2x} ${cp2y}, ${next.x} ${next.y}`;
  }

  return path;
}

export function SmoothLineChart({
  data,
  min = 0,
  max: customMax,
  ticks: customTicks,
  lineColor = 'var(--color-primary)',
  areaColor,
  axisLabel,
  ariaLabel,
  renderTooltip,
  height = 220,
  className,
}: SmoothLineChartProps) {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);
  const gradientId = useId();

  const validValues = data.map((d) => d.value).filter((v): v is number => v != null);
  const highestValue = validValues.length > 0 ? Math.max(...validValues) : 10;
  const lowestValue = validValues.length > 0 ? Math.min(...validValues) : 0;

  const chartMax = customMax ?? Math.max(10, Math.ceil(highestValue * 1.15));
  const chartMin = min ?? Math.min(0, Math.floor(lowestValue));

  const ticks = useMemo(() => {
    if (customTicks && customTicks.length > 0) return customTicks;
    const step = (chartMax - chartMin) / 4;
    return [chartMin, chartMin + step, chartMin + step * 2, chartMin + step * 3, chartMax];
  }, [chartMax, chartMin, customTicks]);

  const plotWidth = WIDTH - PADDING.left - PADDING.right;
  const plotHeight = height - PADDING.top - PADDING.bottom;
  const baseline = PADDING.top + plotHeight;

  // Map data to x, y coordinates
  const points = useMemo(() => {
    if (data.length === 0) return [];
    const stepX = data.length > 1 ? plotWidth / (data.length - 1) : plotWidth / 2;

    return data.map((item, index) => {
      const x = PADDING.left + index * stepX;
      let y = baseline;
      if (item.value != null) {
        const valFraction = (item.value - chartMin) / (chartMax - chartMin);
        y = baseline - valFraction * plotHeight;
      }
      return { x, y, item, index };
    });
  }, [data, plotWidth, baseline, chartMin, chartMax, plotHeight]);

  const activePoints = points.filter((p) => p.item.value != null);
  const linePath = useMemo(() => getSmoothPath(activePoints), [activePoints]);

  const areaPath = useMemo(() => {
    if (activePoints.length === 0) return '';
    const firstX = activePoints[0].x;
    const lastX = activePoints[activePoints.length - 1].x;
    return `${linePath} L ${lastX} ${baseline} L ${firstX} ${baseline} Z`;
  }, [activePoints, linePath, baseline]);

  const hoveredPoint = hoveredIndex != null ? points[hoveredIndex] : null;

  return (
    <div className={clsx('relative w-full', className)}>
      <svg
        viewBox={`0 0 ${WIDTH} ${height}`}
        className="w-full h-auto overflow-visible select-none"
        aria-label={ariaLabel}
        role="img"
      >
        <defs>
          <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={areaColor || lineColor} stopOpacity="0.28" />
            <stop offset="85%" stopColor={areaColor || lineColor} stopOpacity="0.02" />
            <stop offset="100%" stopColor={areaColor || lineColor} stopOpacity="0" />
          </linearGradient>
        </defs>

        {/* Y-Axis Gridlines & Labels */}
        {ticks.map((tickValue) => {
          const y = baseline - ((tickValue - chartMin) / (chartMax - chartMin)) * plotHeight;
          return (
            <g key={`tick-${tickValue}`}>
              <line
                x1={PADDING.left}
                y1={y}
                x2={WIDTH - PADDING.right}
                y2={y}
                stroke="var(--color-border)"
                strokeDasharray="4 4"
                strokeWidth="1"
                opacity="0.6"
              />
              <text
                x={PADDING.left - 8}
                y={y + 3}
                textAnchor="end"
                className="fill-[var(--color-text-muted)] text-[10px] font-medium"
              >
                {formatNumber(tickValue, tickValue % 1 === 0 ? 0 : 1)}
              </text>
            </g>
          );
        })}

        {axisLabel && (
          <text
            x={PADDING.left}
            y={PADDING.top - 8}
            className="fill-[var(--color-text-muted)] text-[10px] font-semibold uppercase tracking-wider"
          >
            {axisLabel}
          </text>
        )}

        {/* Soft Area Gradient Under Curve */}
        {areaPath && <path d={areaPath} fill={`url(#${gradientId})`} />}

        {/* Smooth Curved Line */}
        {linePath && (
          <path
            d={linePath}
            fill="none"
            stroke={lineColor}
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeLinejoin="round"
            className="transition-all duration-300"
          />
        )}

        {/* Interactive Vertical Cursor Guide */}
        {hoveredPoint && (
          <line
            x1={hoveredPoint.x}
            y1={PADDING.top}
            x2={hoveredPoint.x}
            y2={baseline}
            stroke="var(--color-border-strong)"
            strokeDasharray="3 3"
            strokeWidth="1.5"
          />
        )}

        {/* Data Points */}
        {activePoints.map((point) => {
          const isHovered = hoveredIndex === point.index;
          return (
            <g key={`pt-${point.index}`}>
              <circle
                cx={point.x}
                cy={point.y}
                r={isHovered ? 5.5 : 3.5}
                fill="var(--color-surface)"
                stroke={lineColor}
                strokeWidth={isHovered ? 3 : 2}
                className="transition-all duration-200 cursor-pointer"
              />
              {/* Invisible larger hover hit area */}
              <rect
                x={point.x - 15}
                y={PADDING.top}
                width={30}
                height={plotHeight}
                fill="transparent"
                className="cursor-pointer"
                onMouseEnter={() => setHoveredIndex(point.index)}
                onMouseLeave={() => setHoveredIndex(null)}
              />
            </g>
          );
        })}

        {/* X-Axis Labels (Spread reasonably) */}
        {points.map((point, index) => {
          // Label every 3-4 items if many
          const shouldLabel =
            data.length <= 12 || index === 0 || index === data.length - 1 || index % 3 === 0;
          if (!shouldLabel) return null;

          return (
            <text
              key={`xlabel-${point.index}`}
              x={point.x}
              y={baseline + 18}
              textAnchor="middle"
              className="fill-[var(--color-text-muted)] text-[11px] font-medium"
            >
              {point.item.label}
            </text>
          );
        })}
      </svg>

      {/* Floating Tooltip */}
      {hoveredPoint && hoveredPoint.item.value != null && (
        <div
          className="absolute z-10 pointer-events-none -translate-x-1/2 rounded-md bg-[var(--color-surface)] border border-[var(--color-border)] p-2 shadow-md text-xs"
          style={{
            left: `${(hoveredPoint.x / WIDTH) * 100}%`,
            top: `${(hoveredPoint.y / height) * 100 - 45}%`,
          }}
        >
          {renderTooltip ? (
            renderTooltip(hoveredPoint.item)
          ) : (
            <div>
              <div className="font-semibold text-[var(--color-text-primary)]">
                {hoveredPoint.item.label}
              </div>
              <div className="text-[var(--color-text-secondary)]">
                Rating: <strong className="text-[var(--color-primary)]">{formatNumber(hoveredPoint.item.value, 1)}</strong>/10
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
