'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { groomApi } from '../services/groomService';
import type { TodayTaskItem } from '../types';

export function TodayChecklist() {
  const [tasks, setTasks] = useState<TodayTaskItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [completingId, setCompletingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const loadTasks = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await groomApi.getTodayTasks();
      setTasks(data);
    } catch (err) {
      console.error('Lỗi khi nạp công việc hôm nay:', err);
      setError('Không tải được danh sách công việc. Vui lòng thử lại sau.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadTasks();
  }, [loadTasks]);

  async function handleGenerateRoutine() {
    setGenerating(true);
    try {
      await groomApi.generateRoutine();
      await loadTasks();
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Sinh việc thường nhật thất bại.');
    } finally {
      setGenerating(false);
    }
  }

  async function handleComplete(refId: number) {
    setCompletingId(refId);
    try {
      await groomApi.completeTask(refId);
      await loadTasks();
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Hoàn thành công việc thất bại.');
    } finally {
      setCompletingId(null);
    }
  }

  // Sắp xếp theo dòng thời gian trong ngày (Plan 6)
  const sortedTasks = useMemo(() => {
    return [...tasks].sort((a, b) => a.startTime.localeCompare(b.startTime));
  }, [tasks]);

  const stats = useMemo(() => {
    const total = tasks.length;
    const completed = tasks.filter((t) => t.status === 'COMPLETED').length;
    return { total, completed };
  }, [tasks]);

  if (loading) return <ListSkeleton rows={5} />;

  if (error) {
    return (
      <Panel padded>
        <EmptyState icon="alert-triangle" title="Lỗi nạp dữ liệu" description={error} />
      </Panel>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-[18px] font-semibold text-[var(--color-text-primary)]">
            Bảng công việc hôm nay (Today Checklist)
          </h1>
          <p className="text-[12px] text-[var(--color-text-secondary)]">
            Tổng hợp lịch chăm sóc thường nhật (SOP), buổi tập và theo dõi thú y cho các chuồng bạn phụ trách.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="secondary"
            size="sm"
            onClick={handleGenerateRoutine}
            disabled={generating}
          >
            {generating ? 'Đang sinh việc...' : '⚡ Sinh việc thường nhật (SOP)'}
          </Button>
          <Button variant="secondary" size="sm" onClick={() => loadTasks()}>
            Làm mới
          </Button>
        </div>
      </div>

      {/* Thống kê tiến độ trong ngày */}
      <Panel padded>
        <div className="flex items-center justify-between text-[13px]">
          <span className="font-semibold text-[var(--color-text-primary)]">
            Tiến độ công việc hôm nay: {stats.completed}/{stats.total} mục hoàn tất
          </span>
          <span className="text-[12px] text-[var(--color-text-muted)]">
            {stats.total > 0 ? Math.round((stats.completed / stats.total) * 100) : 0}%
          </span>
        </div>
        <div className="mt-2 h-2 w-full rounded-full bg-[var(--color-surface-muted)] overflow-hidden">
          <div
            className="h-full rounded-full bg-[var(--color-primary)] transition-all duration-300"
            style={{
              width: `${stats.total > 0 ? Math.round((stats.completed / stats.total) * 100) : 0}%`,
            }}
          />
        </div>
      </Panel>

      {/* Danh sách công việc theo dòng thời gian */}
      {sortedTasks.length === 0 ? (
        <Panel padded>
          <EmptyState
            icon="clipboard"
            title="Chưa có công việc nào hôm nay"
            description="Hãy bấm 'Sinh việc thường nhật (SOP)' ở góc trên để tạo lịch cho ăn, dọn chuồng và tắm chải."
          />
        </Panel>
      ) : (
        <div className="space-y-2">
          {sortedTasks.map((task, idx) => {
            const isCompleted = task.status === 'COMPLETED';
            return (
              <div
                key={`${task.source}-${task.refId}-${idx}`}
                className={`flex flex-wrap items-center justify-between gap-3 rounded-[var(--radius-md)] border p-3 text-[13px] transition ${
                  isCompleted
                    ? 'border-[var(--color-border)] bg-[var(--color-surface-muted)] opacity-75'
                    : 'border-[var(--color-border-strong)] bg-[var(--color-surface)] shadow-sm'
                }`}
              >
                <div className="flex items-start gap-3">
                  {/* Thời gian */}
                  <div className="font-mono text-[12px] font-bold text-[var(--color-text-primary)] w-16 pt-0.5">
                    {task.startTime.substring(0, 5)}
                    {task.endTime && `–${task.endTime.substring(0, 5)}`}
                  </div>

                  {/* Chi tiết việc */}
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-[var(--color-text-primary)]">
                        {task.title}
                      </span>
                      <Pill
                        tone={
                          task.source === 'SOP'
                            ? 'primary'
                            : task.source === 'WORKOUT'
                            ? 'warning'
                            : 'info'
                        }
                        size="sm"
                      >
                        {task.source}
                      </Pill>
                    </div>

                    <div className="mt-1 flex flex-wrap items-center gap-2 text-[12px] text-[var(--color-text-secondary)]">
                      <span>🏇 Chiến mã: <strong>{task.horseName}</strong></span>
                      {task.note && <span>• {task.note}</span>}
                    </div>
                  </div>
                </div>

                {/* Hành động dựa trên actionable (Plan 6) */}
                <div className="flex items-center gap-2">
                  {task.actionable && !isCompleted ? (
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => handleComplete(task.refId)}
                      disabled={completingId === task.refId}
                    >
                      {completingId === task.refId ? 'Đang lưu...' : '✓ Hoàn thành'}
                    </Button>
                  ) : isCompleted ? (
                    <Pill tone="success" size="sm">
                      ✓ Đã hoàn thành
                    </Pill>
                  ) : task.source === 'WORKOUT' ? (
                    <span className="text-[11px] text-[var(--color-text-muted)] italic">
                      (Trainer đóng)
                    </span>
                  ) : (
                    <span className="text-[11px] text-[var(--color-text-muted)] italic">
                      (Thú y phụ trách)
                    </span>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
