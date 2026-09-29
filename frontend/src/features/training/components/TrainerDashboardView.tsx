'use client';

import { useEffect, useState, useMemo } from 'react';
import Link from 'next/link';
import { trainingService } from '../services/trainingService';
import type { TrainerDashboardHorse } from '../types';
import { Panel } from '@/components/ui/Panel';
import { Icon } from '@/components/ui/Icon';
import { Button } from '@/components/ui/Button';

export function TrainerDashboardView() {
  const [horses, setHorses] = useState<TrainerDashboardHorse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [searchTerm, setSearchTerm] = useState('');
  const [filterMode, setFilterMode] = useState<'ALL' | 'ACTIVE' | 'ALERT'>('ALL');

  const loadDashboard = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await trainingService.getDashboard();
      setHorses(data);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Không thể tải dữ liệu dashboard huấn luyện';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadDashboard();
  }, []);

  // KPIs
  const totalHorses = horses.length;
  const activeCount = horses.filter((h) => h.planStatus === 'ACTIVE').length;
  const alertHorsesCount = horses.filter((h) => (h.alertCount ?? h.alertsCount ?? 0) > 0).length;

  const validRatings = horses
    .map((h) => h.avgPerformanceRating30d)
    .filter((r): r is number => r != null);
  const avgTeamPerformance =
    validRatings.length > 0
      ? (validRatings.reduce((sum, r) => sum + r, 0) / validRatings.length).toFixed(1)
      : null;

  // Distribution
  const performanceDist = useMemo(() => {
    let excellent = 0; // >= 8
    let good = 0; // >= 6 & < 8
    let needImprovement = 0; // < 6
    let noData = 0;

    horses.forEach((h) => {
      const rating = h.avgPerformanceRating30d ?? h.latestPerformanceRating;
      if (rating == null) {
        noData++;
      } else if (rating >= 8) {
        excellent++;
      } else if (rating >= 6) {
        good++;
      } else {
        needImprovement++;
      }
    });

    return { excellent, good, needImprovement, noData };
  }, [horses]);

  // Filtered horses
  const filteredHorses = useMemo(() => {
    return horses.filter((h) => {
      const matchSearch = h.horseName.toLowerCase().includes(searchTerm.toLowerCase());
      if (!matchSearch) return false;

      if (filterMode === 'ACTIVE') return h.planStatus === 'ACTIVE';
      if (filterMode === 'ALERT') return (h.alertCount ?? h.alertsCount ?? 0) > 0;
      return true;
    });
  }, [horses, searchTerm, filterMode]);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-[var(--color-text-primary)]">
            Tổng Quan Thể Lực & Tiến Độ Huấn Luyện
          </h1>
          <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
            Theo dõi tiến độ toàn bộ chiến mã trong khu vực huấn luyện và phát hiện sớm rủi ro quá tải
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Link href="/trainer/plans">
            <Button variant="secondary" size="sm">
              <Icon name="clipboard" size={14} />
              Quản lý kế hoạch
            </Button>
          </Link>
          <Button variant="secondary" size="sm" onClick={loadDashboard}>
            <Icon name="refresh" size={14} />
            Làm mới
          </Button>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <Panel padded className="space-y-1">
          <div className="flex items-center gap-1.5 text-[var(--color-text-muted)]">
            <Icon name="horse" size={14} />
            <span className="text-[11px] font-semibold uppercase tracking-wider">Tổng chiến mã</span>
          </div>
          <div className="flex items-baseline gap-1">
            <span className="font-metric text-2xl font-bold text-[var(--color-text-primary)]">
              {totalHorses}
            </span>
            <span className="text-xs text-[var(--color-text-secondary)]">con</span>
          </div>
          <p className="text-[11px] text-[var(--color-text-muted)]">Khu vực phụ trách</p>
        </Panel>

        <Panel padded className="space-y-1">
          <div className="flex items-center gap-1.5 text-blue-600">
            <Icon name="activity" size={14} />
            <span className="text-[11px] font-semibold uppercase tracking-wider">Đang huấn luyện</span>
          </div>
          <div className="flex items-baseline gap-1">
            <span className="font-metric text-2xl font-bold text-blue-600">
              {activeCount}
            </span>
            <span className="text-xs text-[var(--color-text-secondary)]">kế hoạch Active</span>
          </div>
          <p className="text-[11px] text-[var(--color-text-muted)]">Có lịch tập thường xuyên</p>
        </Panel>

        <Panel padded className="space-y-1">
          <div className="flex items-center gap-1.5 text-red-600">
            <Icon name="alert-triangle" size={14} />
            <span className="text-[11px] font-semibold uppercase tracking-wider">Cảnh báo rủi ro</span>
          </div>
          <div className="flex items-baseline gap-1">
            <span
              className={`font-metric text-2xl font-bold ${
                alertHorsesCount > 0 ? 'text-red-600' : 'text-emerald-600'
              }`}
            >
              {alertHorsesCount}
            </span>
            <span className="text-xs text-[var(--color-text-secondary)]">ngựa cần theo dõi</span>
          </div>
          {/*
            Chỉ liệt kê đúng những luật ĐANG chạy trong getHorseAlerts.
            Luật "phong độ tụt" cần 3 buổi giảm liên tiếp nên rất hiếm khi
            kích hoạt — ghi vào đây sẽ khiến người dùng tưởng cứ tụt điểm là
            có cảnh báo, rồi không thấy gì lại nghĩ hệ thống hỏng.
          */}
          <p className="text-[11px] text-[var(--color-text-muted)]">
            Nhịp tim vượt ngưỡng · hồi phục kém · sự cố lặp lại
          </p>
        </Panel>

        <Panel padded className="space-y-1">
          <div className="flex items-center gap-1.5 text-emerald-600">
            <Icon name="trending-up" size={14} />
            <span className="text-[11px] font-semibold uppercase tracking-wider">Phong độ TB (30d)</span>
          </div>
          <div className="flex items-baseline gap-1">
            <span className="font-metric text-2xl font-bold text-emerald-600">
              {avgTeamPerformance ? `${avgTeamPerformance}/10` : '-'}
            </span>
          </div>
          <p className="text-[11px] text-[var(--color-text-muted)]">Toàn bộ đàn chiến mã</p>
        </Panel>
      </div>

      {/* Performance Distribution Bar */}
      <Panel padded className="space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h3 className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-secondary)]">
            Phân Bổ Phong Độ Toàn Đội
          </h3>
          <div className="flex items-center gap-4 text-xs">
            <span className="flex items-center gap-1.5 text-[var(--color-text-secondary)]">
              <span className="h-2 w-2 rounded-full bg-emerald-500" />
              Xuất sắc (≥8.0): <strong>{performanceDist.excellent}</strong>
            </span>
            <span className="flex items-center gap-1.5 text-[var(--color-text-secondary)]">
              <span className="h-2 w-2 rounded-full bg-blue-500" />
              Đạt chuẩn (6.0 - 7.9): <strong>{performanceDist.good}</strong>
            </span>
            <span className="flex items-center gap-1.5 text-[var(--color-text-secondary)]">
              <span className="h-2 w-2 rounded-full bg-amber-500" />
              Cần cải thiện (&lt;6.0): <strong>{performanceDist.needImprovement}</strong>
            </span>
            <span className="flex items-center gap-1.5 text-[var(--color-text-muted)]">
              <span className="h-2 w-2 rounded-full bg-gray-300 dark:bg-gray-700" />
              Chưa có dữ liệu: <strong>{performanceDist.noData}</strong>
            </span>
          </div>
        </div>

        {/* Stacked distribution bar */}
        {totalHorses > 0 && (
          <div className="flex h-3 w-full overflow-hidden rounded-full bg-[var(--color-surface-subtle)]">
            {performanceDist.excellent > 0 && (
              <div
                style={{ width: `${(performanceDist.excellent / totalHorses) * 100}%` }}
                className="bg-emerald-500 transition-all"
                title={`Xuất sắc: ${performanceDist.excellent}`}
              />
            )}
            {performanceDist.good > 0 && (
              <div
                style={{ width: `${(performanceDist.good / totalHorses) * 100}%` }}
                className="bg-blue-500 transition-all"
                title={`Đạt chuẩn: ${performanceDist.good}`}
              />
            )}
            {performanceDist.needImprovement > 0 && (
              <div
                style={{ width: `${(performanceDist.needImprovement / totalHorses) * 100}%` }}
                className="bg-amber-500 transition-all"
                title={`Cần cải thiện: ${performanceDist.needImprovement}`}
              />
            )}
            {performanceDist.noData > 0 && (
              <div
                style={{ width: `${(performanceDist.noData / totalHorses) * 100}%` }}
                className="bg-gray-300 dark:bg-gray-700 transition-all"
                title={`Chưa có dữ liệu: ${performanceDist.noData}`}
              />
            )}
          </div>
        )}
      </Panel>

      {/* Table Section with Filter & Search */}
      <Panel padded className="space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setFilterMode('ALL')}
              className={`rounded px-3 py-1.5 text-xs font-medium transition-colors ${
                filterMode === 'ALL'
                  ? 'bg-[var(--color-primary)] text-white shadow-sm'
                  : 'bg-[var(--color-surface-subtle)] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-hover)]'
              }`}
            >
              Tất cả ({totalHorses})
            </button>
            <button
              type="button"
              onClick={() => setFilterMode('ACTIVE')}
              className={`rounded px-3 py-1.5 text-xs font-medium transition-colors ${
                filterMode === 'ACTIVE'
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'bg-[var(--color-surface-subtle)] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-hover)]'
              }`}
            >
              Đang huấn luyện ({activeCount})
            </button>
            <button
              type="button"
              onClick={() => setFilterMode('ALERT')}
              className={`rounded px-3 py-1.5 text-xs font-medium transition-colors ${
                filterMode === 'ALERT'
                  ? 'bg-red-600 text-white shadow-sm'
                  : 'bg-[var(--color-surface-subtle)] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-hover)]'
              }`}
            >
              Có cảnh báo ({alertHorsesCount})
            </button>
          </div>

          <div className="flex items-center gap-2">
            <div className="relative">
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Tìm tên chiến mã..."
                className="w-56 rounded border border-[var(--color-border)] bg-[var(--color-surface)] py-1 pl-7 pr-2.5 text-xs text-[var(--color-text-primary)] placeholder-[var(--color-text-muted)] focus:border-[var(--color-primary)] focus:outline-none"
              />
              <span className="pointer-events-none absolute left-2 top-1.5 text-[var(--color-text-muted)]">
                <Icon name="search" size={12} />
              </span>
            </div>
          </div>
        </div>

        {error && (
          <div className="rounded-[var(--radius-md)] border border-red-200 bg-red-50 p-3 text-xs text-red-700">
            {error}
          </div>
        )}

        {loading ? (
          <div className="flex h-48 items-center justify-center">
            <div className="h-6 w-6 animate-spin rounded-full border-2 border-[var(--color-primary)] border-t-transparent" />
          </div>
        ) : filteredHorses.length === 0 ? (
          <div className="py-12 text-center">
            <p className="text-sm font-medium text-[var(--color-text-secondary)]">
              Không tìm thấy chiến mã nào phù hợp với bộ lọc
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-[var(--color-border)] text-[var(--color-text-secondary)]">
                  <th className="py-3 px-3 font-semibold">Chiến mã</th>
                  <th className="py-3 px-3 font-semibold">Khóa huấn luyện</th>
                  <th className="py-3 px-3 font-semibold">Trạng thái</th>
                  <th className="py-3 px-3 font-semibold">Tiến độ khóa</th>
                  <th className="py-3 px-3 font-semibold text-center">Phong độ gần nhất</th>
                  <th className="py-3 px-3 font-semibold text-center">TB 30 ngày</th>
                  <th className="py-3 px-3 font-semibold text-center">Cảnh báo</th>
                  <th className="py-3 px-3 font-semibold text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--color-border-subtle)] text-[var(--color-text-primary)]">
                {filteredHorses.map((horse) => {
                  return (
                    <tr
                      key={horse.horseId}
                      className="hover:bg-[var(--color-surface-subtle)] transition-colors"
                    >
                      <td className="py-3 px-3 font-semibold whitespace-nowrap">
                        <Link
                          href={`/trainer/horses/${horse.horseId}`}
                          className="hover:text-[var(--color-primary)] transition-colors"
                        >
                          {horse.horseName}
                        </Link>
                      </td>
                      <td className="py-3 px-3 text-[var(--color-text-secondary)]">
                        {horse.courseName || (
                          <span className="italic text-[var(--color-text-muted)]">Chưa đăng ký</span>
                        )}
                      </td>
                      <td className="py-3 px-3 whitespace-nowrap">
                        {horse.planStatus === 'ACTIVE' && (
                          <span className="inline-flex items-center gap-1 rounded-full bg-blue-100 dark:bg-blue-900/40 text-blue-700 dark:text-blue-300 px-2 py-0.5 text-[10px] font-semibold">
                            <span className="h-1.5 w-1.5 rounded-full bg-blue-500" />
                            Đang tập
                          </span>
                        )}
                        {horse.planStatus === 'UPCOMING' && (
                          <span className="inline-flex items-center gap-1 rounded-full bg-amber-100 dark:bg-amber-900/40 text-amber-700 dark:text-amber-300 px-2 py-0.5 text-[10px] font-semibold">
                            <span className="h-1.5 w-1.5 rounded-full bg-amber-500" />
                            Sắp tới
                          </span>
                        )}
                        {horse.planStatus === 'COMPLETED' && (
                          <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 dark:bg-emerald-900/40 text-emerald-700 dark:text-emerald-300 px-2 py-0.5 text-[10px] font-semibold">
                            <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
                            Hoàn thành
                          </span>
                        )}
                        {(horse.planStatus === 'NO_PLAN' || !horse.planStatus) && (
                          <span className="inline-flex items-center gap-1 rounded-full bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 px-2 py-0.5 text-[10px] font-medium">
                            Chưa có plan
                          </span>
                        )}
                        {horse.planStatus === 'CANCELLED' && (
                          <span className="inline-flex items-center gap-1 rounded-full bg-red-100 dark:bg-red-900/40 text-red-700 dark:text-red-300 px-2 py-0.5 text-[10px] font-medium">
                            Đã huỷ
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-3 min-w-[140px]">
                        {horse.totalSessions > 0 ? (
                          <div className="space-y-1">
                            <div className="flex items-center justify-between text-[10px] text-[var(--color-text-secondary)]">
                              <span>
                                {horse.completedSessions}/{horse.totalSessions} buổi
                              </span>
                              <span className="font-semibold">{horse.progressPercent}%</span>
                            </div>
                            <div className="h-1.5 w-full overflow-hidden rounded-full bg-[var(--color-surface-subtle)]">
                              <div
                                style={{ width: `${Math.min(100, horse.progressPercent)}%` }}
                                className="h-full bg-blue-500 rounded-full transition-all"
                              />
                            </div>
                          </div>
                        ) : (
                          <span className="text-[var(--color-text-muted)]">-</span>
                        )}
                      </td>
                      <td className="py-3 px-3 text-center">
                        {horse.latestPerformanceRating != null ? (
                          <span
                            className={`inline-block rounded px-1.5 py-0.5 font-bold ${
                              horse.latestPerformanceRating >= 8
                                ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300'
                                : horse.latestPerformanceRating >= 6
                                ? 'bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300'
                                : 'bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300'
                            }`}
                          >
                            {horse.latestPerformanceRating}/10
                          </span>
                        ) : (
                          <span className="text-[var(--color-text-muted)]">-</span>
                        )}
                      </td>
                      <td className="py-3 px-3 text-center">
                        {horse.avgPerformanceRating30d != null ? (
                          <span className="font-semibold text-emerald-600">
                            {horse.avgPerformanceRating30d.toFixed(1)}
                          </span>
                        ) : (
                          <span className="text-[var(--color-text-muted)]">-</span>
                        )}
                      </td>
                      <td className="py-3 px-3 text-center">
                        {(horse.alertCount ?? horse.alertsCount ?? 0) > 0 ? (
                          <span className="inline-flex items-center gap-1 rounded-full bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300 px-2 py-0.5 text-[10px] font-bold">
                            <Icon name="alert-triangle" size={10} />
                            {horse.alertCount ?? horse.alertsCount} cảnh báo
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 text-emerald-600 text-[10px] font-medium">
                            <Icon name="check" size={12} />
                            An toàn
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-3 text-right whitespace-nowrap">
                        <Link href={`/trainer/horses/${horse.horseId}`}>
                          <Button variant="secondary" size="sm">
                            <Icon name="trending-up" size={12} />
                            Xem thể lực
                          </Button>
                        </Link>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Panel>
    </div>
  );
}
