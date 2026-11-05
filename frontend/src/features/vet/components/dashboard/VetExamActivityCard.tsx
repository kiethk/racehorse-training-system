"use client";

import { TrendCard } from "@/features/dashboard/components/TrendCard";
import { TrendTooltip } from "@/features/dashboard/components/TrendTooltip";
import { admissionsApi } from "@/features/admissions/services/api";
import { formatDate } from "@/lib/display";
import {
  examActivityByWeek,
  getTodayLocalDate,
  type VetWeekActivity,
} from "../../lib/dashboardUtils";
import type { Trend } from "@/features/dashboard/types";

const MAX_TICKS = 4;

/** A whole-number axis from 0 that fits the tallest week, in at most MAX_TICKS steps (same rule as HealthTrendCard). */
function countAxis(weeks: VetWeekActivity[]) {
  const tallest = Math.max(1, ...weeks.map((week) => week.completed));
  const step = Math.ceil(tallest / MAX_TICKS);
  const ticks = Array.from({ length: Math.ceil(tallest / step) + 1 }, (_, index) => index * step);
  return { max: ticks[ticks.length - 1], ticks };
}

async function loadExamActivity(): Promise<Trend<VetWeekActivity>> {
  // The BE list endpoint forces the caller's own scope for VETERINARIAN, so this needs no filters.
  // shortcut: assumes <300 exams per vet lifetime; upgrade to a BE trend endpoint when it grows.
  const page = await admissionsApi.getCareSchedules({ size: 300 });
  const { fromDate, toDate, items } = examActivityByWeek(page.content, getTodayLocalDate());
  return { fromDate, toDate, items };
}

/**
 * The vet's own delivered workload: completed exams per week over the last 12 weeks, next to the
 * stable-wide health trend. Completed-only mirrors the performance chart ("sessions actually run").
 */
export function VetExamActivityCard() {
  return (
    <TrendCard<VetWeekActivity>
      title="My Clinical Activity"
      emptyText="No completed exams in this period."
      load={loadExamActivity}
      chart={(weeks) => ({
        min: 0,
        ...countAxis(weeks),
        axisLabel: "Exams",
        ariaLabel: "Completed exams per week",
        bucketDays: 7,
        labelEvery: 1,
        getDate: (week) => week.weekStart,
        getValue: (week) => week.completed,
        renderTooltip: (week) => (
          <TrendTooltip
            title={`Week of ${formatDate(week.weekStart)}`}
            rows={[
              ["Completed", week.completed],
              ["Urgent", week.urgent],
              ["In progress", week.inProgress],
            ]}
          />
        ),
      })}
    />
  );
}
