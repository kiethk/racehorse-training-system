'use client';

import React, { useEffect, useState, useMemo } from 'react';
import { Panel, SectionTitle, SegmentedControl, Skeleton, EmptyState } from '@/components/ui';
import { edaAnalyticsService } from '../services/edaAnalyticsService';
import type { FleetEdaResponse, IndividualHorseEdaResponse } from '../types/eda';
import { CorrelationBadge } from './CorrelationBadge';
import { StatisticalSummaryTable } from './StatisticalSummaryTable';
import { EdaScatterChart, type ChartDataPoint } from './EdaScatterChart';

type ViewMode = 'FLEET' | 'INDIVIDUAL';

export interface HorseEdaOption {
  id: number;
  name: string;
  breed?: string;
}

export interface HorseEdaAnalyticsCardProps {
  initialHorseId?: number;
  availableHorses?: HorseEdaOption[];
}

export function HorseEdaAnalyticsCard({ initialHorseId, availableHorses }: HorseEdaAnalyticsCardProps) {
  const [viewMode, setViewMode] = useState<ViewMode>(initialHorseId ? 'INDIVIDUAL' : 'FLEET');
  const [fleetData, setFleetData] = useState<FleetEdaResponse | null>(null);
  const [individualData, setIndividualData] = useState<IndividualHorseEdaResponse | null>(null);
  const [selectedHorseId, setSelectedHorseId] = useState<number | null>(
    initialHorseId || (availableHorses && availableHorses.length > 0 ? availableHorses[0].id : null)
  );

  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Danh sách ngựa để chọn: ưu tiên availableHorses hoặc trích xuất từ fleetData.points
  const horseOptions: HorseEdaOption[] = useMemo(() => {
    if (availableHorses && availableHorses.length > 0) {
      return availableHorses;
    }
    if (fleetData?.points && fleetData.points.length > 0) {
      return fleetData.points.map((p) => ({
        id: p.horseId,
        name: p.horseName,
        breed: p.breed,
      }));
    }
    return [];
  }, [availableHorses, fleetData]);

  // Tự động gán selectedHorseId nếu chưa có
  useEffect(() => {
    if (!selectedHorseId && horseOptions.length > 0) {
      setSelectedHorseId(horseOptions[0].id);
    }
  }, [selectedHorseId, horseOptions]);

  // Tải dữ liệu toàn đàn
  useEffect(() => {
    let active = true;
    edaAnalyticsService
      .getFleetEdaAnalytics(180)
      .then((data) => {
        if (active) {
          setFleetData(data);
          if (!selectedHorseId && data.points.length > 0) {
            setSelectedHorseId(data.points[0].horseId);
          }
          setLoading(false);
        }
      })
      .catch((err) => {
        if (active) {
          setError(err?.message || 'Không thể tải dữ liệu phân tích EDA');
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  // Tải dữ liệu cá nhân khi chuyển sang mode INDIVIDUAL hoặc đổi ngựa
  useEffect(() => {
    if (viewMode === 'INDIVIDUAL') {
      if (!selectedHorseId) {
        setLoading(false);
        return;
      }
      let active = true;
      setLoading(true);
      edaAnalyticsService
        .getIndividualEdaAnalytics(selectedHorseId, 365)
        .then((data) => {
          if (active) {
            setIndividualData(data);
            setLoading(false);
          }
        })
        .catch((err) => {
          if (active) {
            setError(err?.message || 'Không thể tải phân tích chi tiết của chiến mã');
            setLoading(false);
          }
        });

      return () => {
        active = false;
      };
    }
  }, [viewMode, selectedHorseId]);

  // Chuyển đổi dữ liệu points sang format của Chart
  const chartPoints: ChartDataPoint[] = useMemo(() => {
    if (viewMode === 'FLEET') {
      if (!fleetData?.points) return [];
      return fleetData.points.map((p) => ({
        id: p.horseId,
        label: p.horseName,
        subLabel: `${p.breed} · Chuồng: ${p.stallCode || 'N/A'}`,
        x: p.avgSpeedKmh,
        y: p.recoveryHeartRateBpm,
        maxHr: p.maxHeartRateBpm,
        isOutlier: p.isOutlier,
        outlierReason: p.outlierReason,
      }));
    } else {
      if (!individualData?.points) return [];
      return individualData.points.map((p) => ({
        id: p.workoutId,
        label: `Buổi tập: ${p.sessionName}`,
        subLabel: `Ngày: ${p.sessionDate} ${p.distanceMeters ? `· ${p.distanceMeters}m` : ''}`,
        x: p.speedKmh,
        y: p.recoveryHeartRateBpm,
        maxHr: p.maxHeartRateBpm,
        isOutlier: p.isOutlier,
        outlierReason: p.outlierReason,
      }));
    }
  }, [viewMode, fleetData, individualData]);

  const activeStats = viewMode === 'FLEET' ? fleetData : individualData;

  const outliersList = useMemo(() => {
    return chartPoints.filter((p) => p.isOutlier);
  }, [chartPoints]);

  if (error) {
    return (
      <Panel padded>
        <EmptyState icon="alert-triangle" title="Không thể hiển thị phân tích EDA" description={error} />
      </Panel>
    );
  }

  return (
    <Panel padded className="space-y-4">
      {/* Header & Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-[var(--color-border)] pb-3">
        <div>
          <div className="flex items-center gap-2">
            <SectionTitle>Phân Tích Tương Quan & Đặc Trưng EDA</SectionTitle>
            <span className="text-[11px] font-semibold uppercase tracking-wider px-2 py-0.5 rounded bg-[var(--color-surface-muted)] text-[var(--color-text-secondary)]">
              AI Analytics
            </span>
          </div>
          <p className="text-xs text-[var(--color-text-secondary)] mt-0.5">
            Khám phá mối tương quan giữa tốc độ bài tập và nhịp tim hồi phục, tự động phát hiện các điểm dị biệt (Tukey 1.5×IQR).
          </p>
        </div>

        <div className="flex items-center gap-2 self-start sm:self-auto">
          {viewMode === 'INDIVIDUAL' && horseOptions.length > 0 && (
            <select
              aria-label="Chọn ngựa cần phân tích"
              value={selectedHorseId ?? ''}
              onChange={(e) => setSelectedHorseId(Number(e.target.value))}
              className="text-xs bg-[var(--color-surface)] border border-[var(--color-border)] rounded-[var(--radius-sm)] px-2.5 py-1.5 font-medium text-[var(--color-text-primary)]"
            >
              {horseOptions.map((h) => (
                <option key={h.id} value={h.id}>
                  {h.name} {h.breed ? `(${h.breed})` : ''}
                </option>
              ))}
            </select>
          )}

          <SegmentedControl<ViewMode>
            label="Chế độ phân tích EDA"
            value={viewMode}
            onChange={setViewMode}
            options={[
              { value: 'FLEET', label: 'Toàn đàn (Fleet)' },
              { value: 'INDIVIDUAL', label: 'Cá nhân (Horse)' },
            ]}
          />
        </div>
      </div>

      {loading || !activeStats ? (
        <div className="space-y-3 py-6">
          <Skeleton className="h-6 w-1/3" />
          <Skeleton className="h-[280px] w-full" />
          <div className="grid grid-cols-2 gap-3">
            <Skeleton className="h-24 w-full" />
            <Skeleton className="h-24 w-full" />
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          {/* Correlation Indicator Row */}
          <div className="flex flex-wrap items-center justify-between gap-3 bg-[var(--color-surface-muted)]/50 p-2.5 rounded-[var(--radius-md)] border border-[var(--color-border)]">
            <CorrelationBadge
              strength={activeStats.correlation.strength}
              pearsonR={activeStats.correlation.pearsonR}
            />

            <div className="text-right text-xs text-[var(--color-text-secondary)]">
              <div>
                Phương trình hồi quy: <code className="text-[var(--color-text-primary)] font-semibold">y = {activeStats.correlation.slope.toFixed(2)}x + {activeStats.correlation.intercept.toFixed(1)}</code>
              </div>
              <div className="text-[11px] mt-0.5">
                Mẫu quan sát: <b>{activeStats.correlation.sampleSize}</b> điểm dữ liệu
              </div>
            </div>
          </div>

          {/* Scatter Chart */}
          <div className="bg-[var(--color-surface)] border border-[var(--color-border)] rounded-[var(--radius-md)] p-3">
            <EdaScatterChart
              points={chartPoints}
              slope={activeStats.correlation.slope}
              intercept={activeStats.correlation.intercept}
              xAxisLabel={viewMode === 'FLEET' ? 'Tốc độ TB toàn đàn (km/h)' : 'Tốc độ buổi tập (km/h)'}
              yAxisLabel="Nhịp tim hồi phục sau bài tập (bpm)"
            />
          </div>

          {/* Descriptive Statistics Tables */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <StatisticalSummaryTable
              title="Phân bố Tốc độ (Speed Distribution)"
              unit="km/h"
              stats={activeStats.speedStats}
            />
            <StatisticalSummaryTable
              title="Phân bố Nhịp tim hồi phục (Recovery HR Distribution)"
              unit="bpm"
              stats={activeStats.recoveryHeartRateStats}
            />
          </div>

          {/* Outlier Insights Panel if any */}
          {outliersList.length > 0 && (
            <div className="rounded-[var(--radius-md)] border border-[var(--color-warning)]/30 bg-[var(--color-warning-subtle)]/40 p-3 text-xs">
              <div className="flex items-center gap-1.5 font-semibold text-[var(--color-warning)] mb-1">
                <span>⚠️ Phát hiện {outliersList.length} điểm dữ liệu dị biệt (Tukey's Outliers):</span>
              </div>
              <ul className="list-disc list-inside space-y-0.5 text-[var(--color-text-secondary)]">
                {outliersList.map((p) => (
                  <li key={p.id}>
                    <b className="text-[var(--color-text-primary)]">{p.label}</b>: {p.outlierReason} ({p.x} km/h, {p.y} bpm)
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </Panel>
  );
}
