'use client';

import { formatDate, formatNumber } from '@/lib/display';
import { dashboardService } from '../services/dashboardService';
import type { PerformanceWeek, Trend } from '../types';
import { TrendCard } from './TrendCard';
import { TrendTooltip } from './TrendTooltip';

const TICKS = [0, 2, 4, 6, 8, 10];
const WEEK_DAYS = 7;

async function loadPerformance(): Promise<Trend<PerformanceWeek>> {
  const trend = await dashboardService.getPerformanceTrend();
  return { fromDate: trend.fromDate, toDate: trend.toDate, items: trend.weeks };
}

/**
 * The training progress of the horses in the viewer's scope: the average rating of the sessions completed each week.
 * Each horse counts once per week, because different horses train on different days and a daily average would say
 * more about who trained that day than about the form of the horses.
 */
export function PerformanceTrendCard() {
  return (
    <TrendCard<PerformanceWeek>
      title="Performance"
      caption="Average performance rating of completed sessions per week, each horse counted once"
      emptyText="No completed sessions in this period."
      load={loadPerformance}
      chart={{
        min: 0,
        max: 10,
        ticks: TICKS,
        axisLabel: 'Rating',
        ariaLabel: 'Performance rating per week',
        bucketDays: WEEK_DAYS,
        labelEvery: 1,
        getDate: (week) => week.weekStart,
        getValue: (week) => week.averageRating,
        renderTooltip: (week) => (
          <TrendTooltip
            title={`Week of ${formatDate(week.weekStart)}`}
            rows={[
              ['Horses', week.horses],
              ['Sessions', week.sessions],
              ['Avg rating', week.averageRating != null ? `${formatNumber(week.averageRating, 1)}/10` : '—'],
              ['Avg speed', week.averageSpeedKmh != null ? `${formatNumber(week.averageSpeedKmh, 1)} km/h` : '—'],
              ['Avg max HR', week.averageMaxHeartRate != null ? `${formatNumber(week.averageMaxHeartRate, 0)} bpm` : '—'],
            ]}
          />
        ),
      }}
    />
  );
}
