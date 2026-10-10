'use client';

import { SegmentedControl } from '@/components/ui/SegmentedControl';
import { TBody, THead, Table, Td, Th, Tr } from '@/components/ui/Table';
import { Input } from '@/components/ui/Input';
import { useEffect, useState, useCallback } from 'react';
import Link from 'next/link';
import { trainingService } from '../services/trainingService';
import type { HorseFitnessTrendItem, HorseAlert, ReadinessAssessment } from '../types';
import { FitnessTrendChart } from './FitnessTrendChart';
import { Panel } from '@/components/ui/Panel';
import { Icon } from '@/components/ui/Icon';
import { Button } from '@/components/ui/Button';
import { ScreenLayout } from '@/components/ui/ScreenLayout';
import { displayError, formatDate } from '@/lib/display';

interface HorseFitnessTrendViewProps {
  horseId: number;
}

type DatePreset = '7D' | '30D' | '90D' | 'ALL' | 'CUSTOM';

export function HorseFitnessTrendView({ horseId }: HorseFitnessTrendViewProps) {
  const [horse, setHorse] = useState<{ id: number; name: string; ueln: string; status: string } | null>(null);
  const [alerts, setAlerts] = useState<HorseAlert[]>([]);
  const [trend, setTrend] = useState<HorseFitnessTrendItem[]>([]);
  const [readiness, setReadiness] = useState<ReadinessAssessment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [preset, setPreset] = useState<DatePreset>('30D');
  const [fromDate, setFromDate] = useState<string>('');
  const [toDate, setToDate] = useState<string>('');

  // Calculate the date range for the selected preset.
  const getDateRange = useCallback((selectedPreset: DatePreset) => {
    const today = new Date();
    const to = today.toISOString().split('T')[0];

    if (selectedPreset === '7D') {
      const d = new Date(today);
      d.setDate(d.getDate() - 7);
      return { from: d.toISOString().split('T')[0], to };
    }
    if (selectedPreset === '30D') {
      const d = new Date(today);
      d.setDate(d.getDate() - 30);
      return { from: d.toISOString().split('T')[0], to };
    }
    if (selectedPreset === '90D') {
      const d = new Date(today);
      d.setDate(d.getDate() - 90);
      return { from: d.toISOString().split('T')[0], to };
    }
    if (selectedPreset === 'ALL') {
      return { from: undefined, to: undefined };
    }
    return { from: fromDate || undefined, to: toDate || undefined };
  }, [fromDate, toDate]);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const { from, to } = getDateRange(preset);
      const [horseData, alertsData, trendData, readinessData] = await Promise.all([
        trainingService.getHorseById(horseId).catch(() => null),
        trainingService.getHorseAlerts(horseId).catch(() => []),
        trainingService.getFitnessTrend(horseId, from, to),
        trainingService.getReadinessHistory(horseId, false).catch(() => []),
      ]);

      if (horseData) setHorse(horseData);
      setAlerts(alertsData);
      setTrend(trendData);
      setReadiness(readinessData);
    } catch (err: unknown) {
      setError(displayError(err, 'Unable to load horse fitness data.'));
    } finally {
      setLoading(false);
    }
  }, [horseId, preset, getDateRange]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadData();
  }, [loadData]);

  const handlePresetChange = (newPreset: DatePreset) => {
    setPreset(newPreset);
  };

  const handleCustomFilter = (e: React.FormEvent) => {
    e.preventDefault();
    setPreset('CUSTOM');
    loadData();
  };

  // Calculate summary metrics from the trend.
  const completedCount = trend.length;
  const avgRating =
    completedCount > 0
      ? (
          trend.reduce((sum, item) => sum + (item.performanceRating || 0), 0) /
          (trend.filter((t) => t.performanceRating != null).length || 1)
        ).toFixed(1)
      : null;

  const maxRecordedHR =
    completedCount > 0
      ? Math.max(...trend.map((t) => t.maxHeartRate || 0))
      : null;

  const topRecordedSpeed =
    completedCount > 0
      ? Math.max(...trend.map((t) => t.topSpeedKmh || 0))
      : null;

  return (
    <ScreenLayout variant="detail">
      {/* Breadcrumb & Navigation */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-2 text-sm text-[var(--color-text-secondary)]">
          <Link
            href="/trainer/dashboard"
            className="hover:text-[var(--color-text-primary)] transition-colors"
          >
            Dashboard
          </Link>
          <span>/</span>
          <Link
            href="/trainer/horses"
            className="hover:text-[var(--color-text-primary)] transition-colors"
          >
            Horses
          </Link>
          <span>/</span>
          <span className="font-semibold text-[var(--color-text-primary)]">
            {horse?.name || `Horse #${horseId}`}
          </span>
        </div>

        <div className="flex items-center gap-2">
          <Link href="/trainer/horses">
            <Button variant="secondary" size="sm">
              <Icon name="arrow-left" size={14} />
              Back to horses
            </Button>
          </Link>
          <Button variant="secondary" size="sm" onClick={() => loadData()}>
            <Icon name="refresh" size={14} />
            Refresh
          </Button>
        </div>
      </div>

      {/* Main Header Card */}
      <Panel padded className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold tracking-tight text-[var(--color-text-primary)]">
              {horse?.name || `Horse #${horseId}`}
            </h1>
            {horse?.ueln && (
              <span className="rounded bg-[var(--color-surface-subtle)] px-2 py-0.5 text-xs font-metric font-medium text-[var(--color-text-secondary)]">
                UELN: {horse.ueln}
              </span>
            )}
            {horse?.status && (
              <span className="rounded-full bg-[var(--color-info-soft)] px-2.5 py-0.5 text-xs font-medium text-[var(--color-info)]">
                {horse.status}
              </span>
            )}
          </div>
          <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
            Track fitness trends, injury risks and development progress.
          </p>
        </div>

        {/* Quick stat highlights */}
        <div className="flex items-center gap-6">
          <div className="text-right">
            <span className="text-xs uppercase text-[var(--color-text-muted)]">Completed sessions</span>
            <p className="font-metric text-xl font-bold text-[var(--color-text-primary)]">
              {completedCount}
            </p>
          </div>
          <div className="text-right">
            <span className="text-xs uppercase text-[var(--color-text-muted)]">Average performance</span>
            <p className="font-metric text-xl font-bold text-[var(--color-success)]">
              {avgRating ? `${avgRating}/10` : '-'}
            </p>
          </div>
          <div className="text-right">
            <span className="text-xs uppercase text-[var(--color-text-muted)]">Max HR</span>
            <p className={`font-metric text-xl font-bold ${maxRecordedHR && maxRecordedHR > 220 ? 'text-[var(--color-danger)]' : 'text-[var(--color-danger)]'}`}>
              {maxRecordedHR ? `${maxRecordedHR} bpm` : '-'}
            </p>
          </div>
          <div className="text-right">
            <span className="text-xs uppercase text-[var(--color-text-muted)]">Top Speed</span>
            <p className="font-metric text-xl font-bold text-[var(--color-primary)]">
              {topRecordedSpeed ? `${topRecordedSpeed} km/h` : '-'}
            </p>
          </div>
        </div>
      </Panel>

      {/* Real-time Alerts Section */}
      <div>
        <div className="mb-2 flex items-center justify-between">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-[var(--color-text-secondary)]">
            Fitness and injury risk alerts
          </h2>
          {alerts.length > 0 && (
            <span className="rounded-full bg-[var(--color-danger-soft)] text-[var(--color-danger)] px-2 py-0.5 text-xs font-semibold">
              {alerts.length} active alerts
            </span>
          )}
        </div>

        {alerts.length === 0 ? (
          <div className="flex items-center gap-3 rounded-[var(--radius-md)] border border-[var(--color-success)] bg-[var(--color-success-soft)] p-4 text-[var(--color-success)]">
            <Icon name="check" size={20} className="text-[var(--color-success)] flex-shrink-0" />
            <div>
              <p className="text-sm font-semibold">Fitness indicators are within safe ranges</p>
              <p className="text-xs text-[var(--color-success)]">
                No elevated heart rate, poor recovery or recurring incidents detected.
              </p>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {/*
              Alerts are based on the LAST 30 DAYS of data, not just the most recent
              session. One abnormal session stays here for up to 30 days, even
              after later sessions are back to normal — see the recorded date at
              the bottom corner of each card to know when it happened.
            */}
            {alerts.map((alert, idx) => {
              const isDanger = alert.severity === 'DANGER';
              return (
                <div
                  key={`${alert.ruleCode}-${idx}`}
                  className={`rounded-[var(--radius-md)] border p-4 shadow-[var(--shadow-panel)] ${
                    isDanger
                      ? 'border-[var(--color-danger)] bg-[var(--color-danger-soft)]'
                      : 'border-[var(--color-warning)] bg-[var(--color-warning-soft)]'
                  }`}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <Icon
                        name="alert-triangle"
                        size={18}
                        className={isDanger ? 'text-[var(--color-danger)]' : 'text-[var(--color-warning)]'}
                      />
                      <h4
                        className={`text-sm font-bold ${
                          isDanger ? 'text-[var(--color-danger)]' : 'text-[var(--color-warning)]'
                        }`}
                      >
                        {alert.title}
                      </h4>
                    </div>
                    <span
                      className={`rounded px-1.5 py-0.5 text-xs font-bold uppercase tracking-wider ${
                        isDanger
                          ? 'bg-[var(--color-danger-soft)] text-[var(--color-danger)]'
                          : 'bg-[var(--color-warning-soft)] text-[var(--color-warning)]'
                      }`}
                    >
                      {isDanger ? 'Critical' : 'Warning'}
                    </span>
                  </div>

                  <p
                    className={`mt-2 text-xs leading-relaxed ${
                      isDanger ? 'text-[var(--color-danger)]' : 'text-[var(--color-warning)]'
                    }`}
                  >
                    {alert.description}
                  </p>

                  <div className="mt-3 flex items-center justify-between border-t border-black/5 pt-2 text-xs text-[var(--color-text-secondary)]">
                    <span>
                      Value: <strong>{alert.metricValue}</strong> / Threshold:{' '}
                      <strong>{alert.thresholdValue}</strong>
                    </span>
                    {/*
                      Say "Recorded on" explicitly instead of leaving a bare timestamp.
                      Alerts scan the last 30 days, so they still show even when the latest
                      session is normal — without saying so, a user who fixes the
                      metric and still sees the light on will think the system is broken.
                    */}
                    <span>
                      Recorded on{' '}
                      <strong>
                        {formatDate(alert.triggeredAt)}
                      </strong>
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Date Filter Bar */}
      <Panel padded className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          <span className="text-xs font-medium text-[var(--color-text-secondary)]">
            Date range:
          </span>
          <SegmentedControl
            label="Date range"
            value={preset as DatePreset}
            onChange={handlePresetChange}
            options={[
              { value: '7D', label: '7 days' },
              { value: '30D', label: '30 days' },
              { value: '90D', label: '90 days' },
              { value: 'ALL', label: 'All time' },
            ]}
          />
        </div>

        <form onSubmit={handleCustomFilter} className="flex items-center gap-2">
          <Input
            className="min-h-8 w-auto"
            type="date"
            value={fromDate}
            onChange={(e) => setFromDate(e.target.value)}
            aria-label="Start date"
          />
          <span className="text-xs text-[var(--color-text-muted)]">-</span>
          <Input
            className="min-h-8 w-auto"
            type="date"
            value={toDate}
            onChange={(e) => setToDate(e.target.value)}
            aria-label="End date"
          />
          <Button type="submit" variant="secondary" size="sm">
            <Icon name="filter" size={12} />
            Apply
          </Button>
        </form>
      </Panel>

      {/* Error display */}
      {error && (
        <div className="rounded-[var(--radius-md)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-4 text-xs text-[var(--color-danger)]">
          {error}
        </div>
      )}

      {/* Main Charts Section */}
      <Panel padded>
        {loading ? (
          <div className="flex h-64 items-center justify-center">
            <div className="h-6 w-6 animate-spin rounded-full border-2 border-[var(--color-primary)] border-t-transparent" />
          </div>
        ) : (
          <FitnessTrendChart trend={trend} readiness={readiness} />
        )}
      </Panel>

      {/* Completed Workouts Data Table */}
      <Panel padded>
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-base font-semibold text-[var(--color-text-primary)]">
            Completed session history ({trend.length})
          </h3>
          <span className="text-xs text-[var(--color-text-muted)]">
            Data from completed training sessions
          </span>
        </div>

        {trend.length === 0 ? (
          <div className="py-8 text-center text-xs text-[var(--color-text-muted)]">
            No completed sessions were found in the selected date range.
          </div>
        ) : (
          <Table bare>
              <THead>
                <Tr className="border-b border-[var(--color-border)] text-[var(--color-text-secondary)]">
                  <Th className="py-2.5 px-3 font-semibold">Date</Th>
                  <Th className="py-2.5 px-3 font-semibold">Subject</Th>
                  <Th className="py-2.5 px-3 font-semibold text-right">Distance (m)</Th>
                  <Th className="py-2.5 px-3 font-semibold text-right">Speed (average / max)</Th>
                  <Th className="py-2.5 px-3 font-semibold text-right">Heart rate (average / max / recovery)</Th>
                  <Th className="py-2.5 px-3 font-semibold text-center">Performance</Th>
                </Tr>
              </THead>
              <TBody>
                {trend.map((row) => (
                  <Tr key={row.workoutId} className="hover:bg-[var(--color-surface-subtle)] transition-colors">
                    <Td className="py-2.5 px-3 font-medium whitespace-nowrap">
                      {row.date}
                    </Td>
                    <Td className="py-2.5 px-3 font-medium">
                      {row.subjectName}
                    </Td>
                    <Td className="py-2.5 px-3 text-right">
                      {row.distanceMeters ? `${row.distanceMeters.toLocaleString()} m` : '-'}
                    </Td>
                    <Td className="py-2.5 px-3 text-right whitespace-nowrap">
                      <span>{row.averageSpeedKmh != null ? `${row.averageSpeedKmh} km/h` : '-'}</span>
                      <span className="text-[var(--color-text-muted)]"> / </span>
                      <span className="font-semibold text-[var(--color-primary)]">
                        {row.topSpeedKmh != null ? `${row.topSpeedKmh} km/h` : '-'}
                      </span>
                    </Td>
                    <Td className="py-2.5 px-3 text-right whitespace-nowrap">
                      <span>{row.averageHeartRate != null ? `${row.averageHeartRate}` : '-'}</span>
                      <span className="text-[var(--color-text-muted)]"> / </span>
                      <span
                        className={`font-semibold ${
                          row.maxHeartRate && row.maxHeartRate > 220 ? 'text-[var(--color-danger)]' : 'text-[var(--color-danger)]'
                        }`}
                      >
                        {row.maxHeartRate != null ? `${row.maxHeartRate}` : '-'}
                      </span>
                      <span className="text-[var(--color-text-muted)]"> / </span>
                      <span
                        className={`font-semibold ${
                          row.recoveryHeartRate && row.recoveryHeartRate > 100
                            ? 'text-[var(--color-warning)]'
                            : 'text-[var(--color-success)]'
                        }`}
                      >
                        {row.recoveryHeartRate != null ? `${row.recoveryHeartRate}` : '-'}
                      </span>
                      <span className="text-xs text-[var(--color-text-muted)] ml-1">bpm</span>
                    </Td>
                    <Td className="py-2.5 px-3 text-center">
                      {row.performanceRating != null ? (
                        <span
                          className={`inline-block rounded-full px-2 py-0.5 text-xs font-bold ${
                            row.performanceRating >= 8
                              ? 'bg-[var(--color-success-soft)] text-[var(--color-success)]'
                              : row.performanceRating >= 6
                              ? 'bg-[var(--color-info-soft)] text-[var(--color-info)]'
                              : 'bg-[var(--color-warning-soft)] text-[var(--color-warning)]'
                          }`}
                        >
                          {row.performanceRating}/10
                        </span>
                      ) : (
                        <span className="text-[var(--color-text-muted)]">-</span>
                      )}
                    </Td>
                  </Tr>
                ))}
              </TBody>
            </Table>
        )}
      </Panel>
    </ScreenLayout>
  );
}
