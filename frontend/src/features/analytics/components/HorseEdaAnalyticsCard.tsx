'use client';

import React, { useEffect, useState, useMemo } from 'react';
import { Panel, SectionTitle, SegmentedControl, Skeleton, EmptyState, Tooltip, Icon, Notice } from '@/components/ui';
import { edaAnalyticsService } from '../services/edaAnalyticsService';
import type { FleetEdaResponse, IndividualHorseEdaResponse } from '../types/eda';
import { CorrelationBadge } from './CorrelationBadge';
import { StatisticalSummaryTable } from './StatisticalSummaryTable';
import { EdaScatterChart, type ChartDataPoint } from './EdaScatterChart';

type ViewMode = 'FLEET' | 'INDIVIDUAL';

export interface HorseEdaOption {
  id: number;
  name: string;
  breed?: string | null;
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

  // Candidate horses for selection: prefer availableHorses or extract from fleetData.points
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

  // Auto-assign selectedHorseId if not already set
  useEffect(() => {
    if (!selectedHorseId && horseOptions.length > 0) {
      setSelectedHorseId(horseOptions[0].id);
    }
  }, [selectedHorseId, horseOptions]);

  // Fetch fleet EDA analytics data
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
          setError(err?.message || 'Failed to load fleet EDA analytics data');
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  // Fetch individual horse data when switching to INDIVIDUAL or changing horse
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
            setError(err?.message || 'Failed to load individual horse analytics');
            setLoading(false);
          }
        });

      return () => {
        active = false;
      };
    }
  }, [viewMode, selectedHorseId]);

  // Transform points for chart display
  const chartPoints: ChartDataPoint[] = useMemo(() => {
    if (viewMode === 'FLEET') {
      if (!fleetData?.points) return [];
      return fleetData.points.map((p) => ({
        id: p.horseId,
        label: p.horseName,
        horseName: p.horseName,
        subLabel: `${p.breed} · Stall: ${p.stallCode || 'N/A'}`,
        x: p.avgSpeedKmh,
        y: p.recoveryHeartRateBpm,
        maxHr: p.maxHeartRateBpm,
        isOutlier: p.isOutlier,
        outlierReason: p.outlierReason,
        isFleet: true,
      }));
    } else {
      if (!individualData?.points) return [];
      return individualData.points.map((p) => ({
        id: p.workoutId,
        label: `Session: ${p.sessionName}`,
        subject: p.sessionName,
        date: p.sessionDate,
        distanceMeters: p.distanceMeters,
        subLabel: `Date: ${p.sessionDate} ${p.distanceMeters ? `· ${p.distanceMeters}m` : ''}`,
        x: p.speedKmh,
        y: p.recoveryHeartRateBpm,
        maxHr: p.maxHeartRateBpm,
        isOutlier: p.isOutlier,
        outlierReason: p.outlierReason,
        isFleet: false,
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
        <EmptyState icon="alert-triangle" title="Unable to load EDA analytics" description={error} />
      </Panel>
    );
  }

  return (
    <Panel padded className="space-y-4">
      {/* Header & Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-[var(--color-border)] pb-3">
        <div>
          <SectionTitle>Cardio Fitness & Speed Correlation</SectionTitle>
          <p className="text-xs text-[var(--color-text-secondary)] mt-0.5">
            Explore how workout speed affects recovery heart rate, with automated detection of unusual sessions.
          </p>
        </div>

        <div className="flex items-center gap-2 self-start sm:self-auto">
          {viewMode === 'INDIVIDUAL' && (
            <select
              aria-label="Select horse to analyze"
              value={selectedHorseId ?? ''}
              onChange={(e) => setSelectedHorseId(Number(e.target.value))}
              className="text-xs bg-[var(--color-surface)] border border-[var(--color-border)] rounded-[var(--radius-sm)] px-2.5 py-1.5 font-medium text-[var(--color-text-primary)]"
            >
              {horseOptions.length === 0 ? (
                <option value="" disabled>
                  Select horse...
                </option>
              ) : (
                horseOptions.map((h) => (
                  <option key={h.id} value={h.id}>
                    {h.name} {h.breed ? `(${h.breed})` : ''}
                  </option>
                ))
              )}
            </select>
          )}

          <SegmentedControl<ViewMode>
            label="EDA Analysis Mode"
            value={viewMode}
            onChange={setViewMode}
            options={[
              { value: 'FLEET', label: 'Fleet Overview' },
              { value: 'INDIVIDUAL', label: 'Individual Horse' },
            ]}
          />
        </div>
      </div>

      {loading ? (
        <div className="space-y-3 py-6">
          <Skeleton className="h-6 w-1/3" />
          <Skeleton className="h-[280px] w-full" />
          <Skeleton className="h-32 w-full" />
        </div>
      ) : !activeStats || chartPoints.length === 0 ? (
        <div className="py-6">
          <EmptyState
            icon="activity"
            title="No workout data available"
            description="There are no completed workouts with recorded speed and recovery heart rate in this time period."
          />
        </div>
      ) : (
        <div className="space-y-4">
          {/* Correlation Indicator Row */}
          <div className="flex flex-wrap items-center justify-between gap-3 bg-[var(--color-surface-muted)]/50 p-2.5 rounded-[var(--radius-md)] border border-[var(--color-border)]">
            <CorrelationBadge
              strength={activeStats.correlation.strength}
              pearsonR={activeStats.correlation.pearsonR}
            />

            <div className="text-right text-xs text-[var(--color-text-secondary)] space-y-1">
              <div className="flex items-center justify-end gap-1.5">
                <span>
                  Cardiac Response:{' '}
                  <strong className="text-[var(--color-text-primary)] font-medium">
                    +{activeStats.correlation.slope.toFixed(1)} bpm
                  </strong>{' '}
                  per km/h speed increase
                </span>
                <Tooltip content={`Linear model: HR = ${activeStats.correlation.slope.toFixed(2)} × speed + ${activeStats.correlation.intercept.toFixed(1)}`}>
                  <button
                    type="button"
                    aria-label="Regression formula details"
                    className="inline-flex items-center text-[var(--color-text-muted)] hover:text-[var(--color-text-primary)] transition-colors focus:outline-none"
                  >
                    <Icon name="info" size={14} />
                  </button>
                </Tooltip>
              </div>
              <div className="text-[11px]">
                Sample Size: <b>{activeStats.correlation.sampleSize}</b> data points
              </div>
            </div>
          </div>

          {/* Scatter Chart */}
          <div className="bg-[var(--color-surface)] border border-[var(--color-border)] rounded-[var(--radius-md)] p-3">
            <EdaScatterChart
              title={viewMode === 'FLEET' ? 'Fleet Bivariate Distribution' : 'Workout Bivariate Distribution'}
              points={chartPoints}
              slope={activeStats.correlation.slope}
              intercept={activeStats.correlation.intercept}
              xAxisLabel={viewMode === 'FLEET' ? 'Fleet Average Speed (km/h)' : 'Workout Speed (km/h)'}
              yAxisLabel="Post-Workout Recovery Heart Rate (bpm)"
            />
          </div>

          {/* Descriptive Statistics Table */}
          <StatisticalSummaryTable
            speedStats={activeStats.speedStats}
            recoveryHeartRateStats={activeStats.recoveryHeartRateStats}
            speedHeader={viewMode === 'FLEET' ? 'Average Speed (km/h)' : 'Workout Speed (km/h)'}
            recoveryHrHeader="Recovery HR (bpm)"
          />

          {/* Outlier Insights Panel if any */}
          {outliersList.length > 0 && (
            <Notice tone="warning" title={`Detected ${outliersList.length} unusual workout session(s):`}>
              <ul className="list-disc list-inside space-y-0.5 text-xs text-[var(--color-text-secondary)] mt-1">
                {outliersList.map((p) => (
                  <li key={p.id}>
                    <b className="text-[var(--color-text-primary)]">{p.label}</b>: {p.outlierReason} ({p.x} km/h, {p.y} bpm)
                  </li>
                ))}
              </ul>
            </Notice>
          )}
        </div>
      )}
    </Panel>
  );
}
