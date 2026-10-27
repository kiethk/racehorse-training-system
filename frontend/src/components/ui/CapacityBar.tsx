'use client';

import clsx from 'clsx';
import { formatNumber } from '@/lib/display';

export interface CapacityBarProps {
  label: string;
  total: number;
  occupied: number;
  reserved?: number;
  available?: number;
  unit?: string;
  size?: 'sm' | 'md';
  showBreakdown?: boolean;
  className?: string;
}

export function CapacityBar({
  label,
  total,
  occupied,
  reserved = 0,
  available,
  unit = 'stalls',
  size = 'md',
  showBreakdown = true,
  className,
}: CapacityBarProps) {
  const safeTotal = Math.max(0, total);
  const safeOccupied = Math.max(0, occupied);
  const safeReserved = Math.max(0, reserved);
  const calculatedAvailable = available !== undefined ? Math.max(0, available) : Math.max(0, safeTotal - safeOccupied - safeReserved);

  const occupiedPercent = safeTotal > 0 ? (safeOccupied / safeTotal) * 100 : 0;
  const reservedPercent = safeTotal > 0 ? (safeReserved / safeTotal) * 100 : 0;
  const totalOccupiedPercent = occupiedPercent + reservedPercent;

  // Tone based on total occupancy threshold
  const tone = totalOccupiedPercent >= 90
    ? 'danger'
    : totalOccupiedPercent >= 75
    ? 'warning'
    : 'success';

  const barHeight = size === 'sm' ? 'h-2' : 'h-3';

  return (
    <div className={clsx('space-y-1.5', className)}>
      <div className="flex items-center justify-between text-xs font-medium">
        <span className="text-[var(--color-text-primary)]">{label}</span>
        <span className="text-[var(--color-text-secondary)]">
          <span className="font-semibold text-[var(--color-text-primary)]">
            {safeOccupied + safeReserved}
          </span>
          /{safeTotal} {unit} ({formatNumber(totalOccupiedPercent, 1)}%)
        </span>
      </div>

      <div
        className={clsx(
          'w-full overflow-hidden rounded-full bg-[var(--color-neutral-subtle)] flex',
          barHeight
        )}
        role="progressbar"
        aria-label={`${label} capacity`}
        aria-valuenow={Math.round(totalOccupiedPercent)}
        aria-valuemin={0}
        aria-valuemax={100}
      >
        {/* Occupied Segment */}
        {occupiedPercent > 0 && (
          <div
            style={{ width: `${occupiedPercent}%` }}
            className={clsx(
              'transition-all duration-300',
              tone === 'danger' && 'bg-[var(--color-danger)]',
              tone === 'warning' && 'bg-[var(--color-warning)]',
              tone === 'success' && 'bg-[var(--color-primary)]'
            )}
            title={`Occupied: ${safeOccupied} (${formatNumber(occupiedPercent, 1)}%)`}
          />
        )}

        {/* Reserved Segment */}
        {reservedPercent > 0 && (
          <div
            style={{ width: `${reservedPercent}%` }}
            className="bg-amber-400 opacity-80 transition-all duration-300"
            title={`Reserved: ${safeReserved} (${formatNumber(reservedPercent, 1)}%)`}
          />
        )}
      </div>

      {showBreakdown && (
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-[11px] text-[var(--color-text-muted)]">
          <span className="inline-flex items-center gap-1">
            <span
              className={clsx(
                'inline-block h-2 w-2 rounded-full',
                tone === 'danger' ? 'bg-[var(--color-danger)]' : 'bg-[var(--color-primary)]'
              )}
            />
            <span>Occupied: <strong>{safeOccupied}</strong></span>
          </span>

          {safeReserved > 0 && (
            <span className="inline-flex items-center gap-1">
              <span className="inline-block h-2 w-2 rounded-full bg-amber-400" />
              <span>Reserved: <strong>{safeReserved}</strong></span>
            </span>
          )}

          <span className="inline-flex items-center gap-1">
            <span className="inline-block h-2 w-2 rounded-full bg-[var(--color-neutral-subtle)] border border-[var(--color-border)]" />
            <span>Available: <strong>{calculatedAvailable}</strong></span>
          </span>
        </div>
      )}
    </div>
  );
}
