'use client';

import { useState, type ReactNode } from 'react';
import { cn } from '@/lib/cn';
import { formatDate } from '@/lib/display';

const WIDTH = 800;
const HEIGHT = 240;
const PADDING = { top: 25, right: 30, bottom: 40, left: 45 };
const PLOT_WIDTH = WIDTH - PADDING.left - PADDING.right;
const PLOT_HEIGHT = HEIGHT - PADDING.top - PADDING.bottom;
const BASELINE = PADDING.top + PLOT_HEIGHT;
/** A guard so a wrong date range can never build a huge chart. */
const MAX_PERIODS = 366;
const SHORT_DATE: Intl.DateTimeFormatOptions = { day: '2-digit', month: 'short' };

export interface PeriodBarChartProps<T> {
  /** First day of the axis and the last day it must reach (YYYY-MM-DD). */
  fromDate: string;
  toDate: string;
  /** The periods that have data. */
  items: T[];
  /** The first day (YYYY-MM-DD) of the period an item belongs to; it must fall on the grid that starts at `fromDate`. */
  getDate: (item: T) => string;
  /** The bar height of an item, or null for no bar. Values are clamped to [min, max]. */
  getValue: (item: T) => number | null;
  /** What the tooltip of an item shows. */
  renderTooltip: (item: T) => ReactNode;
  /** Length of one bar in days: 1 for a day (the default), 7 for a week. */
  bucketDays?: number;
  /** A date label under every n-th bar; 5 suits daily bars over a month, 1 suits twelve weekly bars. */
  labelEvery?: number;
  min: number;
  max: number;
  /** Values that get a grid line and a label on the left axis. */
  ticks: number[];
  /** Short name of the value axis, above the labels. */
  axisLabel?: string;
  ariaLabel: string;
}

function parseIso(iso: string): Date {
  const [year, month, day] = iso.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function toIso(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

/** The first day of every period from `fromDate` up to and including `toDate`. */
function periodStarts(fromDate: string, toDate: string, bucketDays: number): string[] {
  const starts: string[] = [];
  const cursor = parseIso(fromDate);
  const end = parseIso(toDate);
  while (cursor <= end && starts.length < MAX_PERIODS) {
    starts.push(toIso(cursor));
    cursor.setDate(cursor.getDate() + bucketDays);
  }
  return starts;
}

function yOf(value: number, min: number, max: number): number {
  const clamped = Math.max(min, Math.min(max, value));
  return BASELINE - ((clamped - min) / (max - min || 1)) * PLOT_HEIGHT;
}

interface Hover<T> {
  x: number;
  y: number;
  item: T;
}

/**
 * One bar per period (a day or a week) over a date range. It knows nothing about what the bars measure: the caller
 * supplies the values, the axis and the tooltip. Hovering a period (the whole column is the target, so the tooltip does
 * not flicker) shows the tooltip and dims the other bars.
 */
export function PeriodBarChart<T>({
  fromDate,
  toDate,
  items,
  getDate,
  getValue,
  renderTooltip,
  bucketDays = 1,
  labelEvery = 5,
  min,
  max,
  ticks,
  axisLabel,
  ariaLabel,
}: PeriodBarChartProps<T>) {
  const [hover, setHover] = useState<Hover<T> | null>(null);

  const starts = periodStarts(fromDate, toDate, bucketDays);
  const itemByStart = new Map(items.map((item) => [getDate(item), item]));
  const slot = PLOT_WIDTH / Math.max(starts.length, 1);
  const barWidth = Math.max(4, slot * 0.6);

  return (
    <div className="scroll-slim w-full overflow-x-auto">
      <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} role="img" aria-label={ariaLabel} className="max-h-64 w-full min-w-[650px]">
        {ticks.map((tick) => {
          const y = yOf(tick, min, max);
          return (
            <g key={tick}>
              <line x1={PADDING.left} y1={y} x2={PADDING.left + PLOT_WIDTH} y2={y} stroke="var(--color-border)" strokeDasharray="3 3" />
              <text x={PADDING.left - 8} y={y + 3} textAnchor="end" fontSize="12" fill="var(--color-text-muted)">
                {tick}
              </text>
            </g>
          );
        })}
        {axisLabel && (
          <text x={PADDING.left - 8} y={PADDING.top - 12} textAnchor="end" fontSize="11" fill="var(--color-text-muted)">
            {axisLabel}
          </text>
        )}

        {starts.map((start, index) => {
          const x = PADDING.left + slot * index + slot / 2;
          const item = itemByStart.get(start);
          const value = item === undefined ? null : getValue(item);
          const top = value === null ? null : yOf(value, min, max);
          const active = hover !== null && item !== undefined && getDate(hover.item) === start;
          return (
            <g key={start}>
              {top !== null && (
                <rect
                  x={x - barWidth / 2}
                  y={top}
                  width={barWidth}
                  height={BASELINE - top}
                  rx={0.5}
                  fill="var(--color-primary)"
                  className={cn('transition-opacity duration-[var(--duration-fast)]', hover && !active && 'opacity-40')}
                />
              )}
              {index % labelEvery === 0 && (
                <text x={x} y={BASELINE + 18} textAnchor="middle" fontSize="12" fill="var(--color-text-secondary)">
                  {formatDate(start, SHORT_DATE)}
                </text>
              )}
              {item !== undefined && (
                <rect
                  x={x - slot / 2}
                  y={PADDING.top}
                  width={slot}
                  height={PLOT_HEIGHT}
                  fill="transparent"
                  className="cursor-pointer"
                  onMouseEnter={(event) => {
                    const box = event.currentTarget.getBoundingClientRect();
                    setHover({ x: box.left + box.width / 2, y: box.top, item });
                  }}
                  onMouseLeave={() => setHover(null)}
                />
              )}
            </g>
          );
        })}
      </svg>

      {hover && (
        <div
          className="pointer-events-none fixed z-[var(--z-dropdown)] -translate-x-1/2 -translate-y-full rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-2.5 text-xs shadow-[var(--shadow-popover)]"
          style={{ left: hover.x, top: hover.y - 8 }}
        >
          {renderTooltip(hover.item)}
        </div>
      )}
    </div>
  );
}
