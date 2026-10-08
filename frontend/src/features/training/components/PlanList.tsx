'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/Button';
import { Panel, SectionTitle } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { trainingApi } from '../services/trainingService';
import type { PlanSummaryResponse } from '../types';

type StatusFilter = 'ALL' | 'UPCOMING' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

const STATUS_LABEL: Record<Exclude<StatusFilter, 'ALL'>, string> = {
  UPCOMING: 'Sắp bắt đầu',
  ACTIVE: 'Đang tập',
  COMPLETED: 'Đã hoàn thành',
  CANCELLED: 'Đã huỷ',
};

const STATUS_TONE: Record<Exclude<StatusFilter, 'ALL'>, 'info' | 'success' | 'neutral'> = {
  UPCOMING: 'info',
  ACTIVE: 'success',
  COMPLETED: 'success',
  CANCELLED: 'neutral',
};

export function PlanList() {
  const [plans, setPlans] = useState<PlanSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<StatusFilter>('ALL');

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      setPlans(await trainingApi.getPlans());
    } catch (err) {
      console.error('Lỗi khi nạp danh sách kế hoạch:', err);
      setError('Không tải được danh sách kế hoạch huấn luyện.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const filtered = useMemo(
    () => (filter === 'ALL' ? plans : plans.filter((p) => p.status === filter)),
    [plans, filter],
  );

  if (loading) return <ListSkeleton rows={6} />;

  if (error) {
    return (
      <Panel padded>
        <EmptyState icon="alert-triangle" title="Lỗi tải dữ liệu" description={error} />
      </Panel>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-[18px] font-semibold tracking-tight text-[var(--color-text-primary)]">
            Kế hoạch huấn luyện
          </h1>
          <p className="text-[12px] text-[var(--color-text-secondary)]">
            Theo dõi tiến độ tập luyện của từng chiến mã.
          </p>
        </div>
        <Link href="/trainer/plans/new">
          <Button variant="primary" size="sm">+ Ghi danh chiến mã</Button>
        </Link>
      </div>

      <Panel padded>
        <div className="flex flex-wrap items-center gap-2">
          <SectionTitle>Bộ lọc</SectionTitle>
          {(['ALL', 'UPCOMING', 'ACTIVE', 'COMPLETED', 'CANCELLED'] as StatusFilter[]).map((s) => (
            <button
              key={s}
              type="button"
              onClick={() => setFilter(s)}
              className={`rounded-full px-3 py-1 text-[11px] font-medium transition ${
                filter === s
                  ? 'bg-[var(--color-primary)] text-[var(--color-text-inverse)]'
                  : 'bg-[var(--color-surface-muted)] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface)]'
              }`}
            >
              {s === 'ALL' ? `Tất cả (${plans.length})` : STATUS_LABEL[s]}
            </button>
          ))}
        </div>
      </Panel>

      {filtered.length === 0 ? (
        <Panel padded>
          <EmptyState
            icon="clipboard"
            title="Chưa có kế hoạch nào"
            description="Ghi danh chiến mã vào một khoá huấn luyện để bắt đầu."
          />
        </Panel>
      ) : (
        <div className="space-y-2">
          {filtered.map((p) => {
            // Buổi đã huỷ bị trừ khỏi mẫu số — khoá 12 buổi huỷ 1 sẽ đạt
            // 100% ở 11 buổi thay vì treo mãi ở 91%.
            const denominator = Math.max(p.totalSessions - p.cancelledSessions, 1);
            const percent = Math.min(
              Math.round((p.completedSessions / denominator) * 100),
              100,
            );

            return (
              <Panel key={p.planId} padded>
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <strong className="text-[14px] text-[var(--color-text-primary)]">
                        {p.horseName}
                      </strong>
                      <Pill tone={STATUS_TONE[p.status]} size="sm">
                        {STATUS_LABEL[p.status]}
                      </Pill>
                    </div>
                    <div className="mt-0.5 text-[12px] text-[var(--color-text-secondary)]">
                      {p.courseName}
                    </div>
                    <div className="mt-0.5 text-[11px] text-[var(--color-text-muted)]">
                      {p.startDate} → {p.endDate}
                    </div>
                  </div>

                  <div className="flex items-center gap-4">
                    <div className="w-40">
                      <div className="flex justify-between text-[11px] text-[var(--color-text-secondary)]">
                        <span>
                          {p.completedSessions}/{denominator} buổi
                        </span>
                        <span>{percent}%</span>
                      </div>
                      <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-[var(--color-surface-muted)]">
                        <div
                          className="h-full rounded-full bg-[var(--color-primary)]"
                          style={{ width: `${percent}%` }}
                        />
                      </div>
                      {p.cancelledSessions > 0 && (
                        <div className="mt-0.5 text-[10px] text-[var(--color-text-muted)]">
                          Đã huỷ {p.cancelledSessions} buổi
                        </div>
                      )}
                    </div>

                    <Link href={`/trainer/plans/${p.planId}`}>
                      <Button variant="secondary" size="sm">Chi tiết</Button>
                    </Link>
                  </div>
                </div>
              </Panel>
            );
          })}
        </div>
      )}
    </div>
  );
}
