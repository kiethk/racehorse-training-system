'use client';

import { useEffect, useState } from 'react';
import { EmptyState, Panel, SectionTitle, Skeleton, SmoothLineChart, type DataPoint } from '@/components/ui';
import { displayError, formatDate, formatNumber } from '@/lib/display';
import { dashboardService } from '../../services/dashboardService';
import type { PerformanceTrend, PerformanceWeek } from '../../types';

const SHORT_DATE: Intl.DateTimeFormatOptions = { day: '2-digit', month: 'short' };

export function PerformanceLineTrendCard() {
  const [trend, setTrend] = useState<PerformanceTrend | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    dashboardService
      .getPerformanceTrend()
      .then((data) => {
        if (active) {
          setTrend(data);
          setLoading(false);
        }
      })
      .catch((cause) => {
        if (active) {
          setError(displayError(cause, 'Unable to load performance trajectory.'));
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  if (loading) {
    return (
      <Panel padded>
        <Skeleton className="h-56 w-full" />
      </Panel>
    );
  }

  if (error) {
    return (
      <Panel padded>
        <EmptyState icon="alert-triangle" title="Performance trend unavailable" description={error} />
      </Panel>
    );
  }

  if (!trend || trend.weeks.length === 0) {
    return (
      <Panel padded className="space-y-2">
        <SectionTitle>Performance Trajectory</SectionTitle>
        <p className="text-xs text-[var(--color-text-secondary)]">
          Weekly equine performance trajectory across active training plans.
        </p>
        <EmptyState icon="activity" title="No completed sessions yet" description="Completed training sessions will plot a performance curve here." />
      </Panel>
    );
  }

  const chartData: DataPoint[] = trend.weeks.map((week: PerformanceWeek) => ({
    date: week.weekStart,
    label: formatDate(week.weekStart, SHORT_DATE),
    value: week.averageRating,
  }));

  const weekMap = new Map<string, PerformanceWeek>();
  trend.weeks.forEach((w) => weekMap.set(w.weekStart, w));

  return (
    <Panel padded className="space-y-3">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-1">
        <div>
          <SectionTitle>Performance Trajectory</SectionTitle>
          <p className="text-xs text-[var(--color-text-secondary)] mt-0.5">
            Average performance rating of completed sessions per week ({formatDate(trend.fromDate, SHORT_DATE)} – {formatDate(trend.toDate, SHORT_DATE)}).
          </p>
        </div>
        <span className="text-[11px] font-medium text-[var(--color-text-muted)]">
          Scale: 0 – 10 Rating
        </span>
      </div>

      <div className="pt-2">
        <SmoothLineChart
          data={chartData}
          min={0}
          max={10}
          ticks={[0, 2.5, 5, 7.5, 10]}
          axisLabel="Rating"
          ariaLabel="Performance rating weekly curve"
          lineColor="#0284c7"
          height={210}
          renderTooltip={(point) => {
            const raw = weekMap.get(point.date);
            return (
              <div className="space-y-1">
                <div className="font-semibold text-[var(--color-text-primary)]">
                  Week of {point.label}
                </div>
                <div className="text-[11px] text-[var(--color-text-secondary)] space-y-0.5">
                  <div>Avg Rating: <strong className="text-sky-600 dark:text-sky-400">{formatNumber(point.value, 1)}</strong>/10</div>
                  {raw && <div>Horses active: <strong>{raw.horses}</strong> ({raw.sessions} sessions)</div>}
                  {raw?.averageSpeedKmh != null && <div>Avg Speed: {formatNumber(raw.averageSpeedKmh, 1)} km/h</div>}
                </div>
              </div>
            );
          }}
        />
      </div>
    </Panel>
  );
}
