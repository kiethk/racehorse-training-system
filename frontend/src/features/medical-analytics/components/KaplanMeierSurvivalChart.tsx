'use client';

import React, { useState, useEffect, useCallback, useMemo } from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  ReferenceLine,
} from 'recharts';
import { Panel, Button, Spinner, Notice, Pill } from '@/components/ui';
import { medicalEdaService } from '../services/medicalEdaService';
import type {
  SurvivalStratifyBy,
  SurvivalEdaResponse,
  SurvivalStratum,
  KaplanMeierStep,
} from '../types/medicalEda';

const STRATUM_COLORS = [
  '#2563eb', // Blue
  '#10b981', // Emerald
  '#f59e0b', // Amber
  '#8b5cf6', // Violet
  '#ec4899', // Pink
  '#06b6d4', // Cyan
  '#f97316', // Orange
  '#64748b', // Slate
];

export const KaplanMeierSurvivalChart: React.FC = () => {
  const [stratifyBy, setStratifyBy] = useState<SurvivalStratifyBy>('TREATMENT_PLAN');
  const [loading, setLoading] = useState<boolean>(true);
  const [data, setData] = useState<SurvivalEdaResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await medicalEdaService.getSurvivalAnalytics(stratifyBy);
      setData(res);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load survival analytics';
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, [stratifyBy]);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  // Combine stepped steps across all strata into a unified time series for Recharts
  const { chartData, maxDay } = useMemo(() => {
    if (!data || !data.strata || data.strata.length === 0) {
      return { chartData: [], maxDay: 30 };
    }

    // Collect all unique day points across all strata
    const daySet = new Set<number>();
    daySet.add(0);

    data.strata.forEach((stratum) => {
      stratum.steps.forEach((step) => daySet.add(step.days));
    });

    const sortedDays = Array.from(daySet).sort((a, b) => a - b);
    const max = sortedDays.length > 0 ? sortedDays[sortedDays.length - 1] : 30;
    const maxAxisDay = Math.max(max, 10);
    if (maxAxisDay > max && sortedDays.length > 0) {
      sortedDays.push(maxAxisDay);
    }

    // For each stratum, build a quick lookup of survival by day
    // Since steps are piecewise constant (stepAfter), for any day t, S(t) is the S value of the latest step <= t.
    const rows = sortedDays.map((day) => {
      const row: Record<string, number | null> = { day };

      data.strata.forEach((stratum) => {
        let lastProb: number = 1.0;
        for (const step of stratum.steps) {
          if (step.days <= day) {
            lastProb = step.survivalProbability;
          } else {
            break;
          }
        }
        // Percentage (0 - 100%)
        row[`stratum_${stratum.stratumId}`] = Math.round(lastProb * 1000) / 10;
      });

      return row;
    });

    return { chartData: rows, maxDay: maxAxisDay };
  }, [data]);

  return (
    <Panel padded className="space-y-4">
      {/* Header & Stratification Filter */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-[var(--color-border)] pb-3">
        <div>
          <h3 className="text-base font-bold text-[var(--color-text-primary)]">
            Injury Recovery Timeline by Treatment
          </h3>
          <p className="text-xs text-[var(--color-text-muted)] mt-0.5">
            Time required for horses to recover and return to full training.
          </p>
        </div>

        {/* Stratification Selector */}
        <div className="flex items-center gap-2">
          <label htmlFor="stratify-select" className="text-xs font-semibold text-[var(--color-text-secondary)]">
            Compare By:
          </label>
          <select
            id="stratify-select"
            value={stratifyBy}
            onChange={(e) => setStratifyBy(e.target.value as SurvivalStratifyBy)}
            className="rounded-md border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-1.5 text-xs font-medium text-[var(--color-text-primary)] shadow-sm focus:border-[var(--color-primary)] focus:outline-none"
          >
            <option value="TREATMENT_PLAN">Compare by Treatment Plan</option>
            <option value="BODY_ZONE">Compare by Body Zone</option>
          </select>
        </div>
      </div>

      {error && (
        <Notice tone="error" className="text-xs">
          <div className="flex items-center justify-between">
            <span>{error}</span>
            <Button variant="secondary" size="sm" onClick={fetchData}>
              Retry
            </Button>
          </div>
        </Notice>
      )}

      {loading ? (
        <div className="flex h-72 items-center justify-center">
          <Spinner />
        </div>
      ) : !data || data.strata.length === 0 ? (
        <div className="py-12 text-center text-xs text-[var(--color-text-muted)]">
          No injury recovery records available for recovery timeline analysis.
        </div>
      ) : (
        <div className="space-y-4">
          {/* Main Stepped Chart */}
          <div className="h-80 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart
                data={chartData}
                margin={{ top: 15, right: 30, left: 10, bottom: 20 }}
              >
                <CartesianGrid vertical={false} stroke="var(--color-border, #e5e7eb)" strokeDasharray="3 3" opacity={0.4} />

                <XAxis
                  dataKey="day"
                  type="number"
                  domain={[0, Math.max(maxDay, 10)]}
                  tick={{ fontSize: 11, fill: 'var(--color-text-muted, #6b7280)' }}
                  unit=" d"
                  label={{
                    value: 'Days Post-Injury',
                    position: 'insideBottom',
                    offset: -10,
                    fontSize: 11,
                    fill: 'var(--color-text-muted, #6b7280)',
                  }}
                />

                <YAxis
                  domain={[0, 100]}
                  tick={{ fontSize: 11, fill: 'var(--color-text-muted, #6b7280)' }}
                  unit="%"
                  label={{
                    value: 'Horses Still Recovering (%)',
                    angle: -90,
                    position: 'insideLeft',
                    fontSize: 11,
                    fill: 'var(--color-text-muted, #6b7280)',
                  }}
                />

                <Tooltip
                  content={({ active, payload, label }) => {
                    if (!active || !payload || !payload.length) return null;
                    return (
                      <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] p-2.5 shadow-md text-xs space-y-1.5 min-w-[180px]">
                        <div className="font-bold text-[var(--color-text-primary)] border-b border-[var(--color-border)] pb-1">
                          Day {label} Post-Injury
                        </div>
                        {payload.map((entry, entryIdx) => {
                          const badgeColor = entry.color || (entry as { stroke?: string }).stroke || STRATUM_COLORS[entryIdx % STRATUM_COLORS.length];
                          return (
                            <div key={entryIdx} className="flex items-center justify-between gap-3 text-[11px]">
                              <span className="flex items-center gap-1.5">
                                <span
                                  className="inline-block h-2 w-2 rounded-full"
                                  style={{ backgroundColor: badgeColor }}
                                />
                                <span className="text-[var(--color-text-secondary)]">{entry.name}:</span>
                              </span>
                              <span className="font-mono font-semibold text-[var(--color-text-primary)]">
                                {entry.value}%
                              </span>
                            </div>
                          );
                        })}
                      </div>
                    );
                  }}
                />

                <Legend
                  verticalAlign="top"
                  wrapperStyle={{ paddingBottom: '10px', fontSize: '11px' }}
                />

                

                {/* Stepped lines for each stratum */}
                {data.strata.map((stratum, idx) => (
                  <Line
                    key={stratum.stratumId}
                    type="stepAfter"
                    dataKey={`stratum_${stratum.stratumId}`}
                    name={stratum.stratumName}
                    stroke={STRATUM_COLORS[idx % STRATUM_COLORS.length]}
                    strokeWidth={2}
                    dot={false}
                    activeDot={{ r: 5 }}
                  />
                ))}
                {/* 50% Median Recovery Reference Line */}
                <ReferenceLine
                  y={50}
                  stroke="#9ca3af"
                  strokeDasharray="4 4"
                  strokeWidth={1}
                  label={{
                    value: 'Typical 50% Recovery Mark',
                    fill: '#6b7280',
                    fontSize: 10,
                    position: 'insideBottomRight',
                  }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>

          {/* Stratum Summary Table */}
          <div className="overflow-x-auto rounded-lg border border-[var(--color-border)]">
            <table className="w-full text-left text-xs text-[var(--color-text-primary)]">
              <thead className="bg-[var(--color-surface-subtle)] text-[var(--color-text-muted)] font-semibold border-b border-[var(--color-border)]">
                <tr>
                  <th className="px-3 py-2">{stratifyBy === 'TREATMENT_PLAN' ? 'Treatment Plan' : 'Body Zone'}</th>
                  <th className="px-3 py-2 text-right">Cases Under Care</th>
                  <th className="px-3 py-2 text-right">Recovered</th>
                  <th className="px-3 py-2 text-right">Still Recovering</th>
                  <th className="px-3 py-2 text-right">Recovery Rate</th>
                  <th className="px-3 py-2 text-right">Typical Recovery (Days)</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--color-border)]">
                {data.strata.map((stratum, idx) => {
                  const rate =
                    stratum.totalCases > 0
                      ? Math.round((stratum.recoveredCount / stratum.totalCases) * 100)
                      : 0;
                  const color = STRATUM_COLORS[idx % STRATUM_COLORS.length];

                  return (
                    <tr key={stratum.stratumId} className="hover:bg-[var(--color-surface-subtle)]">
                      <td className="px-3 py-2 font-medium flex items-center gap-2">
                        <span
                          className="inline-block h-2.5 w-2.5 rounded-full flex-shrink-0"
                          style={{ backgroundColor: color }}
                        />
                        <span>{stratum.stratumName}</span>
                      </td>
                      <td className="px-3 py-2 text-right font-mono">{stratum.totalCases}</td>
                      <td className="px-3 py-2 text-right font-mono text-emerald-600 font-semibold">
                        {stratum.recoveredCount}
                      </td>
                      <td className="px-3 py-2 text-right font-mono text-amber-600 font-semibold">
                        {stratum.censoredCount}
                      </td>
                      <td className="px-3 py-2 text-right font-mono">{rate}%</td>
                      <td className="px-3 py-2 text-right font-mono font-bold">
                        {stratum.medianRecoveryDays != null ? (
                          <Pill tone="info" size="sm">
                            {stratum.medianRecoveryDays.toFixed(1)} days
                          </Pill>
                        ) : (
                          <span className="text-[var(--color-text-muted)] italic">Not reached (&gt;50% active)</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </Panel>
  );
};
