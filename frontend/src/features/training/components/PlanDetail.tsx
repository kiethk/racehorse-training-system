'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { trainingApi } from '../services/trainingService';
import type { HorseTrainingPlanDetailResponse, PlanWorkoutItemResponse } from '../types';
import { CompleteWorkoutDialog } from './CompleteWorkoutDialog';

interface PlanDetailProps {
  planId: number;
}

export function PlanDetail({ planId }: PlanDetailProps) {
  const [detail, setDetail] = useState<HorseTrainingPlanDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [activeWorkout, setActiveWorkout] = useState<PlanWorkoutItemResponse | null>(null);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await trainingApi.getPlanById(planId);
      setDetail(data);
    } catch (err) {
      console.error('Lỗi khi nạp chi tiết kế hoạch:', err);
      setError('Không tải được thông tin kế hoạch huấn luyện.');
    } finally {
      setLoading(false);
    }
  }, [planId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadData();
  }, [loadData]);

  // Tính thanh tiến độ theo công thức: mẫu số trừ buổi đã huỷ (Plan 6)
  const progress = useMemo(() => {
    if (!detail?.workouts || detail.workouts.length === 0) {
      return { percent: 0, completed: 0, total: 0, cancelled: 0 };
    }
    const completed = detail.workouts.filter((w) => w.status === 'COMPLETED').length;
    const cancelled = detail.workouts.filter((w) => w.status === 'CANCELLED').length;
    const denominator = Math.max(detail.workouts.length - cancelled, 1);
    const percent = Math.min(Math.round((completed / denominator) * 100), 100);
    return { percent, completed, total: denominator, cancelled };
  }, [detail]);

  if (loading) return <ListSkeleton rows={5} />;

  if (error || !detail) {
    return (
      <Panel padded>
        <EmptyState icon="alert-triangle" title="Không tìm thấy kế hoạch" description={error || 'Kế hoạch không tồn tại.'} />
      </Panel>
    );
  }

  const { plan, workouts } = detail;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-[18px] font-semibold text-[var(--color-text-primary)]">
              Kế hoạch huấn luyện #{plan.id}
            </h1>
            <Pill
              tone={
                plan.status === 'COMPLETED'
                  ? 'success'
                  : plan.status === 'ACTIVE'
                  ? 'primary'
                  : plan.status === 'CANCELLED'
                  ? 'danger'
                  : 'info'
              }
            >
              {plan.status}
            </Pill>
          </div>
          <p className="text-[12px] text-[var(--color-text-secondary)] mt-0.5">
            Từ {plan.startDate} đến {plan.endDate} • {workouts.length} buổi tập dự kiến
          </p>
        </div>

        <div className="flex gap-2">
          <Link href="/trainer/courses">
            <Button variant="secondary" size="sm">
              ← Danh sách khóa học
            </Button>
          </Link>
          <Link href="/trainer/schedule">
            <Button variant="secondary" size="sm">
              Xem lịch Lot tổng thể
            </Button>
          </Link>
        </div>
      </div>

      {/* Thanh tiến độ */}
      <Panel padded>
        <div className="space-y-2">
          <div className="flex items-center justify-between text-[13px]">
            <span className="font-medium text-[var(--color-text-primary)]">
              Tiến độ hoàn thành: {progress.completed}/{progress.total} buổi ({progress.percent}%)
            </span>
            {progress.cancelled > 0 && (
              <span className="text-[11px] text-[var(--color-text-muted)]">
                (Đã huỷ {progress.cancelled} buổi)
              </span>
            )}
          </div>
          <div className="h-2.5 w-full rounded-full bg-[var(--color-surface-muted)] overflow-hidden">
            <div
              className="h-full rounded-full bg-[var(--color-primary)] transition-all duration-300"
              style={{ width: `${progress.percent}%` }}
            />
          </div>
        </div>
      </Panel>

      {/* Danh sách các buổi tập */}
      <Panel padded>
        <h2 className="text-[15px] font-semibold text-[var(--color-text-primary)] mb-4">
          Lịch trình chi tiết từng buổi tập
        </h2>

        {workouts.length === 0 ? (
          <p className="text-[12px] text-[var(--color-text-muted)] italic">
            Chưa có buổi tập nào được phân bổ trong kế hoạch này.
          </p>
        ) : (
          <div className="divide-y divide-[var(--color-border)]">
            {workouts.map((w, index) => (
              <div key={w.workoutId} className="py-3 flex flex-wrap items-center justify-between gap-3">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-[12px] font-bold text-[var(--color-primary)]">
                      #{index + 1}
                    </span>
                    <span className="text-[13px] font-semibold text-[var(--color-text-primary)]">
                      {w.subjectName}
                    </span>
                    <Pill
                      tone={
                        w.status === 'COMPLETED'
                          ? 'success'
                          : w.status === 'CANCELLED'
                          ? 'danger'
                          : 'info'
                      }
                      size="sm"
                    >
                      {w.status}
                    </Pill>
                  </div>
                  <div className="mt-1 text-[11px] text-[var(--color-text-secondary)] flex flex-wrap gap-3">
                    <span>📅 Ngày: <strong>{w.lotDate}</strong></span>
                    <span>⏱️ Khung giờ: <strong>{w.startTime} – {w.endTime}</strong></span>
                    <span>🏷️ Lot ID: #{w.lotId}</span>
                    {w.assignedGroomId && <span>👤 Groom: #{w.assignedGroomId}</span>}
                  </div>

                  {w.status === 'COMPLETED' && (
                    <div className="mt-2 rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] p-2 text-[11px] text-[var(--color-text-secondary)] space-y-0.5">
                      <div className="font-medium text-[var(--color-text-primary)]">
                        ⭐ Đánh giá: {w.performanceRating}/10
                        {w.topSpeedKmh && ` • Max: ${w.topSpeedKmh} km/h`}
                        {w.actualDistanceMeters && ` • Cự ly: ${w.actualDistanceMeters}m`}
                      </div>
                      {w.trainerFeedback && <div>Nhận xét: {w.trainerFeedback}</div>}
                    </div>
                  )}
                </div>

                {w.status === 'SCHEDULED' && (
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => setActiveWorkout(w)}
                  >
                    Nhập kết quả buổi tập
                  </Button>
                )}
              </div>
            ))}
          </div>
        )}
      </Panel>

      <CompleteWorkoutDialog
        workout={activeWorkout}
        open={activeWorkout !== null}
        onClose={() => setActiveWorkout(null)}
        onSuccess={loadData}
      />
    </div>
  );
}
