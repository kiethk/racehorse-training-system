'use client';

import React, { useState, useEffect, useCallback } from 'react';
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
  ReferenceArea,
  Dot,
} from 'recharts';
import { Panel, Button, Pill, Spinner, Notice, Icon } from '@/components/ui';
import { medicalEdaService } from '../services/medicalEdaService';
import type { BioMetricType, BioSpcSeriesResponse, BioSpcPoint } from '../types/medicalEda';
import { formatDateTime } from '@/lib/display';

export interface BioSpcControlChartProps {
  horseId: number;
  horseName?: string;
}

interface CustomDotProps {
  cx?: number;
  cy?: number;
  payload?: BioSpcPoint;
}

const CustomizedAnomalyDot: React.FC<CustomDotProps> = ({ cx, cy, payload }) => {
  if (!cx || !cy || !payload) return null;

  if (payload.isSpecialCause) {
    return (
      <g>
        <circle cx={cx} cy={cy} r={7} fill="#ef4444" opacity={0.35}>
          <animate attributeName="r" values="5;11;5" dur="1.8s" repeatCount="indefinite" />
          <animate attributeName="opacity" values="0.5;0;0.5" dur="1.8s" repeatCount="indefinite" />
        </circle>
        <circle cx={cx} cy={cy} r={5} fill="#dc2626" stroke="#ffffff" strokeWidth={2} />
      </g>
    );
  }

  return <circle cx={cx} cy={cy} r={3} fill="var(--color-primary, #2563eb)" />;
};

