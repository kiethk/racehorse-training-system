'use client';

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
              <span className="rounded-full bg-blue-100 dark:bg-blue-900/30 px-2.5 py-0.5 text-xs font-medium text-blue-700 dark:text-blue-300">
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
            <p className="font-metric text-xl font-bold text-emerald-600">
              {avgRating ? `${avgRating}/10` : '-'}
            </p>
          </div>
          <div className="text-right">
            <span className="text-xs uppercase text-[var(--color-text-muted)]">Max HR</span>
            <p className={`font-metric text-xl font-bold ${maxRecordedHR && maxRecordedHR > 220 ? 'text-red-600' : 'text-rose-500'}`}>
              {maxRecordedHR ? `${maxRecordedHR} bpm` : '-'}
            </p>
          </div>
          <div className="text-right">
            <span className="text-xs uppercase text-[var(--color-text-muted)]">Top Speed</span>
            <p className="font-metric text-xl font-bold text-indigo-600">
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
            <span className="rounded-full bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400 px-2 py-0.5 text-xs font-semibold">
              {alerts.length} active alerts
            </span>
          )}
        </div>

        {alerts.length === 0 ? (
          <div className="flex items-center gap-3 rounded-[var(--radius-md)] border border-emerald-200 bg-emerald-50 dark:border-emerald-900/40 dark:bg-emerald-950/20 p-4 text-emerald-800 dark:text-emerald-300">
            <Icon name="check" size={20} className="text-emerald-600 dark:text-emerald-400 flex-shrink-0" />
            <div>
              <p className="text-sm font-semibold">Fitness indicators are within safe ranges</p>
              <p className="text-xs text-emerald-700 dark:text-emerald-400">
                No elevated heart rate, poor recovery or recurring incidents detected.
              </p>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {/*
              Cảnh báo dựa trên dữ liệu 30 NGÀY GẦN NHẤT, không chỉ buổi mới
              nhất. Một buổi bất thường sẽ còn hiện ở đây tới 30 ngày, kể cả
              khi các buổi sau đã trở lại bình thường — xem ngày ghi nhận ở
              góc dưới mỗi thẻ để biết chuyện xảy ra lúc nào.
            */}
            {alerts.map((alert, idx) => {
              const isDanger = alert.severity === 'DANGER';
              return (
                <div
                  key={`${alert.ruleCode}-${idx}`}
                  className={`rounded-[var(--radius-md)] border p-4 shadow-sm ${
                    isDanger
                      ? 'border-red-300 bg-red-50/80 dark:border-red-900 dark:bg-red-950/30'
                      : 'border-amber-300 bg-amber-50/80 dark:border-amber-900 dark:bg-amber-950/30'
                  }`}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <Icon
                        name="alert-triangle"
                        size={18}
                        className={isDanger ? 'text-red-600 dark:text-red-400' : 'text-amber-600 dark:text-amber-400'}
                      />
                      <h4
                        className={`text-sm font-bold ${
                          isDanger ? 'text-red-900 dark:text-red-200' : 'text-amber-900 dark:text-amber-200'
                        }`}
                      >
                        {alert.title}
                      </h4>
                    </div>
                    <span
                      className={`rounded px-1.5 py-0.5 text-[10px] font-bold uppercase tracking-wider ${
                        isDanger
                          ? 'bg-red-200 text-red-900 dark:bg-red-900/60 dark:text-red-200'
                          : 'bg-amber-200 text-amber-900 dark:bg-amber-900/60 dark:text-amber-200'
                      }`}
                    >
                      {isDanger ? 'Critical' : 'Warning'}
                    </span>
                  </div>

                  <p
                    className={`mt-2 text-xs leading-relaxed ${
                      isDanger ? 'text-red-800 dark:text-red-300' : 'text-amber-800 dark:text-amber-300'
                    }`}
                  >
                    {alert.description}
                  </p>

                  <div className="mt-3 flex items-center justify-between border-t border-black/5 dark:border-white/5 pt-2 text-[11px] text-[var(--color-text-secondary)]">
                    <span>
                      Value: <strong>{alert.metricValue}</strong> / Threshold:{' '}
                      <strong>{alert.thresholdValue}</strong>
                    </span>
                    {/*
                      Ghi rõ "Ghi nhận ngày" chứ không để trơ một mốc thời gian.
                      Cảnh báo quét 30 ngày gần nhất nên vẫn hiện dù buổi tập
                      mới nhất đã bình thường — không nói rõ thì người dùng sửa
                      chỉ số xong thấy đèn chưa tắt lại tưởng hệ thống hỏng.
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
          {(['7D', '30D', '90D', 'ALL'] as DatePreset[]).map((p) => (
            <button
              key={p}
              type="button"
              onClick={() => handlePresetChange(p)}
              className={`rounded px-3 py-1 text-xs font-medium transition-colors ${
                preset === p
                  ? 'bg-[var(--color-primary)] text-white shadow-sm'
                  : 'bg-[var(--color-surface-subtle)] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-hover)]'
              }`}
            >
              {p === '7D'
                ? '7 days'
                : p === '30D'
                ? '30 days'
                : p === '90D'
                ? '90 days'
                : 'All time'}
            </button>
          ))}
        </div>

        <form onSubmit={handleCustomFilter} className="flex items-center gap-2">
          <input
            type="date"
            value={fromDate}
            onChange={(e) => setFromDate(e.target.value)}
            className="rounded border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-1 text-xs text-[var(--color-text-primary)] focus:border-[var(--color-primary)] focus:outline-none"
            aria-label="Start date"
          />
          <span className="text-xs text-[var(--color-text-muted)]">-</span>
          <input
            type="date"
            value={toDate}
            onChange={(e) => setToDate(e.target.value)}
            className="rounded border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-1 text-xs text-[var(--color-text-primary)] focus:border-[var(--color-primary)] focus:outline-none"
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
        <div className="rounded-[var(--radius-md)] border border-red-200 bg-red-50 p-4 text-xs text-red-700">
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
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-[var(--color-border)] text-[var(--color-text-secondary)]">
                  <th className="py-2.5 px-3 font-semibold">Date</th>
                  <th className="py-2.5 px-3 font-semibold">Subject</th>
                  <th className="py-2.5 px-3 font-semibold text-right">Distance (m)</th>
                  <th className="py-2.5 px-3 font-semibold text-right">Speed (average / max)</th>
                  <th className="py-2.5 px-3 font-semibold text-right">Heart rate (average / max / recovery)</th>
                  <th className="py-2.5 px-3 font-semibold text-center">Performance</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--color-border-subtle)] text-[var(--color-text-primary)]">
                {trend.map((row) => (
                  <tr key={row.workoutId} className="hover:bg-[var(--color-surface-subtle)] transition-colors">
                    <td className="py-2.5 px-3 font-medium whitespace-nowrap">
                      {row.date}
                    </td>
                    <td className="py-2.5 px-3 font-medium">
                      {row.subjectName}
                    </td>
                    <td className="py-2.5 px-3 text-right">
                      {row.distanceMeters ? `${row.distanceMeters.toLocaleString()} m` : '-'}
                    </td>
                    <td className="py-2.5 px-3 text-right whitespace-nowrap">
                      <span>{row.averageSpeedKmh != null ? `${row.averageSpeedKmh} km/h` : '-'}</span>
                      <span className="text-[var(--color-text-muted)]"> / </span>
                      <span className="font-semibold text-indigo-600">
                        {row.topSpeedKmh != null ? `${row.topSpeedKmh} km/h` : '-'}
                      </span>
                    </td>
                    <td className="py-2.5 px-3 text-right whitespace-nowrap">
                      <span>{row.averageHeartRate != null ? `${row.averageHeartRate}` : '-'}</span>
                      <span className="text-[var(--color-text-muted)]"> / </span>
                      <span
                        className={`font-semibold ${
                          row.maxHeartRate && row.maxHeartRate > 220 ? 'text-red-600' : 'text-rose-600'
                        }`}
                      >
                        {row.maxHeartRate != null ? `${row.maxHeartRate}` : '-'}
                      </span>
                      <span className="text-[var(--color-text-muted)]"> / </span>
                      <span
                        className={`font-semibold ${
                          row.recoveryHeartRate && row.recoveryHeartRate > 100
                            ? 'text-amber-600'
                            : 'text-emerald-600'
                        }`}
                      >
                        {row.recoveryHeartRate != null ? `${row.recoveryHeartRate}` : '-'}
                      </span>
                      <span className="text-[10px] text-[var(--color-text-muted)] ml-1">bpm</span>
                    </td>
                    <td className="py-2.5 px-3 text-center">
                      {row.performanceRating != null ? (
                        <span
                          className={`inline-block rounded-full px-2 py-0.5 text-[11px] font-bold ${
                            row.performanceRating >= 8
                              ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300'
                              : row.performanceRating >= 6
                              ? 'bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300'
                              : 'bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300'
                          }`}
                        >
                          {row.performanceRating}/10
                        </span>
                      ) : (
                        <span className="text-[var(--color-text-muted)]">-</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Panel>
    </ScreenLayout>
  );
}
