'use client';

import { useEffect, useState } from 'react';
import { EmptyState, Panel, PeriodBarChart, SectionTitle, Skeleton, type PeriodBarChartProps } from '@/components/ui';
import { displayError, formatDate } from '@/lib/display';
import type { Trend } from '../types';

const SHORT_DATE: Intl.DateTimeFormatOptions = { day: '2-digit', month: 'short' };

type ChartSettings<TItem> = Omit<PeriodBarChartProps<TItem>, 'fromDate' | 'toDate' | 'items'>;

/**
 * A dashboard card with a title, one line saying what the chart shows and over which dates, and a bar chart. It loads
 * its own data, so a failure only affects this card and the rest of the dashboard stays usable.
 */
export function TrendCard<TItem>({
  title,
  caption,
  emptyText,
  load,
  chart,
}: {
  title: string;
  /** What the bars show, for example "Average performance rating per week". The date range is appended. */
  caption: string;
  emptyText: string;
  /** Must be a stable function (a module-level function), because it is the dependency of the loading effect. */
  load: () => Promise<Trend<TItem>>;
  /** Static settings, or a function of the loaded items when the axis depends on the data. */
  chart: ChartSettings<TItem> | ((items: TItem[]) => ChartSettings<TItem>);
}) {
  const [trend, setTrend] = useState<Trend<TItem> | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    load()
      .then((data) => active && setTrend(data))
      .catch((cause) => active && setError(displayError(cause, `Unable to load the ${title.toLowerCase()} chart.`)));
    return () => {
      active = false;
    };
  }, [load, title]);

  if (error) {
    return (
      <Panel padded>
        <EmptyState icon="alert-triangle" title={`${title} unavailable`} description={error} />
      </Panel>
    );
  }
  if (!trend) return <Panel padded><Skeleton className="h-56 w-full" /></Panel>;

  return (
    <Panel padded className="space-y-2">
      <div>
        <SectionTitle>{title}</SectionTitle>
        <p className="text-xs text-[var(--color-text-secondary)]">
          {caption}, {formatDate(trend.fromDate, SHORT_DATE)} – {formatDate(trend.toDate, SHORT_DATE)}.
        </p>
      </div>

      {trend.items.length === 0 ? (
        <p className="py-8 text-center text-xs text-[var(--color-text-muted)]">{emptyText}</p>
      ) : (
        <PeriodBarChart
          fromDate={trend.fromDate}
          toDate={trend.toDate}
          items={trend.items}
          {...(typeof chart === 'function' ? chart(trend.items) : chart)}
        />
      )}
    </Panel>
  );
}