export const BioSpcControlChart: React.FC<BioSpcControlChartProps> = ({ horseId, horseName }) => {
  const [metric, setMetric] = useState<BioMetricType>('TEMPERATURE');
  const [loading, setLoading] = useState<boolean>(true);
  const [data, setData] = useState<BioSpcSeriesResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const fetchSpcData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await medicalEdaService.getBioSpcSeries(horseId, metric);
      setData(res);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load biometric SPC series';
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, [horseId, metric]);

  useEffect(() => {
    void fetchSpcData();
  }, [fetchSpcData]);

  const unit = metric === 'TEMPERATURE' ? '°C' : 'bpm';
  const metricLabel = metric === 'TEMPERATURE' ? 'Body Temperature' : 'Resting Heart Rate';

  // Chart domain computation
  const yDomain = React.useMemo(() => {
    if (!data || !data.points || data.points.length === 0) {
      return metric === 'TEMPERATURE' ? [36.0, 40.0] : [20, 60];
    }
    const vals = data.points.map((p) => p.value);
    if (data.upperControlLimit != null) vals.push(data.upperControlLimit);
    if (data.lowerControlLimit != null) vals.push(data.lowerControlLimit);
    vals.push(data.speciesMinLimit, data.speciesMaxLimit);

    const min = Math.floor(Math.min(...vals) - (metric === 'TEMPERATURE' ? 0.3 : 4));
    const max = Math.ceil(Math.max(...vals) + (metric === 'TEMPERATURE' ? 0.3 : 4));
    return [min, max];
  }, [data, metric]);

  return (
    <Panel padded className="space-y-4">
      {/* Header & Metric Switcher */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-[var(--color-border)] pb-3">
        <div>
          <div className="flex items-center gap-2">
            <Icon name="activity" size={18} className="text-[var(--color-primary)]" />
            <h3 className="text-base font-bold text-[var(--color-text-primary)]">
              Personalized Bio-Process Control (Bio-SPC)
            </h3>
          </div>
          <p className="text-xs text-[var(--color-text-muted)] mt-0.5">
            Shewhart statistical time-series monitoring ({horseName || data?.horseName || `Horse #${horseId}`})
          </p>
        </div>

        {/* Metric Toggle Tabs */}
        <div className="flex items-center gap-1.5 p-1 bg-[var(--color-surface-subtle)] rounded-lg border border-[var(--color-border)]">
          <Button
            type="button"
            variant={metric === 'TEMPERATURE' ? 'primary' : 'tertiary'}
            size="sm"
            onClick={() => setMetric('TEMPERATURE')}
          >
            Temperature (°C)
          </Button>
          <Button
            type="button"
            variant={metric === 'RESTING_HEART_RATE' ? 'primary' : 'tertiary'}
            size="sm"
            onClick={() => setMetric('RESTING_HEART_RATE')}
          >
            Heart Rate (bpm)
          </Button>
        </div>
      </div>

      {/* Baseline Status Banner */}
      {data && (
        <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <Pill tone={data.isBaselineActive ? 'success' : 'warning'} size="sm">
                {data.isBaselineActive
                  ? 'Personalized Baseline Active'
                  : `Accumulating Baseline (${data.sampleCount}/5 readings)`}
              </Pill>
              <span className="text-xs text-[var(--color-text-secondary)]">
                {data.isBaselineActive
                  ? `Baseline Mean: ${data.baselineMean?.toFixed(2)} ${unit} (σ = ${data.baselineStdDev?.toFixed(2)})`
                  : `Standard species physiological range enforced [${data.speciesMinLimit} - ${data.speciesMaxLimit} ${unit}]`}
              </span>
            </div>

            {data.isBaselineActive && (
              <div className="flex items-center gap-3 text-[11px] text-[var(--color-text-muted)]">
                <span>
                  <strong className="text-red-500">UCL (+3σ):</strong> {data.upperControlLimit?.toFixed(2)} {unit}
                </span>
                <span>
                  <strong className="text-emerald-600">CL (μ):</strong> {data.baselineMean?.toFixed(2)} {unit}
                </span>
                <span>
                  <strong className="text-blue-500">LCL (-3σ):</strong> {data.lowerControlLimit?.toFixed(2)} {unit}
                </span>
              </div>
            )}
          </div>
        </div>
      )}

      {error && (
        <Notice tone="error" className="text-xs">
          <div className="flex items-center justify-between">
            <span>{error}</span>
            <Button variant="secondary" size="sm" onClick={fetchSpcData}>
              Retry
            </Button>
          </div>
        </Notice>
      )}

      {/* Chart Canvas */}
      {loading ? (
        <div className="flex h-72 items-center justify-center">
          <Spinner />
        </div>
      ) : !data || data.points.length === 0 ? (
        <div className="py-12 text-center text-xs text-[var(--color-text-muted)]">
          No {metricLabel.toLowerCase()} readings recorded yet.
        </div>
      ) : (
        <div className="h-80 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart
              data={data.points}
              margin={{ top: 15, right: 30, left: 10, bottom: 20 }}
            >
              <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border, #e5e7eb)" opacity={0.6} />

              <XAxis
                dataKey="recordedAt"
                tickFormatter={(val) => {
                  const d = new Date(val);
                  return `${d.getMonth() + 1}/${d.getDate()}`;
                }}
                tick={{ fontSize: 11, fill: 'var(--color-text-muted, #6b7280)' }}
              />

              <YAxis
                domain={yDomain}
                tick={{ fontSize: 11, fill: 'var(--color-text-muted, #6b7280)' }}
                unit={` ${unit}`}
              />

              <Tooltip
                content={({ active, payload }) => {
                  if (!active || !payload || !payload.length) return null;
                  const pt = payload[0].payload as BioSpcPoint;
                  return (
                    <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] p-2.5 shadow-md text-xs space-y-1">
                      <div className="text-[var(--color-text-muted)] font-mono text-[10px]">
                        {formatDateTime(pt.recordedAt)}
                      </div>
                      <div className="font-bold text-sm text-[var(--color-text-primary)]">
                        {pt.value} {unit}
                      </div>
                      {pt.isSpecialCause ? (
                        <div className="text-red-500 font-semibold flex items-center gap-1 pt-0.5">
                          <Icon name="alert-triangle" size={14} />
                          <span>{pt.anomalyDescription || 'Special Cause Anomaly'}</span>
                        </div>
                      ) : (
                        <div className="text-emerald-600 font-medium">In Control (Normal)</div>
                      )}
                    </div>
                  );
                }}
              />

              <Legend verticalAlign="top" wrapperStyle={{ paddingBottom: '10px', fontSize: '11px' }} />

              {/* Normal Control Band Reference Area */}
              {data.isBaselineActive && data.upperControlLimit != null && data.lowerControlLimit != null && (
                <ReferenceArea
                  y1={data.lowerControlLimit}
                  y2={data.upperControlLimit}
                  fill="#10b981"
                  fillOpacity={0.06}
                />
              )}

              {/* Upper Control Limit (UCL = +3 SD) */}
              {data.upperControlLimit != null && (
                <ReferenceLine
                  y={data.upperControlLimit}
                  stroke="#ef4444"
                  strokeDasharray="5 5"
                  strokeWidth={1.5}
                  label={{
                    value: `UCL (+3σ): ${data.upperControlLimit.toFixed(2)}`,
                    fill: '#ef4444',
                    fontSize: 10,
                    position: 'insideTopRight',
                  }}
                />
              )}

              {/* Upper Warning Limit (UWL = +2 SD) */}
              {data.upperWarningLimit != null && (
                <ReferenceLine
                  y={data.upperWarningLimit}
                  stroke="#f59e0b"
                  strokeDasharray="3 3"
                  strokeWidth={1}
                />
              )}

              {/* Center Line (Baseline Mean) */}
              {data.baselineMean != null && (
                <ReferenceLine
                  y={data.baselineMean}
                  stroke="#10b981"
                  strokeWidth={1.5}
                  label={{
                    value: `Mean (μ): ${data.baselineMean.toFixed(2)}`,
                    fill: '#10b981',
                    fontSize: 10,
                    position: 'insideBottomRight',
                  }}
                />
              )}

              {/* Lower Warning Limit (LWL = -2 SD) */}
              {data.lowerWarningLimit != null && (
                <ReferenceLine
                  y={data.lowerWarningLimit}
                  stroke="#3b82f6"
                  strokeDasharray="3 3"
                  strokeWidth={1}
                />
              )}

              {/* Lower Control Limit (LCL = -3 SD) */}
              {data.lowerControlLimit != null && (
                <ReferenceLine
                  y={data.lowerControlLimit}
                  stroke="#3b82f6"
                  strokeDasharray="5 5"
                  strokeWidth={1.5}
                  label={{
                    value: `LCL (-3σ): ${data.lowerControlLimit.toFixed(2)}`,
                    fill: '#3b82f6',
                    fontSize: 10,
                    position: 'insideBottomRight',
                  }}
                />
              )}

              {/* Telemetry Series Line */}
              <Line
                type="monotone"
                dataKey="value"
                name={`${metricLabel} (${unit})`}
                stroke="var(--color-primary, #2563eb)"
                strokeWidth={2}
                dot={<CustomizedAnomalyDot />}
                activeDot={{ r: 6 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}
    </Panel>
  );
};
