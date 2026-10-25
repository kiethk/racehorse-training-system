'use client';

import { formatDate } from '@/lib/display';
import { dashboardService } from '../services/dashboardService';
import type { HealthDay, Trend } from '../types';
import { TrendCard } from './TrendCard';
import { TrendTooltip } from './TrendTooltip';

/** How many horses a tooltip names before it says "and N more". */
const TOOLTIP_HORSES = 5;
const MAX_TICKS = 4;

async function loadHealth(): Promise<Trend<HealthDay>> {
  const trend = await dashboardService.getHealthTrend();
  return { fromDate: trend.fromDate, toDate: trend.toDate, items: trend.days };
}

/** A whole-number axis from 0 that fits the tallest bar, in at most MAX_TICKS steps. */
function countAxis(days: HealthDay[]) {
  const tallest = Math.max(1, ...days.map((day) => day.abnormalHorses.length));
  const step = Math.ceil(tallest / MAX_TICKS);
  const ticks = Array.from({ length: Math.ceil(tallest / step) + 1 }, (_, index) => index * step);
  return { max: ticks[ticks.length - 1], ticks };
}

/**
 * The horses that need attention: how many horses in the viewer's scope had a health reading outside the normal range
 * on each day. Averaging the readings would say more about which horses were examined that day than about their health.
 */
export function HealthTrendCard() {
  return (
    <TrendCard<HealthDay>
      title="Health"
      caption="Horses with an abnormal health reading per day"
      emptyText="No health readings in this period."
      load={loadHealth}
      chart={(days) => ({
        min: 0,
        ...countAxis(days),
        axisLabel: 'Horses',
        ariaLabel: 'Horses with an abnormal health reading per day',
        getDate: (day) => day.date,
        getValue: (day) => day.abnormalHorses.length,
        renderTooltip: (day) => (
          <TrendTooltip
            title={formatDate(day.date)}
            rows={[
              ['Readings', day.readings],
              ['Abnormal horses', day.abnormalHorses.length],
            ]}
          >
            {day.abnormalHorses.length > 0 && (
              <ul className="mt-1.5 space-y-0.5 border-t border-[var(--color-border)] pt-1.5">
                {day.abnormalHorses.slice(0, TOOLTIP_HORSES).map((horse) => (
                  <li key={horse.horseId}>
                    <span className="font-medium text-[var(--color-text-primary)]">{horse.horseName}</span>
                    <span className="text-[var(--color-text-secondary)]"> · {horse.issues.join(', ')}</span>
                  </li>
                ))}
                {day.abnormalHorses.length > TOOLTIP_HORSES && (
                  <li className="text-[var(--color-text-muted)]">and {day.abnormalHorses.length - TOOLTIP_HORSES} more</li>
                )}
              </ul>
            )}
          </TrendTooltip>
        ),
      })}
    />
  );
}
