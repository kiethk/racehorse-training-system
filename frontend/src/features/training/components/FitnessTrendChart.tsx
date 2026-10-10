'use client';

import { SegmentedControl } from '@/components/ui/SegmentedControl';
import { useState } from 'react';
import type { HorseFitnessTrendItem, ReadinessAssessment } from '../types';

interface FitnessTrendChartProps {
  trend: HorseFitnessTrendItem[];
  readiness: ReadinessAssessment[];
}

type MetricView = 'ALL' | 'PERFORMANCE' | 'HEART_RATE' | 'SPEED_DISTANCE';

export function FitnessTrendChart({ trend, readiness }: FitnessTrendChartProps) {
  const [activeTab, setActiveTab] = useState<MetricView>('ALL');
  const [hoveredPoint, setHoveredPoint] = useState<{
    x: number;
    y: number;
    data: HorseFitnessTrendItem;
  } | null>(null);

  const [hoveredReadiness, setHoveredReadiness] = useState<{
    x: number;
    y: number;
    data: ReadinessAssessment;
  } | null>(null);

  if (!trend || trend.length === 0) {
    return (
      <div className="flex h-64 flex-col items-center justify-center rounded-[var(--radius-md)] border border-dashed border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-6 text-center">
        <p className="text-sm font-medium text-[var(--color-text-secondary)]">
          No completed training sessions are available for this date range.
        </p>
        <p className="mt-1 text-xs text-[var(--color-text-muted)]">
          Fitness charts will appear here after sessions are completed and their metrics are recorded.
        </p>
      </div>
    );
  }

  // Sort by date ascending
  const sortedTrend = [...trend].sort(
    (a, b) => new Date(a.date).getTime() - new Date(b.date).getTime()
  );

  const width = 800;
  const height = 240;
  const padding = { top: 25, right: 30, bottom: 40, left: 45 };
  const chartW = width - padding.left - padding.right;
  const chartH = height - padding.top - padding.bottom;

  // Compute the X position by index (or date)
  const getX = (index: number) => {
    if (sortedTrend.length <= 1) return padding.left + chartW / 2;
    return padding.left + (index / (sortedTrend.length - 1)) * chartW;
  };

  // Helper to compute Y from min and max
  const getY = (val: number | null | undefined, minVal: number, maxVal: number) => {
    if (val === null || val === undefined) return null;
    const clamped = Math.max(minVal, Math.min(maxVal, val));
    const ratio = (clamped - minVal) / (maxVal - minVal || 1);
    return padding.top + chartH - ratio * chartH;
  };

  // Map the readiness dates onto X positions on the trend's time axis
  const getReadinessX = (assessmentDate: string) => {
    const time = new Date(assessmentDate).getTime();
    const firstTime = new Date(sortedTrend[0].date).getTime();
    const lastTime = new Date(sortedTrend[sortedTrend.length - 1].date).getTime();

    if (firstTime === lastTime) return padding.left + chartW / 2;
    const ratio = (time - firstTime) / (lastTime - firstTime);
    const clampedRatio = Math.max(0, Math.min(1, ratio));
    return padding.left + clampedRatio * chartW;
  };

  // Build the SVG path from the list of points
  const makePath = (
    accessor: (d: HorseFitnessTrendItem) => number | null | undefined,
    minVal: number,
    maxVal: number
  ) => {
    const points: [number, number][] = [];
    sortedTrend.forEach((d, i) => {
      const v = accessor(d);
      if (v !== null && v !== undefined) {
        const x = getX(i);
        const y = getY(v, minVal, maxVal);
        if (y !== null) points.push([x, y]);
      }
    });

    if (points.length === 0) return '';
    if (points.length === 1) {
      return `M ${points[0][0]} ${points[0][1]} L ${points[0][0] + 0.1} ${points[0][1]}`;
    }

    return points.reduce((acc, curr, idx) => {
      return idx === 0 ? `M ${curr[0]} ${curr[1]}` : `${acc} L ${curr[0]} ${curr[1]}`;
    }, '');
  };

  return (
    <div className="space-y-4">
      {/* Header & Tabs */}
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--color-border)] pb-3">
        <div>
          <h3 className="text-base font-semibold text-[var(--color-text-primary)]">
            Fitness and performance trends
          </h3>
          <p className="text-xs text-[var(--color-text-secondary)]">
            Track fitness trends across training sessions and periodic assessments.
          </p>
        </div>
        <SegmentedControl
          label="Chart"
          value={activeTab}
          onChange={setActiveTab}
          options={[
            { value: 'ALL', label: 'All metrics' },
            { value: 'PERFORMANCE', label: 'Performance and readiness' },
            { value: 'HEART_RATE', label: 'Heart rate (BPM)' },
            { value: 'SPEED_DISTANCE', label: 'Speed and distance' },
          ]}
        />
      </div>

      {/* Chart 1: Performance Rating (1-10) + Readiness Assessment (Scatter markers) */}
      {(activeTab === 'ALL' || activeTab === 'PERFORMANCE') && (
        <div className="relative rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4">
          <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-secondary)]">
              Performance rating (1–10) and readiness score
            </span>
            <div className="flex items-center gap-4 text-xs">
              <span className="flex items-center gap-1.5 text-[var(--color-text-secondary)]">
                <span className="h-2.5 w-2.5 rounded-full bg-[var(--color-success)]" />
                Session performance rating
              </span>
              <span className="flex items-center gap-1.5 text-[var(--color-text-secondary)]">
                <span className="h-2.5 w-2.5 rotate-45 transform bg-[var(--color-primary)]" />
                Periodic readiness score
              </span>
            </div>
          </div>

          <div className="relative w-full overflow-x-auto">
            <svg
              viewBox={`0 0 ${width} ${height}`}
              className="w-full min-w-[650px] overflow-visible"
              style={{ maxHeight: '250px' }}
            >
              {/* Grid Lines */}
              {[0, 2, 4, 6, 8, 10].map((val) => {
                const y = getY(val, 0, 10) || 0;
                return (
                  <g key={val}>
                    <line
                      x1={padding.left}
                      y1={y}
                      x2={padding.left + chartW}
                      y2={y}
                      stroke="var(--color-border)"
                      strokeDasharray="3 3"
                    />
                    <text
                      x={padding.left - 8}
                      y={y + 3}
                      textAnchor="end"
                      fontSize="10"
                      fill="var(--color-text-muted)"
                    >
                      {val}
                    </text>
                  </g>
                );
              })}

              {/* X Axis Labels */}
              {sortedTrend.map((d, i) => {
                if (sortedTrend.length > 12 && i % Math.ceil(sortedTrend.length / 10) !== 0) {
                  return null;
                }
                const x = getX(i);
                return (
                  <text
                    key={d.workoutId}
                    x={x}
                    y={padding.top + chartH + 18}
                    textAnchor="middle"
                    fontSize="9"
                    fill="var(--color-text-secondary)"
                  >
                    {d.date.slice(5)}
                  </text>
                );
              })}

              {/* Path for Performance Rating */}
              <path
                d={makePath((d) => d.performanceRating, 0, 10)}
                fill="none"
                stroke="var(--color-success)"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              />

              {/* Points for Performance Rating */}
              {sortedTrend.map((d, i) => {
                const y = getY(d.performanceRating, 0, 10);
                if (y === null) return null;
                const x = getX(i);
                return (
                  <circle
                    key={d.workoutId}
                    cx={x}
                    cy={y}
                    r="4.5"
                    fill="var(--color-success)"
                    stroke="var(--color-surface)"
                    strokeWidth="1.5"
                    className="cursor-pointer transition-transform hover:scale-150"
                    onMouseEnter={(e) => {
                      const rect = e.currentTarget.getBoundingClientRect();
                      setHoveredPoint({ x: rect.left, y: rect.top, data: d });
                    }}
                    onMouseLeave={() => setHoveredPoint(null)}
                  />
                );
              })}

              {/* Scatter Markers for Readiness Assessments (Discrete, not connected) */}
              {readiness &&
                readiness.map((r) => {
                  const score =
                    r.fitnessScore !== null && r.fitnessScore !== undefined
                      ? Number(r.fitnessScore)
                      : null;
                  if (score === null) return null;
                  const x = getReadinessX(r.assessmentDate);
                  const y = getY(score, 0, 10);
                  if (y === null) return null;

                  return (
                    <polygon
                      key={r.id}
                      points={`${x},${y - 6} ${x + 6},${y} ${x},${y + 6} ${x - 6},${y}`}
                      fill="var(--color-isolated)"
                      stroke="var(--color-surface)"
                      strokeWidth="1.5"
                      className="cursor-pointer transition-transform hover:scale-150"
                      onMouseEnter={(e) => {
                        const rect = e.currentTarget.getBoundingClientRect();
                        setHoveredReadiness({ x: rect.left, y: rect.top, data: r });
                      }}
                      onMouseLeave={() => setHoveredReadiness(null)}
                    />
                  );
                })}
            </svg>
          </div>
        </div>
      )}

      {/* Chart 2: Heart Rate (bpm) with Danger Thresholds (220 & 100 bpm) */}
      {(activeTab === 'ALL' || activeTab === 'HEART_RATE') && (
        <div className="relative rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4">
          <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-secondary)]">
              Heart rate (BPM) and safety threshold
            </span>
            <div className="flex items-center gap-4 text-xs">
              <span className="flex items-center gap-1.5 text-[var(--color-text-secondary)]">
                <span className="h-2 w-4 bg-[var(--color-danger)] rounded-sm" />
                Max HR
              </span>
              <span className="flex items-center gap-1.5 text-[var(--color-text-secondary)]">
                <span className="h-2 w-4 bg-[var(--color-info)] rounded-sm" />
                Avg HR
              </span>
              <span className="flex items-center gap-1.5 text-[var(--color-text-secondary)]">
                <span className="h-2 w-4 bg-[var(--color-warning)] rounded-sm" />
                Recovery HR
              </span>
              <span className="flex items-center gap-1.5 text-[var(--color-danger)] font-medium">
                <span className="h-0.5 w-3 border-t-2 border-dashed border-[var(--color-danger)]" />
                Maximum threshold: 220 bpm
              </span>
            </div>
          </div>

          <div className="relative w-full overflow-x-auto">
            <svg
              viewBox={`0 0 ${width} ${height}`}
              className="w-full min-w-[650px] overflow-visible"
              style={{ maxHeight: '250px' }}
            >
              {/* Grid Lines: 60, 100, 140, 180, 220, 240 */}
              {[60, 100, 140, 180, 220, 240].map((val) => {
                const y = getY(val, 50, 250) || 0;
                const is220 = val === 220;
                const is100 = val === 100;
                return (
                  <g key={val}>
                    <line
                      x1={padding.left}
                      y1={y}
                      x2={padding.left + chartW}
                      y2={y}
                      stroke={
                        is220 ? 'var(--color-danger)' : is100 ? 'var(--color-warning)' : 'var(--color-border)'
                      }
                      strokeWidth={is220 || is100 ? 1.5 : 1}
                      strokeDasharray={is220 || is100 ? '4 4' : '3 3'}
                    />
                    <text
                      x={padding.left - 8}
                      y={y + 3}
                      textAnchor="end"
                      fontSize="10"
                      fill={
                        is220
                          ? 'var(--color-danger)'
                          : is100
                          ? 'var(--color-warning)'
                          : 'var(--color-text-muted)'
                      }
                      fontWeight={is220 || is100 ? '600' : 'normal'}
                    >
                      {val}
                    </text>
                  </g>
                );
              })}

              {/* X Axis Labels */}
              {sortedTrend.map((d, i) => {
                if (sortedTrend.length > 12 && i % Math.ceil(sortedTrend.length / 10) !== 0) {
                  return null;
                }
                const x = getX(i);
                return (
                  <text
                    key={d.workoutId}
                    x={x}
                    y={padding.top + chartH + 18}
                    textAnchor="middle"
                    fontSize="9"
                    fill="var(--color-text-secondary)"
                  >
                    {d.date.slice(5)}
                  </text>
                );
              })}

              {/* Max HR Line */}
              <path
                d={makePath((d) => d.maxHeartRate, 50, 250)}
                fill="none"
                stroke="var(--color-danger)"
                strokeWidth="2"
                strokeLinecap="round"
              />

              {/* Avg HR Line */}
              <path
                d={makePath((d) => d.averageHeartRate, 50, 250)}
                fill="none"
                stroke="var(--color-info)"
                strokeWidth="2"
                strokeLinecap="round"
              />

              {/* Recovery HR Line */}
              <path
                d={makePath((d) => d.recoveryHeartRate, 50, 250)}
                fill="none"
                stroke="var(--color-warning)"
                strokeWidth="2"
                strokeLinecap="round"
              />

              {/* Data points */}
              {sortedTrend.map((d, i) => {
                const x = getX(i);
                const yMax = getY(d.maxHeartRate, 50, 250);
                const yAvg = getY(d.averageHeartRate, 50, 250);
                const yRec = getY(d.recoveryHeartRate, 50, 250);

                return (
                  <g key={d.workoutId}>
                    {yMax !== null && (
                      <circle
                        cx={x}
                        cy={yMax}
                        r="3.5"
                        fill="var(--color-danger)"
                        stroke="var(--color-surface)"
                        strokeWidth="1"
                        className="cursor-pointer hover:scale-150 transition-transform"
                        onMouseEnter={(e) => {
                          const rect = e.currentTarget.getBoundingClientRect();
                          setHoveredPoint({ x: rect.left, y: rect.top, data: d });
                        }}
                        onMouseLeave={() => setHoveredPoint(null)}
                      />
                    )}
                    {yAvg !== null && (
                      <circle
                        cx={x}
                        cy={yAvg}
                        r="3.5"
                        fill="var(--color-info)"
                        stroke="var(--color-surface)"
                        strokeWidth="1"
                        className="cursor-pointer hover:scale-150 transition-transform"
                        onMouseEnter={(e) => {
                          const rect = e.currentTarget.getBoundingClientRect();
                          setHoveredPoint({ x: rect.left, y: rect.top, data: d });
                        }}
                        onMouseLeave={() => setHoveredPoint(null)}
                      />
                    )}
                    {yRec !== null && (
                      <circle
                        cx={x}
                        cy={yRec}
                        r="3.5"
                        fill="var(--color-warning)"
                        stroke="var(--color-surface)"
                        strokeWidth="1"
                        className="cursor-pointer hover:scale-150 transition-transform"
                        onMouseEnter={(e) => {
                          const rect = e.currentTarget.getBoundingClientRect();
                          setHoveredPoint({ x: rect.left, y: rect.top, data: d });
                        }}
                        onMouseLeave={() => setHoveredPoint(null)}
                      />
                    )}
                  </g>
                );
              })}
            </svg>
          </div>
        </div>
      )}

      {/* Chart 3: Speed & Distance */}
      {(activeTab === 'ALL' || activeTab === 'SPEED_DISTANCE') && (
        <div className="relative rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4">
          <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-secondary)]">
              Speed (km/h) and distance (m)
            </span>
            <div className="flex items-center gap-4 text-xs">
              <span className="flex items-center gap-1.5 text-[var(--color-text-secondary)]">
                <span className="h-2 w-4 bg-[var(--color-primary)] rounded-sm" />
                Maximum speed
              </span>
              <span className="flex items-center gap-1.5 text-[var(--color-text-secondary)]">
                <span className="h-2 w-4 bg-[var(--color-finance)] rounded-sm" />
                Average speed
              </span>
            </div>
          </div>

          <div className="relative w-full overflow-x-auto">
            <svg
              viewBox={`0 0 ${width} ${height}`}
              className="w-full min-w-[650px] overflow-visible"
              style={{ maxHeight: '250px' }}
            >
              {/* Grid Lines: 0 to 70 km/h */}
              {[0, 15, 30, 45, 60, 75].map((val) => {
                const y = getY(val, 0, 75) || 0;
                return (
                  <g key={val}>
                    <line
                      x1={padding.left}
                      y1={y}
                      x2={padding.left + chartW}
                      y2={y}
                      stroke="var(--color-border)"
                      strokeDasharray="3 3"
                    />
                    <text
                      x={padding.left - 8}
                      y={y + 3}
                      textAnchor="end"
                      fontSize="10"
                      fill="var(--color-text-muted)"
                    >
                      {val}
                    </text>
                  </g>
                );
              })}

              {/* X Axis Labels */}
              {sortedTrend.map((d, i) => {
                if (sortedTrend.length > 12 && i % Math.ceil(sortedTrend.length / 10) !== 0) {
                  return null;
                }
                const x = getX(i);
                return (
                  <text
                    key={d.workoutId}
                    x={x}
                    y={padding.top + chartH + 18}
                    textAnchor="middle"
                    fontSize="9"
                    fill="var(--color-text-secondary)"
                  >
                    {d.date.slice(5)}
                  </text>
                );
              })}

              {/* Top Speed Line */}
              <path
                d={makePath((d) => d.topSpeedKmh, 0, 75)}
                fill="none"
                stroke="var(--color-primary)"
                strokeWidth="2"
                strokeLinecap="round"
              />

              {/* Avg Speed Line */}
              <path
                d={makePath((d) => d.averageSpeedKmh, 0, 75)}
                fill="none"
                stroke="var(--color-finance)"
                strokeWidth="2"
                strokeLinecap="round"
              />

              {/* Data points */}
              {sortedTrend.map((d, i) => {
                const x = getX(i);
                const yTop = getY(d.topSpeedKmh, 0, 75);
                const yAvg = getY(d.averageSpeedKmh, 0, 75);

                return (
                  <g key={d.workoutId}>
                    {yTop !== null && (
                      <circle
                        cx={x}
                        cy={yTop}
                        r="3.5"
                        fill="var(--color-primary)"
                        stroke="var(--color-surface)"
                        strokeWidth="1"
                        className="cursor-pointer hover:scale-150 transition-transform"
                        onMouseEnter={(e) => {
                          const rect = e.currentTarget.getBoundingClientRect();
                          setHoveredPoint({ x: rect.left, y: rect.top, data: d });
                        }}
                        onMouseLeave={() => setHoveredPoint(null)}
                      />
                    )}
                    {yAvg !== null && (
                      <circle
                        cx={x}
                        cy={yAvg}
                        r="3.5"
                        fill="var(--color-finance)"
                        stroke="var(--color-surface)"
                        strokeWidth="1"
                        className="cursor-pointer hover:scale-150 transition-transform"
                        onMouseEnter={(e) => {
                          const rect = e.currentTarget.getBoundingClientRect();
                          setHoveredPoint({ x: rect.left, y: rect.top, data: d });
                        }}
                        onMouseLeave={() => setHoveredPoint(null)}
                      />
                    )}
                  </g>
                );
              })}
            </svg>
          </div>
        </div>
      )}

      {/* Floating Tooltip for Workout Point */}
      {hoveredPoint && (
        <div
          className="pointer-events-none fixed z-50 -translate-x-1/2 -translate-y-full rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-2.5 shadow-[var(--shadow-popover)]"
          style={{
            left: `${hoveredPoint.x}px`,
            top: `${hoveredPoint.y - 10}px`,
          }}
        >
          <p className="text-xs font-bold text-[var(--color-text-primary)]">
            {hoveredPoint.data.date} • {hoveredPoint.data.subjectName}
          </p>
          <div className="mt-1 space-y-0.5 text-xs text-[var(--color-text-secondary)]">
            <p>
              Performance:{' '}
              <span className="font-semibold text-[var(--color-success)]">
                {hoveredPoint.data.performanceRating ?? 'N/A'}/10
              </span>
            </p>
            <p>
              Heart rate:{' '}
              <span>
                Avg {hoveredPoint.data.averageHeartRate ?? '-'} | Max{' '}
                <strong className={hoveredPoint.data.maxHeartRate && hoveredPoint.data.maxHeartRate > 220 ? 'text-[var(--color-danger)]' : ''}>
                  {hoveredPoint.data.maxHeartRate ?? '-'}
                </strong>{' '}
                | Rec{' '}
                <strong className={hoveredPoint.data.recoveryHeartRate && hoveredPoint.data.recoveryHeartRate > 100 ? 'text-[var(--color-warning)]' : ''}>
                  {hoveredPoint.data.recoveryHeartRate ?? '-'}
                </strong>{' '}
                bpm
              </span>
            </p>
            <p>
              Speed: Average {hoveredPoint.data.averageSpeedKmh ?? '-'} km/h | Max{' '}
              {hoveredPoint.data.topSpeedKmh ?? '-'} km/h
            </p>
            {hoveredPoint.data.distanceMeters && (
              <p>Distance: {hoveredPoint.data.distanceMeters.toLocaleString()} m</p>
            )}
          </div>
        </div>
      )}

      {/* Floating Tooltip for Readiness Point */}
      {hoveredReadiness && (
        <div
          className="pointer-events-none fixed z-50 -translate-x-1/2 -translate-y-full rounded-[var(--radius-md)] border border-[var(--color-primary)] bg-[var(--color-surface)] p-2.5 shadow-[var(--shadow-popover)]"
          style={{
            left: `${hoveredReadiness.x}px`,
            top: `${hoveredReadiness.y - 10}px`,
          }}
        >
          <p className="text-xs font-bold text-[var(--color-primary)]">
            Periodic readiness assessment · {hoveredReadiness.data.assessmentDate}
          </p>
          <div className="mt-1 space-y-0.5 text-xs text-[var(--color-text-secondary)]">
            <p>
              Fitness score:{' '}
              <strong className="text-[var(--color-primary)]">
                {hoveredReadiness.data.fitnessScore ?? 'Not rated'}/10
              </strong>
            </p>
            <p>
              Status: <strong>{hoveredReadiness.data.readinessStatus}</strong>
            </p>
            {hoveredReadiness.data.estimatedMonthsToRace !== null && (
              <p>Estimated time to race readiness: {hoveredReadiness.data.estimatedMonthsToRace} months</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
