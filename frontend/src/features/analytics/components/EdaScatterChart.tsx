'use client';

import React, { useMemo } from 'react';
import {
  ResponsiveContainer,
  ScatterChart,
  Scatter,
  XAxis,
  YAxis,
  ZAxis,
  Tooltip,
  Cell,
} from 'recharts';
import { calculateTrendLinePoints } from '../lib/edaTransform';

export interface ChartDataPoint {
  id: number;
  label: string;
  subLabel?: string | null;
  x: number; // Tốc độ (km/h)
  y: number; // Nhịp tim hồi phục (bpm)
  maxHr?: number | null;
  isOutlier: boolean;
  outlierReason?: string | null;
}

interface EdaScatterChartProps {
  points: ChartDataPoint[];
  slope: number;
  intercept: number;
  xAxisLabel?: string;
  yAxisLabel?: string;
}

export function EdaScatterChart({
  points,
  slope,
  intercept,
  xAxisLabel = 'Tốc độ trung bình (km/h)',
  yAxisLabel = 'Nhịp tim hồi phục (bpm)',
}: EdaScatterChartProps) {
  const { minX, maxX, minY, maxY, trendLineData } = useMemo(() => {
    if (!points || points.length === 0) {
      return { minX: 30, maxX: 70, minY: 60, maxY: 160, trendLineData: [] };
    }

    const xVals = points.map((p) => p.x);
    const yVals = points.map((p) => p.y);

    const minXVal = Math.floor(Math.min(...xVals) * 0.95);
    const maxXVal = Math.ceil(Math.max(...xVals) * 1.05);
    const minYVal = Math.floor(Math.min(...yVals) * 0.90);
    const maxYVal = Math.ceil(Math.max(...yVals) * 1.10);

    const trend = calculateTrendLinePoints(minXVal, maxXVal, slope, intercept);

    return {
      minX: minXVal,
      maxX: maxXVal,
      minY: minYVal,
      maxY: maxYVal,
      trendLineData: trend,
    };
  }, [points, slope, intercept]);

  const CustomTooltip = ({ active, payload }: any) => {
    if (!active || !payload || !payload.length) return null;
    const data: ChartDataPoint = payload[0].payload;
    if (!data.label) return null;

    return (
      <div className="bg-[var(--color-surface)] border border-[var(--color-border)] rounded-md p-2.5 shadow-lg text-xs min-w-[190px]">
        <div className="font-semibold text-sm text-[var(--color-text-primary)] mb-1">
          {data.label}
        </div>
        {data.subLabel && (
          <div className="text-[11px] text-[var(--color-text-secondary)] mb-1.5">
            {data.subLabel}
          </div>
        )}
        <div className="space-y-1">
          <div className="flex justify-between gap-4">
            <span className="text-[var(--color-text-secondary)]">Tốc độ:</span>
            <span className="font-medium text-[var(--color-text-primary)]">{data.x} km/h</span>
          </div>
          <div className="flex justify-between gap-4">
            <span className="text-[var(--color-text-secondary)]">Hồi phục tim:</span>
            <span className="font-medium text-[var(--color-text-primary)]">{data.y} bpm</span>
          </div>
          {data.maxHr != null && (
            <div className="flex justify-between gap-4">
              <span className="text-[var(--color-text-secondary)]">Nhịp tim max:</span>
              <span className="font-medium text-[var(--color-text-primary)]">{data.maxHr} bpm</span>
            </div>
          )}
          {data.isOutlier && (
            <div className="mt-1.5 pt-1.5 border-t border-[var(--color-danger)]/30 text-[11px] text-[var(--color-danger)] font-medium">
              ⚠️ Điểm dị biệt (Outlier):
              <div className="font-normal text-[10px] text-[var(--color-text-primary)] mt-0.5">
                {data.outlierReason || 'Giá trị lệch chuẩn > 1.5 IQR'}
              </div>
            </div>
          )}
        </div>
      </div>
    );
  };

  return (
    <div className="w-full h-[320px] flex flex-col">
      <div className="flex items-center justify-end gap-3 text-xs text-[var(--color-text-secondary)] mb-1">
        <span className="flex items-center gap-1.5">
          <span className="w-2.5 h-2.5 rounded-full bg-[var(--color-info)]" /> Bình thường
        </span>
        <span className="flex items-center gap-1.5">
          <span className="w-2.5 h-2.5 rounded-full bg-[var(--color-danger)] ring-2 ring-[var(--color-danger)]/30" /> Dị biệt (Outlier)
        </span>
        {trendLineData.length > 0 && (
          <span className="flex items-center gap-1.5">
            <span className="w-4 h-0.5 border-t-2 border-dashed border-[var(--color-warning)]" /> Đường hồi quy (Trend)
          </span>
        )}
      </div>

      <div className="flex-1 w-full min-h-[280px]">
        <ResponsiveContainer width="100%" height="100%">
          <ScatterChart margin={{ top: 10, right: 20, bottom: 25, left: 10 }}>
            <XAxis
              type="number"
              dataKey="x"
              name="Tốc độ"
              unit=" km/h"
              domain={[minX, maxX]}
              label={{ value: xAxisLabel, position: 'bottom', offset: 10, fontSize: 11, fill: 'var(--color-text-secondary)' }}
              tick={{ fontSize: 11, fill: 'var(--color-text-secondary)' }}
            />
            <YAxis
              type="number"
              dataKey="y"
              name="Nhịp tim hồi phục"
              unit=" bpm"
              domain={[minY, maxY]}
              label={{ value: yAxisLabel, angle: -90, position: 'left', offset: 0, fontSize: 11, fill: 'var(--color-text-secondary)' }}
              tick={{ fontSize: 11, fill: 'var(--color-text-secondary)' }}
            />
            <ZAxis range={[50, 90]} />
            <Tooltip content={<CustomTooltip />} />

            {/* Đường hồi quy tuyến tính xu hướng */}
            {trendLineData.length > 0 && (
              <Scatter
                name="Trendline"
                data={trendLineData}
                line={{ stroke: 'var(--color-warning)', strokeWidth: 2, strokeDasharray: '4 4' }}
                shape={() => null}
                legendType="none"
              />
            )}

            {/* Các điểm dữ liệu phân tán */}
            <Scatter name="DataPoints" data={points}>
              {points.map((entry, index) => (
                <Cell
                  key={`cell-${entry.id}-${index}`}
                  fill={entry.isOutlier ? 'var(--color-danger)' : 'var(--color-info)'}
                  stroke={entry.isOutlier ? '#991b1b' : '#1e40af'}
                  strokeWidth={entry.isOutlier ? 2 : 1}
                />
              ))}
            </Scatter>
          </ScatterChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
